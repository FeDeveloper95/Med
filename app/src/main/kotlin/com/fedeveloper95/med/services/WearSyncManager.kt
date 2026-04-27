package com.fedeveloper95.med.services

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

@SuppressLint("StaticFieldLeak")
object WearSyncManager : DataClient.OnDataChangedListener {
    private var dataClient: DataClient? = null
    private var appContext: Context? = null

    fun initialize(context: Context) {
        if (dataClient == null) {
            appContext = context.applicationContext
            dataClient = Wearable.getDataClient(appContext!!)
            dataClient?.addListener(this)
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        if (appContext == null) return
        val prefs = appContext!!.getSharedPreferences("med_settings", Context.MODE_PRIVATE)

        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val path = event.dataItem.uri.path
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap

                if (path == "/settings") {
                    val alarms = dataMap.getBoolean("alarms_enabled", true)
                    val events = dataMap.getStringArray("enabled_events") ?: emptyArray()

                    prefs.edit()
                        .putBoolean("pref_wear_ring_alarms", alarms)
                        .putStringSet("pref_wear_enabled_events", events.toSet())
                        .apply()
                }
            }
        }
    }

    @SuppressLint("VisibleForTests")
    fun syncData(medicines: List<String>, events: List<String>) {
        val request = PutDataMapRequest.create("/med_data").apply {
            dataMap.putStringArray("medicines", medicines.toTypedArray())
            dataMap.putStringArray("events", events.toTypedArray())
            dataMap.putLong("timestamp", System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()
        dataClient?.putDataItem(request)
    }

    @SuppressLint("VisibleForTests")
    fun syncSettings(alarmsEnabled: Boolean, enabledEvents: Set<String>, availableEvents: Set<String>) {
        val request = PutDataMapRequest.create("/settings").apply {
            dataMap.putBoolean("alarms_enabled", alarmsEnabled)
            dataMap.putStringArray("enabled_events", enabledEvents.toTypedArray())
            dataMap.putStringArray("available_events", availableEvents.toTypedArray())
            dataMap.putLong("timestamp", System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()
        dataClient?.putDataItem(request)
    }
}
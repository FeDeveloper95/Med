package com.fedeveloper95.med.services

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

@SuppressLint("StaticFieldLeak")
object WearDataManager : DataClient.OnDataChangedListener {

    val enabledEvents = mutableStateListOf<String>()
    val availableEvents = mutableStateListOf<String>()
    val medicines = mutableStateListOf<String>()
    val eventsList = mutableStateListOf<String>()
    val itemStates = mutableStateMapOf<String, Boolean>()

    var alarmsEnabled by mutableStateOf(true)

    private var dataClient: DataClient? = null

    fun initialize(context: Context) {
        if (dataClient == null) {
            dataClient = Wearable.getDataClient(context.applicationContext)
            dataClient?.addListener(this)
            fetchInitialData()
            requestSyncFromPhone()
        }
    }

    @SuppressLint("VisibleForTests")
    fun fetchInitialData() {
        dataClient?.dataItems?.addOnSuccessListener { items ->
            for (item in items) {
                val dataMap = DataMapItem.fromDataItem(item).dataMap
                when (item.uri.path) {
                    "/settings" -> {
                        alarmsEnabled = dataMap.getBoolean("alarms_enabled", true)

                        val avail = dataMap.getStringArray("available_events") ?: emptyArray()
                        availableEvents.clear()
                        val validAvailable = avail.filter { it.isNotBlank() }
                        availableEvents.addAll(validAvailable)

                        val events = dataMap.getStringArray("enabled_events") ?: emptyArray()
                        enabledEvents.clear()
                        enabledEvents.addAll(events.filter { it.isNotBlank() && validAvailable.contains(it) })
                    }
                    "/med_data" -> {
                        val meds = dataMap.getStringArray("medicines") ?: emptyArray()
                        medicines.clear()
                        meds.forEach { med ->
                            val isTaken = med.contains("✔") || med.contains("✓") || med.contains("✅")
                            val clean = med.replace("✔", "").replace("✓", "").replace("✅", "").trim()
                            if (clean.isNotBlank()) {
                                itemStates[clean] = isTaken
                                medicines.add(clean)
                            }
                        }

                        val evs = dataMap.getStringArray("events") ?: emptyArray()
                        eventsList.clear()
                        evs.forEach { ev ->
                            val isTaken = ev.contains("✔") || ev.contains("✓") || ev.contains("✅")
                            val clean = ev.replace("✔", "").replace("✓", "").replace("✅", "").trim()
                            if (clean.isNotBlank()) {
                                itemStates[clean] = isTaken
                                eventsList.add(clean)
                            }
                        }
                    }
                }
            }
        }
    }

    fun requestSyncFromPhone() {
        val request = PutDataMapRequest.create("/request_sync").apply {
            dataMap.putLong("timestamp", System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()
        dataClient?.putDataItem(request)
    }

    fun syncSettings() {
        val request = PutDataMapRequest.create("/settings").apply {
            dataMap.putBoolean("alarms_enabled", alarmsEnabled)
            dataMap.putStringArray("enabled_events", enabledEvents.toTypedArray())
            dataMap.putStringArray("available_events", availableEvents.toTypedArray())
            dataMap.putLong("timestamp", System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()
        dataClient?.putDataItem(request)
    }

    fun sendEventToPhone(eventName: String) {
        val request = PutDataMapRequest.create("/new_event").apply {
            dataMap.putString("event_name", eventName)
            dataMap.putLong("timestamp", System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()
        dataClient?.putDataItem(request)
    }

    fun openUrlOnPhone(url: String) {
        val request = PutDataMapRequest.create("/open_url").apply {
            dataMap.putString("url", url)
            dataMap.putLong("timestamp", System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()
        dataClient?.putDataItem(request)
    }

    fun setItemTaken(itemName: String, isTaken: Boolean, type: String) {
        itemStates[itemName] = isTaken

        val request = PutDataMapRequest.create("/item_taken").apply {
            dataMap.putString("item_name", itemName)
            dataMap.putBoolean("is_taken", isTaken)
            dataMap.putString("item_type", type)
            dataMap.putLong("timestamp", System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()

        dataClient?.putDataItem(request)
    }

    fun deleteItem(itemName: String, type: String) {
        if (type == "medicine") {
            medicines.remove(itemName)
        } else {
            eventsList.remove(itemName)
        }
        itemStates.remove(itemName)

        val request = PutDataMapRequest.create("/delete_item").apply {
            dataMap.putString("item_name", itemName)
            dataMap.putString("item_type", type)
            dataMap.putLong("timestamp", System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()

        dataClient?.putDataItem(request)
    }

    @SuppressLint("VisibleForTests")
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val path = event.dataItem.uri.path
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                when (path) {
                    "/settings" -> {
                        alarmsEnabled = dataMap.getBoolean("alarms_enabled", true)

                        val avail = dataMap.getStringArray("available_events") ?: emptyArray()
                        availableEvents.clear()
                        val validAvailable = avail.filter { it.isNotBlank() }
                        availableEvents.addAll(validAvailable)

                        val events = dataMap.getStringArray("enabled_events") ?: emptyArray()
                        enabledEvents.clear()
                        enabledEvents.addAll(events.filter { it.isNotBlank() && validAvailable.contains(it) })
                    }
                    "/med_data" -> {
                        val meds = dataMap.getStringArray("medicines") ?: emptyArray()
                        medicines.clear()
                        meds.forEach { med ->
                            val isTaken = med.contains("✔") || med.contains("✓") || med.contains("✅")
                            val clean = med.replace("✔", "").replace("✓", "").replace("✅", "").trim()
                            if (clean.isNotBlank()) {
                                itemStates[clean] = isTaken
                                medicines.add(clean)
                            }
                        }

                        val evs = dataMap.getStringArray("events") ?: emptyArray()
                        eventsList.clear()
                        evs.forEach { ev ->
                            val isTaken = ev.contains("✔") || ev.contains("✓") || ev.contains("✅")
                            val clean = ev.replace("✔", "").replace("✓", "").replace("✅", "").trim()
                            if (clean.isNotBlank()) {
                                itemStates[clean] = isTaken
                                eventsList.add(clean)
                            }
                        }
                    }
                }
            }
        }
    }
}
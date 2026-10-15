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

data class WearItem(
    val title: String,
    val time: String,
    val isTaken: Boolean,
    val iconName: String,
    val colorCode: String
)

@SuppressLint("StaticFieldLeak")
object WearDataManager : DataClient.OnDataChangedListener {
    val enabledEvents = mutableStateListOf<String>()
    val availableEvents = mutableStateListOf<String>()
    val presetDetails = mutableStateListOf<String>()
    val medicines = mutableStateListOf<WearItem>()
    val eventsList = mutableStateListOf<WearItem>()
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
                        enabledEvents.addAll(events.filter {
                            it.isNotBlank() && validAvailable.contains(it)
                        })

                        val details = dataMap.getStringArray("preset_details") ?: emptyArray()
                        presetDetails.clear()
                        presetDetails.addAll(details)
                    }
                    "/med_data" -> {
                        val medsMaps = dataMap.getDataMapArrayList("medicines") ?: arrayListOf()
                        medicines.clear()
                        medsMaps.forEach { map ->
                            val title = map.getString("title", "")
                            val time = map.getString("time", "")
                            val isTaken = map.getBoolean("isTaken", false)
                            val iconName = map.getString("iconName", "")
                            val colorCode = map.getString("colorCode", "")

                            if (title.isNotBlank()) {
                                itemStates[title] = isTaken
                                medicines.add(WearItem(title, time, isTaken, iconName, colorCode))
                            }
                        }

                        val evsMaps = dataMap.getDataMapArrayList("events") ?: arrayListOf()
                        eventsList.clear()
                        evsMaps.forEach { map ->
                            val title = map.getString("title", "")
                            val time = map.getString("time", "")
                            val isTaken = map.getBoolean("isTaken", false)
                            val iconName = map.getString("iconName", "")
                            val colorCode = map.getString("colorCode", "")

                            if (title.isNotBlank()) {
                                itemStates[title] = isTaken
                                eventsList.add(WearItem(title, time, isTaken, iconName, colorCode))
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

    fun sendActionToPhone(path: String, itemIds: LongArray, notifId: Int) {
        val request = PutDataMapRequest.create(path).apply {
            dataMap.putLongArray("item_ids", itemIds)
            dataMap.putInt("notif_id", notifId)
            dataMap.putLong("timestamp", System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()
        dataClient?.putDataItem(request)
    }

    fun syncSettings() {
        val request = PutDataMapRequest.create("/settings").apply {
            dataMap.putBoolean("alarms_enabled", alarmsEnabled)
            dataMap.putStringArray("enabled_events", enabledEvents.toTypedArray())
            dataMap.putStringArray("available_events", availableEvents.toTypedArray())
            dataMap.putStringArray("preset_details", presetDetails.toTypedArray())
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

        if (type == "medicine") {
            val idx = medicines.indexOfFirst { it.title == itemName }
            if (idx != -1) medicines[idx] = medicines[idx].copy(isTaken = isTaken)
        } else {
            val idx = eventsList.indexOfFirst { it.title == itemName }
            if (idx != -1) eventsList[idx] = eventsList[idx].copy(isTaken = isTaken)
        }

        val request = PutDataMapRequest.create("/item_taken").apply {
            dataMap.putString("item_name", itemName)
            dataMap.putBoolean("is_taken", isTaken)
            dataMap.putString("item_type", type)
            dataMap.putLong("timestamp", System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()
        dataClient?.putDataItem(request)
    }

    fun deleteItem(itemName: String, type: String) {
        itemStates.remove(itemName)

        if (type == "medicine") {
            medicines.removeAll { it.title == itemName }
        } else {
            eventsList.removeAll { it.title == itemName }
        }

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
                        enabledEvents.addAll(events.filter {
                            it.isNotBlank() && validAvailable.contains(it)
                        })

                        val details = dataMap.getStringArray("preset_details") ?: emptyArray()
                        presetDetails.clear()
                        presetDetails.addAll(details)
                    }
                    "/med_data" -> {
                        val medsMaps = dataMap.getDataMapArrayList("medicines") ?: arrayListOf()
                        medicines.clear()
                        medsMaps.forEach { map ->
                            val title = map.getString("title", "")
                            val time = map.getString("time", "")
                            val isTaken = map.getBoolean("isTaken", false)
                            val iconName = map.getString("iconName", "")
                            val colorCode = map.getString("colorCode", "")

                            if (title.isNotBlank()) {
                                itemStates[title] = isTaken
                                medicines.add(WearItem(title, time, isTaken, iconName, colorCode))
                            }
                        }

                        val evsMaps = dataMap.getDataMapArrayList("events") ?: arrayListOf()
                        eventsList.clear()
                        evsMaps.forEach { map ->
                            val title = map.getString("title", "")
                            val time = map.getString("time", "")
                            val isTaken = map.getBoolean("isTaken", false)
                            val iconName = map.getString("iconName", "")
                            val colorCode = map.getString("colorCode", "")

                            if (title.isNotBlank()) {
                                itemStates[title] = isTaken
                                eventsList.add(WearItem(title, time, isTaken, iconName, colorCode))
                            }
                        }
                    }
                }
            }
        }
    }
}
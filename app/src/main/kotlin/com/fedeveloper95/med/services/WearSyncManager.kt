package com.fedeveloper95.med.services

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import com.fedeveloper95.med.ItemType
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

@SuppressLint("StaticFieldLeak")
object WearSyncManager {
    private var dataClient: DataClient? = null
    private var appContext: Context? = null

    fun initialize(context: Context) {
        if (dataClient == null) {
            appContext = context.applicationContext
            dataClient = Wearable.getDataClient(appContext!!)
        }
    }

    @SuppressLint("VisibleForTests")
    fun processPendingWearActions(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = Wearable.getDataClient(context.applicationContext)
                val dataItems = com.google.android.gms.tasks.Tasks.await(client.dataItems)
                var hasUpdates = false

                for (item in dataItems) {
                    val path = item.uri.path
                    if (path == "/item_taken" || path == "/delete_item" || path == "/new_event") {
                        val dataMap = DataMapItem.fromDataItem(item).dataMap
                        val updated = applyWearActionToStorage(context, path, dataMap)
                        if (updated) hasUpdates = true
                        client.deleteDataItems(item.uri)
                    }
                }

                if (hasUpdates) {
                    context.sendBroadcast(Intent("com.fedeveloper95.med.RELOAD_DATA").setPackage(context.packageName))
                    context.sendBroadcast(Intent("com.fedeveloper95.med.REFRESH_DATA").setPackage(context.packageName))
                    val updateIntent = Intent(context, AppWearListenerService::class.java).apply {
                        action = "FORCE_SYNC"
                    }
                    context.startService(updateIntent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun applyWearActionToStorage(context: Context, path: String, dataMap: DataMap): Boolean {
        val items = DataRepository.loadData(context).toMutableList()
        val today = LocalDate.now()
        var updated = false

        when (path) {
            "/item_taken" -> {
                val itemName = dataMap.getString("item_name") ?: return false
                val isTaken = dataMap.getBoolean("is_taken")
                val cleanItemName = if (itemName.contains("-")) itemName.substringAfter("-").trim() else itemName.trim()

                for (i in items.indices) {
                    val item = items[i]
                    val itemTitle = item.title.trim()
                    if (itemTitle.equals(cleanItemName, ignoreCase = true) || itemName.contains(itemTitle, ignoreCase = true) || cleanItemName.contains(itemTitle, ignoreCase = true)) {
                        val history = HashMap(item.takenHistory)
                        if (isTaken) {
                            history[today] = LocalTime.now()
                        } else {
                            history.remove(today)
                        }
                        items[i] = item.copy(takenHistory = history)
                        updated = true
                    }
                }
            }
            "/delete_item" -> {
                val itemName = dataMap.getString("item_name") ?: return false
                val cleanItemName = if (itemName.contains("-")) itemName.substringAfter("-").trim() else itemName.trim()

                val iterator = items.iterator()
                while (iterator.hasNext()) {
                    val item = iterator.next()
                    val itemTitle = item.title.trim()
                    if (itemTitle.equals(cleanItemName, ignoreCase = true) || itemName.contains(itemTitle, ignoreCase = true) || cleanItemName.contains(itemTitle, ignoreCase = true)) {
                        iterator.remove()
                        updated = true
                    }
                }
            }
            "/new_event" -> {
                val eventName = dataMap.getString("event_name") ?: return false
                val prefs = context.getSharedPreferences("med_settings", Context.MODE_PRIVATE)
                val jsonString = prefs.getString("pref_presets_ordered", null)
                var foundIcon = "Event"
                var foundColor = "dynamic"

                if (jsonString != null) {
                    try {
                        val jsonArray = org.json.JSONArray(jsonString)
                        for (i in 0 until jsonArray.length()) {
                            val preset = jsonArray.getString(i)
                            val parts = preset.split("|")
                            val name = parts.getOrNull(1) ?: ""
                            if (name.equals(eventName, ignoreCase = true)) {
                                foundIcon = parts.getOrNull(2) ?: "Event"
                                foundColor = parts.getOrNull(3) ?: "dynamic"
                                break
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    val oldSet = prefs.getStringSet("pref_presets", null)
                    oldSet?.forEach { preset ->
                        val parts = preset.split("|")
                        val name = parts.getOrNull(1) ?: ""
                        if (name.equals(eventName, ignoreCase = true)) {
                            foundIcon = parts.getOrNull(2) ?: "Event"
                            foundColor = parts.getOrNull(3) ?: "dynamic"
                            return@forEach
                        }
                    }
                }

                val newItem = MedData(
                    id = System.nanoTime(),
                    groupId = System.currentTimeMillis(),
                    type = ItemType.Event,
                    title = eventName,
                    iconName = foundIcon,
                    colorCode = foundColor,
                    frequencyLabel = "",
                    creationDate = LocalDate.now(),
                    creationTime = LocalTime.now(),
                    takenHistory = hashMapOf(),
                    recurrenceDays = null,
                    endDate = null,
                    notes = null,
                    displayOrder = 0,
                    intervalGap = null,
                    category = null,
                    notificationType = 0,
                    medicineType = 0,
                    dose = 0.0,
                    unit = "",
                    totalQuantity = 0.0
                )
                items.add(newItem)
                updated = true
            }
        }

        if (updated) {
            DataRepository.saveData(context, items)
        }
        return updated
    }

    @SuppressLint("VisibleForTests")
    fun syncData(medicines: ArrayList<DataMap>, events: ArrayList<DataMap>) {
        val request = PutDataMapRequest.create("/med_data").apply {
            dataMap.putDataMapArrayList("medicines", medicines)
            dataMap.putDataMapArrayList("events", events)
            dataMap.putLong("timestamp", System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()
        val task = dataClient?.putDataItem(request)
        if (task != null) {
            try {
                com.google.android.gms.tasks.Tasks.await(task)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @SuppressLint("VisibleForTests")
    fun syncSettings(
        alarmsEnabled: Boolean,
        enabledEvents: Set<String>,
        availableEvents: Set<String>,
        presetDetails: List<String> = emptyList()
    ) {
        val request = PutDataMapRequest.create("/settings").apply {
            dataMap.putBoolean("alarms_enabled", alarmsEnabled)
            dataMap.putStringArray("enabled_events", enabledEvents.toTypedArray())
            dataMap.putStringArray("available_events", availableEvents.toTypedArray())
            dataMap.putStringArray("preset_details", presetDetails.toTypedArray())
            dataMap.putLong("timestamp", System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()
        val task = dataClient?.putDataItem(request)
        if (task != null) {
            try {
                com.google.android.gms.tasks.Tasks.await(task)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun triggerWatchAlarm(itemIds: LongArray, title: String, notifId: Int) {
        val request = PutDataMapRequest.create("/trigger_alarm").apply {
            dataMap.putLongArray("item_ids", itemIds)
            dataMap.putString("title", title)
            dataMap.putInt("notif_id", notifId)
            dataMap.putLong("timestamp", System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()
        val task = dataClient?.putDataItem(request)
        if (task != null) {
            try {
                com.google.android.gms.tasks.Tasks.await(task)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
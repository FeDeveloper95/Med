package com.fedeveloper95.med.services

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.fedeveloper95.med.ItemType
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class AppWearListenerService : WearableListenerService() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "FORCE_SYNC") {
            syncDataToWear(applicationContext)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    fun syncDataToWear(context: Context) = runBlocking(Dispatchers.IO) {
        val items = DataRepository.loadData(context)
        val today = LocalDate.now()

        val medicinesList = ArrayList<DataMap>()
        val eventsList = ArrayList<DataMap>()

        items.forEach { item ->
            if (item.iconName == "DIVIDER" || item.title.isBlank()) return@forEach

            val isTaken = item.takenHistory.containsKey(today)
            val timeStr = item.creationTime.format(DateTimeFormatter.ofPattern("HH:mm"))

            val itemData = DataMap().apply {
                putString("title", item.title)
                putString("time", timeStr)
                putBoolean("isTaken", isTaken)
                putString("iconName", item.iconName ?: "")
                putString("colorCode", item.colorCode ?: "")
            }

            if (item.type == ItemType.Medicine) {
                val isAfterStart = !today.isBefore(item.creationDate)
                val isBeforeEnd = item.endDate == null || !today.isAfter(item.endDate)
                val isCorrectDay =
                    item.recurrenceDays.isNullOrEmpty() || item.recurrenceDays.contains(today.dayOfWeek)
                val isCorrectGap =
                    item.intervalGap == null || java.time.temporal.ChronoUnit.DAYS.between(
                        item.creationDate,
                        today
                    ) % item.intervalGap == 0L

                if (isAfterStart && isBeforeEnd && isCorrectDay && isCorrectGap) {
                    medicinesList.add(itemData)
                }
            } else if (item.type == ItemType.Event) {
                if (item.creationDate == today) {
                    eventsList.add(itemData)
                }
            } else if (item.type == ItemType.Illness) {
                val isAfterStart = !today.isBefore(item.creationDate)
                val isBeforeEnd = item.endDate == null || !today.isAfter(item.endDate)
                if (isAfterStart && isBeforeEnd) {
                    eventsList.add(itemData)
                }
            }
        }

        val request = PutDataMapRequest.create("/med_data").apply {
            dataMap.putDataMapArrayList("medicines", medicinesList)
            dataMap.putDataMapArrayList("events", eventsList)
            dataMap.putLong("timestamp", System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()

        try {
            com.google.android.gms.tasks.Tasks.await(Wearable.getDataClient(context).putDataItem(request))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val path = event.dataItem.uri.path
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val uri = event.dataItem.uri

                when (path) {
                    "/request_sync" -> runBlocking(Dispatchers.IO) {
                        WearSyncManager.initialize(applicationContext)
                        syncDataToWear(applicationContext)

                        val prefs = applicationContext.getSharedPreferences(
                            "med_settings",
                            MODE_PRIVATE
                        )
                        val ringOnWatch = prefs.getBoolean("pref_wear_ring_alarms", true)

                        val allNames = mutableSetOf<String>()
                        val presetDetails = mutableListOf<String>()
                        val jsonString = prefs.getString("pref_presets_ordered", null)

                        if (jsonString != null) {
                            try {
                                val jsonArray = org.json.JSONArray(jsonString)
                                for (i in 0 until jsonArray.length()) {
                                    val preset = jsonArray.getString(i)
                                    val parts = preset.split("|")
                                    val name = parts.getOrNull(1)
                                    if (!name.isNullOrBlank()) {
                                        allNames.add(name)
                                        val icon = parts.getOrNull(2) ?: "Event"
                                        val color = parts.getOrNull(3) ?: "dynamic"
                                        presetDetails.add(name + "|" + icon + "|" + color)
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        } else {
                            val oldSet = prefs.getStringSet("pref_presets", null)
                            oldSet?.forEach { preset ->
                                val parts = preset.split("|")
                                val name = parts.getOrNull(1)
                                if (!name.isNullOrBlank()) {
                                    allNames.add(name)
                                    val icon = parts.getOrNull(2) ?: "Event"
                                    val color = parts.getOrNull(3) ?: "dynamic"
                                    presetDetails.add(name + "|" + icon + "|" + color)
                                }
                            }
                        }

                        val savedEvents = prefs.getStringSet("pref_wear_enabled_events", null)
                        val wearEnabledEvents =
                            savedEvents?.filter { allNames.contains(it) }?.toSet() ?: allNames

                        WearSyncManager.syncSettings(ringOnWatch, wearEnabledEvents, allNames, presetDetails)

                        sendBroadcast(Intent("com.fedeveloper95.med.RELOAD_DATA").setPackage(packageName))
                        sendBroadcast(Intent("com.fedeveloper95.med.REFRESH_DATA").setPackage(packageName))

                        try {
                            com.google.android.gms.tasks.Tasks.await(Wearable.getDataClient(applicationContext).deleteDataItems(uri))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    "/new_event" -> runBlocking(Dispatchers.IO) {
                        val eventName = dataMap.getString("event_name") ?: return@runBlocking
                        val items = DataRepository.loadData(applicationContext).toMutableList()

                        val prefs = applicationContext.getSharedPreferences(
                            "med_settings",
                            MODE_PRIVATE
                        )
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
                        DataRepository.saveData(applicationContext, items)
                        sendBroadcast(Intent("com.fedeveloper95.med.RELOAD_DATA").setPackage(packageName))
                        sendBroadcast(Intent("com.fedeveloper95.med.REFRESH_DATA").setPackage(packageName))
                        syncDataToWear(applicationContext)

                        try {
                            com.google.android.gms.tasks.Tasks.await(Wearable.getDataClient(applicationContext).deleteDataItems(uri))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    "/open_url" -> runBlocking(Dispatchers.IO) {
                        val url = dataMap.getString("url") ?: return@runBlocking
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        try {
                            startActivity(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        try {
                            com.google.android.gms.tasks.Tasks.await(Wearable.getDataClient(applicationContext).deleteDataItems(uri))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    "/item_taken" -> runBlocking(Dispatchers.IO) {
                        val itemName = dataMap.getString("item_name") ?: return@runBlocking
                        val isTaken = dataMap.getBoolean("is_taken")

                        val items = DataRepository.loadData(applicationContext).toMutableList()
                        val today = LocalDate.now()
                        var updated = false

                        val cleanItemName = if (itemName.contains("-")) {
                            itemName.substringAfter("-").trim()
                        } else {
                            itemName.trim()
                        }

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

                        if (updated) {
                            DataRepository.saveData(applicationContext, items)
                            sendBroadcast(Intent("com.fedeveloper95.med.RELOAD_DATA").setPackage(packageName))
                            sendBroadcast(Intent("com.fedeveloper95.med.REFRESH_DATA").setPackage(packageName))
                            syncDataToWear(applicationContext)
                        }
                        try {
                            com.google.android.gms.tasks.Tasks.await(Wearable.getDataClient(applicationContext).deleteDataItems(uri))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    "/delete_item" -> runBlocking(Dispatchers.IO) {
                        val itemName = dataMap.getString("item_name") ?: return@runBlocking

                        val items = DataRepository.loadData(applicationContext).toMutableList()
                        var updated = false

                        val cleanItemName = if (itemName.contains("-")) {
                            itemName.substringAfter("-").trim()
                        } else {
                            itemName.trim()
                        }

                        val iterator = items.iterator()
                        while (iterator.hasNext()) {
                            val item = iterator.next()
                            val itemTitle = item.title.trim()

                            if (itemTitle.equals(cleanItemName, ignoreCase = true) || itemName.contains(itemTitle, ignoreCase = true) || cleanItemName.contains(itemTitle, ignoreCase = true)) {
                                iterator.remove()
                                updated = true
                            }
                        }

                        if (updated) {
                            DataRepository.saveData(applicationContext, items)
                            sendBroadcast(Intent("com.fedeveloper95.med.RELOAD_DATA").setPackage(packageName))
                            sendBroadcast(Intent("com.fedeveloper95.med.REFRESH_DATA").setPackage(packageName))
                            syncDataToWear(applicationContext)
                        }
                        try {
                            com.google.android.gms.tasks.Tasks.await(Wearable.getDataClient(applicationContext).deleteDataItems(uri))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    "/settings" -> runBlocking(Dispatchers.IO) {
                        val alarms = dataMap.getBoolean("alarms_enabled", true)
                        val events = dataMap.getStringArray("enabled_events") ?: emptyArray()

                        val prefs = applicationContext.getSharedPreferences("med_settings", MODE_PRIVATE)
                        prefs.edit()
                            .putBoolean("pref_wear_ring_alarms", alarms)
                            .putStringSet("pref_wear_enabled_events", events.toSet())
                            .apply()

                        try {
                            com.google.android.gms.tasks.Tasks.await(Wearable.getDataClient(applicationContext).deleteDataItems(uri))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    "/action_take", "/action_snooze" -> runBlocking(Dispatchers.IO) {
                        val itemIds = dataMap.getLongArray("item_ids") ?: return@runBlocking
                        val notifId = dataMap.getInt("notif_id", -1)

                        val actionIntent = Intent(applicationContext, NotificationReceiver::class.java).apply {
                            action = if (path == "/action_take") NotificationReceiver.ACTION_TAKEN else NotificationReceiver.ACTION_SNOOZE
                            putExtra("ITEM_IDS", itemIds)
                            putExtra("NOTIF_ID", notifId)
                        }
                        applicationContext.sendBroadcast(actionIntent)

                        try {
                            com.google.android.gms.tasks.Tasks.await(Wearable.getDataClient(applicationContext).deleteDataItems(uri))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }
}
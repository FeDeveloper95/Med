package com.fedeveloper95.med.services

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.fedeveloper95.med.ItemType
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class AppWearListenerService : WearableListenerService() {

    private fun syncDataToWear(context: android.content.Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val items = DataRepository.loadData(context)
            val today = LocalDate.now()

            val medicinesList = mutableListOf<String>()
            val eventsList = mutableListOf<String>()

            items.forEach { item ->
                if (item.iconName == "DIVIDER" || item.title.isBlank()) return@forEach

                val isTaken = item.takenHistory.containsKey(today)
                val checkMark = if (isTaken) "✔ " else ""
                val timeStr = item.creationTime.format(DateTimeFormatter.ofPattern("HH:mm"))
                val displayStr = "$checkMark$timeStr - ${item.title}"

                if (item.type == ItemType.Medicine) {
                    val isAfterStart = !today.isBefore(item.creationDate)
                    val isBeforeEnd = item.endDate == null || !today.isAfter(item.endDate)
                    val isCorrectDay = item.recurrenceDays.isNullOrEmpty() || item.recurrenceDays.contains(today.dayOfWeek)
                    val isCorrectGap = item.intervalGap == null || java.time.temporal.ChronoUnit.DAYS.between(item.creationDate, today) % item.intervalGap == 0L

                    if (isAfterStart && isBeforeEnd && isCorrectDay && isCorrectGap) {
                        medicinesList.add(displayStr)
                    }
                } else if (item.type == ItemType.Event) {
                    if (item.creationDate == today) {
                        eventsList.add(displayStr)
                    }
                }
            }

            val request = PutDataMapRequest.create("/med_data").apply {
                dataMap.putStringArray("medicines", medicinesList.toTypedArray())
                dataMap.putStringArray("events", eventsList.toTypedArray())
                dataMap.putLong("timestamp", System.currentTimeMillis())
            }.asPutDataRequest().setUrgent()

            Wearable.getDataClient(context).putDataItem(request)
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val path = event.dataItem.uri.path
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap

                when (path) {
                    "/request_sync" -> {
                        WearSyncManager.initialize(applicationContext)
                        syncDataToWear(applicationContext)
                    }
                    "/new_event" -> {
                        val eventName = dataMap.getString("event_name") ?: return
                        CoroutineScope(Dispatchers.IO).launch {
                            val items = DataRepository.loadData(applicationContext).toMutableList()

                            val prefs = applicationContext.getSharedPreferences("med_settings", Context.MODE_PRIVATE)
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
                                        if (name == eventName) {
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
                                    if (name == eventName) {
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
                                category = null
                            )
                            items.add(newItem)
                            DataRepository.saveData(applicationContext, items)
                            sendBroadcast(Intent("com.fedeveloper95.med.REFRESH_DATA").setPackage(packageName))
                            syncDataToWear(applicationContext)
                        }
                    }
                    "/open_url" -> {
                        val url = dataMap.getString("url") ?: return
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        try {
                            startActivity(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    "/item_taken" -> {
                        val itemName = dataMap.getString("item_name") ?: return
                        val itemTypeStr = dataMap.getString("item_type") ?: return
                        val isTaken = dataMap.getBoolean("is_taken")

                        CoroutineScope(Dispatchers.IO).launch {
                            val items = DataRepository.loadData(applicationContext).toMutableList()
                            val today = LocalDate.now()
                            var updated = false

                            val targetType = if (itemTypeStr == "medicine") ItemType.Medicine else ItemType.Event

                            val cleanItemName = if (itemName.contains("-")) {
                                itemName.substringAfter("-").trim()
                            } else {
                                itemName.trim()
                            }

                            for (i in items.indices) {
                                val item = items[i]
                                if (item.type != targetType) continue

                                val itemTitle = item.title.trim()

                                if (itemTitle.equals(cleanItemName, ignoreCase = true) || itemName.contains(itemTitle, ignoreCase = true)) {
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
                                sendBroadcast(Intent("com.fedeveloper95.med.REFRESH_DATA").setPackage(packageName))
                                syncDataToWear(applicationContext)
                            }
                        }
                    }
                    "/delete_item" -> {
                        val itemName = dataMap.getString("item_name") ?: return
                        val itemTypeStr = dataMap.getString("item_type") ?: return

                        CoroutineScope(Dispatchers.IO).launch {
                            val items = DataRepository.loadData(applicationContext).toMutableList()
                            var updated = false

                            val targetType = if (itemTypeStr == "medicine") ItemType.Medicine else ItemType.Event

                            val cleanItemName = if (itemName.contains("-")) {
                                itemName.substringAfter("-").trim()
                            } else {
                                itemName.trim()
                            }

                            val iterator = items.iterator()
                            while (iterator.hasNext()) {
                                val item = iterator.next()
                                if (item.type != targetType) continue

                                val itemTitle = item.title.trim()

                                if (itemTitle.equals(cleanItemName, ignoreCase = true) || itemName.contains(itemTitle, ignoreCase = true)) {
                                    iterator.remove()
                                    updated = true
                                }
                            }

                            if (updated) {
                                DataRepository.saveData(applicationContext, items)
                                sendBroadcast(Intent("com.fedeveloper95.med.REFRESH_DATA").setPackage(packageName))
                                syncDataToWear(applicationContext)
                            }
                        }
                    }
                }
            }
        }
    }
}
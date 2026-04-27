package com.fedeveloper95.med.services

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.fedeveloper95.med.ItemType
import com.fedeveloper95.med.loadPresets
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class AppWearListenerService : WearableListenerService() {

    private fun syncAllDataToWear(context: Context) {
        val items = DataRepository.loadData(context)
        val today = LocalDate.now()
        val itemsToday = items.filter { item ->
            when (item.type) {
                ItemType.Event -> item.creationDate == today
                ItemType.Medicine -> {
                    val isAfterStart = !today.isBefore(item.creationDate)
                    val isBeforeEnd = item.endDate == null || !today.isAfter(item.endDate)
                    val isCorrectDay = item.recurrenceDays.isNullOrEmpty() || item.recurrenceDays.contains(today.dayOfWeek)
                    val isCorrectGap = item.intervalGap == null || ChronoUnit.DAYS.between(item.creationDate, today) % item.intervalGap == 0L
                    isAfterStart && isBeforeEnd && isCorrectDay && isCorrectGap
                }
            }
        }.sortedWith(compareBy({ it.displayOrder }, { it.creationTime }))

        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        val meds = itemsToday.filter { it.type == ItemType.Medicine }.map {
            val taken = if (it.takenHistory.containsKey(today)) "✅ " else ""
            "$taken${it.creationTime.format(formatter)} - ${it.title}"
        }
        val evs = itemsToday.filter { it.type == ItemType.Event && it.title.isNotBlank() && it.iconName != "DIVIDER" }.map {
            "${it.creationTime.format(formatter)} - ${it.title}"
        }

        WearSyncManager.syncData(meds, evs)
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        WearSyncManager.initialize(applicationContext)
        WearSyncManager.onDataChanged(dataEvents)

        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val path = event.dataItem.uri.path
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap

                if (path == "/open_url") {
                    val url = dataMap.getString("url")
                    if (url != null) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    }
                }

                if (path == "/request_sync") {
                    val prefs = applicationContext.getSharedPreferences("med_settings", Context.MODE_PRIVATE)
                    val alarms = prefs.getBoolean("pref_wear_ring_alarms", true)
                    val presetsStrings = loadPresets(prefs)
                    val allNames = presetsStrings.mapNotNull { it.split("|").getOrNull(1) }.toSet()
                    val savedEvents = prefs.getStringSet("pref_wear_enabled_events", null)
                    val enabledEvents = savedEvents ?: allNames

                    WearSyncManager.syncSettings(alarms, enabledEvents, allNames)
                    syncAllDataToWear(applicationContext)
                }

                if (path == "/new_event") {
                    val eventName = dataMap.getString("event_name")

                    if (eventName != null) {
                        val items = DataRepository.loadData(applicationContext).toMutableList()
                        val prefs = applicationContext.getSharedPreferences("med_settings", Context.MODE_PRIVATE)
                        val presets = loadPresets(prefs)

                        var foundIcon = "Event"
                        var foundColor = "dynamic"

                        for (preset in presets) {
                            val parts = preset.split("|")
                            if (parts.size >= 4 && parts[1] == eventName) {
                                foundIcon = parts[2]
                                foundColor = parts[3]
                                break
                            }
                        }

                        val currentOrder = (items.maxOfOrNull { it.displayOrder } ?: 0) + 1

                        val newItem = MedData(
                            id = System.nanoTime(),
                            type = ItemType.Event,
                            title = eventName,
                            creationDate = LocalDate.now(),
                            creationTime = LocalTime.now(),
                            iconName = foundIcon,
                            colorCode = foundColor,
                            displayOrder = currentOrder
                        )
                        items.add(newItem)
                        DataRepository.saveData(applicationContext, items)

                        syncAllDataToWear(applicationContext)

                        val intent = Intent("com.fedeveloper95.med.RELOAD_DATA")
                        intent.setPackage(packageName)
                        sendBroadcast(intent)
                    }
                }
            }
        }
    }
}
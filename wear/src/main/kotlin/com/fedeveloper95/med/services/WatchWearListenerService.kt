package com.fedeveloper95.med.services

import android.content.Intent
import com.fedeveloper95.med.AlarmActivity
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService

class WatchWearListenerService : WearableListenerService() {
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        WearDataManager.initialize(applicationContext)
        WearDataManager.onDataChanged(dataEvents)

        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val path = event.dataItem.uri.path
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap

                if (path == "/trigger_alarm") {
                    val title = dataMap.getString("title", "")
                    val itemIds = dataMap.getLongArray("item_ids") ?: longArrayOf()
                    val notifId = dataMap.getInt("notif_id", -1)

                    val intent = Intent(applicationContext, AlarmActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        putExtra("title", title)
                        putExtra("item_ids", itemIds)
                        putExtra("notif_id", notifId)
                    }
                    applicationContext.startActivity(intent)
                }
            }
        }
    }
}
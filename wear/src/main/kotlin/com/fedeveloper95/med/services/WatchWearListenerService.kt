package com.fedeveloper95.med.services

import com.fedeveloper95.med.services.WearDataManager
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.WearableListenerService

class WatchWearListenerService : WearableListenerService() {
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        WearDataManager.initialize(applicationContext)
        WearDataManager.onDataChanged(dataEvents)
    }
}
package com.fedeveloper95.med.services

import android.content.Context
import com.google.android.gms.wearable.DataMap

object WearSyncManager {
    fun initialize(context: Context) {}

    fun processPendingWearActions(context: Context) {}

    fun syncData(medicines: ArrayList<DataMap>, events: ArrayList<DataMap>) {}

    fun syncSettings(
        alarmsEnabled: Boolean,
        enabledEvents: Set<String>,
        availableEvents: Set<String>,
        presetDetails: List<String> = emptyList()
    ) {}

    fun triggerWatchAlarm(itemIds: LongArray, title: String, notifId: Int) {}
}
package com.fedeveloper95.med.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder

class AppWearListenerService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    fun syncDataToWear(context: Context) {
    }
}
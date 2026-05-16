package com.fedeveloper95.med.services

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle

object HandoffHelper {
    fun init(application: Application) {
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (Build.VERSION.SDK_INT >= 37) {
                    try {
                        val method = Activity::class.java.getMethod("setHandoffEnabled", Boolean::class.javaPrimitiveType)
                        method.invoke(activity, true)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
}
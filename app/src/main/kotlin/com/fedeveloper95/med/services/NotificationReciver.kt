package com.fedeveloper95.med.services

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.fedeveloper95.med.AlarmActivity
import com.fedeveloper95.med.ItemType
import com.fedeveloper95.med.MainActivity
import com.fedeveloper95.med.PREF_FULL_SCREEN_ALARM
import com.fedeveloper95.med.PREF_SNOOZE_DURATION
import com.fedeveloper95.med.R
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class NotificationReceiver : BroadcastReceiver() {

    companion object {
        const val ALARM_CHANNEL_ID = "med_alarms_fullscreen_v1"
        const val SIMPLE_NOTIF_CHANNEL_ID = "med_alarms_simple_v1"
        const val ACTION_SHOW_NOTIFICATION = "ACTION_SHOW_NOTIFICATION"
        const val ACTION_TAKEN = "ACTION_TAKEN"
        const val ACTION_SNOOZE = "ACTION_SNOOZE"

        fun scheduleNotification(context: Context, item: MedData) {
            if (item.type != ItemType.Medicine) return

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = ACTION_SHOW_NOTIFICATION
                putExtra("ITEM_ID", item.id)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                item.id.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val nextDateTime = getNextOccurrence(item)

            if (nextDateTime != null) {
                val timeInMillis = nextDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
                    } else {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
                    }
                } catch (e: SecurityException) {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
                }
            } else {
                alarmManager.cancel(pendingIntent)
            }
        }

        private fun getNextOccurrence(item: MedData): LocalDateTime? {
            var date = LocalDate.now()
            val now = LocalTime.now()

            if (isValidDate(item, date) && item.creationTime.isAfter(now)) {
                return LocalDateTime.of(date, item.creationTime)
            }

            for (i in 1..30) {
                date = date.plusDays(1)
                if (isValidDate(item, date)) {
                    return LocalDateTime.of(date, item.creationTime)
                }
            }
            return null
        }

        private fun isValidDate(item: MedData, date: LocalDate): Boolean {
            if (date.isBefore(item.creationDate)) return false
            if (item.endDate != null && date.isAfter(item.endDate)) return false
            if (!item.recurrenceDays.isNullOrEmpty() && !item.recurrenceDays.contains(date.dayOfWeek)) return false
            if (item.intervalGap != null) {
                val daysBetween = ChronoUnit.DAYS.between(item.creationDate, date)
                if (daysBetween % item.intervalGap != 0L) return false
            }
            return true
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val itemId = intent.getLongExtra("ITEM_ID", -1L)

        val prefs = context.getSharedPreferences("med_settings", Context.MODE_PRIVATE)
        val useFullScreen = prefs.getBoolean(PREF_FULL_SCREEN_ALARM, true)
        val snoozeDuration = prefs.getInt(PREF_SNOOZE_DURATION, 10)

        when (action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                val items = DataRepository.loadData(context)
                items.forEach { item ->
                    if (item.type == ItemType.Medicine) {
                        scheduleNotification(context, item)
                    }
                }
            }

            ACTION_SHOW_NOTIFICATION -> {
                if (itemId != -1L) {
                    val items = DataRepository.loadData(context)
                    val item = items.find { it.id == itemId } ?: return

                    createNotificationChannels(context)

                    val channelId = if (useFullScreen) ALARM_CHANNEL_ID else SIMPLE_NOTIF_CHANNEL_ID

                    val soundUri = if (useFullScreen) {
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    } else {
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    }

                    val builder = NotificationCompat.Builder(context, channelId)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(context.getString(R.string.notif_reminder_title, item.title))
                        .setContentText(context.getString(R.string.notif_reminder_desc))
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setCategory(if (useFullScreen) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
                        .setSound(soundUri)
                        .setVibrate(if (useFullScreen) longArrayOf(0, 1000, 1000) else longArrayOf(0, 500, 500))
                        .setAutoCancel(!useFullScreen)

                    val takeIntent = Intent(context, NotificationReceiver::class.java).apply {
                        this.action = ACTION_TAKEN
                        putExtra("ITEM_ID", item.id)
                    }
                    val takePendingIntent = PendingIntent.getBroadcast(
                        context,
                        item.id.toInt(),
                        takeIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val snoozeIntent = Intent(context, NotificationReceiver::class.java).apply {
                        this.action = ACTION_SNOOZE
                        putExtra("ITEM_ID", item.id)
                    }
                    val snoozePendingIntent = PendingIntent.getBroadcast(
                        context,
                        item.id.toInt() + 100000,
                        snoozeIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    builder.addAction(R.drawable.ic_notification, context.getString(R.string.notif_action_taken), takePendingIntent)
                    builder.addAction(R.drawable.ic_notification, context.getString(R.string.notif_action_snooze), snoozePendingIntent)

                    if (useFullScreen) {
                        val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            putExtra("ITEM_ID", item.id)
                            putExtra("ITEM_TITLE", item.title)
                        }
                        val fullScreenPendingIntent = PendingIntent.getActivity(
                            context,
                            item.id.toInt(),
                            fullScreenIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        builder.setFullScreenIntent(fullScreenPendingIntent, true)
                        builder.setContentIntent(fullScreenPendingIntent)
                        builder.setOngoing(true)
                    } else {
                        val contentIntent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        val contentPendingIntent = PendingIntent.getActivity(
                            context,
                            item.id.toInt() + 200000,
                            contentIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        builder.setContentIntent(contentPendingIntent)
                    }

                    val notification = builder.build()

                    if (useFullScreen) {
                        notification.flags = notification.flags or Notification.FLAG_INSISTENT
                    }

                    val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notifManager.notify(item.id.toInt(), notification)
                }
            }

            ACTION_TAKEN -> {
                if (itemId != -1L) {
                    val items = DataRepository.loadData(context).toMutableList()
                    val index = items.indexOfFirst { it.id == itemId }
                    if (index != -1) {
                        val item = items[index]

                        val newHistory = HashMap(item.takenHistory)
                        newHistory[LocalDate.now()] = LocalTime.now()
                        items[index] = item.copy(takenHistory = newHistory)
                        DataRepository.saveData(context, items)

                        scheduleNotification(context, items[index])
                        context.sendBroadcast(Intent("com.fedeveloper95.med.REFRESH_DATA").setPackage(context.packageName))
                    }

                    val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notifManager.cancel(itemId.toInt())
                    context.sendBroadcast(Intent("ACTION_CLOSE_ALARM_ACTIVITY"))
                }
            }

            ACTION_SNOOZE -> {
                if (itemId != -1L) {
                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

                    val snoozeIntent = Intent(context, NotificationReceiver::class.java).apply {
                        this.action = ACTION_SHOW_NOTIFICATION
                        putExtra("ITEM_ID", itemId)
                    }
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        itemId.toInt(),
                        snoozeIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val snoozeTime = System.currentTimeMillis() + (snoozeDuration * 60 * 1000)
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                            alarmManager.set(AlarmManager.RTC_WAKEUP, snoozeTime, pendingIntent)
                        } else {
                            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeTime, pendingIntent)
                        }
                    } catch (e: SecurityException) {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, snoozeTime, pendingIntent)
                    }

                    val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notifManager.cancel(itemId.toInt())
                    context.sendBroadcast(Intent("ACTION_CLOSE_ALARM_ACTIVITY"))
                }
            }
        }
    }

    private fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val name = context.getString(R.string.notif_channel_alarms_name)
            val descriptionText = context.getString(R.string.notif_channel_alarms_desc)

            val alarmChannel = NotificationChannel(ALARM_CHANNEL_ID, name, NotificationManager.IMPORTANCE_HIGH).apply {
                description = descriptionText
                val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()
                setSound(alarmUri, audioAttributes)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 1000)
            }

            val simpleNotifChannel = NotificationChannel(SIMPLE_NOTIF_CHANNEL_ID, name, NotificationManager.IMPORTANCE_HIGH).apply {
                description = descriptionText
                val notifUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()
                setSound(notifUri, audioAttributes)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 500)
            }

            notificationManager.createNotificationChannel(alarmChannel)
            notificationManager.createNotificationChannel(simpleNotifChannel)
        }
    }
}
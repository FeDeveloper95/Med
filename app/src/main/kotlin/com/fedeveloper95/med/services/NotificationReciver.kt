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
import com.fedeveloper95.med.PREF_SHOW_NOTIFICATIONS
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
        const val ACTION_RESCHEDULE_ALL = "ACTION_RESCHEDULE_ALL"

        fun scheduleNotification(context: Context, item: MedData) {
            if (item.type != ItemType.Medicine) return

            val prefs = context.getSharedPreferences("med_settings", Context.MODE_PRIVATE)
            val globalShowNotif = prefs.getBoolean(PREF_SHOW_NOTIFICATIONS, true)
            val globalUseFullScreen = prefs.getBoolean(PREF_FULL_SCREEN_ALARM, true)

            val effectiveType = when (item.notificationType) {
                1 -> "NONE"
                2 -> "NORMAL"
                3 -> "ALARM"
                else -> {
                    if (!globalShowNotif) "NONE"
                    else if (globalUseFullScreen) "ALARM" else "NORMAL"
                }
            }

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

            if (effectiveType == "NONE") {
                alarmManager.cancel(pendingIntent)
                return
            }

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
        val prefs = context.getSharedPreferences("med_settings", Context.MODE_PRIVATE)

        when (action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED, ACTION_RESCHEDULE_ALL -> {
                val items = DataRepository.loadData(context)
                items.forEach { item ->
                    if (item.type == ItemType.Medicine) {
                        scheduleNotification(context, item)
                    }
                }
            }

            ACTION_SHOW_NOTIFICATION -> {
                val triggerItemId = intent.getLongExtra("ITEM_ID", -1L)
                val isSnooze = intent.getBooleanExtra("IS_SNOOZE", false)
                if (triggerItemId == -1L) return

                val items = DataRepository.loadData(context)
                val triggerItem = items.find { it.id == triggerItemId } ?: return

                val groupItems = if (isSnooze) {
                    listOf(triggerItem)
                } else {
                    items.filter {
                        it.type == ItemType.Medicine &&
                                it.creationTime == triggerItem.creationTime &&
                                isValidDate(it, LocalDate.now())
                    }
                }

                val globalShowNotif = prefs.getBoolean(PREF_SHOW_NOTIFICATIONS, true)
                val globalUseFullScreen = prefs.getBoolean(PREF_FULL_SCREEN_ALARM, true)

                val validGroupItems = groupItems.filter { item ->
                    val effectiveType = when (item.notificationType) {
                        1 -> "NONE"
                        2 -> "NORMAL"
                        3 -> "ALARM"
                        else -> if (!globalShowNotif) "NONE" else if (globalUseFullScreen) "ALARM" else "NORMAL"
                    }
                    effectiveType != "NONE"
                }

                if (validGroupItems.isEmpty()) return

                if (!isSnooze && validGroupItems.size > 1) {
                    val leaderId = validGroupItems.minOf { it.id }
                    if (triggerItemId != leaderId) return
                }

                val useFullScreen = validGroupItems.any { item ->
                    val effectiveType = when (item.notificationType) {
                        1 -> "NONE"
                        2 -> "NORMAL"
                        3 -> "ALARM"
                        else -> if (!globalShowNotif) "NONE" else if (globalUseFullScreen) "ALARM" else "NORMAL"
                    }
                    effectiveType == "ALARM"
                }

                val wearAlarmsEnabled = prefs.getBoolean("pref_wear_ring_alarms", false)
                val shouldTriggerWear = useFullScreen && wearAlarmsEnabled

                createNotificationChannels(context)
                val channelId = if (useFullScreen && !shouldTriggerWear) ALARM_CHANNEL_ID else SIMPLE_NOTIF_CHANNEL_ID

                val notifId = validGroupItems.minOf { it.id }.toInt()
                val isGrouped = validGroupItems.size > 1
                val titles = validGroupItems.joinToString(", ") { it.title }
                val itemIds = validGroupItems.map { it.id }.toLongArray()

                WearSyncManager.initialize(context)

                val builder = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(if (isGrouped) context.getString(R.string.notif_grouped_title) else context.getString(R.string.notif_reminder_title, triggerItem.title))
                    .setContentText(if (isGrouped) titles else context.getString(R.string.notif_reminder_desc))
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setAutoCancel(!useFullScreen || shouldTriggerWear)

                if (useFullScreen && !shouldTriggerWear) {
                    val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    builder.setCategory(NotificationCompat.CATEGORY_ALARM)
                    builder.setSound(soundUri)
                    builder.setVibrate(longArrayOf(0, 1000, 1000))
                } else if (shouldTriggerWear) {
                    WearSyncManager.triggerWatchAlarm(itemIds, titles, notifId)
                    builder.setCategory(NotificationCompat.CATEGORY_REMINDER)
                    builder.setSilent(true)
                } else {
                    builder.setCategory(NotificationCompat.CATEGORY_REMINDER)
                    builder.setDefaults(NotificationCompat.DEFAULT_ALL)
                }

                val takeIntent = Intent(context, NotificationReceiver::class.java).apply {
                    this.action = ACTION_TAKEN
                    putExtra("ITEM_IDS", itemIds)
                    putExtra("NOTIF_ID", notifId)
                }
                val takePendingIntent = PendingIntent.getBroadcast(
                    context,
                    notifId,
                    takeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val snoozeIntent = Intent(context, NotificationReceiver::class.java).apply {
                    this.action = ACTION_SNOOZE
                    putExtra("ITEM_IDS", itemIds)
                    putExtra("NOTIF_ID", notifId)
                }
                val snoozePendingIntent = PendingIntent.getBroadcast(
                    context,
                    notifId + 100000,
                    snoozeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val takeActionText = if (isGrouped) context.getString(R.string.notif_action_take_all) else context.getString(R.string.notif_action_taken)

                builder.addAction(R.drawable.ic_notification, takeActionText, takePendingIntent)
                builder.addAction(R.drawable.ic_notification, context.getString(R.string.notif_action_snooze), snoozePendingIntent)

                if (useFullScreen && !shouldTriggerWear) {
                    val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        putExtra("ITEM_IDS", itemIds)
                        putExtra("ITEM_TITLE", titles)
                        putExtra("NOTIF_ID", notifId)
                    }
                    val fullScreenPendingIntent = PendingIntent.getActivity(
                        context,
                        notifId,
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
                        notifId + 200000,
                        contentIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    builder.setContentIntent(contentPendingIntent)
                }

                val notification = builder.build()

                if (useFullScreen && !shouldTriggerWear) {
                    notification.flags = notification.flags or Notification.FLAG_INSISTENT
                }

                val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notifManager.notify(notifId, notification)
            }

            ACTION_TAKEN -> {
                val itemIds = intent.getLongArrayExtra("ITEM_IDS") ?: run {
                    val singleId = intent.getLongExtra("ITEM_ID", -1L)
                    if (singleId != -1L) longArrayOf(singleId) else null
                }

                if (itemIds != null) {
                    val items = DataRepository.loadData(context).toMutableList()
                    var isDataUpdated = false

                    for (id in itemIds) {
                        val index = items.indexOfFirst { it.id == id }
                        if (index != -1) {
                            val item = items[index]
                            val newHistory = HashMap(item.takenHistory)
                            newHistory[LocalDate.now()] = LocalTime.now()
                            items[index] = item.copy(takenHistory = newHistory)
                            isDataUpdated = true
                            scheduleNotification(context, items[index])
                        }
                    }

                    if (isDataUpdated) {
                        DataRepository.saveData(context, items)
                        context.sendBroadcast(Intent("com.fedeveloper95.med.REFRESH_DATA").setPackage(context.packageName))
                    }

                    val notifId = intent.getIntExtra("NOTIF_ID", -1)
                    if (notifId != -1) {
                        val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notifManager.cancel(notifId)
                    } else if (itemIds.size == 1) {
                        val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notifManager.cancel(itemIds[0].toInt())
                    }

                    context.sendBroadcast(Intent("ACTION_CLOSE_ALARM_ACTIVITY"))
                }
            }

            ACTION_SNOOZE -> {
                val itemIds = intent.getLongArrayExtra("ITEM_IDS") ?: run {
                    val singleId = intent.getLongExtra("ITEM_ID", -1L)
                    if (singleId != -1L) longArrayOf(singleId) else null
                }

                if (itemIds != null) {
                    val snoozeDuration = prefs.getInt(PREF_SNOOZE_DURATION, 10)
                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    val snoozeTime = System.currentTimeMillis() + (snoozeDuration * 60 * 1000)

                    for (id in itemIds) {
                        val snoozeIntent = Intent(context, NotificationReceiver::class.java).apply {
                            this.action = ACTION_SHOW_NOTIFICATION
                            putExtra("ITEM_ID", id)
                            putExtra("IS_SNOOZE", true)
                        }
                        val pendingIntent = PendingIntent.getBroadcast(
                            context,
                            id.toInt() + 100000,
                            snoozeIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )

                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                                alarmManager.set(AlarmManager.RTC_WAKEUP, snoozeTime, pendingIntent)
                            } else {
                                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeTime, pendingIntent)
                            }
                        } catch (e: SecurityException) {
                            alarmManager.set(AlarmManager.RTC_WAKEUP, snoozeTime, pendingIntent)
                        }
                    }

                    val notifId = intent.getIntExtra("NOTIF_ID", -1)
                    if (notifId != -1) {
                        val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notifManager.cancel(notifId)
                    } else if (itemIds.size == 1) {
                        val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notifManager.cancel(itemIds[0].toInt())
                    }

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

            val alarmChannel = NotificationChannel(
                ALARM_CHANNEL_ID,
                name,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
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

            val simpleNotifChannel = NotificationChannel(
                SIMPLE_NOTIF_CHANNEL_ID,
                name,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = descriptionText
            }

            notificationManager.createNotificationChannel(alarmChannel)
            notificationManager.createNotificationChannel(simpleNotifChannel)
        }
    }
}
package com.fedeveloper95.med.services

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.fedeveloper95.med.ItemType
import com.fedeveloper95.med.MainActivity
import com.fedeveloper95.med.MedItem
import com.fedeveloper95.med.R
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.ArrayList
import java.util.HashMap

class NotificationReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_SHOW = "ACTION_SHOW_NOTIFICATION"
        const val ACTION_TAKEN = "ACTION_TAKEN"
        const val ACTION_SKIP = "ACTION_SKIP"
        const val ACTION_SNOOZE = "ACTION_SNOOZE"

        const val EXTRA_ITEM_ID = "EXTRA_ITEM_ID"
        const val EXTRA_ITEM_TITLE = "EXTRA_ITEM_TITLE"
        const val EXTRA_NOTIF_ID = "EXTRA_NOTIF_ID"

        private const val CHANNEL_ID = "med_reminders_channel"
        private const val FILE_NAME = "med_data.dat"

        fun scheduleNotification(context: Context, item: MedItem) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            var date = LocalDate.now()
            val time = item.creationTime

            if (LocalDateTime.of(date, time).isBefore(LocalDateTime.now())) {
                date = date.plusDays(1)
            }

            var daysChecked = 0
            while (daysChecked < 365 * 2) {
                if (item.endDate != null && date.isAfter(item.endDate)) {
                    return
                }

                if (item.recurrenceDays.isNullOrEmpty() || item.recurrenceDays.contains(date.dayOfWeek)) {
                    break
                }

                date = date.plusDays(1)
                daysChecked++
            }

            val scheduledDateTime = LocalDateTime.of(date, time)

            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = ACTION_SHOW
                putExtra(EXTRA_ITEM_ID, item.id)
                putExtra(EXTRA_ITEM_TITLE, item.title)
                putExtra(EXTRA_NOTIF_ID, item.id.toInt())
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                item.id.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            scheduledDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                            pendingIntent
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            scheduledDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                            pendingIntent
                        )
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        scheduledDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                        pendingIntent
                    )
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        if (Intent.ACTION_BOOT_COMPLETED == action || Intent.ACTION_MY_PACKAGE_REPLACED == action) {
            rescheduleAll(context)
            return
        }

        val itemId = intent.getLongExtra(EXTRA_ITEM_ID, -1L)
        val itemTitle = intent.getStringExtra(EXTRA_ITEM_TITLE) ?: context.getString(R.string.medicine_label)
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, 0)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        when (action) {
            ACTION_SHOW -> {
                showNotification(context, notificationManager, itemId, itemTitle, notifId)
                val item = loadItem(context, itemId)
                if (item != null) {
                    scheduleNotification(context, item)
                }
            }
            ACTION_TAKEN -> {
                markAsTaken(context, itemId)
                notificationManager.cancel(notifId)
                Toast.makeText(context, context.getString(R.string.taken_message), Toast.LENGTH_SHORT).show()
            }
            ACTION_SKIP -> {
                notificationManager.cancel(notifId)
                Toast.makeText(context, context.getString(R.string.skipped_message), Toast.LENGTH_SHORT).show()
            }
            ACTION_SNOOZE -> {
                notificationManager.cancel(notifId)
                snoozeNotification(context, itemId, itemTitle, notifId)
                Toast.makeText(context, context.getString(R.string.snoozed_message), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun rescheduleAll(context: Context) {
        try {
            context.openFileInput(FILE_NAME).use {
                val list = ObjectInputStream(it).readObject() as ArrayList<MedItem>
                for (item in list) {
                    if (item.type == ItemType.Medicine) {
                        scheduleNotification(context, item)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadItem(context: Context, itemId: Long): MedItem? {
        try {
            context.openFileInput(FILE_NAME).use {
                val list = ObjectInputStream(it).readObject() as ArrayList<MedItem>
                return list.find { item -> item.id == itemId }
            }
        } catch (e: Exception) {
            return null
        }
    }

    private fun showNotification(
        context: Context,
        notificationManager: NotificationManager,
        itemId: Long,
        title: String,
        notifId: Int
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notif_channel_desc)
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val yesIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = ACTION_TAKEN
            putExtra(EXTRA_ITEM_ID, itemId)
            putExtra(EXTRA_NOTIF_ID, notifId)
        }
        val yesPending = PendingIntent.getBroadcast(context, notifId * 10 + 1, yesIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val skipIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = ACTION_SKIP
            putExtra(EXTRA_NOTIF_ID, notifId)
        }
        val skipPending = PendingIntent.getBroadcast(context, notifId * 10 + 2, skipIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val snoozeIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_ITEM_ID, itemId)
            putExtra(EXTRA_ITEM_TITLE, title)
            putExtra(EXTRA_NOTIF_ID, notifId)
        }
        val snoozePending = PendingIntent.getBroadcast(context, notifId * 10 + 3, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val contentIntent = Intent(context, MainActivity::class.java)
        val contentPending = PendingIntent.getActivity(context, 0, contentIntent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notif_title))
            .setContentText(context.getString(R.string.notif_content, title))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPending)
            .addAction(0, context.getString(R.string.action_yes), yesPending)
            .addAction(0, context.getString(R.string.action_snooze), snoozePending)
            .addAction(0, context.getString(R.string.action_skip), skipPending)

        notificationManager.notify(notifId, builder.build())
    }

    private fun snoozeNotification(context: Context, itemId: Long, title: String, notifId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val snoozeTime = System.currentTimeMillis() + 10 * 60 * 1000

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = ACTION_SHOW
            putExtra(EXTRA_ITEM_ID, itemId)
            putExtra(EXTRA_ITEM_TITLE, title)
            putExtra(EXTRA_NOTIF_ID, notifId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notifId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeTime, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeTime, pendingIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun markAsTaken(context: Context, itemId: Long) {
        try {
            val items = ArrayList<MedItem>()
            try {
                context.openFileInput(FILE_NAME).use {
                    val list = ObjectInputStream(it).readObject() as ArrayList<MedItem>
                    items.addAll(list)
                }
            } catch (e: Exception) { }

            val today = LocalDate.now()
            var modified = false
            for (i in items.indices) {
                if (items[i].id == itemId) {
                    val newHistory = HashMap(items[i].takenHistory)
                    newHistory[today] = LocalTime.now()
                    items[i] = items[i].copy(takenHistory = newHistory)
                    modified = true
                    break
                }
            }

            if (modified) {
                context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE).use {
                    ObjectOutputStream(it).writeObject(items)
                }
                context.sendBroadcast(Intent("com.fedeveloper95.med.REFRESH_DATA").setPackage(context.packageName))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
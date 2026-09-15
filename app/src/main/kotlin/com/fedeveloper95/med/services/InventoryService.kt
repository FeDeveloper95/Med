package com.fedeveloper95.med.services

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.fedeveloper95.med.MainActivity
import com.fedeveloper95.med.R

/**
 * Per-medication supply (inventory) tracking.
 *
 * Supply data lives on each [MedData] as:
 *  - [MedData.supplyDosesLeft]      current stock in doses (null = tracking off)
 *  - [MedData.supplyDosesPerRefill] how many doses a refill adds (e.g. 30 tablets)
 *  - [MedData.supplyLowThreshold]   fire one alert when dosesLeft <= this
 *  - [MedData.supplyAlertShown]     deduplication flag for the low-supply alert
 *
 * Doses are counted per calendar day (each MedData entry is one daily time slot),
 * so taking or un-taking a dose adjusts the stock by exactly one dose.
 *
 * Low-supply alerts use per-item notification IDs so multiple medications can be
 * low at the same time without overwriting each other, and stale alerts are
 * cancelled when stock is refilled or the item is edited/deleted.
 */
object InventoryService {

    const val INVENTORY_CHANNEL_ID = "med_inventory_v1"

    /** Base for per-item alert IDs; dose alarms use `item.id.toInt()` directly. */
    private const val ALERT_ID_BASE = 900000000

    private fun alertId(item: MedData): Int = ALERT_ID_BASE + (item.id % 100000).toInt()

    /**
     * Applies the stock change for a dose being logged or un-logged.
     * Returns the updated item; the caller is responsible for persisting it.
     */
    fun applyInventoryChange(context: Context, item: MedData, isTaken: Boolean): MedData {
        val left = item.supplyDosesLeft ?: return item
        val updated = item.copy(supplyDosesLeft = (left + if (isTaken) -1 else 1).coerceAtLeast(0))
        return evaluateItem(context, updated)
    }

    /**
     * Evaluates one item against its threshold: posts the low-supply alert when the
     * stock is newly at/below it, and cancels a stale alert once stock is refilled.
     * Returns the item with [MedData.supplyAlertShown] updated accordingly.
     */
    fun evaluateItem(context: Context, item: MedData): MedData {
        val left = item.supplyDosesLeft ?: return item
        val threshold = item.supplyLowThreshold ?: return item
        if (left > threshold) {
            cancelLowSupplyNotification(context, item)
            return item
        }
        if (item.supplyAlertShown) return item
        postLowSupplyNotification(context, item)
        return item.copy(supplyAlertShown = true)
    }

    /**
     * Evaluates every item in place — used after app start, boot/reinstall, or
     * import so a supply that is *already* low alerts even without a dose event.
     * Within one pass only the first entry per title alerts (dose slots of one
     * medication share a title). Returns true when any item changed and the list
     * should be persisted.
     */
    fun evaluateAll(context: Context, items: MutableList<MedData>): Boolean {
        var changed = false
        val alertedTitles = mutableSetOf<String>()
        for (i in items.indices) {
            val item = items[i]
            val left = item.supplyDosesLeft ?: continue
            val threshold = item.supplyLowThreshold
            if (threshold != null && left <= threshold && !item.supplyAlertShown &&
                alertedTitles.add(item.title)
            ) {
                items[i] = evaluateItem(context, item)
                changed = true
            }
        }
        return changed
    }

    private fun postLowSupplyNotification(context: Context, item: MedData) {
        val remaining = item.supplyDosesPerRefill ?: 0
        val text = if (remaining > 0) {
            context.getString(R.string.inventory_low_desc_refill, item.supplyDosesLeft ?: 0, remaining)
        } else {
            context.getString(R.string.inventory_low_desc, item.supplyDosesLeft ?: 0)
        }

        val contentIntent = PendingIntent.getActivity(
            context,
            item.id.toInt(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, INVENTORY_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.inventory_low_title, item.title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(alertId(item), notification)
    }

    /** Clears the low-supply alert notification for this item, if one is showing. */
    fun cancelLowSupplyNotification(context: Context, item: MedData) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(alertId(item))
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = android.app.NotificationChannel(
                INVENTORY_CHANNEL_ID,
                context.getString(R.string.inventory_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.inventory_channel_desc)
            }
            nm.createNotificationChannel(channel)
        }
    }
}

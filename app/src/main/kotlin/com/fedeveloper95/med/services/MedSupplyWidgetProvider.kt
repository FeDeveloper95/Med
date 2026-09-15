package com.fedeveloper95.med.services

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import com.fedeveloper95.med.ItemType
import com.fedeveloper95.med.MainActivity
import com.fedeveloper95.med.R

/**
 * Home-screen widget that always shows the medication with the lowest supply.
 *
 * Refreshed automatically from [DataRepository.saveData] (every data mutation
 * flows through it) and by the system on boot / widget add via [onUpdate].
 */
class MedSupplyWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        /** Re-renders every placed supply widget. Safe to call from any thread. */
        fun updateAll(context: Context) {
            try {
                val manager = AppWidgetManager.getInstance(context) ?: return
                val ids = manager.getAppWidgetIds(
                    ComponentName(context, MedSupplyWidgetProvider::class.java)
                )
                for (appWidgetId in ids) {
                    updateWidget(context, manager, appWidgetId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun updateWidget(
            context: Context,
            manager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val items = try {
                DataRepository.loadData(context)
            } catch (e: Exception) {
                emptyList()
            }

            val tracked = items.filter {
                it.type == ItemType.Medicine && it.supplyDosesLeft != null
            }

            val views = RemoteViews(context.packageName, R.layout.med_supply_widget)

            if (tracked.isEmpty()) {
                views.setViewVisibility(R.id.widget_content, android.view.View.GONE)
                views.setViewVisibility(R.id.widget_empty, android.view.View.VISIBLE)
            } else {
                val lowest = tracked.minByOrNull { it.supplyDosesLeft ?: 0 }!!
                val left = lowest.supplyDosesLeft ?: 0
                val low = lowest.supplyLowThreshold != null &&
                        left <= lowest.supplyLowThreshold!!

                views.setViewVisibility(R.id.widget_content, android.view.View.VISIBLE)
                views.setViewVisibility(R.id.widget_empty, android.view.View.GONE)
                views.setTextViewText(R.id.widget_title, lowest.title)
                views.setTextViewText(
                    R.id.widget_count,
                    context.getString(R.string.supply_badge_format, left)
                )
                views.setTextColor(
                    R.id.widget_count,
                    if (low) context.getColor(R.color.widget_low) else Color.WHITE
                )

                val refill = lowest.supplyDosesPerRefill ?: 0
                if (refill > 0) {
                    views.setViewVisibility(R.id.widget_progress, android.view.View.VISIBLE)
                    views.setProgressBar(
                        R.id.widget_progress,
                        refill,
                        left.coerceIn(0, refill),
                        false
                    )
                } else {
                    views.setViewVisibility(R.id.widget_progress, android.view.View.GONE)
                }
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            manager.updateAppWidget(appWidgetId, views)
        }
    }
}

package com.aliadas.utils

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import android.util.Log

/**
 * Obtiene el último desbloqueo real del teléfono.
 *
 * En Android 9+ se consulta KEYGUARD_HIDDEN, que sigue disponible aunque Aliadas
 * no estuviera abierta en el momento del desbloqueo. ACTION_USER_PRESENT se
 * conserva como respaldo para fabricantes que no exponen todos los eventos.
 */
object LastUnlockManager {
    private const val PREFS_NAME = "ConfiguracionApp"
    private const val LAST_UNLOCK_KEY = "ultima_vez"
    private const val LOOKBACK_MILLIS = 7L * 24 * 60 * 60 * 1000

    fun recordUnlock(context: Context, timestamp: Long = System.currentTimeMillis()) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(LAST_UNLOCK_KEY, timestamp)
            .apply()
    }

    fun getLastUnlockTime(context: Context): Long {
        val savedTime = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(LAST_UNLOCK_KEY, 0L)
        val usageStatsTime = queryLastKeyguardHidden(context)
        val mostRecent = maxOf(savedTime, usageStatsTime)

        if (mostRecent > savedTime) {
            recordUnlock(context, mostRecent)
        }
        return mostRecent
    }

    fun hasUsageAccess(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return true

        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun queryLastKeyguardHidden(context: Context): Long {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P || !hasUsageAccess(context)) {
            return 0L
        }

        return try {
            val now = System.currentTimeMillis()
            val usageStatsManager =
                context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val events = usageStatsManager.queryEvents(now - LOOKBACK_MILLIS, now)
            val event = UsageEvents.Event()
            var latest = 0L

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.KEYGUARD_HIDDEN) {
                    latest = maxOf(latest, event.timeStamp)
                }
            }
            latest
        } catch (e: Exception) {
            Log.w("ALIADAS_UNLOCK", "No fue posible consultar el último desbloqueo", e)
            0L
        }
    }
}

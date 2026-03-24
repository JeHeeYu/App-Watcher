package app.appwatcher.startup

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import java.lang.SecurityException

object AppMonitorScheduler {
    private const val REQUEST_CODE_MONITOR = 41_000

    fun scheduleNextCheck(
        context: Context,
        reason: String,
        delayMsOverride: Long? = null
    ) {
        val appContext = context.applicationContext
        val preferences = AutoLaunchPreferences(appContext)
        val selectedPackages = preferences.selectedPackages()
        if (!preferences.monitorEnabled() || selectedPackages.isEmpty()) {
            cancel(appContext)
            return
        }

        val pendingIntent = monitorPendingIntent(
            appContext,
            reason,
            PendingIntent.FLAG_UPDATE_CURRENT
        ) ?: return
        val triggerAtMillis = SystemClock.elapsedRealtime() + (delayMsOverride ?: preferences.monitorIntervalMs())
        val alarmManager = appContext.getSystemService(AlarmManager::class.java)
        scheduleAlarm(alarmManager, triggerAtMillis, pendingIntent)
    }

    fun cancel(context: Context) {
        val appContext = context.applicationContext
        val pendingIntent = monitorPendingIntent(appContext, "cancel", PendingIntent.FLAG_NO_CREATE)
        if (pendingIntent != null) {
            val alarmManager = appContext.getSystemService(AlarmManager::class.java)
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun monitorPendingIntent(
        context: Context,
        reason: String,
        flag: Int
    ): PendingIntent? {
        val intent = Intent(context, AppMonitorReceiver::class.java).apply {
            putExtra(AppMonitorReceiver.EXTRA_REASON, reason)
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_MONITOR,
            intent,
            flag or immutableFlag()
        )
    }

    private fun immutableFlag(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0

    private fun scheduleAlarm(
        alarmManager: AlarmManager,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent
    ) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        }
    }
}

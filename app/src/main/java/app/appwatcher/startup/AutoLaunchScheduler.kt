package app.appwatcher.startup

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.os.SystemClock
import java.lang.SecurityException

object AutoLaunchScheduler {
    private const val REQUEST_CODE_BASE = 20_000
    private const val QUICK_RETRY_DELAY_MS = 5_000L

    fun scheduleRegisteredPackages(
        context: Context,
        reason: String,
        initialDelayMsOverride: Long? = null
    ) {
        val preferences = AutoLaunchPreferences(context)
        val packages = preferences.selectedPackages()
        schedulePackages(
            context = context,
            packages = packages,
            initialDelayMs = initialDelayMsOverride ?: preferences.initialDelayMs(),
            betweenDelayMs = preferences.betweenDelayMs(),
            reason = reason
        )
    }

    fun schedulePackages(
        context: Context,
        packages: List<String>,
        initialDelayMs: Long,
        betweenDelayMs: Long,
        reason: String
    ) {
        val appContext = context.applicationContext
        cancelScheduledPackages(appContext)

        if (packages.isEmpty()) return

        val alarmManager = appContext.getSystemService(AlarmManager::class.java)
        packages.forEachIndexed { index, packageName ->
            val delayMs = initialDelayMs + (betweenDelayMs * index)
            val triggerAtMillis = SystemClock.elapsedRealtime() + delayMs
            val pendingIntent = PendingIntent.getActivity(
                appContext,
                requestCodeFor(index),
                LaunchProxyActivity.createIntent(
                    context = appContext,
                    packageName = packageName,
                    reason = reason,
                    sequenceIndex = index
                ),
                PendingIntent.FLAG_UPDATE_CURRENT or immutableFlag()
            )

            scheduleExact(
                alarmManager = alarmManager,
                triggerAtMillis = triggerAtMillis,
                pendingIntent = pendingIntent
            )
        }
    }

    fun rescheduleSinglePackage(
        context: Context,
        packageName: String,
        delayMs: Long,
        reason: String
    ) {
        val appContext = context.applicationContext
        val requestCode = REQUEST_CODE_BASE + packageName.hashCode()
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            requestCode,
            LaunchProxyActivity.createIntent(
                context = appContext,
                packageName = packageName,
                reason = reason,
                sequenceIndex = -1
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or immutableFlag()
        )
        val alarmManager = appContext.getSystemService(AlarmManager::class.java)
        scheduleExact(
            alarmManager = alarmManager,
            triggerAtMillis = SystemClock.elapsedRealtime() + delayMs,
            pendingIntent = pendingIntent
        )
    }

    fun scheduleQuickRetry(context: Context, reason: String) {
        val preferences = AutoLaunchPreferences(context)
        val packages = preferences.selectedPackages()
        if (packages.isEmpty()) return

        schedulePackages(
            context = context,
            packages = packages,
            initialDelayMs = QUICK_RETRY_DELAY_MS,
            betweenDelayMs = preferences.betweenDelayMs(),
            reason = reason
        )
    }

    private fun cancelScheduledPackages(context: Context) {
        val appContext = context.applicationContext
        val packages = AutoLaunchPreferences(appContext).selectedPackages()
        val alarmManager = appContext.getSystemService(AlarmManager::class.java)
        packages.indices.forEach { index ->
            val pendingIntent = PendingIntent.getActivity(
                appContext,
                requestCodeFor(index),
                LaunchProxyActivity.createIntent(
                    context = appContext,
                    packageName = "",
                    reason = "cancel",
                    sequenceIndex = index
                ),
                PendingIntent.FLAG_NO_CREATE or immutableFlag()
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    private fun requestCodeFor(index: Int): Int = REQUEST_CODE_BASE + index

    private fun immutableFlag(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0

    private fun scheduleExact(
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
        } catch (throwable: SecurityException) {
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

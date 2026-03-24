package app.appwatcher.startup

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AppMonitorReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        val preferences = AutoLaunchPreferences(appContext)
        val selectedPackages = preferences.selectedPackages()

        if (!preferences.monitorEnabled() || selectedPackages.isEmpty()) {
            AppMonitorScheduler.cancel(appContext)
            return
        }

        val deadPackages = selectedPackages.filterNot { packageName ->
            AppProcessWatcher.isPackageRunning(appContext, packageName)
        }

        if (deadPackages.isNotEmpty()) {
            AutoLaunchScheduler.schedulePackages(
                context = appContext,
                packages = deadPackages,
                initialDelayMs = 0L,
                betweenDelayMs = preferences.betweenDelayMs(),
                reason = "monitor_restart:${intent.getStringExtra(EXTRA_REASON).orEmpty()}"
            )
        }

        AppMonitorScheduler.scheduleNextCheck(
            context = appContext,
            reason = "monitor_loop"
        )
    }

    companion object {
        const val EXTRA_REASON = "extra_reason"
    }
}

package app.appwatcher.startup

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val preferences = AutoLaunchPreferences(context)
        val selectedPackages = preferences.selectedPackages()
        if (selectedPackages.isEmpty()) return

        val reason = intent.action ?: "unknown_boot_event"

        val directLaunchIntent = LaunchProxyActivity.createIntent(
            context = context,
            packageName = selectedPackages.first(),
            reason = "direct:$reason",
            sequenceIndex = 0
        )
        try {
            context.startActivity(directLaunchIntent)
        } catch (throwable: RuntimeException) {
        }

        AutoLaunchScheduler.scheduleQuickRetry(
            context = context,
            reason = "quick_retry:$reason"
        )
        AutoLaunchScheduler.scheduleRegisteredPackages(
            context = context,
            reason = reason
        )
        AppMonitorScheduler.scheduleNextCheck(
            context = context,
            reason = "boot:$reason"
        )
    }
}

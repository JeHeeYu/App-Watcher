package app.appwatcher.startup

import android.app.Activity
import android.app.KeyguardManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle

class LaunchProxyActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME).orEmpty()
        val reason = intent.getStringExtra(EXTRA_REASON).orEmpty()
        intent.getIntExtra(EXTRA_SEQUENCE_INDEX, -1)

        if (packageName.isBlank()) {
            finishWithoutAnimation()
            return
        }

        val keyguardManager = getSystemService(KeyguardManager::class.java)
        val isDeviceLocked = keyguardManager?.isDeviceLocked == true
        if (isDeviceLocked) {
            AutoLaunchScheduler.rescheduleSinglePackage(
                context = this,
                packageName = packageName,
                delayMs = 10_000L,
                reason = "locked_retry:$reason"
            )
            finishWithoutAnimation()
            return
        }

        launchTarget(packageName)
        finishWithoutAnimation()
    }

    private fun launchTarget(packageName: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName) ?: return

        launchIntent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or
                Intent.FLAG_ACTIVITY_NO_ANIMATION
        )

        try {
            startActivity(launchIntent)
        } catch (throwable: ActivityNotFoundException) {
        } catch (throwable: SecurityException) {
        } catch (throwable: RuntimeException) {
        }
    }

    private fun finishWithoutAnimation() {
        finish()
        overridePendingTransition(0, 0)
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_REASON = "extra_reason"
        const val EXTRA_SEQUENCE_INDEX = "extra_sequence_index"

        fun createIntent(
            context: Context,
            packageName: String,
            reason: String,
            sequenceIndex: Int
        ): Intent {
            return Intent(context, LaunchProxyActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                putExtra(EXTRA_PACKAGE_NAME, packageName)
                putExtra(EXTRA_REASON, reason)
                putExtra(EXTRA_SEQUENCE_INDEX, sequenceIndex)
            }
        }
    }
}

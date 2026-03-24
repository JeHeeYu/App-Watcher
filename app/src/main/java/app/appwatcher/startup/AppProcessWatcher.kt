package app.appwatcher.startup

import android.app.ActivityManager
import android.content.Context

object AppProcessWatcher {
    fun isPackageRunning(context: Context, packageName: String): Boolean {
        val normalizedPackageName = packageName.trim()
        if (normalizedPackageName.isEmpty()) return false

        val activityManager = context.getSystemService(ActivityManager::class.java)
        val runningProcesses = activityManager?.runningAppProcesses.orEmpty()
        return runningProcesses.any { processInfo ->
            processInfo.processName == normalizedPackageName ||
                processInfo.pkgList?.contains(normalizedPackageName) == true
        }
    }
}

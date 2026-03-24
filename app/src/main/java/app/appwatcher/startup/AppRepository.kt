package app.appwatcher.startup

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import app.appwatcher.model.LaunchableApp

class AppRepository(private val context: Context) {
    private val packageManager: PackageManager = context.packageManager

    fun loadLaunchableApps(): List<LaunchableApp> {
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager
            .queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
            .asSequence()
            .mapNotNull { resolveInfo ->
                val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null
                val packageName = activityInfo.packageName
                if (packageName == context.packageName) return@mapNotNull null
                val label = resolveInfo.loadLabel(packageManager)?.toString().orEmpty().ifBlank { packageName }
                val icon = resolveInfo.loadIcon(packageManager)
                LaunchableApp(packageName = packageName, label = label, icon = icon)
            }
            .distinctBy { it.packageName }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER, LaunchableApp::label))
            .toList()
    }

    fun findLaunchableApp(packageName: String): LaunchableApp? {
        val normalizedPackageName = packageName.trim()
        if (normalizedPackageName.isEmpty()) return null

        val launchIntent = packageManager.getLaunchIntentForPackage(normalizedPackageName) ?: return null
        val appInfo = try {
            packageManager.getApplicationInfo(normalizedPackageName, 0)
        } catch (_: PackageManager.NameNotFoundException) {
            return null
        }

        val label = packageManager.getApplicationLabel(appInfo).toString().ifBlank {
            normalizedPackageName
        }

        if (launchIntent.component == null) return null

        return LaunchableApp(
            packageName = normalizedPackageName,
            label = label,
            icon = packageManager.getApplicationIcon(appInfo)
        )
    }
}

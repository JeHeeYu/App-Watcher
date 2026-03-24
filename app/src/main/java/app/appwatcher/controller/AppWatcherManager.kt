package app.appwatcher.controller

import android.app.AlarmManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import app.appwatcher.model.LaunchableApp
import app.appwatcher.startup.AppRepository
import app.appwatcher.startup.AutoLaunchPreferences

data class PermissionStatus(
    val canDrawOverlays: Boolean,
    val canScheduleExactAlarms: Boolean,
    val ignoresBatteryOptimizations: Boolean,
    val notificationsAllowed: Boolean
) {
    val grantedCount: Int
        get() = listOf(
            canDrawOverlays,
            canScheduleExactAlarms,
            ignoresBatteryOptimizations,
            notificationsAllowed
        ).count { it }
}

data class ScreenData(
    val permissions: PermissionStatus,
    val availableApps: List<LaunchableApp>,
    val registeredApps: List<LaunchableApp>,
    val initialDelayInput: String,
    val betweenDelayInput: String
)

data class PackageMutationResult(
    val message: String,
    val registeredApps: List<LaunchableApp>
)

class AppWatcherManager(private val context: Context) {
    private val preferences = AutoLaunchPreferences(context)
    private val repository = AppRepository(context)

    fun permissionStatus(): PermissionStatus {
        return PermissionStatus(
            canDrawOverlays = Settings.canDrawOverlays(context),
            canScheduleExactAlarms = canScheduleExactAlarms(context),
            ignoresBatteryOptimizations = isIgnoringBatteryOptimizations(context),
            notificationsAllowed = areNotificationsAllowed(context)
        )
    }

    fun screenData(): ScreenData {
        return ScreenData(
            permissions = permissionStatus(),
            availableApps = availableApps(),
            registeredApps = registeredApps(),
            initialDelayInput = (preferences.initialDelayMs() / 1000L).toString(),
            betweenDelayInput = (preferences.betweenDelayMs() / 1000L).toString()
        )
    }

    fun availableApps(): List<LaunchableApp> = repository.loadLaunchableApps()

    fun registeredApps(): List<LaunchableApp> {
        return preferences.selectedPackages().mapNotNull(repository::findLaunchableApp)
    }

    fun registerApp(launchableApp: LaunchableApp): PackageMutationResult {
        val updatedPackages = preferences.selectedPackages().toMutableList()
        if (launchableApp.packageName in updatedPackages) {
            return PackageMutationResult(
                message = "이미 등록된 패키지입니다.",
                registeredApps = registeredApps()
            )
        }

        updatedPackages.add(launchableApp.packageName)
        preferences.setSelectedPackages(updatedPackages)
        return PackageMutationResult(
            message = "패키지를 등록했습니다.",
            registeredApps = registeredApps()
        )
    }

    fun removePackage(packageName: String): PackageMutationResult {
        val updatedPackages = preferences.selectedPackages().toMutableList().apply {
            remove(packageName)
        }
        preferences.setSelectedPackages(updatedPackages)
        return PackageMutationResult(
            message = "패키지를 제거했습니다.",
            registeredApps = registeredApps()
        )
    }

    fun updateInitialDelay(input: String) {
        input.toLongOrNull()?.let { seconds ->
            preferences.setInitialDelayMs(seconds * 1000L)
        }
    }

    fun updateBetweenDelay(input: String) {
        input.toLongOrNull()?.let { seconds ->
            preferences.setBetweenDelayMs(seconds * 1000L)
        }
    }
}

private fun canScheduleExactAlarms(context: Context): Boolean {
    val alarmManager = context.getSystemService(AlarmManager::class.java)
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        alarmManager.canScheduleExactAlarms()
    } else {
        true
    }
}

private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val powerManager = context.getSystemService(PowerManager::class.java)
    return powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
}

private fun areNotificationsAllowed(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

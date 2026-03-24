package app.appwatcher.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.ImageView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import app.appwatcher.controller.AppWatcherManager
import app.appwatcher.controller.PermissionStatus
import app.appwatcher.model.LaunchableApp
import app.appwatcher.ui.components.PermissionOverviewCard
import app.appwatcher.ui.theme.AppWatcherTheme

@Composable
fun AppWatcherScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val manager = remember { AppWatcherManager(context) }

    var permissions by remember {
        mutableStateOf(
            PermissionStatus(
                canDrawOverlays = false,
                canScheduleExactAlarms = false,
                ignoresBatteryOptimizations = false,
                notificationsAllowed = false
            )
        )
    }
    var message by remember { mutableStateOf<String?>(null) }
    var availableApps by remember { mutableStateOf(emptyList<LaunchableApp>()) }
    var registeredApps by remember { mutableStateOf(emptyList<LaunchableApp>()) }
    var initialDelayInput by remember { mutableStateOf("0") }
    var betweenDelayInput by remember { mutableStateOf("0") }
    var monitorEnabled by remember { mutableStateOf(true) }
    var monitorIntervalInput by remember { mutableStateOf("60") }

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { }

    fun refreshScreenData() {
        val screenData = manager.screenData()
        permissions = screenData.permissions
        availableApps = screenData.availableApps
        registeredApps = screenData.registeredApps
        initialDelayInput = screenData.initialDelayInput
        betweenDelayInput = screenData.betweenDelayInput
        monitorEnabled = screenData.monitorEnabled
        monitorIntervalInput = screenData.monitorIntervalInput
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        refreshScreenData()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshScreenData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val registeredPackageNames = registeredApps.map { it.packageName }.toSet()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "AppWatch",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            PermissionOverviewCard(
                grantedCount = permissions.grantedCount,
                totalCount = 4,
                overlayEnabled = permissions.canDrawOverlays,
                exactAlarmEnabled = permissions.canScheduleExactAlarms,
                batteryIgnoreEnabled = permissions.ignoresBatteryOptimizations,
                notificationsEnabled = permissions.notificationsAllowed,
                onOverlayClick = {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        android.net.Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                },
                onExactAlarmClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = android.net.Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    }
                },
                onBatteryIgnoreClick = {
                    val intent = Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        android.net.Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                },
                onNotificationsClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        context.startActivity(intent)
                    }
                }
            )
        }
        item {
            message?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DelaySettings(
                        initialDelayInput = initialDelayInput,
                        betweenDelayInput = betweenDelayInput,
                        monitorEnabled = monitorEnabled,
                        monitorIntervalInput = monitorIntervalInput,
                        onInitialDelayChanged = {
                            initialDelayInput = it
                            manager.updateInitialDelay(it)
                        },
                        onBetweenDelayChanged = {
                            betweenDelayInput = it
                            manager.updateBetweenDelay(it)
                        },
                        onMonitorEnabledChanged = {
                            monitorEnabled = it
                            manager.updateMonitorEnabled(it)
                        },
                        onMonitorIntervalChanged = {
                            monitorIntervalInput = it
                            manager.updateMonitorInterval(it)
                        }
                    )
                }
            }
        }
        item {
            SectionHeaderCard(
                title = "등록된 앱 ${registeredApps.size}개"
            )
        }
        items(registeredApps, key = { "registered:${it.packageName}" }) { registeredApp ->
            AppSelectionRow(
                app = registeredApp,
                selected = true,
                onToggle = {
                    val result = manager.removePackage(registeredApp.packageName)
                    registeredApps = result.registeredApps
                    message = result.message
                }
            )
        }
        item {
            SectionHeaderCard(
                title = "설치된 앱 ${availableApps.size}개"
            )
        }
        items(availableApps, key = { "available:${it.packageName}" }) { availableApp ->
            AppSelectionRow(
                app = availableApp,
                selected = availableApp.packageName in registeredPackageNames,
                onToggle = {
                    if (availableApp.packageName in registeredPackageNames) {
                        val result = manager.removePackage(availableApp.packageName)
                        registeredApps = result.registeredApps
                        message = result.message
                    } else {
                        val result = manager.registerApp(availableApp)
                        registeredApps = result.registeredApps
                        message = result.message
                    }
                }
            )
        }
        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppWatcherScreenPreview() {
    AppWatcherTheme {
        Surface {
            AppWatcherScreen()
        }
    }
}

@Composable
private fun DelaySettings(
    initialDelayInput: String,
    betweenDelayInput: String,
    monitorEnabled: Boolean,
    monitorIntervalInput: String,
    onInitialDelayChanged: (String) -> Unit,
    onBetweenDelayChanged: (String) -> Unit,
    onMonitorEnabledChanged: (Boolean) -> Unit,
    onMonitorIntervalChanged: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "실행 타이밍",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        OutlinedTextField(
            value = initialDelayInput,
            onValueChange = onInitialDelayChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("부팅 후 첫 실행까지(초)") },
            singleLine = true
        )
        OutlinedTextField(
            value = betweenDelayInput,
            onValueChange = onBetweenDelayChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("다음 앱 실행 간격(초)") },
            singleLine = true
        )
        HorizontalDivider()
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = "상태 체크",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (monitorEnabled) "죽으면 다시 실행" else "체크 안 함",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                modifier = Modifier.scale(0.82f),
                checked = monitorEnabled,
                onCheckedChange = onMonitorEnabledChanged
            )
        }
        OutlinedTextField(
            value = monitorIntervalInput,
            onValueChange = onMonitorIntervalChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("체크 간격(초)") },
            singleLine = true
        )
    }
}

@Composable
private fun SectionHeaderCard(title: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun AppSelectionRow(
    app: LaunchableApp,
    selected: Boolean,
    onToggle: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 76.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AndroidView(
                    factory = { imageContext ->
                        ImageView(imageContext).apply {
                            scaleType = ImageView.ScaleType.CENTER_CROP
                        }
                    },
                    modifier = Modifier.size(40.dp),
                    update = { imageView ->
                        imageView.setImageDrawable(app.icon)
                    }
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            TextButton(
                modifier = Modifier.align(Alignment.CenterEnd),
                onClick = onToggle
            ) {
                Text(
                    text = if (selected) "제거" else "등록",
                    color = if (selected) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }
        }
    }
}

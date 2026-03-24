package app.appwatcher

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import app.appwatcher.model.LaunchableApp
import app.appwatcher.startup.AppRepository
import app.appwatcher.startup.AutoLaunchPreferences
import app.appwatcher.startup.AutoLaunchScheduler
import app.appwatcher.ui.theme.AppWatcherTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppWatcherTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AutoStartManagerScreen(
                        modifier = Modifier.padding(innerPadding),
                        onRunNow = { AutoLaunchScheduler.scheduleRegisteredPackages(this, "manual_button", 0L) },
                        onOpenBatterySettings = {
                            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AutoStartManagerScreen(
    modifier: Modifier = Modifier,
    onRunNow: () -> Unit = {},
    onOpenBatterySettings: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val preferences = remember { AutoLaunchPreferences(context) }
    val repository = remember { AppRepository(context) }
    val canDrawOverlays = remember { mutableStateOf(false) }
    val canScheduleExactAlarms = remember { mutableStateOf(false) }
    val ignoresBatteryOptimizations = remember { mutableStateOf(false) }
    val notificationsAllowed = remember { mutableStateOf(false) }
    var packageInput by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var registeredApps by remember { mutableStateOf(emptyList<LaunchableApp>()) }
    var initialDelayInput by remember { mutableStateOf((preferences.initialDelayMs() / 1000L).toString()) }
    var betweenDelayInput by remember { mutableStateOf((preferences.betweenDelayMs() / 1000L).toString()) }
    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { }

    fun refreshPermissionState() {
        canDrawOverlays.value = Settings.canDrawOverlays(context)
        canScheduleExactAlarms.value = canScheduleExactAlarms(context)
        ignoresBatteryOptimizations.value = isIgnoringBatteryOptimizations(context)
        notificationsAllowed.value = areNotificationsAllowed(context)
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        refreshPermissionState()
        registeredApps = withContext(Dispatchers.IO) {
            preferences.selectedPackages().mapNotNull(repository::findLaunchableApp)
        }
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermissionState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "자동 실행 패키지 등록",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "패키지명을 등록해 두면 부팅 후 시스템 예약 실행으로 등록 순서대로 실행을 시도합니다.",
            style = MaterialTheme.typography.bodyMedium
        )
        PermissionStatusCard(
            title = "오버레이 권한",
            enabled = canDrawOverlays.value,
            buttonText = "오버레이 허용",
            onClick = {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:${context.packageName}")
                )
                context.startActivity(intent)
            }
        )
        PermissionStatusCard(
            title = "정확한 알람",
            enabled = canScheduleExactAlarms.value,
            buttonText = "알람 권한 열기",
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = android.net.Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                }
            }
        )
        PermissionStatusCard(
            title = "배터리 최적화 제외",
            enabled = ignoresBatteryOptimizations.value,
            buttonText = "배터리 예외 요청",
            onClick = {
                val intent = Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    android.net.Uri.parse("package:${context.packageName}")
                )
                context.startActivity(intent)
            }
        )
        PermissionStatusCard(
            title = "알림 권한",
            enabled = notificationsAllowed.value,
            buttonText = "알림 허용",
            onClick = {
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
        OutlinedTextField(
            value = packageInput,
            onValueChange = {
                packageInput = it
                message = null
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("패키지명") },
            placeholder = { Text("예: com.kakao.talk") },
            singleLine = true
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    val launchableApp = repository.findLaunchableApp(packageInput)
                    if (launchableApp == null) {
                        message = "설치되어 있고 실행 가능한 패키지인지 확인해 주세요."
                        return@Button
                    }

                    val updatedPackages = preferences.selectedPackages().toMutableList()
                    if (launchableApp.packageName in updatedPackages) {
                        message = "이미 등록된 패키지입니다."
                        return@Button
                    }

                    updatedPackages.add(launchableApp.packageName)
                    preferences.setSelectedPackages(updatedPackages)
                    registeredApps = updatedPackages.mapNotNull(repository::findLaunchableApp)
                    packageInput = ""
                    message = "패키지를 등록했습니다."
                }
            ) {
                Text("패키지 등록")
            }
            Button(onClick = onRunNow) {
                Text("지금 실행")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onOpenBatterySettings) {
                Text("배터리 설정")
            }
        }
        message?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        DelaySettings(
            initialDelayInput = initialDelayInput,
            betweenDelayInput = betweenDelayInput,
            onInitialDelayChanged = {
                initialDelayInput = it
                it.toLongOrNull()?.let { seconds ->
                    preferences.setInitialDelayMs(seconds * 1000L)
                }
            },
            onBetweenDelayChanged = {
                betweenDelayInput = it
                it.toLongOrNull()?.let { seconds ->
                    preferences.setBetweenDelayMs(seconds * 1000L)
                }
            }
        )
        Text(
            text = "등록된 패키지 ${registeredApps.size}개",
            style = MaterialTheme.typography.titleMedium
        )
        HorizontalDivider()
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(registeredApps, key = { it.packageName }) { registeredApp ->
                RegisteredPackageRow(
                    app = registeredApp,
                    onRemove = {
                        val updatedPackages = preferences.selectedPackages().toMutableList().apply {
                            remove(registeredApp.packageName)
                        }
                        preferences.setSelectedPackages(updatedPackages)
                        registeredApps = updatedPackages.mapNotNull(repository::findLaunchableApp)
                        message = "패키지를 제거했습니다."
                    }
                )
            }
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AppWatcherTheme {
        Surface {
            AutoStartManagerScreen()
        }
    }
}

@Composable
private fun DelaySettings(
    initialDelayInput: String,
    betweenDelayInput: String,
    onInitialDelayChanged: (String) -> Unit,
    onBetweenDelayChanged: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = initialDelayInput,
            onValueChange = onInitialDelayChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("부팅 후 대기 시간(초)") },
            singleLine = true
        )
        OutlinedTextField(
            value = betweenDelayInput,
            onValueChange = onBetweenDelayChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("앱 사이 딜레이(초)") },
            singleLine = true
        )
    }
}

@Composable
private fun RegisteredPackageRow(
    app: LaunchableApp,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(text = app.label, style = MaterialTheme.typography.bodyLarge)
            Text(text = app.packageName, style = MaterialTheme.typography.bodySmall)
        }
        TextButton(onClick = onRemove) {
            Text("삭제", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun PermissionStatusCard(
    title: String,
    enabled: Boolean,
    buttonText: String,
    onClick: () -> Unit
) {
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = if (enabled) "허용됨" else "필요함",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
            }
            Button(onClick = onClick) {
                Text(buttonText)
            }
        }
    }
}

private fun canScheduleExactAlarms(context: android.content.Context): Boolean {
    val alarmManager = context.getSystemService(AlarmManager::class.java)
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        alarmManager.canScheduleExactAlarms()
    } else {
        true
    }
}

private fun isIgnoringBatteryOptimizations(context: android.content.Context): Boolean {
    val powerManager = context.getSystemService(PowerManager::class.java)
    return powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
}

private fun areNotificationsAllowed(context: android.content.Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

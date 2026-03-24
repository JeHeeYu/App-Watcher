package app.appwatcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PermissionOverviewCard(
    grantedCount: Int,
    totalCount: Int,
    overlayEnabled: Boolean,
    exactAlarmEnabled: Boolean,
    batteryIgnoreEnabled: Boolean,
    notificationsEnabled: Boolean,
    onOverlayClick: () -> Unit,
    onExactAlarmClick: () -> Unit,
    onBatteryIgnoreClick: () -> Unit,
    onNotificationsClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column {
                Text(
                    text = "권한 준비",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "$grantedCount/$totalCount 완료",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (grantedCount == totalCount) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
            }
            PermissionSummaryItem(
                label = "오버레이",
                enabled = overlayEnabled,
                onClick = onOverlayClick
            )
            PermissionSummaryItem(
                label = "정확한 알람",
                enabled = exactAlarmEnabled,
                onClick = onExactAlarmClick
            )
            PermissionSummaryItem(
                label = "배터리 최적화",
                enabled = batteryIgnoreEnabled,
                onClick = onBatteryIgnoreClick
            )
            PermissionSummaryItem(
                label = "알림",
                enabled = notificationsEnabled,
                onClick = onNotificationsClick
            )
        }
    }
}

@Composable
private fun PermissionSummaryItem(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .clip(CardDefaults.shape)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(8.dp)
                        .clip(CardDefaults.shape)
                        .background(
                            if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Text(
                text = if (enabled) "완료" else "허용",
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
    }
}

package app.appwatcher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val AppWatcherColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = AppSurface,
    primaryContainer = PrimaryBlueLight,
    onPrimaryContainer = PrimaryBlueDark,
    secondary = PrimaryBlueDark,
    onSecondary = AppSurface,
    secondaryContainer = AppSurfaceMuted,
    onSecondaryContainer = AppTextPrimary,
    tertiary = PrimaryBlue,
    onTertiary = AppSurface,
    background = AppBackground,
    onBackground = AppTextPrimary,
    surface = AppSurface,
    onSurface = AppTextPrimary,
    surfaceVariant = AppSurfaceMuted,
    onSurfaceVariant = AppTextSecondary,
    error = AppError,
    onError = AppSurface
)

@Composable
fun AppWatcherTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppWatcherColorScheme,
        typography = Typography,
        content = content
    )
}

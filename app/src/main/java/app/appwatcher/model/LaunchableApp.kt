package app.appwatcher.model

import android.graphics.drawable.Drawable

data class LaunchableApp(
    val packageName: String,
    val label: String,
    val icon: Drawable?
)

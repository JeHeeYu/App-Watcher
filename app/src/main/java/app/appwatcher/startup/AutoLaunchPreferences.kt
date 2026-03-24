package app.appwatcher.startup

import android.content.Context

class AutoLaunchPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun selectedPackages(): List<String> {
        val rawValue = prefs.all[KEY_SELECTED_PACKAGES] ?: return emptyList()

        val packages = when (rawValue) {
            is String -> rawValue
                .split(PACKAGE_SEPARATOR)
                .map(String::trim)
                .filter(String::isNotEmpty)

            is Set<*> -> rawValue
                .filterIsInstance<String>()
                .map(String::trim)
                .filter(String::isNotEmpty)

            else -> emptyList()
        }

        if (rawValue is Set<*>) {
            setSelectedPackages(packages)
        }

        return packages
    }

    fun setSelectedPackages(packageNames: List<String>) {
        val serialized = packageNames
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()
            .joinToString(PACKAGE_SEPARATOR)
        prefs.edit().putString(KEY_SELECTED_PACKAGES, serialized).apply()
    }

    fun initialDelayMs(): Long = prefs.getLong(KEY_INITIAL_DELAY_MS, DEFAULT_INITIAL_DELAY_MS)

    fun setInitialDelayMs(value: Long) {
        prefs.edit().putLong(KEY_INITIAL_DELAY_MS, value.coerceAtLeast(0L)).apply()
    }

    fun betweenDelayMs(): Long = prefs.getLong(KEY_BETWEEN_DELAY_MS, DEFAULT_BETWEEN_DELAY_MS)

    fun setBetweenDelayMs(value: Long) {
        prefs.edit().putLong(KEY_BETWEEN_DELAY_MS, value.coerceAtLeast(0L)).apply()
    }

    companion object {
        private const val PREFS_NAME = "auto_launch_prefs"
        private const val KEY_SELECTED_PACKAGES = "selected_packages"
        private const val KEY_INITIAL_DELAY_MS = "initial_delay_ms"
        private const val KEY_BETWEEN_DELAY_MS = "between_delay_ms"
        private const val PACKAGE_SEPARATOR = "\n"

        const val DEFAULT_INITIAL_DELAY_MS = 3_000L
        const val DEFAULT_BETWEEN_DELAY_MS = 1_500L
    }
}

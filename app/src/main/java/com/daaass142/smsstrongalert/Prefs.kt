package com.daaass142.smsstrongalert

import android.content.Context

object Prefs {
    private const val FILE = "local_settings"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_PLATES = "plates"
    private const val KEY_DURATION_SECONDS = "duration_seconds"
    private const val KEY_FORCE_MAX_VOLUME = "force_max_volume"

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun plates(context: Context): List<String> =
        prefs(context)
            .getString(KEY_PLATES, "")
            .orEmpty()
            .lineSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()
            .toList()

    fun savePlates(context: Context, raw: String) {
        val normalized = raw
            .lineSequence()
            .flatMap { it.split(',', '，', ';', '；').asSequence() }
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()
            .joinToString("\n")
        prefs(context).edit().putString(KEY_PLATES, normalized).apply()
    }

    fun durationSeconds(context: Context): Int =
        prefs(context).getInt(KEY_DURATION_SECONDS, 60).coerceIn(15, 300)

    fun setDurationSeconds(context: Context, seconds: Int) {
        prefs(context).edit().putInt(KEY_DURATION_SECONDS, seconds.coerceIn(15, 300)).apply()
    }

    fun forceMaxVolume(context: Context): Boolean =
        prefs(context).getBoolean(KEY_FORCE_MAX_VOLUME, true)

    fun setForceMaxVolume(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_FORCE_MAX_VOLUME, enabled).apply()
    }
}

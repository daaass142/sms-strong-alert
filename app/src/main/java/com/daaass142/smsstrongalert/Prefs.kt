package com.daaass142.smsstrongalert

import android.content.Context

object Prefs {
    private const val FILE = "local_settings"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_PLATES = "plates"
    private const val KEY_DURATION_SECONDS = "duration_seconds"
    private const val KEY_FORCE_MAX_VOLUME = "force_max_volume"
    private const val KEY_LAST_SMS_OBSERVED_AT = "last_sms_observed_at"
    private const val KEY_LAST_SMS_BODY_LENGTH = "last_sms_body_length"
    private const val KEY_LAST_SMS_PARTS = "last_sms_parts"
    private const val KEY_LAST_SMS_SENDER_PRESENT = "last_sms_sender_present"

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

    data class SmsObservation(
        val observedAtMillis: Long,
        val bodyLength: Int,
        val partCount: Int,
        val senderPresent: Boolean,
    ) {
        val hasRealDecodedData: Boolean
            get() = observedAtMillis > 0L && bodyLength > 0 && partCount > 0
    }

    fun recordSmsObservation(
        context: Context,
        observedAtMillis: Long,
        bodyLength: Int,
        partCount: Int,
        senderPresent: Boolean,
    ) {
        prefs(context).edit()
            .putLong(KEY_LAST_SMS_OBSERVED_AT, observedAtMillis)
            .putInt(KEY_LAST_SMS_BODY_LENGTH, bodyLength.coerceAtLeast(0))
            .putInt(KEY_LAST_SMS_PARTS, partCount.coerceAtLeast(0))
            .putBoolean(KEY_LAST_SMS_SENDER_PRESENT, senderPresent)
            .apply()
    }

    fun lastSmsObservation(context: Context): SmsObservation {
        val p = prefs(context)
        return SmsObservation(
            observedAtMillis = p.getLong(KEY_LAST_SMS_OBSERVED_AT, 0L),
            bodyLength = p.getInt(KEY_LAST_SMS_BODY_LENGTH, 0),
            partCount = p.getInt(KEY_LAST_SMS_PARTS, 0),
            senderPresent = p.getBoolean(KEY_LAST_SMS_SENDER_PRESENT, false),
        )
    }
}

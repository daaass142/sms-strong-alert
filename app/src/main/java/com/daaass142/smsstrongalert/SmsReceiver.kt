package com.daaass142.smsstrongalert

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        if (!Prefs.isEnabled(context)) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isEmpty()) return

        val body = messages.joinToString(separator = "") { it.messageBody.orEmpty() }.trim()
        val sender = messages.firstNotNullOfOrNull { it.displayOriginatingAddress }
            ?: messages.firstNotNullOfOrNull { it.originatingAddress }
            ?: "未知号码"

        val result = SmsMatcher.match(body, Prefs.plates(context))
        if (!result.matched) return

        AlertNotifier.show(
            context = context.applicationContext,
            payload = AlertPayload(
                body = body,
                sender = sender,
                plate = result.matchedPlate,
                receivedAtMillis = System.currentTimeMillis(),
            ),
        )
    }
}

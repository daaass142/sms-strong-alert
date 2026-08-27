package com.daaass142.smsstrongalert

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isEmpty()) return

        val body = messages.joinToString(separator = "") { it.messageBody.orEmpty() }.trim()
        val rawSender = messages.firstNotNullOfOrNull { it.displayOriginatingAddress }
            ?: messages.firstNotNullOfOrNull { it.originatingAddress }

        // Keep only proof that Android actually delivered and decoded SMS data.
        // The SMS body and sender are never persisted.
        if (body.isNotEmpty()) {
            Prefs.recordSmsObservation(
                context = context.applicationContext,
                observedAtMillis = System.currentTimeMillis(),
                bodyLength = body.length,
                partCount = messages.size,
                senderPresent = !rawSender.isNullOrBlank(),
            )
        }

        if (!Prefs.isEnabled(context)) return

        val sender = rawSender ?: "未知号码"
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

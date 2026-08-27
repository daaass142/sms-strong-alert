package com.daaass142.smsstrongalert

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build

object AlertNotifier {
    private const val CHANNEL_ID = "parking_alert_v1"
    const val NOTIFICATION_ID = 41021

    const val EXTRA_BODY = "body"
    const val EXTRA_SENDER = "sender"
    const val EXTRA_PLATE = "plate"
    const val EXTRA_RECEIVED_AT = "received_at"

    fun show(context: Context, payload: AlertPayload) {
        val manager = context.getSystemService(NotificationManager::class.java)
        ensureChannel(manager)

        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val alertIntent = Intent(context, AlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_BODY, payload.body.take(1000))
            putExtra(EXTRA_SENDER, payload.sender.take(100))
            putExtra(EXTRA_PLATE, payload.plate.orEmpty().take(32))
            putExtra(EXTRA_RECEIVED_AT, payload.receivedAtMillis)
        }

        val alertPendingIntent = PendingIntent.getActivity(
            context,
            1,
            alertIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            Intent(context, StopAlertReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title = if (payload.plate.isNullOrBlank()) {
            "🚨 检测到疑似违停短信"
        } else {
            "🚨 ${payload.plate} 可能需要立即移车"
        }

        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText("短信已到，点击立即处理")
            .setStyle(
                Notification.BigTextStyle().bigText(
                    payload.body.ifBlank { "检测到疑似违停提醒" },
                ),
            )
            .setCategory(Notification.CATEGORY_ALARM)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setPriority(Notification.PRIORITY_MAX)
            .setContentIntent(alertPendingIntent)
            .setFullScreenIntent(alertPendingIntent, true)
            .setOngoing(true)
            .setAutoCancel(false)
            .setTimeoutAfter(Prefs.durationSeconds(context) * 1000L)
            .addAction(
                Notification.Action.Builder(
                    null,
                    "收到，停止提醒",
                    stopPendingIntent,
                ).build(),
            )
            .build()
            .apply {
                flags = flags or Notification.FLAG_INSISTENT or Notification.FLAG_NO_CLEAR
            }

        manager.notify(NOTIFICATION_ID, notification)
    }

    fun cancel(context: Context) {
        context.getSystemService(NotificationManager::class.java)
            .cancel(NOTIFICATION_ID)
    }

    private fun ensureChannel(manager: NotificationManager) {
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val channel = NotificationChannel(
            CHANNEL_ID,
            "违停短信强提醒",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "本地识别到疑似违停短信后进行高优先级提醒"
            enableLights(true)
            lightColor = 0xFFFF0000.toInt()
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 900, 350, 900, 350)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setSound(soundUri, audioAttributes)
            if (manager.isNotificationPolicyAccessGranted) {
                setBypassDnd(true)
            }
        }

        manager.createNotificationChannel(channel)
    }
}

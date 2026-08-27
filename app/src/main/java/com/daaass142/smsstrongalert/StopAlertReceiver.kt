package com.daaass142.smsstrongalert

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class StopAlertReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        AlertNotifier.cancel(context)
        AlertSoundController.stop()
    }
}

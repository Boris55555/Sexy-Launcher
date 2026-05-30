package com.boris55555.sexylauncher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmUpdateReceiver(private val onAlarmChanged: () -> Unit) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == "android.app.action.NEXT_ALARM_CLOCK_CHANGED") {
            onAlarmChanged()
        }
    }
}

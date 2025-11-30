package com.example.sexylauncher.birthdays

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

class BirthdayAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notificationText = intent.getStringExtra(EXTRA_TEXT) ?: return
        val notificationId = intent.getIntExtra(EXTRA_ID, 0)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, "birthday_reminders")
            .setContentTitle("Upcoming Birthday")
            .setContentText(notificationText)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    companion object {
        const val EXTRA_TEXT = "com.example.sexylauncher.birthdays.EXTRA_TEXT"
        const val EXTRA_ID = "com.example.sexylauncher.birthdays.EXTRA_ID"
    }
}

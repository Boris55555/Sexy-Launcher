package com.boris55555.sexylauncher.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Schedules and cancels reminders using [AlarmManager].
 *
 * @param context The application context.
 */
class ReminderScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Schedules a reminder notification.
     * It sets alarms for the reminder itself, and for any pre-reminder notifications.
     *
     * @param reminder The reminder to schedule.
     */
    fun schedule(reminder: Reminder) {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            putExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_ID, reminder.id)
            putExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_TITLE, reminder.title)
            putExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_CONTENT, reminder.content)
        }

        val reminderTime = LocalDateTime.of(reminder.date, reminder.time)

        // Schedule main reminder
        val mainPendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            reminderTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            mainPendingIntent
        )

        // Schedule for each reminder day
        reminder.reminderDays.forEach { day ->
            val triggerTime = reminderTime.minusDays(day.toLong())
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                // Use a unique request code for each day to avoid conflicts
                "${reminder.id}_$day".hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            )
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                pendingIntent
            )
        }

        // Schedule for each reminder hour
        reminder.reminderHours?.let { hour ->
            val triggerTime = reminderTime.minusHours(hour.toLong())
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                 // Use a unique request code for each hour to avoid conflicts
                "${reminder.id}_hour_$hour".hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            )
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                pendingIntent
            )
        }
    }

    /**
     * Cancels a scheduled reminder notification.
     *
     * @param reminder The reminder to cancel.
     */
    fun cancel(reminder: Reminder) {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java)

        // Cancel main reminder
        val mainPendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )
        alarmManager.cancel(mainPendingIntent)

        // Cancel for each reminder day
        reminder.reminderDays.forEach { day ->
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                "${reminder.id}_$day".hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            )
            alarmManager.cancel(pendingIntent)
        }

        // Cancel for each reminder hour
        reminder.reminderHours?.let { hour ->
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                "${reminder.id}_hour_$hour".hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            )
            alarmManager.cancel(pendingIntent)
        }
    }
}

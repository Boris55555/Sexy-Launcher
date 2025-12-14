package com.boris55555.sexylauncher.birthdays

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class BirthdayAlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(birthday: Birthday) {
        val reminderDays = birthday.reminderDays ?: return
        val reminderTime = birthday.reminderTime ?: return

        val today = LocalDate.now()
        var nextBirthdayDate = birthday.date.withYear(today.year)

        if (nextBirthdayDate.isBefore(today) || (nextBirthdayDate.isEqual(today) && reminderTime.isBefore(java.time.LocalTime.now()))) {
            nextBirthdayDate = nextBirthdayDate.plusYears(1)
        }

        val age = ChronoUnit.YEARS.between(birthday.date, nextBirthdayDate).toInt()

        for (i in 0..reminderDays) {
            val reminderDateTime = nextBirthdayDate
                .minusDays(i.toLong())
                .atTime(reminderTime)

            if (reminderDateTime.isBefore(LocalDateTime.now())) {
                continue
            }

            val text = when (i) {
                0 -> "${birthday.name} is turning $age today!"
                1 -> "${birthday.name} will be $age tomorrow!"
                else -> "${birthday.name} will be $age in $i days!"
            }

            val intent = Intent(context, BirthdayAlarmReceiver::class.java).apply {
                putExtra(BirthdayAlarmReceiver.EXTRA_TEXT, text)
                putExtra(BirthdayAlarmReceiver.EXTRA_ID, birthday.id)
            }

            val requestCode = "${birthday.id}_${i}".hashCode()

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                reminderDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                pendingIntent
            )
        }
    }

    fun cancel(birthday: Birthday) {
        val reminderDays = birthday.reminderDays ?: return

        for (i in 0..reminderDays) {
            val intent = Intent(context, BirthdayAlarmReceiver::class.java)
            val requestCode = "${birthday.id}_${i}".hashCode()
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )

            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }
}

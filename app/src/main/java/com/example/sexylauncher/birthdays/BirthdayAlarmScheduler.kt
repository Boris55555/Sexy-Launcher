package com.example.sexylauncher.birthdays

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
        if (birthday.reminderDays == null || birthday.reminderTime == null) return

        val today = LocalDate.now()
        var nextBirthdayDate = birthday.date.withYear(today.year)

        if (nextBirthdayDate.isBefore(today) || (nextBirthdayDate.isEqual(today) && birthday.reminderTime.isBefore(java.time.LocalTime.now()))) {
            nextBirthdayDate = nextBirthdayDate.plusYears(1)
        }

        val age = ChronoUnit.YEARS.between(birthday.date, nextBirthdayDate).toInt()

        for (daysBefore in 1..birthday.reminderDays) {
            val reminderDateTime = nextBirthdayDate
                .minusDays(daysBefore.toLong())
                .atTime(birthday.reminderTime)

            if (reminderDateTime.isBefore(LocalDateTime.now())) {
                continue
            }

            val intent = Intent(context, BirthdayAlarmReceiver::class.java).apply {
                putExtra(BirthdayAlarmReceiver.EXTRA_TEXT, "${birthday.name} will be $age years old in $daysBefore days!")
                putExtra(BirthdayAlarmReceiver.EXTRA_ID, birthday.id)
            }

            val requestCode = "${birthday.id}$daysBefore".hashCode()

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
        if (birthday.reminderDays == null) return

        for (daysBefore in 1..birthday.reminderDays) {
            val intent = Intent(context, BirthdayAlarmReceiver::class.java)
            val requestCode = "${birthday.id}$daysBefore".hashCode()
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

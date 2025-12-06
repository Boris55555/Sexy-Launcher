package com.boris55555.sexylauncher.reminders

import java.time.LocalDate
import java.time.LocalTime

data class Reminder(
    val id: Int,
    val title: String,
    val content: String,
    val date: LocalDate,
    val time: LocalTime,
    val reminderDays: List<Int>,
    val reminderHours: Int?
)

package com.boris55555.sexylauncher.birthdays

import java.time.LocalDate
import java.time.LocalTime

data class Birthday(
    val id: Int,
    val name: String,
    val date: LocalDate,
    val reminderDays: Int? = null,
    val reminderTime: LocalTime? = null
)

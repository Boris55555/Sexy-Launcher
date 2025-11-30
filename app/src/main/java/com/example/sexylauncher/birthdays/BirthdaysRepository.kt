package com.example.sexylauncher.birthdays

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private const val PREFS_NAME = "BirthdayPrefs"
private const val KEY_BIRTHDAYS = "birthdays"

class BirthdaysRepository(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    private val timeFormatter = DateTimeFormatter.ISO_LOCAL_TIME
    private val alarmScheduler = BirthdayAlarmScheduler(context)

    private val _birthdays = MutableStateFlow<List<Birthday>>(emptyList())
    val birthdays: StateFlow<List<Birthday>> = _birthdays

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == KEY_BIRTHDAYS) {
            _birthdays.value = getBirthdays()
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(listener)
        // Initial load
        _birthdays.value = getBirthdays()
    }

    fun addBirthday(name: String, date: LocalDate, reminderDays: Int?, reminderTime: LocalTime?) {
        val currentBirthdays = getBirthdays()
        val nextId = (currentBirthdays.maxOfOrNull { it.id } ?: 0) + 1
        val newBirthday = Birthday(id = nextId, name = name, date = date, reminderDays = reminderDays, reminderTime = reminderTime)
        val updatedBirthdays = currentBirthdays + newBirthday
        saveBirthdays(updatedBirthdays)
        alarmScheduler.schedule(newBirthday)
    }

    fun updateBirthday(id: Int, name: String, date: LocalDate, reminderDays: Int?, reminderTime: LocalTime?) {
        val currentBirthdays = getBirthdays()
        val birthdayToUpdate = currentBirthdays.find { it.id == id } ?: return
        val updatedBirthday = birthdayToUpdate.copy(name = name, date = date, reminderDays = reminderDays, reminderTime = reminderTime)
        val updatedBirthdays = currentBirthdays.map { if (it.id == id) updatedBirthday else it }
        saveBirthdays(updatedBirthdays)
        alarmScheduler.cancel(birthdayToUpdate)
        alarmScheduler.schedule(updatedBirthday)
    }

    fun removeBirthday(id: Int) {
        val currentBirthdays = getBirthdays()
        val birthdayToRemove = currentBirthdays.find { it.id == id } ?: return
        val updatedBirthdays = currentBirthdays.filter { it.id != id }
        saveBirthdays(updatedBirthdays)
        alarmScheduler.cancel(birthdayToRemove)
    }

    fun cleanup() {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    private fun saveBirthdays(birthdays: List<Birthday>) {
        val birthdaysString = birthdays.joinToString(";") { 
            val reminderDaysStr = it.reminderDays?.toString() ?: ""
            val reminderTimeStr = it.reminderTime?.format(timeFormatter) ?: ""
            "${it.id},${it.name},${it.date.format(dateFormatter)},$reminderDaysStr,$reminderTimeStr"
        }
        prefs.edit().putString(KEY_BIRTHDAYS, birthdaysString).apply()
    }

    private fun getBirthdays(): List<Birthday> {
        val birthdaysString = prefs.getString(KEY_BIRTHDAYS, null) ?: return emptyList()
        if (birthdaysString.isEmpty()) return emptyList()
        return birthdaysString.split(";").mapNotNull { 
            val parts = it.split(",", limit = 5)
            if (parts.size == 5) {
                try {
                    val id = parts[0].toInt()
                    val name = parts[1]
                    val date = LocalDate.parse(parts[2], dateFormatter)
                    val reminderDays = parts[3].ifEmpty { null }?.toInt()
                    val reminderTime = parts[4].ifEmpty { null }?.let { LocalTime.parse(it, timeFormatter) }
                    Birthday(id, name, date, reminderDays, reminderTime)
                } catch (e: Exception) {
                    null // Or log error
                }
            } else {
                null // or log error
            }
        }
    }
}

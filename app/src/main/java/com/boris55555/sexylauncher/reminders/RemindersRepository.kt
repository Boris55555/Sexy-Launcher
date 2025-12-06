package com.boris55555.sexylauncher.reminders

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private const val PREFS_NAME = "ReminderPrefs"
private const val KEY_REMINDERS = "reminders"

class RemindersRepository(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    private val timeFormatter = DateTimeFormatter.ISO_LOCAL_TIME
    private val scheduler = ReminderScheduler(context)

    private val _reminders = MutableStateFlow<List<Reminder>>(emptyList())
    val reminders: StateFlow<List<Reminder>> = _reminders

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == KEY_REMINDERS) {
            _reminders.value = getReminders()
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(listener)
        _reminders.value = getReminders()
        removeExpiredReminders()
    }

    fun addReminder(title: String, content: String, date: LocalDate, time: LocalTime, reminderDays: List<Int>, reminderHours: Int?) {
        val currentReminders = getReminders()
        val nextId = (currentReminders.maxOfOrNull { it.id } ?: 0) + 1
        val newReminder = Reminder(nextId, title, content, date, time, reminderDays, reminderHours)
        val updatedReminders = currentReminders + newReminder
        saveReminders(updatedReminders)
        scheduler.schedule(newReminder)
    }

    fun updateReminder(id: Int, title: String, content: String, date: LocalDate, time: LocalTime, reminderDays: List<Int>, reminderHours: Int?) {
        val currentReminders = getReminders()
        val reminderToUpdate = currentReminders.find { it.id == id } ?: return
        scheduler.cancel(reminderToUpdate) // Cancel old reminder
        val updatedReminder = reminderToUpdate.copy(title = title, content = content, date = date, time = time, reminderDays = reminderDays, reminderHours = reminderHours)
        val updatedReminders = currentReminders.map { if (it.id == id) updatedReminder else it }
        saveReminders(updatedReminders)
        scheduler.schedule(updatedReminder) // Schedule new reminder
    }

    fun removeReminder(id: Int) {
        val currentReminders = getReminders()
        val reminderToRemove = currentReminders.find { it.id == id } ?: return
        val updatedReminders = currentReminders.filter { it.id != id }
        saveReminders(updatedReminders)
        scheduler.cancel(reminderToRemove)
    }

    private fun removeExpiredReminders() {
        val currentReminders = getReminders()
        val today = LocalDate.now()
        val filteredReminders = currentReminders.filter { it.date.isAfter(today.minusDays(1)) }
        if (filteredReminders.size != currentReminders.size) {
            saveReminders(filteredReminders)
        }
    }

    fun cleanup() {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    private fun saveReminders(reminders: List<Reminder>) {
        val remindersString = reminders.joinToString(";") { reminder ->
            val days = reminder.reminderDays.joinToString(",")
            val hours = reminder.reminderHours?.toString() ?: ""
            "${reminder.id}|${reminder.title}|${reminder.content}|${reminder.date.format(dateFormatter)}|${reminder.time.format(timeFormatter)}|$days|$hours"
        }
        prefs.edit().putString(KEY_REMINDERS, remindersString).apply()
    }

    private fun getReminders(): List<Reminder> {
        val remindersString = prefs.getString(KEY_REMINDERS, null) ?: return emptyList()
        if (remindersString.isEmpty()) return emptyList()
        return remindersString.split(";").mapNotNull { 
            val parts = it.split("|", limit = 7)
            if (parts.size == 7) {
                try {
                    val id = parts[0].toInt()
                    val title = parts[1]
                    val content = parts[2]
                    val date = LocalDate.parse(parts[3], dateFormatter)
                    val time = LocalTime.parse(parts[4], timeFormatter)
                    val days = parts[5].ifEmpty { null }?.split(",")?.map { it.toInt() } ?: emptyList()
                    val hours = parts[6].ifEmpty { null }?.toInt()
                    Reminder(id, title, content, date, time, days, hours)
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
        }
    }
}

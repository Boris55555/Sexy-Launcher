package com.boris55555.sexylauncher.birthdays

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate
import java.time.LocalTime
import android.database.sqlite.SQLiteDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileOutputStream
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

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

    fun importFromBirdayJson(jsonString: String): Int {
        val gson = Gson()
        val eventType = object : TypeToken<List<Map<String, Any>>>() {}.type
        val importedEvents: List<Map<String, Any>> = try {
            gson.fromJson(jsonString, eventType)
        } catch (e: Exception) {
            return -1
        }

        var importedCount = 0
        val currentBirthdays = getBirthdays()
        var nextId = (currentBirthdays.maxOfOrNull { it.id } ?: 0) + 1
        val updatedBirthdays = currentBirthdays.toMutableList()

        importedEvents.forEach { event ->
            val name = event["name"] as? String ?: ""
            val surname = event["surname"] as? String ?: ""
            val fullName = if (surname.isNotBlank()) "$name $surname" else name
            val originalDateStr = event["originalDate"] as? String // YYYY-MM-DD

            if (fullName.isNotBlank() && originalDateStr != null) {
                try {
                    val date = LocalDate.parse(originalDateStr) // Parses ISO_LOCAL_DATE (YYYY-MM-DD)
                    
                    // Check if already exists (basic name + date check)
                    val exists = currentBirthdays.any { it.name == fullName && it.date == date }
                    if (!exists) {
                        val newBirthday = Birthday(id = nextId++, name = fullName, date = date)
                        updatedBirthdays.add(newBirthday)
                        alarmScheduler.schedule(newBirthday)
                        importedCount++
                    }
                } catch (e: DateTimeParseException) {
                    // Skip invalid dates
                }
            }
        }

        if (importedCount > 0) {
            saveBirthdays(updatedBirthdays)
        }
        return importedCount
    }

    fun importFromBirdayBackup(file: File): Int {
        var importedCount = 0
        val tempDbFile = File(context.cacheDir, "temp_birday_import.db")
        
        try {
            // Copy the source file to a temp file we can open as SQLite
            file.inputStream().use { input ->
                FileOutputStream(tempDbFile).use { output ->
                    input.copyTo(output)
                }
            }

            val db = SQLiteDatabase.openDatabase(tempDbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            val cursor = db.rawQuery("SELECT name, surname, originalDate FROM event", null)

            val currentBirthdays = getBirthdays()
            var nextId = (currentBirthdays.maxOfOrNull { it.id } ?: 0) + 1
            val updatedBirthdays = currentBirthdays.toMutableList()

            if (cursor.moveToFirst()) {
                do {
                    val name = cursor.getString(0) ?: ""
                    val surname = cursor.getString(1) ?: ""
                    val fullName = if (surname.isNotBlank()) "$name $surname" else name
                    val originalDateStr = cursor.getString(2) // Birday stores as Long or String, but usually String YYYY-MM-DD

                    if (fullName.isNotBlank() && originalDateStr != null) {
                        try {
                            // Birday stores dates in ISO format YYYY-MM-DD
                            val date = LocalDate.parse(originalDateStr)
                            
                            val exists = currentBirthdays.any { it.name == fullName && it.date == date }
                            if (!exists) {
                                val newBirthday = Birthday(id = nextId++, name = fullName, date = date)
                                updatedBirthdays.add(newBirthday)
                                alarmScheduler.schedule(newBirthday)
                                importedCount++
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("BirthdaysRepository", "Failed to parse date: $originalDateStr for $fullName", e)
                        }
                    }
                } while (cursor.moveToNext())
            }
            cursor.close()
            db.close()

            if (importedCount > 0) {
                saveBirthdays(updatedBirthdays)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return -1
        } finally {
            tempDbFile.delete()
        }

        return importedCount
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

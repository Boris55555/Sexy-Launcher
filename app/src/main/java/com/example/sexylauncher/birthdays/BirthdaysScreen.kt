package com.example.sexylauncher.birthdays

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun EInkButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RectangleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Color.Black,
            disabledContainerColor = Color.White,
            disabledContentColor = Color.LightGray
        ),
        border = BorderStroke(2.dp, Color.Black)
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BirthdaysScreen(repository: BirthdaysRepository) {
    val birthdays by repository.birthdays.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var birthdayToEditOrDelete by remember { mutableStateOf<Birthday?>(null) }

    val sortedBirthdays = birthdays.sortedBy {
        val today = LocalDate.now()
        val nextBirthdayDate = it.date.withYear(today.year).let { date ->
            if (date.isBefore(today)) date.plusYears(1) else date
        }
        ChronoUnit.DAYS.between(today, nextBirthdayDate)
    }

    if (showAddDialog) {
        AddBirthdayDialog(
            onDismiss = { showAddDialog = false },
            onAddBirthday = { name, date, reminderDays, reminderTime ->
                repository.addBirthday(name, date, reminderDays, reminderTime)
                showAddDialog = false
            }
        )
    }

    birthdayToEditOrDelete?.let { birthday ->
        EditOrDeleteBirthdayDialog(
            birthday = birthday,
            onDismiss = { birthdayToEditOrDelete = null },
            onUpdateBirthday = { name, date, reminderDays, reminderTime ->
                repository.updateBirthday(birthday.id, name, date, reminderDays, reminderTime)
                birthdayToEditOrDelete = null
            },
            onDeleteBirthday = {
                repository.removeBirthday(birthday.id)
                birthdayToEditOrDelete = null
            }
        )
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            EInkButton(onClick = { showAddDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Add Birthday")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        items(sortedBirthdays) { birthday ->
            BirthdayItem(birthday = birthday, onLongClick = { birthdayToEditOrDelete = birthday })
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BirthdayItem(birthday: Birthday, onLongClick: () -> Unit) {
    val age = ChronoUnit.YEARS.between(birthday.date, LocalDate.now())
    val nextBirthday = birthday.date.withYear(LocalDate.now().year).let {
        if (it.isBefore(LocalDate.now())) it.plusYears(1) else it
    }
    val daysUntilNextBirthday = ChronoUnit.DAYS.between(LocalDate.now(), nextBirthday)
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(2.dp, Color.Black)
            .combinedClickable(
                onClick = {},
                onLongClick = onLongClick
            )
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = birthday.name, color = Color.Black)
            Text(text = "Age: $age", color = Color.Black)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(text = "Birthday: ${birthday.date.format(formatter)}", color = Color.Black)
            Text(text = "$daysUntilNextBirthday days until next birthday", color = Color.Black)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBirthdayDialog(
    onDismiss: () -> Unit,
    onAddBirthday: (String, LocalDate, Int?, LocalTime?) -> Unit,
    birthday: Birthday? = null
) {
    var name by remember { mutableStateOf(birthday?.name ?: "") }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = birthday?.date?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    )
    var showDatePicker by remember { mutableStateOf(false) }
    var reminderDays by remember { mutableStateOf(birthday?.reminderDays?.toFloat() ?: 0f) }
    val timePickerState = rememberTimePickerState(
        initialHour = birthday?.reminderTime?.hour ?: 0,
        initialMinute = birthday?.reminderTime?.minute ?: 0
    )
    var showTimePicker by remember { mutableStateOf(false) }

    val selectedDate = datePickerState.selectedDateMillis?.let {
        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
    }
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                EInkButton(onClick = { showDatePicker = false }) { Text("OK") }
            },
            dismissButton = {
                EInkButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
            colors = DatePickerDefaults.colors(containerColor = Color.White)
        ) {
            DatePicker(
                state = datePickerState,
                dateFormatter = DatePickerDefaults.dateFormatter(selectedDateSkeleton = "dd/MM/yyyy"),
                title = {
                    Text(
                        "Select date",
                        modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp)
                    )
                },
                headline = {
                    val date = selectedDate?.format(formatter)
                    Text(
                        date ?: "dd.MM.yyyy",
                        modifier = Modifier.padding(start = 24.dp, end = 12.dp, bottom = 12.dp)
                    )
                },
                colors = DatePickerDefaults.colors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black,
                    headlineContentColor = Color.Black,
                    weekdayContentColor = Color.Black,
                    subheadContentColor = Color.Black,
                    yearContentColor = Color.Black,
                    currentYearContentColor = Color.Black,
                    selectedYearContentColor = Color.White,
                    selectedYearContainerColor = Color.Black,
                    dayContentColor = Color.Black,
                    disabledDayContentColor = Color.Gray,
                    selectedDayContentColor = Color.White,
                    disabledSelectedDayContentColor = Color.White,
                    selectedDayContainerColor = Color.Black,
                    todayContentColor = Color.Black,
                    todayDateBorderColor = Color.Black,
                )
            )
        }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            containerColor = Color.White,
            title = { Text("Select Time", color = Color.Black) },
            text = {
                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(
                        containerColor = Color.White,
                        clockDialColor = Color.LightGray,
                        clockDialSelectedContentColor = Color.White,
                        clockDialUnselectedContentColor = Color.Black,
                        selectorColor = Color.Black,
                        periodSelectorBorderColor = Color.Black,
                        periodSelectorSelectedContainerColor = Color.Black,
                        periodSelectorUnselectedContainerColor = Color.White,
                        periodSelectorSelectedContentColor = Color.White,
                        periodSelectorUnselectedContentColor = Color.Black,
                        timeSelectorSelectedContainerColor = Color.Black,
                        timeSelectorUnselectedContainerColor = Color.White,
                        timeSelectorSelectedContentColor = Color.White,
                        timeSelectorUnselectedContentColor = Color.Black
                    )
                )
            },
            confirmButton = {
                EInkButton(onClick = { showTimePicker = false }) { Text("OK") }
            },
            dismissButton = {
                EInkButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text(if (birthday == null) "Add Birthday" else "Edit Birthday", color = Color.Black) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    shape = RectangleShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color.Black,
                        unfocusedBorderColor = Color.Black,
                        focusedLabelColor = Color.Black,
                        unfocusedLabelColor = Color.Black,
                        cursorColor = Color.Black,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                    )
                )
                Spacer(Modifier.height(16.dp))
                EInkButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(selectedDate?.format(formatter) ?: "Select Date")
                }
                Spacer(Modifier.height(16.dp))
                Text("Reminder: ${if (reminderDays == 0f) "No reminder" else "${reminderDays.toInt()} days before"}")
                Slider(
                    value = reminderDays,
                    onValueChange = { reminderDays = it },
                    valueRange = 0f..14f,
                    steps = 13,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.Black,
                        activeTrackColor = Color.Black,
                        inactiveTrackColor = Color.LightGray,
                        activeTickColor = Color.White,
                        inactiveTickColor = Color.Black
                    )
                )
                if (reminderDays > 0f) {
                    Spacer(Modifier.height(16.dp))
                    EInkButton(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(timePickerState.let { "${it.hour}:${it.minute}" })
                    }
                }
            }
        },
        confirmButton = {
            EInkButton(
                onClick = {
                    if (name.isNotBlank() && selectedDate != null) {
                        val reminderDaysInt = if (reminderDays == 0f) null else reminderDays.toInt()
                        val reminderTime = if (reminderDaysInt != null) LocalTime.of(timePickerState.hour, timePickerState.minute) else null
                        onAddBirthday(name, selectedDate, reminderDaysInt, reminderTime)
                    }
                },
                enabled = name.isNotBlank() && selectedDate != null
            ) {
                Text(if (birthday == null) "Add" else "Update")
            }
        },
        dismissButton = {
            EInkButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditOrDeleteBirthdayDialog(
    birthday: Birthday,
    onDismiss: () -> Unit,
    onUpdateBirthday: (String, LocalDate, Int?, LocalTime?) -> Unit,
    onDeleteBirthday: () -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }

    if (showEditDialog) {
        AddBirthdayDialog(
            onDismiss = { showEditDialog = false },
            onAddBirthday = { name, date, reminderDays, reminderTime ->
                onUpdateBirthday(name, date, reminderDays, reminderTime)
                showEditDialog = false
            },
            birthday = birthday
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = Color.White,
            title = { Text(birthday.name, color = Color.Black) },
            text = { Text("What would you like to do with ${birthday.name}'s birthday?", color = Color.Black) },
            confirmButton = {
                EInkButton(onClick = { showEditDialog = true }) {
                    Text("Edit")
                }
            },
            dismissButton = {
                Row {
                    EInkButton(onClick = onDeleteBirthday) {
                        Text("Delete")
                    }
                    Spacer(Modifier.width(8.dp))
                    EInkButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}

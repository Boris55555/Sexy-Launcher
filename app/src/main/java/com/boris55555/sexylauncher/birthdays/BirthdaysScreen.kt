package com.boris55555.sexylauncher.birthdays

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.boris55555.sexylauncher.DateVisualTransformation
import com.boris55555.sexylauncher.EInkButton
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

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

    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Birthdays",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            EInkButton(onClick = { showAddDialog = true }) {
                Text("Add +")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(sortedBirthdays) { birthday ->
                    BirthdayItem(birthday = birthday, onLongClick = { birthdayToEditOrDelete = birthday })
                }
            }

            if (lazyListState.canScrollBackward) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Scroll Up",
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(8.dp)
                        .clickable {
                            coroutineScope.launch {
                                lazyListState.animateScrollToItem(0)
                            }
                        },
                    tint = Color.Black
                )
            }

            if (lazyListState.canScrollForward) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Scroll Down",
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(8.dp)
                        .clickable {
                            coroutineScope.launch {
                                val lastIndex = lazyListState.layoutInfo.totalItemsCount - 1
                                if (lastIndex >= 0) {
                                    lazyListState.animateScrollToItem(lastIndex)
                                }
                            }
                        },
                    tint = Color.Black
                )
            }
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(2.dp, Color.Black)
            .combinedClickable(
                onClick = {},
                onLongClick = onLongClick
            )
            .padding(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Icon(
                imageVector = Icons.Default.Cake,
                contentDescription = "Birthday",
                tint = Color.Black,
                modifier = Modifier.align(Alignment.CenterStart)
            )
            Text(
                text = birthday.name,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(text = "Age: $age", color = Color.Black)
            Text(text = "($daysUntilNextBirthday days until next birthday)", color = Color.Black, maxLines = 1)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Birthday: ${birthday.date.format(formatter)}",
            color = Color.Black,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
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
    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    var dateString by remember { mutableStateOf(birthday?.date?.format(dateFormatter)?.replace(".", "") ?: "") }

    val parsedDate by remember {
        derivedStateOf {
            try {
                LocalDate.parse(dateString, DateTimeFormatter.ofPattern("ddMMyyyy"))
            } catch (e: DateTimeParseException) {
                null
            }
        }
    }

    val dateValidationError by remember {
        derivedStateOf<String?> {
            if (dateString.isBlank()) {
                null // No error if blank
            } else if (dateString.length < 8) {
                null // No error while typing
            } else {
                val date = parsedDate
                when {
                    date == null -> "Invalid date format"
                    date.isAfter(LocalDate.now()) -> "Birthday can't be in the future"
                    else -> null // All good
                }
            }
        }
    }

    var reminderDays by remember { mutableStateOf(birthday?.reminderDays?.toFloat() ?: 0f) }
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    var timeString by remember { mutableStateOf(birthday?.reminderTime?.format(timeFormatter) ?: "12:00") }
    val parsedTime by remember {
        derivedStateOf {
            try {
                LocalTime.parse(timeString, timeFormatter)
            } catch (e: DateTimeParseException) {
                null
            }
        }
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
                OutlinedTextField(
                    value = dateString,
                    onValueChange = { if (it.length <= 8) dateString = it },
                    label = { Text("Date") },
                    placeholder = { Text("DD.MM.YYYY") },
                    shape = RectangleShape,
                    visualTransformation = DateVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = dateValidationError != null,
                    supportingText = {
                        if (dateValidationError != null) {
                            Text(dateValidationError!!, color = MaterialTheme.colorScheme.error)
                        }
                    },
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
                Text("Reminder: ${if (reminderDays == 0f) "No reminder" else "${reminderDays.roundToInt()} days before"}")
                Slider(
                    value = reminderDays,
                    onValueChange = { reminderDays = it.roundToInt().toFloat() },
                    valueRange = 0f..14f,
                    steps = 13,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.Black,
                        activeTrackColor = Color.Black,
                        inactiveTrackColor = Color.Black,
                        activeTickColor = Color.White,
                        inactiveTickColor = Color.White
                    )
                )
                if (reminderDays > 0f) {
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = timeString,
                        onValueChange = { timeString = it },
                        label = { Text("Time (HH:mm)") },
                        shape = RectangleShape,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                }
            }
        },
        confirmButton = {
            EInkButton(
                onClick = {
                    val date = parsedDate
                    val time = parsedTime
                    if (name.isNotBlank() && date != null) {
                        val reminderDaysInt = if (reminderDays == 0f) null else reminderDays.roundToInt()
                        val reminderTime = if (reminderDaysInt != null) time else null
                        onAddBirthday(name, date, reminderDaysInt, reminderTime)
                    }
                },
                enabled = name.isNotBlank() && parsedDate != null && (reminderDays == 0f || parsedTime != null) && dateValidationError == null
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

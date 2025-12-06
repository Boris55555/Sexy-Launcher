package com.boris55555.sexylauncher.reminders

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.unit.sp
import com.boris55555.sexylauncher.DateVisualTransformation
import com.boris55555.sexylauncher.EInkButton
import com.boris55555.sexylauncher.TimeVisualTransformation
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(repository: RemindersRepository) {
    val reminders by repository.reminders.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var reminderToEditOrDelete by remember { mutableStateOf<Reminder?>(null) }

    val sortedReminders = reminders.sortedBy {
        ChronoUnit.DAYS.between(LocalDate.now(), it.date)
    }

    if (showAddDialog) {
        AddReminderDialog(
            onDismiss = { showAddDialog = false },
            onAddReminder = { title, content, date, time, reminderDays, reminderHours ->
                repository.addReminder(title, content, date, time, reminderDays, reminderHours)
                showAddDialog = false
            }
        )
    }

    reminderToEditOrDelete?.let { reminder ->
        EditOrDeleteReminderDialog(
            reminder = reminder,
            onDismiss = { reminderToEditOrDelete = null },
            onUpdateReminder = { title, content, date, time, reminderDays, reminderHours ->
                repository.updateReminder(reminder.id, title, content, date, time, reminderDays, reminderHours)
                reminderToEditOrDelete = null
            },
            onDeleteReminder = {
                repository.removeReminder(reminder.id)
                reminderToEditOrDelete = null
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
                text = "Reminders",
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
                items(sortedReminders) { reminder ->
                    ReminderItem(reminder = reminder, onLongClick = { reminderToEditOrDelete = reminder })
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
fun ReminderItem(reminder: Reminder, onLongClick: () -> Unit) {
    val daysUntilReminder = ChronoUnit.DAYS.between(LocalDate.now(), reminder.date)
    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

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
                imageVector = Icons.Default.Notifications,
                contentDescription = "Reminder",
                tint = Color.Black,
                modifier = Modifier.align(Alignment.CenterStart)
            )
            Text(
                text = reminder.title,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = reminder.content, color = Color.Black, maxLines = 2)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(text = "Date: ${reminder.date.format(dateFormatter)}", color = Color.Black)
            Text(text = "Time: ${reminder.time.format(timeFormatter)}", color = Color.Black)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "($daysUntilReminder days until reminder)", color = Color.Black, maxLines = 1)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReminderDialog(
    onDismiss: () -> Unit,
    onAddReminder: (String, String, LocalDate, LocalTime, List<Int>, Int?) -> Unit,
    reminder: Reminder? = null
) {
    var title by remember { mutableStateOf(reminder?.title ?: "") }
    var content by remember { mutableStateOf(reminder?.content ?: "") }
    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    var dateString by remember { mutableStateOf(reminder?.date?.format(dateFormatter)?.replace(".", "") ?: "") }

    val parsedDate by remember {
        derivedStateOf {
            try {
                LocalDate.parse(dateString, DateTimeFormatter.ofPattern("ddMMyyyy"))
            } catch (e: DateTimeParseException) {
                null
            }
        }
    }

    val timeFormatter = DateTimeFormatter.ofPattern("HHmm")
    var timeString by remember { mutableStateOf(reminder?.time?.format(timeFormatter) ?: "") }
    val parsedTime by remember {
        derivedStateOf {
            try {
                LocalTime.parse(timeString, DateTimeFormatter.ofPattern("HHmm"))
            } catch (e: DateTimeParseException) {
                null
            }
        }
    }

    var reminderDays by remember { mutableStateOf(reminder?.reminderDays ?: emptyList()) }
    var reminderHoursSliderPosition by remember { mutableStateOf(reminder?.reminderHours?.toFloat() ?: 0f) }


    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text(if (reminder == null) "Add Reminder" else "Edit Reminder", color = Color.Black) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
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
                    value = content,
                    onValueChange = { if (it.length <= 50) content = it },
                    label = { Text("Content (max 50 chars)") },
                    shape = RectangleShape,
                    modifier = Modifier.height(100.dp),
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
                    value = timeString,
                    onValueChange = { if (it.length <= 4) timeString = it },
                    label = { Text("Time") },
                    placeholder = { Text("HH:mm") },
                    shape = RectangleShape,
                    visualTransformation = TimeVisualTransformation(),
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
                Spacer(Modifier.height(16.dp))
                Text("Reminder (days before):", color = Color.Black)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    (1..5).forEach { day ->
                        val isSelected = reminderDays.contains(day)
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                reminderDays = if (isSelected) {
                                    reminderDays - day
                                } else {
                                    (reminderDays + day).sorted()
                                }
                            },
                            shape = RectangleShape,
                            border = BorderStroke(1.dp, Color.Black),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) Color.Black else Color.White,
                                contentColor = if (isSelected) Color.White else Color.Black
                            )
                        ) {
                            Text(text = "+$day", fontSize = 14.sp)
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                val reminderHours = if(reminderHoursSliderPosition > 0f) reminderHoursSliderPosition.roundToInt() else null
                Text("Reminder: ${if (reminderHours != null) "$reminderHours hours before" else "No time-based reminder"}", color = Color.Black)
                Spacer(Modifier.height(8.dp))
                Slider(
                    value = reminderHoursSliderPosition,
                    onValueChange = { reminderHoursSliderPosition = it },
                    valueRange = 0f..5f,
                    steps = 4,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.Black,
                        activeTrackColor = Color.Black,
                        inactiveTrackColor = Color.Black,
                        activeTickColor = Color.White,
                        inactiveTickColor = Color.White
                    )
                )
            }
        },
        confirmButton = {
            EInkButton(
                onClick = {
                    val date = parsedDate
                    val time = parsedTime
                    if (title.isNotBlank() && content.isNotBlank() && date != null && time != null) {
                        val reminderHoursInt = if (reminderHoursSliderPosition > 0) reminderHoursSliderPosition.roundToInt() else null
                        onAddReminder(title, content, date, time, reminderDays, reminderHoursInt)
                    }
                },
                enabled = title.isNotBlank() && content.isNotBlank() && parsedDate != null && parsedTime != null
            ) {
                Text(if (reminder == null) "Add" else "Update")
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
fun EditOrDeleteReminderDialog(
    reminder: Reminder,
    onDismiss: () -> Unit,
    onUpdateReminder: (String, String, LocalDate, LocalTime, List<Int>, Int?) -> Unit,
    onDeleteReminder: () -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }

    if (showEditDialog) {
        AddReminderDialog(
            onDismiss = { showEditDialog = false },
            onAddReminder = { title, content, date, time, reminderDays, reminderHours ->
                onUpdateReminder(title, content, date, time, reminderDays, reminderHours)
                showEditDialog = false
            },
            reminder = reminder
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = Color.White,
            title = { Text(reminder.title, color = Color.Black) },
            text = { Text("What would you like to do with this reminder?", color = Color.Black) },
            confirmButton = {
                EInkButton(onClick = { showEditDialog = true }) {
                    Text("Edit")
                }
            },
            dismissButton = {
                Row {
                    EInkButton(onClick = onDeleteReminder) {
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

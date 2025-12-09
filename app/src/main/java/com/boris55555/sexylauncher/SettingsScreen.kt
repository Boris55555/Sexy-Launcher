package com.boris55555.sexylauncher

import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    favoritesRepository: FavoritesRepository,
    onChooseAlarmAppClicked: () -> Unit,
    onChooseCalendarAppClicked: () -> Unit,
    onChooseSwipeLeftAppClicked: () -> Unit,
    onChooseSwipeRightAppClicked: () -> Unit,
    onBirthdaysClicked: () -> Unit,
    onRemindersClicked: () -> Unit
) {
    val context = LocalContext.current
    var hasNotificationPermission by remember { mutableStateOf(isNotificationServiceEnabled(context)) }
    val alarmAppPackage by favoritesRepository.alarmAppPackage.collectAsState()
    val calendarAppPackage by favoritesRepository.calendarAppPackage.collectAsState()
    val isHomeLocked by favoritesRepository.isHomeLocked.collectAsState()
    val weekStartsOnSunday by favoritesRepository.weekStartsOnSunday.collectAsState()
    val hideLauncherFromAppView by favoritesRepository.hideLauncherFromAppView.collectAsState()
    val gesturesEnabled by favoritesRepository.gesturesEnabled.collectAsState()
    val swipeLeftAction by favoritesRepository.swipeLeftAction.collectAsState()
    val swipeRightAction by favoritesRepository.swipeRightAction.collectAsState()
    val catIconAction by favoritesRepository.catIconAction.collectAsState()
    val disableDuraSpeedNotifications by favoritesRepository.disableDuraSpeedNotifications.collectAsState()

    var showHelpDialog by remember { mutableStateOf(false) }

    val favoriteCount by favoritesRepository.favoriteCount.collectAsState()
    var sliderPosition by remember(favoriteCount) { mutableFloatStateOf(favoriteCount.toFloat()) }

    val alarmAppName = remember(alarmAppPackage) {
        alarmAppPackage?.let {
            try {
                val packageManager = context.packageManager
                packageManager.getApplicationLabel(packageManager.getApplicationInfo(it, 0)).toString()
            } catch (_: PackageManager.NameNotFoundException) {
                "Unknown"
            }
        } ?: "Not Set"
    }

    val calendarAppName = remember(calendarAppPackage) {
        calendarAppPackage?.let {
            try {
                val packageManager = context.packageManager
                packageManager.getApplicationLabel(packageManager.getApplicationInfo(it, 0)).toString()
            } catch (_: PackageManager.NameNotFoundException) {
                "Unknown"
            }
        } ?: "Not Set"
    }

    fun getActionDisplayName(action: String): String {
        return when {
            action == "none" -> "None"
            action == "notifications" -> "Notifications"
            action == "birthdays" -> "Birthdays"
            action == "reminders" -> "Reminders"
            action.startsWith("app:") -> {
                val pkgName = action.substring(4)
                try {
                    val pm = context.packageManager
                    pm.getApplicationLabel(pm.getApplicationInfo(pkgName, 0)).toString()
                } catch (e: PackageManager.NameNotFoundException) {
                    "Unknown App"
                }
            }
            else -> "Unknown"
        }
    }

    // This makes the permission status update if the user grants it and returns to the app
    LaunchedEffect(Unit) {
        while(true) {
            val isEnabled = isNotificationServiceEnabled(context)
            if (isEnabled != hasNotificationPermission) {
                hasNotificationPermission = isEnabled
            }
            delay(1000)
        }
    }

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    val eInkSwitchColors = SwitchDefaults.colors(
        checkedThumbColor = Color.White,
        checkedTrackColor = Color.Black,
        checkedBorderColor = Color.Black,
        uncheckedThumbColor = Color.Black,
        uncheckedTrackColor = Color.White,
        uncheckedBorderColor = Color.Black
    )

    if (showHelpDialog) {
        val helpText = """
            Welcome to Sexy Launcher!

            • Home Screen: Long press an empty space for settings. Double tap to refresh. Long press a favorite to change it. If the homescreen is locked, double-tap the cat icon in the bottom right to open settings.

            • All Apps: Press the button on the right edge to open. Long press an app to rename or uninstall.

            • Settings: Customize your launcher, set it as default, lock the layout, and more.
            """

        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("Quick Guide", color = Color.Black) },
            text = { Text(helpText.trimIndent(), color = Color.Black) },
            confirmButton = {
                Button(
                    onClick = { showHelpDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    border = BorderStroke(1.dp, Color.Black)
                ) {
                    Text("Close")
                }
            },
            containerColor = Color.White
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
                Icon(Icons.Default.Tune, contentDescription = "Control Panel Icon", tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Control Panel", style = MaterialTheme.typography.headlineMedium, color = Color.Black)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !hasNotificationPermission) {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Notification Access", fontSize = 18.sp, color = Color.Black)
                Text(if (hasNotificationPermission) "Granted" else "Tap to grant", color = Color.Black)
            }

            HorizontalDivider(color = Color.Black)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Disable DuraSpeed notifications", fontSize = 18.sp, color = Color.Black)
                Switch(
                    checked = disableDuraSpeedNotifications,
                    onCheckedChange = { favoritesRepository.saveDisableDuraSpeedNotifications(it) },
                    colors = eInkSwitchColors
                )
            }

            HorizontalDivider(color = Color.Black)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Set as default launcher", fontSize = 18.sp, color = Color.Black)
            }

            HorizontalDivider(color = Color.Black)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onChooseAlarmAppClicked() }
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Choose alarm app", fontSize = 18.sp, color = Color.Black)
                Text(alarmAppName, color = Color.Black)
            }

            HorizontalDivider(color = Color.Black)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onChooseCalendarAppClicked() }
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Choose calendar app", fontSize = 18.sp, color = Color.Black)
                Text(calendarAppName, color = Color.Black)
            }

            HorizontalDivider(color = Color.Black)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Add:", fontSize = 18.sp, color = Color.Black)
                Row {
                    EInkButton(onClick = onBirthdaysClicked) {
                        Text("Birthday")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    EInkButton(onClick = onRemindersClicked) {
                        Text("Reminder")
                    }
                }
            }

            HorizontalDivider(color = Color.Black)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Enable Gestures", fontSize = 18.sp, color = Color.Black)
                Switch(
                    checked = gesturesEnabled,
                    onCheckedChange = { favoritesRepository.saveGesturesEnabled(it) },
                    colors = eInkSwitchColors
                )
            }

            if (gesturesEnabled) {
                HorizontalDivider(color = Color.Black)

                var showLeftSwipeMenu by remember { mutableStateOf(false) }
                var showRightSwipeMenu by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Swipe Left", fontSize = 18.sp, color = Color.Black)
                    Box {
                        EInkButton(onClick = { showLeftSwipeMenu = true }) {
                            Text(getActionDisplayName(swipeLeftAction))
                        }
                        DropdownMenu(
                            expanded = showLeftSwipeMenu,
                            onDismissRequest = { showLeftSwipeMenu = false }
                        ) {
                            DropdownMenuItem(text = { Text("None") }, onClick = { 
                                favoritesRepository.saveSwipeLeftAction("none")
                                showLeftSwipeMenu = false
                            })
                            DropdownMenuItem(text = { Text("Birthdays") }, onClick = { 
                                favoritesRepository.saveSwipeLeftAction("birthdays")
                                showLeftSwipeMenu = false
                            })
                            DropdownMenuItem(text = { Text("Reminders") }, onClick = { 
                                favoritesRepository.saveSwipeLeftAction("reminders")
                                showLeftSwipeMenu = false
                            })
                            DropdownMenuItem(text = { Text("Open App...") }, onClick = { 
                                onChooseSwipeLeftAppClicked()
                                showLeftSwipeMenu = false
                            })
                        }
                    }
                }

                HorizontalDivider(color = Color.Black)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Swipe Right", fontSize = 18.sp, color = Color.Black)
                     Box {
                        EInkButton(onClick = { showRightSwipeMenu = true }) {
                            Text(getActionDisplayName(swipeRightAction))
                        }
                        DropdownMenu(
                            expanded = showRightSwipeMenu,
                            onDismissRequest = { showRightSwipeMenu = false }
                        ) {
                            DropdownMenuItem(text = { Text("None") }, onClick = { 
                                favoritesRepository.saveSwipeRightAction("none")
                                showRightSwipeMenu = false
                            })
                            DropdownMenuItem(text = { Text("Birthdays") }, onClick = { 
                                favoritesRepository.saveSwipeRightAction("birthdays")
                                showRightSwipeMenu = false
                            })
                            DropdownMenuItem(text = { Text("Reminders") }, onClick = { 
                                favoritesRepository.saveSwipeRightAction("reminders")
                                showRightSwipeMenu = false
                            })
                            DropdownMenuItem(text = { Text("Open App...") }, onClick = { 
                                onChooseSwipeRightAppClicked()
                                showRightSwipeMenu = false
                            })
                        }
                    }
                }
            }


            HorizontalDivider(color = Color.Black)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("The week starts on Sunday", fontSize = 18.sp, color = Color.Black)
                Switch(
                    checked = weekStartsOnSunday,
                    onCheckedChange = { favoritesRepository.saveWeekStartsOnSunday(it) },
                    colors = eInkSwitchColors
                )
            }

            HorizontalDivider(color = Color.Black)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Lock homescreen", fontSize = 18.sp, color = Color.Black)
                Switch(
                    checked = isHomeLocked,
                    onCheckedChange = { favoritesRepository.saveHomeLocked(it) },
                    colors = eInkSwitchColors
                )
            }

            if (!isHomeLocked) {
                HorizontalDivider(color = Color.Black)

                Column(modifier = Modifier.padding(vertical = 16.dp)) {
                    Text("Favorite App Slots: ${sliderPosition.roundToInt()}", fontSize = 18.sp, color = Color.Black)
                    Slider(
                        value = sliderPosition,
                        onValueChange = { sliderPosition = it },
                        valueRange = 1f..20f,
                        steps = 18,
                        onValueChangeFinished = {
                            favoritesRepository.saveFavoriteCount(sliderPosition.roundToInt())
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = Color.Black,
                            activeTrackColor = Color.Black,
                            inactiveTrackColor = Color.Black,
                            activeTickColor = Color.White,
                            inactiveTickColor = Color.White
                        )
                    )
                }
            }

            if (isHomeLocked) {
                HorizontalDivider(color = Color.Black)

                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                    Text("Cat Icon Action", fontSize = 18.sp, color = Color.Black)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EInkButton(
                            onClick = { favoritesRepository.saveCatIconAction("double_touch") },
                            enabled = catIconAction != "double_touch",
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Double Touch")
                        }
                        EInkButton(
                            onClick = { favoritesRepository.saveCatIconAction("long_press") },
                            enabled = catIconAction != "long_press",
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Long Press")
                        }
                    }
                }
            }

            HorizontalDivider(color = Color.Black)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Hide launcher from app view", fontSize = 18.sp, color = Color.Black)
                Switch(
                    checked = hideLauncherFromAppView,
                    onCheckedChange = { favoritesRepository.saveHideLauncherFromAppView(it) },
                    colors = eInkSwitchColors
                )
            }

            HorizontalDivider(color = Color.Black)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showHelpDialog = true }
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Help", fontSize = 18.sp, color = Color.Black)
            }
        }

        if (scrollState.canScrollBackward) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Scroll Up",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(8.dp)
                    .clickable {
                        coroutineScope.launch {
                            scrollState.animateScrollTo(0)
                        }
                    },
                tint = Color.Black
            )
        }

        if (scrollState.canScrollForward) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Scroll Down",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(8.dp)
                    .clickable {
                        coroutineScope.launch {
                            scrollState.animateScrollTo(scrollState.maxValue)
                        }
                    },
                tint = Color.Black
            )
        }
    }
}

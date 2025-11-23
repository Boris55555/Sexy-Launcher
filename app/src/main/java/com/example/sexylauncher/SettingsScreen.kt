package com.example.sexylauncher

import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    onDismiss: () -> Unit,
    favoritesRepository: FavoritesRepository,
    onChooseAlarmAppClicked: () -> Unit,
    onChooseCalendarAppClicked: () -> Unit
) {
    val context = LocalContext.current
    var hasNotificationPermission by remember { mutableStateOf(isNotificationServiceEnabled(context)) }
    val alarmAppPackage by favoritesRepository.alarmAppPackage.collectAsState()
    val calendarAppPackage by favoritesRepository.calendarAppPackage.collectAsState()
    val isHomeLocked by favoritesRepository.isHomeLocked.collectAsState()
    val weekStartsOnSunday by favoritesRepository.weekStartsOnSunday.collectAsState()

    val favoriteCount by favoritesRepository.favoriteCount.collectAsState()
    var sliderPosition by remember(favoriteCount) { mutableStateOf(favoriteCount.toFloat()) }

    val alarmAppName = remember(alarmAppPackage) {
        alarmAppPackage?.let {
            try {
                val packageManager = context.packageManager
                packageManager.getApplicationLabel(packageManager.getApplicationInfo(it, 0)).toString()
            } catch (e: PackageManager.NameNotFoundException) {
                "Unknown"
            }
        } ?: "Not Set"
    }

    val calendarAppName = remember(calendarAppPackage) {
        calendarAppPackage?.let {
            try {
                val packageManager = context.packageManager
                packageManager.getApplicationLabel(packageManager.getApplicationInfo(it, 0)).toString()
            } catch (e: PackageManager.NameNotFoundException) {
                "Unknown"
            }
        } ?: "Not Set"
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

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 16.dp), color = Color.Black)

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

        Divider(color = Color.Black)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Set as default launcher", fontSize = 18.sp, color = Color.Black)
        }

        Divider(color = Color.Black)

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

        Divider(color = Color.Black)

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

        Divider(color = Color.Black)

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
                onCheckedChange = { favoritesRepository.saveWeekStartsOnSunday(it) }
            )
        }

        if (!isHomeLocked) {
            Divider(color = Color.Black)

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

        Divider(color = Color.Black)

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
                onCheckedChange = { favoritesRepository.saveHomeLocked(it) }
            )
        }
    }
}

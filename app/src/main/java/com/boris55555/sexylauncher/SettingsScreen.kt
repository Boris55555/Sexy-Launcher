package com.boris55555.sexylauncher

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
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
    var hasPhonePermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasContactsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasBluetoothPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasExactAlarmPermission by remember {
        mutableStateOf(
            (context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager).canScheduleExactAlarms()
        )
    }
    val alarmAppPackage by favoritesRepository.alarmAppPackage.collectAsState()
    val calendarAppPackage by favoritesRepository.calendarAppPackage.collectAsState()
    val isHomeLocked by favoritesRepository.isHomeLocked.collectAsState()
    val weekStartsOnSunday by favoritesRepository.weekStartsOnSunday.collectAsState()
    val gesturesEnabled by favoritesRepository.gesturesEnabled.collectAsState()
    val swipeLeftAction by favoritesRepository.swipeLeftAction.collectAsState()
    val swipeRightAction by favoritesRepository.swipeRightAction.collectAsState()
    val disableDuraSpeedNotifications by favoritesRepository.disableDuraSpeedNotifications.collectAsState()
    val selectedFont by favoritesRepository.selectedFont.collectAsState()
    val fontSizeHome by favoritesRepository.fontSizeHome.collectAsState()
    val fontSizeAllApps by favoritesRepository.fontSizeAllApps.collectAsState()
    val fontSizeNotifications by favoritesRepository.fontSizeNotifications.collectAsState()
    val use24hFormat by favoritesRepository.use24hFormat.collectAsState()
    val showNotificationPreviews by favoritesRepository.showNotificationPreviews.collectAsState()
    val notificationMaxCharacters by favoritesRepository.notificationMaxCharacters.collectAsState()
    val preferredAppList by favoritesRepository.preferredAppList.collectAsState()
    val showTop10Stars by favoritesRepository.showTop10Stars.collectAsState()
    val sexyMode by favoritesRepository.sexyMode.collectAsState()
    val sexyAlignment by favoritesRepository.sexyAlignment.collectAsState()
    val showNotesButton by favoritesRepository.showNotesButton.collectAsState()

    var isDefaultLauncher by remember { mutableStateOf(false) }

    // Check if we are the default launcher
    fun checkDefaultLauncher() {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        isDefaultLauncher = resolveInfo?.activityInfo?.packageName == context.packageName
    }

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
            checkDefaultLauncher()
            val isEnabled = isNotificationServiceEnabled(context)
            if (isEnabled != hasNotificationPermission) {
                hasNotificationPermission = isEnabled
            }
            
            val phoneEnabled = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
            if (phoneEnabled != hasPhonePermission) {
                hasPhonePermission = phoneEnabled
            }

            val contactsEnabled = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
            if (contactsEnabled != hasContactsPermission) {
                hasContactsPermission = contactsEnabled
            }

            val smsEnabled = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
            if (smsEnabled != hasSmsPermission) {
                hasSmsPermission = smsEnabled
            }

            val btEnabled = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            if (btEnabled != hasBluetoothPermission) {
                hasBluetoothPermission = btEnabled
            }
            
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            val alarmEnabled = alarmManager.canScheduleExactAlarms()
            if (alarmEnabled != hasExactAlarmPermission) {
                hasExactAlarmPermission = alarmEnabled
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

    val phonePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPhonePermission = permissions.values.all { it }
    }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasContactsPermission = isGranted
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasSmsPermission = isGranted
    }

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasBluetoothPermission = isGranted
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            HeaderSection()

            PermissionSection(
                context = context,
                hasNotificationPermission = hasNotificationPermission,
                hasPhonePermission = hasPhonePermission,
                hasContactsPermission = hasContactsPermission,
                hasSmsPermission = hasSmsPermission,
                hasBluetoothPermission = hasBluetoothPermission,
                hasExactAlarmPermission = hasExactAlarmPermission,
                phonePermissionLauncher = phonePermissionLauncher,
                contactsPermissionLauncher = contactsPermissionLauncher,
                smsPermissionLauncher = smsPermissionLauncher,
                bluetoothPermissionLauncher = bluetoothPermissionLauncher
            )

            AddQuickActionsSection(
                onBirthdaysClicked = onBirthdaysClicked,
                onRemindersClicked = onRemindersClicked
            )

            AppSelectionSection(
                alarmAppName = alarmAppName,
                calendarAppName = calendarAppName,
                onChooseAlarmAppClicked = onChooseAlarmAppClicked,
                onChooseCalendarAppClicked = onChooseCalendarAppClicked
            )

            DefaultLauncherSection(context, isDefaultLauncher)

            HomeScreenSection(
                favoritesRepository = favoritesRepository,
                isHomeLocked = isHomeLocked,
                sexyMode = sexyMode,
                sexyAlignment = sexyAlignment,
                showNotesButton = showNotesButton,
                sliderPosition = sliderPosition,
                onSliderChange = { sliderPosition = it },
                use24hFormat = use24hFormat,
                eInkSwitchColors = eInkSwitchColors
            )

            NotificationSection(
                favoritesRepository = favoritesRepository,
                showNotificationPreviews = showNotificationPreviews,
                notificationMaxCharacters = notificationMaxCharacters,
                sexyMode = sexyMode,
                sexyAlignment = sexyAlignment,
                eInkSwitchColors = eInkSwitchColors
            )

            GestureSection(
                favoritesRepository = favoritesRepository,
                gesturesEnabled = gesturesEnabled,
                swipeLeftAction = swipeLeftAction,
                swipeRightAction = swipeRightAction,
                onChooseSwipeLeftAppClicked = onChooseSwipeLeftAppClicked,
                onChooseSwipeRightAppClicked = onChooseSwipeRightAppClicked,
                getActionDisplayName = { getActionDisplayName(it) },
                eInkSwitchColors = eInkSwitchColors
            )

            GeneralFontSection(
                favoritesRepository = favoritesRepository,
                weekStartsOnSunday = weekStartsOnSunday,
                selectedFont = selectedFont,
                fontSizeHome = fontSizeHome,
                fontSizeAllApps = fontSizeAllApps,
                fontSizeNotifications = fontSizeNotifications,
                eInkSwitchColors = eInkSwitchColors
            )

            AppDrawerSection(
                favoritesRepository = favoritesRepository,
                preferredAppList = preferredAppList,
                showTop10Stars = showTop10Stars,
                eInkSwitchColors = eInkSwitchColors
            )

            ExperimentalSection(
                context = context,
                favoritesRepository = favoritesRepository,
                disableDuraSpeedNotifications = disableDuraSpeedNotifications,
                eInkSwitchColors = eInkSwitchColors
            )
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

@Composable
fun HeaderSection() {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
        Icon(Icons.Default.Tune, contentDescription = "Control Panel Icon", tint = Color.Black)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Control Panel", style = MaterialTheme.typography.headlineMedium, color = Color.Black)
    }
}

@Composable
fun PermissionSection(
    context: Context,
    hasNotificationPermission: Boolean,
    hasPhonePermission: Boolean,
    hasContactsPermission: Boolean,
    hasSmsPermission: Boolean,
    hasBluetoothPermission: Boolean,
    hasExactAlarmPermission: Boolean,
    phonePermissionLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>,
    contactsPermissionLauncher: ManagedActivityResultLauncher<String, Boolean>,
    smsPermissionLauncher: ManagedActivityResultLauncher<String, Boolean>,
    bluetoothPermissionLauncher: ManagedActivityResultLauncher<String, Boolean>
) {
    var isExpanded by remember { mutableStateOf(false) }
    val allGranted = hasNotificationPermission && hasPhonePermission && 
                     hasContactsPermission && hasSmsPermission && 
                     hasBluetoothPermission && hasExactAlarmPermission

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Permissions", 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.Bold, 
                    color = Color.Black
                )
                if (allGranted) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Check, 
                        contentDescription = "All Permissions Granted",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = Color.Black
            )
        }
        
        if (isExpanded) {
            Spacer(modifier = Modifier.height(16.dp))
            PermissionRow(
                title = "Notification Access",
                isGranted = hasNotificationPermission,
                onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
            )

            PermissionRow(
                title = "Phone Call Access",
                isGranted = hasPhonePermission,
                onClick = {
                    phonePermissionLauncher.launch(
                        arrayOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_CALL_LOG)
                    )
                }
            )

            PermissionRow(
                title = "Contact Access",
                isGranted = hasContactsPermission,
                onClick = { contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS) }
            )

            PermissionRow(
                title = "SMS Access",
                isGranted = hasSmsPermission,
                onClick = { smsPermissionLauncher.launch(Manifest.permission.READ_SMS) }
            )

            PermissionRow(
                title = "Bluetooth Access",
                isGranted = hasBluetoothPermission,
                onClick = { bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT) }
            )

            PermissionRow(
                title = "Exact Alarm Access",
                isGranted = hasExactAlarmPermission,
                onClick = {
                    try {
                        context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                    } catch (e: Exception) {
                        Log.e("SettingsScreen", "Error opening exact alarm settings", e)
                    }
                }
            )
        }
    }

    HorizontalDivider(color = Color.Black)
}

@Composable
fun PermissionRow(title: String, isGranted: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isGranted) { onClick() }
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, fontSize = 18.sp, color = Color.Black)
        Text(if (isGranted) "Granted" else "Tap to grant", color = if (isGranted) Color.Black else Color.Gray)
    }
    HorizontalDivider(color = Color.Black)
}

@Composable
fun DefaultLauncherSection(context: Context, isDefault: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Set as default launcher", fontSize = 18.sp, color = Color.Black)
        if (isDefault) {
            Icon(Icons.Default.Check, contentDescription = "Default Launcher", tint = Color.Black)
        }
    }

    HorizontalDivider(color = Color.Black)
}

@Composable
fun AddQuickActionsSection(
    onBirthdaysClicked: () -> Unit,
    onRemindersClicked: () -> Unit
) {
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
}

@Composable
fun AppSelectionSection(
    alarmAppName: String,
    calendarAppName: String,
    onChooseAlarmAppClicked: () -> Unit,
    onChooseCalendarAppClicked: () -> Unit
) {
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
}

@Composable
fun HomeScreenSection(
    favoritesRepository: FavoritesRepository,
    isHomeLocked: Boolean,
    sexyMode: Boolean,
    sexyAlignment: String,
    showNotesButton: Boolean,
    sliderPosition: Float,
    onSliderChange: (Float) -> Unit,
    use24hFormat: Boolean,
    eInkSwitchColors: androidx.compose.material3.SwitchColors
) {
    Spacer(modifier = Modifier.height(32.dp))
    Text("Homescreen settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Black)
    Spacer(modifier = Modifier.height(8.dp))
    HorizontalDivider(color = Color.Black)

    if (sexyMode) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Show Notes button", fontSize = 18.sp, color = Color.Black)
            Switch(
                checked = showNotesButton,
                onCheckedChange = { favoritesRepository.saveShowNotesButton(it) },
                colors = eInkSwitchColors
            )
        }
        HorizontalDivider(color = Color.Black)
    }

    HorizontalDivider(color = Color.Black)

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Sexy Mode", fontSize = 18.sp, color = Color.Black)
                Spacer(modifier = Modifier.height(8.dp))
                EInkButton(
                    onClick = { favoritesRepository.saveSexyMode(true) },
                    enabled = !sexyMode,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Select")
                }
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Text Mode", fontSize = 18.sp, color = Color.Black)
                Spacer(modifier = Modifier.height(8.dp))
                EInkButton(
                    onClick = { favoritesRepository.saveSexyMode(false) },
                    enabled = sexyMode,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Select")
                }
            }
        }
        
        if (sexyMode) {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Left", fontSize = 16.sp, color = Color.Black)
                    Spacer(modifier = Modifier.height(8.dp))
                    EInkButton(
                        onClick = { favoritesRepository.saveSexyAlignment("left") },
                        enabled = sexyAlignment != "left",
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Select")
                    }
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Bottom", fontSize = 16.sp, color = Color.Black)
                    Spacer(modifier = Modifier.height(8.dp))
                    EInkButton(
                        onClick = { favoritesRepository.saveSexyAlignment("bottom") },
                        enabled = sexyAlignment != "bottom",
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Select")
                    }
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
                onValueChange = onSliderChange,
                valueRange = 1f..4f,
                steps = 2,
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

    HorizontalDivider(color = Color.Black)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Use 24h clock format", fontSize = 18.sp, color = Color.Black)
        Switch(
            checked = use24hFormat,
            onCheckedChange = { favoritesRepository.saveUse24hFormat(it) },
            colors = eInkSwitchColors
        )
    }

    HorizontalDivider(color = Color.Black)
}

@Composable
fun NotificationSection(
    favoritesRepository: FavoritesRepository,
    showNotificationPreviews: Boolean,
    notificationMaxCharacters: Int,
    sexyMode: Boolean,
    sexyAlignment: String,
    eInkSwitchColors: androidx.compose.material3.SwitchColors
) {
    // Hidden in Sexy Mode as per request
    if (sexyMode) return

    Spacer(modifier = Modifier.height(32.dp))
    Text("Notifications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Black)
    Spacer(modifier = Modifier.height(8.dp))
    HorizontalDivider(color = Color.Black)

    val description = "Show notifications under favorite apps"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Show notification previews", fontSize = 18.sp, color = Color.Black)
            Text(description, fontSize = 14.sp, color = Color.Gray)
        }
        Switch(
            checked = showNotificationPreviews,
            onCheckedChange = { favoritesRepository.saveShowNotificationPreviews(it) },
            colors = eInkSwitchColors
        )
    }
    HorizontalDivider(color = Color.Black)
    
    var showMaxCharsMenu by remember { mutableStateOf(false) }
    val maxCharsOptions = listOf(20, 40, 60, 80, 100)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Max notification characters", fontSize = 18.sp, color = Color.Black)
        Box {
            EInkButton(onClick = { showMaxCharsMenu = true }) {
                Text(notificationMaxCharacters.toString())
            }
            DropdownMenu(
                expanded = showMaxCharsMenu,
                onDismissRequest = { showMaxCharsMenu = false }
            ) {
                maxCharsOptions.forEach { count ->
                    DropdownMenuItem(
                        text = { Text(count.toString()) },
                        onClick = {
                            favoritesRepository.saveNotificationMaxCharacters(count)
                            showMaxCharsMenu = false
                        }
                    )
                }
            }
        }
    }
    HorizontalDivider(color = Color.Black)
}

@Composable
fun GestureSection(
    favoritesRepository: FavoritesRepository,
    gesturesEnabled: Boolean,
    swipeLeftAction: String,
    swipeRightAction: String,
    onChooseSwipeLeftAppClicked: () -> Unit,
    onChooseSwipeRightAppClicked: () -> Unit,
    getActionDisplayName: (String) -> String,
    eInkSwitchColors: androidx.compose.material3.SwitchColors
) {
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
}

@Composable
fun GeneralFontSection(
    favoritesRepository: FavoritesRepository,
    weekStartsOnSunday: Boolean,
    selectedFont: String,
    fontSizeHome: String,
    fontSizeAllApps: String,
    fontSizeNotifications: String,
    eInkSwitchColors: androidx.compose.material3.SwitchColors
) {
    Spacer(modifier = Modifier.height(32.dp))
    Text("General settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Black)
    Spacer(modifier = Modifier.height(8.dp))
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
    
    Text("Font Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.padding(vertical = 8.dp))
    
    var showFontMenu by remember { mutableStateOf(false) }
    val fonts = listOf("Sans Serif", "Serif", "Monospace")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Font Family", fontSize = 18.sp, color = Color.Black)
        Box {
            EInkButton(onClick = { showFontMenu = true }) {
                Text(selectedFont)
            }
            DropdownMenu(
                expanded = showFontMenu,
                onDismissRequest = { showFontMenu = false }
            ) {
                fonts.forEach { font ->
                    DropdownMenuItem(
                        text = { Text(font) },
                        onClick = {
                            favoritesRepository.saveSelectedFont(font)
                            showFontMenu = false
                        }
                    )
                }
            }
        }
    }

    val fontSizes = listOf("Small", "Normal", "Big")
    FontSizeSelector("Home Font Size", fontSizeHome, fontSizes) { favoritesRepository.saveFontSizeHome(it) }
    FontSizeSelector("All Apps Font Size", fontSizeAllApps, fontSizes) { favoritesRepository.saveFontSizeAllApps(it) }
    FontSizeSelector("Notifications Font Size", fontSizeNotifications, fontSizes) { favoritesRepository.saveFontSizeNotifications(it) }

    HorizontalDivider(color = Color.Black)
}

@Composable
fun AppDrawerSection(
    favoritesRepository: FavoritesRepository,
    preferredAppList: String,
    showTop10Stars: Boolean,
    eInkSwitchColors: androidx.compose.material3.SwitchColors
) {
    Spacer(modifier = Modifier.height(32.dp))
    Text("App Drawer settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Black)
    Spacer(modifier = Modifier.height(8.dp))
    HorizontalDivider(color = Color.Black)

    var showPreferredAppListMenu by remember { mutableStateOf(false) }
    val appLists = listOf("All Apps", "Top 10")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Preferred App List", fontSize = 18.sp, color = Color.Black)
        Box {
            EInkButton(onClick = { showPreferredAppListMenu = true }) {
                Text(preferredAppList)
            }
            DropdownMenu(
                expanded = showPreferredAppListMenu,
                onDismissRequest = { showPreferredAppListMenu = false }
            ) {
                appLists.forEach { listName ->
                    DropdownMenuItem(
                        text = { Text(listName) },
                        onClick = {
                            favoritesRepository.savePreferredAppList(listName)
                            showPreferredAppListMenu = false
                        }
                    )
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
        Text("Show Top 10 stars", fontSize = 18.sp, color = Color.Black)
        Switch(
            checked = showTop10Stars,
            onCheckedChange = { favoritesRepository.saveShowTop10Stars(it) },
            colors = eInkSwitchColors
        )
    }

    HorizontalDivider(color = Color.Black)
}

@Composable
fun ExperimentalSection(
    context: Context,
    favoritesRepository: FavoritesRepository,
    disableDuraSpeedNotifications: Boolean,
    eInkSwitchColors: androidx.compose.material3.SwitchColors
) {
    Spacer(modifier = Modifier.height(32.dp))
    Text("Extra Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Black)
    Spacer(modifier = Modifier.height(8.dp))
    HorizontalDivider(color = Color.Black)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Open App Notifications", fontSize = 18.sp, color = Color.Black)
        EInkButton(onClick = {
            try {
                // Try to open the specific "All Apps" notification list
                val intent = Intent("android.settings.ALL_APPS_NOTIFICATION_SETTINGS")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (e: Exception) {
                try {
                    // Fallback to the component name you mentioned
                    val intent = Intent()
                    intent.setClassName("com.android.settings", "com.android.settings.Settings\$NotificationAppListActivity")
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                } catch (e2: Exception) {
                    try {
                        // Last fallback to general notification settings
                        val intent = Intent("android.settings.NOTIFICATION_SETTINGS")
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    } catch (e3: Exception) {
                        Toast.makeText(context, "Could not open notification settings", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }) {
            Text("Open")
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
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Open DuraSpeed", fontSize = 18.sp, color = Color.Black)
        EInkButton(onClick = {
            val pkgName = "com.mediatek.duraspeed"
            try {
                // Method 1: Standard App Info
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = android.net.Uri.parse("package:$pkgName")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (e: Exception) {
                try {
                    // Method 2: Alternative App Info (some older devices)
                    val intent = Intent()
                    intent.action = android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    intent.addCategory(Intent.CATEGORY_DEFAULT)
                    intent.data = android.net.Uri.parse("package:$pkgName")
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                } catch (e2: Exception) {
                    android.util.Log.e("SettingsScreen", "Failed to open DuraSpeed settings", e2)
                    Toast.makeText(context, "Could not open settings for $pkgName", Toast.LENGTH_SHORT).show()
                }
            }
        }) {
            Text("Open")
        }
    }

    HorizontalDivider(color = Color.Black)
}

@Composable
fun FontSizeSelector(label: String, selectedSize: String, sizes: List<String>, onSizeSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 18.sp, color = Color.Black)
        Box {
            EInkButton(onClick = { expanded = true }) {
                Text(selectedSize)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                sizes.forEach { size ->
                    DropdownMenuItem(
                        text = { Text(size) },
                        onClick = {
                            onSizeSelected(size)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

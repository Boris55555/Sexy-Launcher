package com.boris55555.sexylauncher

import android.app.Activity
import android.app.AlarmManager
import android.Manifest
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.BatteryManager
import android.os.Build
import android.telecom.TelecomManager
import android.telephony.SignalStrength
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessAlarm
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boris55555.sexylauncher.birthdays.BirthdaysRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Locale

@Composable
fun HomeScreen(
    favoritesRepository: FavoritesRepository,
    birthdaysRepository: BirthdaysRepository,
    onShowAllAppsClicked: () -> Unit,
    onShowNotificationsClicked: () -> Unit,
    onShowBirthdaysClicked: () -> Unit,
    onShowRemindersClicked: () -> Unit,
    onLaunchAppClicked: (String) -> Unit,
    onShowSettingsClicked: () -> Unit,
    onEditFavorite: (Int) -> Unit,
    currentPage: Int,
    onCurrentPageChanged: (Int) -> Unit,
    bluetoothState: MainActivity.BluetoothState
) {
    MainHomeScreen(
        favoritesRepository,
        birthdaysRepository,
        onShowAllAppsClicked,
        onShowNotificationsClicked,
        onShowBirthdaysClicked,
        onShowRemindersClicked,
        onLaunchAppClicked,
        onShowSettingsClicked,
        onEditFavorite,
        currentPage,
        onCurrentPageChanged,
        bluetoothState
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainHomeScreen(
    favoritesRepository: FavoritesRepository,
    birthdaysRepository: BirthdaysRepository,
    onShowAllAppsClicked: () -> Unit,
    onShowNotificationsClicked: () -> Unit,
    onShowBirthdaysClicked: () -> Unit,
    onShowRemindersClicked: () -> Unit,
    onLaunchAppClicked: (String) -> Unit,
    onShowSettingsClicked: () -> Unit,
    onEditFavorite: (Int) -> Unit,
    currentPage: Int,
    onCurrentPageChanged: (Int) -> Unit,
    bluetoothState: MainActivity.BluetoothState
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    var showRefreshOverlay by remember { mutableStateOf(false) }

    val favoritePackages by favoritesRepository.favorites.collectAsState()
    val favoriteCount by favoritesRepository.favoriteCount.collectAsState()
    val alarmAppPackage by favoritesRepository.alarmAppPackage.collectAsState()
    val calendarAppPackage by favoritesRepository.calendarAppPackage.collectAsState()
    val isHomeLocked by favoritesRepository.isHomeLocked.collectAsState()
    val customNames by favoritesRepository.customNames.collectAsState()
    val birthdays by birthdaysRepository.birthdays.collectAsState()
    val gesturesEnabled by favoritesRepository.gesturesEnabled.collectAsState()
    val swipeLeftAction by favoritesRepository.swipeLeftAction.collectAsState()
    val swipeRightAction by favoritesRepository.swipeRightAction.collectAsState()
    val keepAllAppsButton = true // Always on
    val showAppIcons by favoritesRepository.showAppIcons.collectAsState()
    val showNotificationPreviews by favoritesRepository.showNotificationPreviews.collectAsState()
    val sexyMode by favoritesRepository.sexyMode.collectAsState()
    val sexyAlignment by favoritesRepository.sexyAlignment.collectAsState()
    val enableCameraShortcut = true // Always on
    val showNotesButton by favoritesRepository.showNotesButton.collectAsState()
    val homeNote by favoritesRepository.homeNote.collectAsState()
    val homeNoteTitle by favoritesRepository.homeNoteTitle.collectAsState()
    val fontSizeHome by favoritesRepository.fontSizeHome.collectAsState()
    val use24hFormat by favoritesRepository.use24hFormat.collectAsState()

    val fontSizeAdjustment = when (fontSizeHome) {
        "Small" -> -2
        "Big" -> 2
        else -> 0
    }

    val favoriteApps = remember(favoritePackages, customNames) {
        favoritePackages.map { pkgName ->
            if (pkgName == null) null
            else {
                try {
                    val intent = Intent(Intent.ACTION_MAIN, null).apply {
                        addCategory(Intent.CATEGORY_LAUNCHER)
                        `package` = pkgName
                    }
                    packageManager.queryIntentActivities(intent, 0).firstOrNull()?.let { resolveInfo ->
                        val originalName = resolveInfo.loadLabel(packageManager).toString()
                        val customName = customNames[pkgName]
                        val appInfo = packageManager.getApplicationInfo(pkgName, 0)
                        val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                        AppInfo(
                            name = customName ?: originalName,
                            packageName = resolveInfo.activityInfo.packageName,
                            customName = customName,
                            isSystemApp = isSystem || pkgName.startsWith("com.mudita")
                        )
                    }
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    val notifications by NotificationListener.notifications.collectAsState()

    // Media Controller State
    var mediaController by remember { mutableStateOf<MediaController?>(null) }
    var mediaMetadata by remember { mutableStateOf<MediaMetadata?>(null) }
    var playbackState by remember { mutableStateOf<PlaybackState?>(null) }

    val controllerCallback = remember {
        object : MediaController.Callback() {
            override fun onMetadataChanged(metadata: MediaMetadata?) {
                mediaMetadata = metadata
            }

            override fun onPlaybackStateChanged(state: PlaybackState?) {
                playbackState = state
                if (state?.state == PlaybackState.STATE_STOPPED) {
                    mediaController = null
                }
            }
        }
    }

    val mediaSessionManager = remember { context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager }
    val componentName = remember { ComponentName(context, NotificationListener::class.java) }

    fun refreshMediaController() {
        val controllers = try {
            mediaSessionManager.getActiveSessions(componentName)
        } catch (e: Exception) {
            null
        }
        val activeController = controllers?.firstOrNull { 
            val state = it.playbackState?.state
            state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_PAUSED || state == PlaybackState.STATE_BUFFERING
        }

        if (activeController != null && activeController.packageName != mediaController?.packageName) {
            mediaController?.unregisterCallback(controllerCallback)
            mediaController = activeController
            activeController.registerCallback(controllerCallback)
            mediaMetadata = activeController.metadata
            playbackState = activeController.playbackState
        }
    }

    DisposableEffect(context) {
        if (!isNotificationServiceEnabled(context)) {
            return@DisposableEffect onDispose {}
        }

        val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            refreshMediaController()
        }

        mediaSessionManager.addOnActiveSessionsChangedListener(sessionListener, componentName)
        refreshMediaController()

        onDispose {
            mediaController?.unregisterCallback(controllerCallback)
            mediaSessionManager.removeOnActiveSessionsChangedListener(sessionListener)
        }
    }

    // Refresh when returning to home or when notifications change
    LaunchedEffect(notifications) {
        if (mediaController == null) {
            refreshMediaController()
        }
    }

    val pageCount = (favoriteCount + 3) / 4
    var nextAlarmTime by remember { mutableStateOf<String?>(null) }
    var nextAlarmDay by remember { mutableStateOf<String?>(null) }

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun updateAlarm() {
        val nextAlarmClock = alarmManager.nextAlarmClock
        if (nextAlarmClock != null) {
            val now = Calendar.getInstance()
            val alarmCalendar = Calendar.getInstance().apply { timeInMillis = nextAlarmClock.triggerTime }
            
            nextAlarmTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(alarmCalendar.time)
            
            nextAlarmDay = when {
                now.get(Calendar.YEAR) == alarmCalendar.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == alarmCalendar.get(Calendar.DAY_OF_YEAR) -> null
                
                now.get(Calendar.YEAR) == alarmCalendar.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) + 1 == alarmCalendar.get(Calendar.DAY_OF_YEAR) -> "Tomorrow"
                
                else -> SimpleDateFormat("EEE", Locale.getDefault()).format(alarmCalendar.time)
            }
        } else {
            nextAlarmTime = null
            nextAlarmDay = null
        }
    }

    DisposableEffect(context) {
        val receiver = AlarmUpdateReceiver(::updateAlarm)
        context.registerReceiver(receiver, IntentFilter("android.app.action.NEXT_ALARM_CLOCK_CHANGED"))
        updateAlarm() // Initial update
        onDispose { context.unregisterReceiver(receiver) }
    }

    if (currentPage >= pageCount) {
        onCurrentPageChanged(0)
    }

    val batteryLevel by context.batteryLevel().collectAsState(initial = null)
    val signalLevel by context.signalLevel().collectAsState(initial = 0)
    var showStatusDetails by remember { mutableStateOf(false) }

    LaunchedEffect(showStatusDetails) {
        if (showStatusDetails) {
            delay(3000)
            showStatusDetails = false
        }
    }
    var favoritesArea by remember { mutableStateOf<Rect?>(null) }
    var showEditNoteDialog by remember { mutableStateOf(false) }

    if (showEditNoteDialog) {
        var tempTitle by remember { mutableStateOf(homeNoteTitle) }
        var tempContent by remember { mutableStateOf(homeNote) }
        AlertDialog(
            onDismissRequest = { showEditNoteDialog = false },
            title = { Text("Edit Home Note", color = Color.Black) },
            text = {
                Column {
                    OutlinedTextField(
                        value = tempTitle,
                        onValueChange = { tempTitle = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Black,
                            unfocusedBorderColor = Color.Black,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tempContent,
                        onValueChange = { tempContent = it },
                        label = { Text("Content") },
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Black,
                            unfocusedBorderColor = Color.Black,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        )
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EInkButton(onClick = {
                        tempTitle = ""
                        tempContent = ""
                        favoritesRepository.saveHomeNoteTitle("")
                        favoritesRepository.saveHomeNote("")
                        showEditNoteDialog = false
                    }) {
                        Text("Clear")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    EInkButton(onClick = { showEditNoteDialog = false }) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    EInkButton(onClick = {
                        favoritesRepository.saveHomeNoteTitle(tempTitle)
                        favoritesRepository.saveHomeNote(tempContent)
                        showEditNoteDialog = false
                    }) {
                        Text("Save")
                    }
                }
            },
            dismissButton = null,
            containerColor = Color.White
        )
    }

    fun handleSwipe(action: String) {
        when (action) {
            "notifications" -> onShowNotificationsClicked()
            "birthdays" -> onShowBirthdaysClicked()
            "reminders" -> onShowRemindersClicked()
            else -> if (action.startsWith("app:")) {
                onLaunchAppClicked(action.substring(4))
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .homeScreenGestures(
                gesturesEnabled = gesturesEnabled,
                favoritesArea = favoritesArea,
                onSwipeUp = onShowAllAppsClicked,
                onSwipeLeft = { handleSwipe(swipeLeftAction) },
                onSwipeRight = { handleSwipe(swipeRightAction) },
                onFavoritesSwipeUp = { if (currentPage < pageCount - 1) onCurrentPageChanged(currentPage + 1) },
                onFavoritesSwipeDown = { if (currentPage > 0) onCurrentPageChanged(currentPage - 1) }
            )
            .pointerInput(isHomeLocked, favoritesArea) {
                detectTapGestures(
                    onLongPress = { if (!isHomeLocked) onShowSettingsClicked() },
                    onDoubleTap = { showRefreshOverlay = true }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val hideStatusBar = true // Always on

            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                if (hideStatusBar) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(top = 8.dp) // Added small padding to align with calendar text
                            .clickable {
                                alarmAppPackage?.let {
                                    val launchIntent = packageManager.getLaunchIntentForPackage(it)
                                    if (launchIntent != null) {
                                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(launchIntent)
                                    }
                                }
                            },
                        horizontalAlignment = Alignment.Start
                    ) {
                        var currentTime by remember(use24hFormat) { 
                            val pattern = if (use24hFormat) "HH:mm" else "h:mm a"
                            mutableStateOf(SimpleDateFormat(pattern, Locale.getDefault()).format(Calendar.getInstance().time)) 
                        }
                        LaunchedEffect(use24hFormat) {
                            while (true) {
                                val pattern = if (use24hFormat) "HH:mm" else "h:mm a"
                                currentTime = SimpleDateFormat(pattern, Locale.getDefault()).format(Calendar.getInstance().time)
                                delay(1000)
                            }
                        }
                        Text(
                            text = currentTime,
                            fontSize = ((if (use24hFormat) 24 else 20) + fontSizeAdjustment).sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        if (nextAlarmTime != null) {
                            Column(horizontalAlignment = Alignment.Start) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.AccessAlarm, 
                                        contentDescription = null, 
                                        tint = Color.Black, 
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = nextAlarmTime!!,
                                        fontSize = (12 + fontSizeAdjustment).sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                                if (nextAlarmDay != null) {
                                    Text(
                                        text = nextAlarmDay!!,
                                        fontSize = (10 + fontSizeAdjustment).sp,
                                        fontWeight = FontWeight.Normal,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    }
                } else if (nextAlarmTime != null) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .clickable {
                                alarmAppPackage?.let {
                                    val launchIntent = packageManager.getLaunchIntentForPackage(it)
                                    if (launchIntent != null) {
                                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(launchIntent)
                                    }
                                }
                            },
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AccessAlarm, contentDescription = "Next Alarm", tint = Color.Black, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = nextAlarmTime!!, fontSize = (20 + fontSizeAdjustment).sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                        if (nextAlarmDay != null) {
                            Text(text = nextAlarmDay!!, fontSize = (14 + fontSizeAdjustment).sp, fontWeight = FontWeight.Normal, color = Color.Black)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clickable {
                            calendarAppPackage?.let {
                                val launchIntent = packageManager.getLaunchIntentForPackage(it)
                                if (launchIntent != null) {
                                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(launchIntent)
                                }
                            }
                        }
                ) {
                    DateText()
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp) // Moved more to the right (away from calendar)
                        .clickable { showStatusDetails = !showStatusDetails },
                    contentAlignment = Alignment.CenterEnd
                ) {
                    if (showStatusDetails) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "LTE ${signalLevel * 25}%",
                                fontSize = (10 + fontSizeAdjustment).sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Text(
                                text = "BAT ${batteryLevel?.level ?: 0}%",
                                fontSize = (10 + fontSizeAdjustment).sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SignalIcon(signalLevel)
                            BatteryIcon(batteryLevel)
                        }
                    }
                }
            }

            val today = LocalDate.now()
            val todaysBirthdays = birthdays.filter { it.date.month == today.month && it.date.dayOfMonth == today.dayOfMonth }

            // Container for notifications and birthdays to stabilize layout
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Fixed-size box for the notification indicator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    NotificationIndicator(
                        notifications = notifications,
                        bluetoothState = bluetoothState,
                        onClick = {
                            NotificationListener.instance?.requestRefresh()
                            onShowNotificationsClicked()
                        }
                    )
                }
                // Fixed-size box for birthday indicator(s)
                Box(modifier = Modifier.height(40.dp), contentAlignment = Alignment.Center) {
                    if (todaysBirthdays.isNotEmpty()) {
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            todaysBirthdays.forEachIndexed { index, birthday ->
                                val age = ChronoUnit.YEARS.between(birthday.date, today)
                                Text(
                                    text = "🎂 ${birthday.name} $age years!",
                                    fontSize = (18 + fontSizeAdjustment).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    maxLines = 1
                                )
                                if (index < todaysBirthdays.lastIndex) {
                                    Spacer(modifier = Modifier.width(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Central Note Area for Sexy Mode (Bottom Alignment only)
            if (sexyMode && sexyAlignment == "bottom") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(2.5f)
                        .padding(horizontal = 48.dp, vertical = 0.dp), // Reduced vertical padding
                    contentAlignment = Alignment.TopCenter // Changed from Center to TopCenter
                ) {
                    if (showNotesButton && (homeNote.isNotBlank() || homeNoteTitle.isNotBlank())) {
                        StickyNote(
                            title = homeNoteTitle,
                            text = homeNote,
                            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                            fontSizeAdjustment = fontSizeAdjustment
                        )
                    }
                }
            } else if (!sexyMode) {
                Spacer(modifier = Modifier.weight(0.3f))
            } else {
                // Sexy Left mode: Use a smaller spacer to push everything to the center
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (sexyMode && sexyAlignment == "bottom") {
                // Sexy Mode Bottom: Favorites in a row at the bottom, MiniPlayer above
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 0.dp), 
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // MiniPlayer above favorites in Bottom mode
                    if (mediaController != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .padding(bottom = 16.dp)
                        ) {
                            MiniPlayer(
                                mediaMetadata = mediaMetadata,
                                playbackState = playbackState,
                                mediaController = mediaController,
                                onClose = { 
                                    mediaController?.transportControls?.stop()
                                    mediaController = null 
                                }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .onGloballyPositioned { favoritesArea = it.boundsInRoot() },
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val pageStart = currentPage * 4
                        val pageEnd = minOf((currentPage + 1) * 4, favoriteCount)

                        for (i in pageStart until pageEnd) {
                            val app = favoriteApps.getOrNull(i)
                            FavoriteAppItem(
                                app = app ?: AppInfo("", "", isSystemApp = false), // Handle null app info for "Add" box
                                notifications = notifications,
                                showAppIcons = showAppIcons,
                                showNotificationPreviews = false, // Always off in Bottom mode
                                sexyMode = true,
                                sexyAlignment = "bottom",
                                onLongClick = { if (!isHomeLocked) onEditFavorite(i) },
                                onClick = {
                                    if (app != null) {
                                        // Standard launch logic...
                                        val isPhoneApp = app.packageName == "com.mudita.dial" || 
                                                         app.packageName.contains("dialer") || 
                                                         app.packageName.contains("telecom")

                                        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                                        val isCallActive = telephonyManager.callState != TelephonyManager.CALL_STATE_IDLE

                                        if (isPhoneApp && isCallActive) {
                                            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                                            try { 
                                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                                                    telecomManager.showInCallScreen(false) 
                                                } else {
                                                    throw SecurityException("Missing permission")
                                                }
                                            } catch (e: Exception) {
                                                val intent = Intent(Intent.ACTION_MAIN).apply {
                                                    addCategory(Intent.CATEGORY_LAUNCHER)
                                                    `package` = app.packageName
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                                                }
                                                context.startActivity(intent)
                                            }
                                        } else {
                                            val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName)
                                            launchIntent?.let {
                                                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                context.startActivity(it)
                                            }
                                        }
                                    } else {
                                        if (!isHomeLocked) onEditFavorite(i)
                                    }
                                },
                                fontSizeAdjustment = fontSizeAdjustment
                            )
                        }
                    }
                }
            } else {
                // Standard or Sexy Mode Left
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (sexyMode) Modifier.weight(3f) else Modifier.padding(bottom = 48.dp)), 
                    verticalAlignment = Alignment.Top // Align to top for better consistency
                ) {
                    if (sexyMode) {
                        // Sexy Mode: Scrollable favorites on the far left, square boxes
                        Column(
                            modifier = Modifier
                                .width(80.dp) // Fixed width for favorites column
                                .padding(start = 4.dp)
                                .onGloballyPositioned { favoritesArea = it.boundsInRoot() },
                            verticalArrangement = Arrangement.Top,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Up arrow if not on the first page
                            if (currentPage > 0) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropUp,
                                    contentDescription = "More favorites above",
                                    tint = Color.Black,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clickable { onCurrentPageChanged(currentPage - 1) }
                                )
                            } else {
                                Spacer(modifier = Modifier.size(24.dp))
                            }

                            val pageStart = currentPage * 4
                            val pageEnd = minOf((currentPage + 1) * 4, favoriteCount)

                            for (i in pageStart until pageEnd) {
                                val app = favoriteApps.getOrNull(i)
                                if (app != null) {
                                    FavoriteAppItem(
                                        app = app, 
                                        notifications = notifications,
                                        showAppIcons = showAppIcons,
                                        showNotificationPreviews = false, // Sticky note replaces previews
                                        sexyMode = sexyMode,
                                        sexyAlignment = sexyAlignment,
                                        onLongClick = { 
                                            if (!isHomeLocked) {
                                                onEditFavorite(i)
                                            } 
                                        }, 
                                        onClick = { 
                                            val isPhoneApp = app.packageName == "com.mudita.dial" || 
                                                             app.packageName.contains("dialer") || 
                                                             app.packageName.contains("telecom")

                                            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                                            val isCallActive = telephonyManager.callState != TelephonyManager.CALL_STATE_IDLE

                                            if (isPhoneApp && isCallActive) {
                                                // If it's the phone app and a call is active, try to bring the in-call UI to front
                                                val intent = Intent(Intent.ACTION_MAIN)
                                                intent.addCategory(Intent.CATEGORY_LAUNCHER)
                                                intent.`package` = app.packageName
                                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                                                
                                                // Also try TelecomManager if API level allows
                                                val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                                                try {
                                                    // showInCallScreen(false) brings the in-call screen to the foreground
                                                    telecomManager.showInCallScreen(false)
                                                } catch (e: SecurityException) {
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    context.startActivity(intent)
                                                }
                                            } else {
                                                val appNotifications = notifications.filter { it.packageName == app.packageName }
                                                val callNotification = appNotifications.find { sbn ->
                                                    getNotificationCategory(sbn, context) == NotificationCategory.CALLS &&
                                                    (sbn.notification.flags and android.app.Notification.FLAG_ONGOING_EVENT) != 0
                                                }

                                                if (callNotification != null && callNotification.notification.contentIntent != null) {
                                                    try {
                                                        callNotification.notification.contentIntent.send()
                                                    } catch (e: Exception) {
                                                        val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName)
                                                        if (launchIntent != null) {
                                                            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                            context.startActivity(launchIntent)
                                                        }
                                                    }
                                                } else {
                                                    val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName)
                                                    if (launchIntent != null) {
                                                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                        context.startActivity(launchIntent)
                                                    }
                                                }
                                            }
                                        },
                                        fontSizeAdjustment = fontSizeAdjustment
                                    )
                                } else {
                                    // Add app box in Sexy Mode
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(80.dp)
                                            .padding(horizontal = 8.dp)
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.width(72.dp)
                                        ) {
                                            Box(
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clickable { if (!isHomeLocked) onEditFavorite(i) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Add app",
                                                tint = Color.Black,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                            // Empty space where the label would be
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }
                                    }
                                }
                            }

                            // Down arrow if not on the last page
                            if (currentPage < pageCount - 1) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "More favorites below",
                                    tint = Color.Black,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clickable { onCurrentPageChanged(currentPage + 1) }
                                )
                            } else {
                                Spacer(modifier = Modifier.size(24.dp))
                            }
                        }
                        
                        // Sticky Note beside Favorites in Left Mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 16.dp, end = 48.dp, top = 8.dp), // Moved up from 24.dp
                            contentAlignment = Alignment.TopCenter
                        ) {
                            if (showNotesButton && (homeNote.isNotBlank() || homeNoteTitle.isNotBlank())) {
                                StickyNote(
                                    title = homeNoteTitle,
                                    text = homeNote,
                                    modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                                    fontSizeAdjustment = fontSizeAdjustment
                                )
                            }
                        }
                    } else {
                        // Standard Layout
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 48.dp), // Enough space for MiniPlayer
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Page indicators
                            if (pageCount > 1) {
                                Column(
                                    modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    repeat(pageCount) { pageIndex ->
                                        val indicatorModifier = if (currentPage == pageIndex) {
                                            Modifier.background(Color.Black, shape = CircleShape)
                                        } else {
                                            Modifier.border(BorderStroke(1.dp, Color.Black), shape = CircleShape)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .padding(vertical = 4.dp)
                                                .size(18.dp)
                                                .then(indicatorModifier)
                                                .clickable { onCurrentPageChanged(pageIndex) }
                                        )
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.width(50.dp))
                            }

                            // Favorites list
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(
                                        start = 16.dp,
                                        end = if (!gesturesEnabled || keepAllAppsButton) 32.dp else 0.dp
                                    )
                                    .onGloballyPositioned { favoritesArea = it.boundsInRoot() },
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.Start
                            ) {
                                val pageStart = currentPage * 4
                                val pageEnd = minOf((currentPage + 1) * 4, favoriteCount)

                                for (i in pageStart until pageEnd) {
                                    val app = favoriteApps.getOrNull(i)
                                    if (app != null) {
                                        FavoriteAppItem(
                                            app = app, 
                                            notifications = notifications,
                                            showAppIcons = showAppIcons,
                                            showNotificationPreviews = showNotificationPreviews,
                                            sexyMode = sexyMode,
                                            onLongClick = { 
                                                if (!isHomeLocked) {
                                                    onEditFavorite(i)
                                                } 
                                            }, 
                                            onClick = { 
                                                val isPhoneApp = app.packageName == "com.mudita.dial" || 
                                                                 app.packageName.contains("dialer") || 
                                                                 app.packageName.contains("telecom")

                                                val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                                                val isCallActive = telephonyManager.callState != TelephonyManager.CALL_STATE_IDLE

                                                if (isPhoneApp && isCallActive) {
                                                    // If it's the phone app and a call is active, try to bring the in-call UI to front
                                                    val intent = Intent(Intent.ACTION_MAIN)
                                                    intent.addCategory(Intent.CATEGORY_LAUNCHER)
                                                    intent.`package` = app.packageName
                                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                                                    
                                                    // Also try TelecomManager if API level allows
                                                    val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                                                    try {
                                                        // showInCallScreen(false) brings the in-call screen to the foreground
                                                        telecomManager.showInCallScreen(false)
                                                    } catch (e: SecurityException) {
                                                        context.startActivity(intent)
                                                    } catch (e: Exception) {
                                                        context.startActivity(intent)
                                                    }
                                                } else {
                                                    val appNotifications = notifications.filter { it.packageName == app.packageName }
                                                    val callNotification = appNotifications.find { sbn ->
                                                        getNotificationCategory(sbn, context) == NotificationCategory.CALLS &&
                                                        (sbn.notification.flags and android.app.Notification.FLAG_ONGOING_EVENT) != 0
                                                    }

                                                    if (callNotification != null && callNotification.notification.contentIntent != null) {
                                                        try {
                                                            callNotification.notification.contentIntent.send()
                                                        } catch (e: Exception) {
                                                            val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName)
                                                            if (launchIntent != null) {
                                                                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                                context.startActivity(launchIntent)
                                                            }
                                                        }
                                                    } else {
                                                        val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName)
                                                        if (launchIntent != null) {
                                                            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                            context.startActivity(launchIntent)
                                                        }
                                                    }
                                                }
                                            },
                                            fontSizeAdjustment = fontSizeAdjustment
                                        )
                                    } else {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier
                                                .padding(8.dp)
                                                .clickable { if (!isHomeLocked) onEditFavorite(i) }
                                        ) {
                                            Text(
                                                text = "[Press here + ]",
                                                fontSize = (32 + fontSizeAdjustment).sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Black
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (sexyMode) {
                Spacer(modifier = Modifier.height(8.dp))
            } else {
                Spacer(modifier = Modifier.weight(0.8f))
            }
        }

        if (!gesturesEnabled || keepAllAppsButton) {
            // All Apps Button (Fixed Position relative to screen center)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(y = (-32).dp) // Absolute shift to align with power button
                    .size(width = 32.dp, height = 100.dp)
                    .background(Color.White)
                    .border(
                        BorderStroke(2.dp, Color.Black),
                        RoundedCornerShape(topStartPercent = 50, bottomStartPercent = 50)
                    )
                    .clip(RoundedCornerShape(topStartPercent = 50, bottomStartPercent = 50))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onShowAllAppsClicked() }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Show all apps",
                    tint = Color.Black
                )
            }

                // Camera Shortcut (Fixed Position relative to All Apps)
            if (enableCameraShortcut) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .offset(y = 59.dp) // -32 (All Apps) + 50 (half All Apps) + 16 (gap) + 25 (half Camera) = 59
                        .size(width = 32.dp, height = 50.dp)
                        .background(Color.White)
                        .border(
                            BorderStroke(2.dp, Color.Black),
                            RoundedCornerShape(topStartPercent = 50, bottomStartPercent = 50)
                        )
                        .clip(RoundedCornerShape(topStartPercent = 50, bottomStartPercent = 50))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            val cameraIntents = listOf(
                                Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),
                                Intent("android.media.action.STILL_IMAGE_CAMERA"),
                                Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
                            )
                            
                            var started = false
                            for (intent in cameraIntents) {
                                try {
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(intent)
                                    started = true
                                    break
                                } catch (_: Exception) {}
                            }
                            
                            if (!started) {
                                // Try common camera package names as last resort
                                val packages = listOf("com.mudita.camera", "com.android.camera", "com.android.camera2", "com.google.android.GoogleCamera")
                                for (pkg in packages) {
                                    try {
                                        val intent = packageManager.getLaunchIntentForPackage(pkg)
                                        if (intent != null) {
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            context.startActivity(intent)
                                            started = true
                                            break
                                        }
                                    } catch (_: Exception) {}
                                }
                            }
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Open Camera",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Add Note Shortcut (+)
                if (sexyMode && showNotesButton) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .offset(y = 117.dp) // 59 (Camera) + 25 (half Camera) + 8 (gap) + 25 (half Plus) = 117
                            .size(width = 32.dp, height = 50.dp)
                            .background(Color.White)
                            .border(
                                BorderStroke(2.dp, Color.Black),
                                RoundedCornerShape(topStartPercent = 50, bottomStartPercent = 50)
                            )
                            .clip(RoundedCornerShape(topStartPercent = 50, bottomStartPercent = 50))
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                showEditNoteDialog = true
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.HistoryEdu,
                            contentDescription = "Add Note",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        if (mediaController != null && !(sexyMode && sexyAlignment == "bottom")) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                MiniPlayer(
                    mediaMetadata = mediaMetadata,
                    playbackState = playbackState,
                    mediaController = mediaController,
                    onClose = { 
                        mediaController?.transportControls?.stop()
                        mediaController = null 
                    }
                )
            }
        }

        // Refresh overlay on top of everything
        if (showRefreshOverlay) {
            Box(modifier = Modifier
                .fillMaxSize()
                .background(Color.Black))
            LaunchedEffect(Unit) {
                delay(120L)
                showRefreshOverlay = false
            }
        }
    }
}

data class BatteryState(val level: Int, val isCharging: Boolean)

fun Context.batteryLevel(): Flow<BatteryState> = callbackFlow {
    val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val status: Int = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || 
                             status == BatteryManager.BATTERY_STATUS_FULL
            
            val percentage = if (level != -1 && scale != -1) {
                (level * 100 / scale.toFloat()).toInt()
            } else {
                -1
            }
            trySend(BatteryState(percentage, isCharging))
        }
    }
    val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
    registerReceiver(batteryReceiver, filter)
    awaitClose { unregisterReceiver(batteryReceiver) }
}

fun Context.signalLevel(): Flow<Int> = callbackFlow {
    val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val callback = object : TelephonyCallback(), TelephonyCallback.SignalStrengthsListener {
            override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                trySend(signalStrength.level)
            }
        }
        telephonyManager.registerTelephonyCallback(mainExecutor, callback)
        awaitClose { telephonyManager.unregisterTelephonyCallback(callback) }
    } else {
        // Fallback for older versions if needed, though Mudita Kompakt is Android 11+
        trySend(0)
        awaitClose { }
    }
}

@Composable
fun SignalIcon(level: Int) {
    // level is 0-4, Horizontal dots
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..4) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .then(
                        if (i <= level) {
                            Modifier.background(Color.Black, shape = CircleShape)
                        } else {
                            Modifier.border(1.dp, Color.Black, shape = CircleShape)
                        }
                    )
            )
        }
    }
}

@Composable
fun BatteryIcon(state: BatteryState?) {
    if (state == null) return
    val level = state.level
    val barLevel = when {
        level > 75 -> 4
        level > 50 -> 3
        level > 25 -> 2
        level > 5 -> 1
        else -> 0
    }
    
    // Horizontal dots battery
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (state.isCharging) {
            Icon(
                imageVector = Icons.Default.Bolt,
                contentDescription = "Charging",
                modifier = Modifier.size(14.dp),
                tint = Color.Black
            )
            Spacer(modifier = Modifier.width(2.dp))
        }
        for (i in 1..4) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .then(
                        if (i <= barLevel) {
                            Modifier.background(Color.Black, shape = CircleShape)
                        } else {
                            Modifier.border(1.dp, Color.Black, shape = CircleShape)
                        }
                    )
            )
        }
    }
}

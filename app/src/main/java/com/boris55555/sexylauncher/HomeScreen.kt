package com.boris55555.sexylauncher

import android.app.Activity
import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessAlarm
import androidx.compose.material.icons.filled.Battery0Bar
import androidx.compose.material.icons.filled.Battery2Bar
import androidx.compose.material.icons.filled.Battery4Bar
import androidx.compose.material.icons.filled.Battery6Bar
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.SignalCellular0Bar
import androidx.compose.material.icons.filled.SignalCellular4Bar
import androidx.compose.material.icons.filled.SignalCellularNull
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
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
import kotlin.math.roundToInt

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
    onCurrentPageChanged: (Int) -> Unit
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
        onCurrentPageChanged
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
    onCurrentPageChanged: (Int) -> Unit
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
    val catIconAction by favoritesRepository.catIconAction.collectAsState()
    val showAppIcons by favoritesRepository.showAppIcons.collectAsState()
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
                .padding(horizontal = 16.dp)
                .padding(top = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val hideStatusBar by favoritesRepository.hideStatusBar.collectAsState()

            Box(modifier = Modifier.fillMaxWidth()) {
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
                    DateText(favoritesRepository)
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
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Fixed-size box for the notification indicator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    NotificationIndicator(
                        notifications = notifications,
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

            Spacer(modifier = Modifier.weight(0.3f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
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
                        .padding(start = 16.dp)
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
                                notifications = notifications.filter { it.packageName == app.packageName }.sortedByDescending { it.postTime }, 
                                showAppIcons = showAppIcons,
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

                                    notifications.filter { it.packageName == app.packageName }.forEach { sbn ->
                                        if (sbn.notification.category != android.app.Notification.CATEGORY_CALL) {
                                            NotificationListener.instance?.dismissNotification(sbn.key)
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
            Spacer(modifier = Modifier.weight(0.8f))
        }

        if (!gesturesEnabled) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(bottom = 80.dp) // Pushed the button up
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
        }

        if (mediaController == null && isHomeLocked) {
            Icon(
                imageVector = Icons.Default.Pets,
                contentDescription = "Settings",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .size(24.dp)
                    .pointerInput(catIconAction) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (catIconAction == "double_touch") {
                                    onShowSettingsClicked()
                                }
                            },
                            onLongPress = {
                                if (catIconAction == "long_press") {
                                    onShowSettingsClicked()
                                }
                            }
                        )
                    },
                tint = Color.Black
            )
        }

        if (mediaController != null) {
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



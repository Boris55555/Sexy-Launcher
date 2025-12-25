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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessAlarm
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Pets
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boris55555.sexylauncher.birthdays.BirthdaysRepository
import com.boris55555.sexylauncher.utils.BrightnessHelper
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
    var showBrightnessButton by remember { mutableStateOf(false) }
    var brightnessButtonPosition by remember { mutableStateOf<Offset?>(null) }

    val favoritePackages by favoritesRepository.favorites.collectAsState()
    val favoriteCount by favoritesRepository.favoriteCount.collectAsState()
    val alarmAppPackage by favoritesRepository.alarmAppPackage.collectAsState()
    val calendarAppPackage by favoritesRepository.calendarAppPackage.collectAsState()
    val isHomeLocked by favoritesRepository.isHomeLocked.collectAsState()
    val customNames by favoritesRepository.customNames.collectAsState()
    val birthdays by birthdaysRepository.birthdays.collectAsState()
    val gesturesEnabled by favoritesRepository.gesturesEnabled.collectAsState()
    val brightnessGestureEnabled by favoritesRepository.brightnessGestureEnabled.collectAsState()
    val swipeLeftAction by favoritesRepository.swipeLeftAction.collectAsState()
    val swipeRightAction by favoritesRepository.swipeRightAction.collectAsState()
    val catIconAction by favoritesRepository.catIconAction.collectAsState()
    val showAppIcons by favoritesRepository.showAppIcons.collectAsState()

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

    DisposableEffect(context) {
        if (!isNotificationServiceEnabled(context)) {
            return@DisposableEffect onDispose {}
        }

        val mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        val componentName = ComponentName(context, NotificationListener::class.java)

        val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            val newController = controllers?.firstOrNull { 
                val state = it.playbackState?.state
                state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_PAUSED
            }

            if (newController?.packageName != mediaController?.packageName) {
                mediaController?.unregisterCallback(controllerCallback)
                mediaController = newController
                newController?.registerCallback(controllerCallback)
                controllerCallback.onMetadataChanged(newController?.metadata)
                controllerCallback.onPlaybackStateChanged(newController?.playbackState)
            }
        }

        mediaSessionManager.addOnActiveSessionsChangedListener(sessionListener, componentName)

        val initialControllers = mediaSessionManager.getActiveSessions(componentName)
        val initialController = initialControllers?.firstOrNull { 
            val state = it.playbackState?.state
            state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_PAUSED
        }
        if (initialController != null) {
            mediaController = initialController
            initialController.registerCallback(controllerCallback)
            controllerCallback.onMetadataChanged(initialController.metadata)
            controllerCallback.onPlaybackStateChanged(initialController.playbackState)
        }

        onDispose {
            mediaController?.unregisterCallback(controllerCallback)
            mediaSessionManager.removeOnActiveSessionsChangedListener(sessionListener)
        }
    }

    val pageCount = (favoriteCount + 3) / 4
    var nextAlarm by remember { mutableStateOf<String?>(null) }

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun updateAlarm() {
        val nextAlarmClock = alarmManager.nextAlarmClock
        nextAlarm = nextAlarmClock?.let {
            val calendar = Calendar.getInstance().apply { timeInMillis = it.triggerTime }
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(calendar.time)
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
            .pointerInput(isHomeLocked, brightnessGestureEnabled, favoritesArea) {
                detectTapGestures(
                    onLongPress = { if (!isHomeLocked) onShowSettingsClicked() },
                    onDoubleTap = { showRefreshOverlay = true },
                    onTap = { offset ->
                        if (brightnessGestureEnabled) {
                            val isTapOnEmptySpace = favoritesArea?.contains(offset)?.not() ?: true
                            if (isTapOnEmptySpace) {
                                brightnessButtonPosition = offset
                                showBrightnessButton = true
                            }
                        }
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                if (nextAlarm != null) {
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
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.AccessAlarm, contentDescription = "Next Alarm", tint = Color.Black)
                        Text(text = nextAlarm!!, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .clickable {
                            calendarAppPackage?.let {
                                val launchIntent = packageManager.getLaunchIntentForPackage(it)
                                if (launchIntent != null) {
                                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(launchIntent)
                                }
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    DateText(favoritesRepository)
                }
                if (batteryLevel != null && batteryLevel!! <= 50) {
                    Text(
                        text = "($batteryLevel%)",
                        modifier = Modifier.align(Alignment.TopEnd),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
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
                                    fontSize = 18.sp,
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

            Spacer(modifier = Modifier.weight(0.2f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 80.dp),
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
                                    val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName)
                                    if (launchIntent != null) {
                                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(launchIntent)
                                    }
                                    notifications.filter { it.packageName == app.packageName }.forEach { sbn ->
                                        NotificationListener.instance?.dismissNotification(sbn.key)
                                    }
                                }
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
                                    fontSize = 32.sp,
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
             Box(modifier = Modifier.align(Alignment.BottomCenter)) {
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

        if (showBrightnessButton) {
            brightnessButtonPosition?.let { position ->
                BrightnessToggleButton(
                    position = position,
                    onTimeout = { showBrightnessButton = false },
                    onClick = {
                        BrightnessHelper.toggleBrightness(context, (context as Activity).window)
                        showBrightnessButton = false
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

@Composable
private fun BrightnessToggleButton(
    position: Offset,
    onTimeout: () -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val window = (context as Activity).window
    val currentBrightness = window.attributes.screenBrightness
    // System default brightness is a negative value. Treat it as "light on".
    val isLightOn = currentBrightness < 0f || currentBrightness > 0.1f

    LaunchedEffect(Unit) {
        delay(3000L)
        onTimeout()
    }

    val density = LocalDensity.current
    val buttonSize = 56.dp
    val buttonSizePx = with(density) { buttonSize.toPx() }

    Box(
        modifier = Modifier
            .offset { IntOffset(
                (position.x - buttonSizePx / 2).roundToInt(),
                (position.y - buttonSizePx / 2).roundToInt()
            ) }
            .size(buttonSize)
            .clip(CircleShape)
            .background(Color.White)
            .border(BorderStroke(2.dp, Color.Black), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            // Use DarkMode icon as a button to turn lights off, and LightMode to turn them on.
            imageVector = if (isLightOn) Icons.Default.DarkMode else Icons.Default.LightMode,
            contentDescription = "Toggle Brightness",
            tint = Color.Black,
            modifier = Modifier.size(32.dp)
        )
    }
}

fun Context.batteryLevel(): Flow<Int> = callbackFlow {
    val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val percentage = if (level != -1 && scale != -1) {
                (level * 100 / scale.toFloat()).toInt()
            } else {
                -1
            }
            trySend(percentage)
        }
    }
    val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
    registerReceiver(batteryReceiver, filter)
    awaitClose { unregisterReceiver(batteryReceiver) }
}

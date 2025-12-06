package com.boris55555.sexylauncher

import android.app.AlarmManager
import android.app.Notification
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
import android.net.Uri
import android.os.BatteryManager
import android.service.notification.StatusBarNotification
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessAlarm
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
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

    val favoriteApps = remember(favoritePackages, customNames) {
        favoritePackages.map { pkgName ->
            if (pkgName == null) null
            else {
                val intent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    `package` = pkgName
                }
                packageManager.queryIntentActivities(intent, 0).firstOrNull()?.let {
                    val originalName = it.loadLabel(packageManager).toString()
                    val customName = customNames[pkgName]
                    AppInfo(
                        name = customName ?: originalName,
                        packageName = it.activityInfo.packageName,
                        customName = customName
                    )
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
                // We only care about sessions that are currently playing or paused
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

        // Set initial controller
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(isHomeLocked) {
                detectTapGestures(
                    onLongPress = { if (!isHomeLocked) onShowSettingsClicked() },
                    onDoubleTap = { showRefreshOverlay = true }
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
                if (batteryLevel != null && batteryLevel!! <= 25) {
                    Text(
                        text = "Battery: ${'$'}batteryLevel%",
                        modifier = Modifier.align(Alignment.TopEnd),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }

            val today = LocalDate.now()
            val todaysBirthdays = birthdays.filter { it.date.month == today.month && it.date.dayOfMonth == today.dayOfMonth }
            val hasReminders = notifications.any { it.packageName == context.packageName }

            // Container for notifications and birthdays to stabilize layout
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Fixed-size box for the notification indicator
                Box(modifier = Modifier.height(56.dp), contentAlignment = Alignment.Center) {
                    if (notifications.isNotEmpty()) {
                        NotificationIndicator(
                            count = notifications.size,
                            hasReminders = hasReminders,
                            onClick = {
                                NotificationListener.instance?.requestRefresh()
                                onShowNotificationsClicked()
                            }
                        )
                    }
                }
                // Fixed-size box for birthday indicator(s)
                Box(modifier = Modifier.height(40.dp), contentAlignment = Alignment.Center) {
                    if (todaysBirthdays.isNotEmpty()) {
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            todaysBirthdays.forEachIndexed { index, birthday ->
                                val age = ChronoUnit.YEARS.between(birthday.date, today)
                                Text(
                                    text = "\uD83C\uDF82 ${birthday.name} $age years!",
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
                        .padding(start = 16.dp),
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
                                notifications = notifications.filter { it.packageName == app.packageName }, 
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
                                    // Special handling for Mudita Messages
                                    if (app.packageName == "com.mudita.messages") {
                                        notifications.filter { it.packageName == app.packageName }.forEach { sbn ->
                                            NotificationListener.instance?.dismissNotification(sbn.key)
                                        }
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

        if (mediaController == null && isHomeLocked) {
            Icon(
                imageVector = Icons.Default.Pets,
                contentDescription = "Settings",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .size(24.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(onDoubleTap = { onShowSettingsClicked() })
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
fun MiniPlayer(
    mediaMetadata: MediaMetadata?,
    playbackState: PlaybackState?,
    mediaController: MediaController?,
    onClose: () -> Unit
) {
    val isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING
    val title = mediaMetadata?.description?.title?.toString() ?: "Unknown Title"
    val artist = mediaMetadata?.description?.subtitle?.toString() ?: "Unknown Artist"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .border(BorderStroke(2.dp, Color.Black), shape = RoundedCornerShape(12.dp))
            .clickable {
                try {
                    mediaController?.sessionActivity?.send()
                } catch (e: Exception) {
                    // Ignore
                }
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.Black)
                Text(text = artist, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.Black)
            }
            
            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { mediaController?.transportControls?.skipToPrevious() }) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", tint = Color.Black)
                }
                IconButton(onClick = {
                    if (isPlaying) {
                        mediaController?.transportControls?.pause()
                    } else {
                        mediaController?.transportControls?.play()
                    }
                }) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.Black
                    )
                }
                IconButton(onClick = { mediaController?.transportControls?.skipToNext() }) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = Color.Black)
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.Black)
                }
            }
        }
    }
}


@Composable
fun DateText(favoritesRepository: FavoritesRepository) {
    val weekStartsOnSunday by favoritesRepository.weekStartsOnSunday.collectAsState()
    val calendar = Calendar.getInstance()

    if (weekStartsOnSunday) {
        calendar.firstDayOfWeek = Calendar.SUNDAY
        calendar.minimalDaysInFirstWeek = 1
    } else {
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.minimalDaysInFirstWeek = 4
    }

    val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
    val dayOfWeek = SimpleDateFormat("EEEE", Locale.getDefault()).format(calendar.time)
    val weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR)
    val month = SimpleDateFormat("MMMM", Locale.getDefault()).format(calendar.time)
    val year = calendar.get(Calendar.YEAR)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$dayOfMonth $dayOfWeek, wk($weekOfYear)",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Text(
            text = "$month $year",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
    }
}

@Composable
fun NotificationIndicator(count: Int, hasReminders: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .border(BorderStroke(2.dp, Color.Black), shape = RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (hasReminders) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Notification Bell",
                tint = Color.Black,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
        Text(
            text = if (count == 1) "New notification" else "$count new notifications", 
            fontSize = 16.sp, 
            color = Color.Black
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FavoriteAppItem(app: AppInfo, notifications: List<StatusBarNotification>, onLongClick: () -> Unit, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(8.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                text = app.name,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color.Black
            )
            if (notifications.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .border(BorderStroke(2.dp, Color.Black), shape = CircleShape)
                        .background(color = Color.White, shape = CircleShape)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (notifications.size > 99) "99+" else notifications.size.toString(),
                        color = Color.Black,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        if (notifications.isNotEmpty()) {
            val firstNotification = notifications.first().notification
            val extras = firstNotification.extras
            val sender = extras.getString(Notification.EXTRA_TITLE)
            val message = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()

            val notificationPreview = when {
                !sender.isNullOrBlank() && !message.isNullOrBlank() -> "$sender: $message"
                !message.isNullOrBlank() -> message
                !sender.isNullOrBlank() -> sender
                else -> null
            }

            if (!notificationPreview.isNullOrBlank()) {
                Text(
                    text = notificationPreview,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                    color = Color.Black
                )
            }
        }
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

@Composable
fun RenameAppDialog(
    appInfo: AppInfo,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
    onUninstall: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    val isSystemApp by remember(appInfo) {
        mutableStateOf(
            try {
                (packageManager.getApplicationInfo(appInfo.packageName, 0).flags and ApplicationInfo.FLAG_SYSTEM) != 0
            } catch (e: Exception) {
                true // Assume system app on error
            }
        )
    }

    var newName by remember { mutableStateOf(appInfo.customName ?: appInfo.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Edit ${appInfo.name}",
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (!isSystemApp && onUninstall != null) {
                    IconButton(onClick = onUninstall) {
                        Icon(Icons.Default.Delete, contentDescription = "Uninstall", tint = Color.Black)
                    }
                }
            }
        },
        text = {
            TextField(
                value = newName,
                onValueChange = { newName = it },
                singleLine = true,
                modifier = Modifier.border(BorderStroke(1.dp, Color.Black)),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onRename(newName) }),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    cursorColor = Color.Black,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White
                )
            )
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = onDismiss,
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    border = BorderStroke(1.dp, Color.Black)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = { onRename(newName) },
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    border = BorderStroke(1.dp, Color.Black)
                ) {
                    Text("Rename")
                }
            }
        },
        dismissButton = {},
        containerColor = Color.White
    )
}

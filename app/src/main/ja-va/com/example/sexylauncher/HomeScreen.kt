package com.example.sexylauncher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.StatusBarNotification
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    favoritesRepository: FavoritesRepository,
    onShowAllAppsClicked: () -> Unit,
    onShowNotificationsClicked: () -> Unit,
    onShowSettingsClicked: () -> Unit,
    onEditFavorite: (Int) -> Unit
) {
    MainHomeScreen(
        favoritesRepository,
        onShowAllAppsClicked,
        onShowNotificationsClicked,
        onShowSettingsClicked,
        onEditFavorite
    )
}

@Composable
fun MainHomeScreen(
    favoritesRepository: FavoritesRepository,
    onShowAllAppsClicked: () -> Unit,
    onShowNotificationsClicked: () -> Unit,
    onShowSettingsClicked: () -> Unit,
    onEditFavorite: (Int) -> Unit
) {
    val context = LocalContext.current
    val packageManager = context.packageManager

    var favoritePackages by remember { mutableStateOf(favoritesRepository.getFavorites()) }
    var favoriteCount by remember { mutableStateOf(favoritesRepository.getFavoriteCount()) }

    LaunchedEffect(Unit) { // Re-check favorites when screen is shown
        favoritePackages = favoritesRepository.getFavorites()
        favoriteCount = favoritesRepository.getFavoriteCount()
    }

    val favoriteApps = remember(favoritePackages) {
        favoritePackages.map { pkgName ->
            if (pkgName == null) null
            else {
                val intent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    `package` = pkgName
                }
                packageManager.queryIntentActivities(intent, 0).firstOrNull()?.let {
                    AppInfo(
                        name = it.loadLabel(packageManager).toString(),
                        packageName = it.activityInfo.packageName
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
            val newController = controllers?.firstOrNull()
            if (newController?.packageName != mediaController?.packageName) {
                mediaController?.unregisterCallback(controllerCallback)
                mediaController = newController
                newController?.registerCallback(controllerCallback)
                controllerCallback.onMetadataChanged(newController?.metadata)
                controllerCallback.onPlaybackStateChanged(newController?.playbackState)
            }
        }

        mediaSessionManager.addOnActiveSessionsChangedListener(sessionListener, componentName)

        val initialController = mediaSessionManager.getActiveSessions(componentName)?.firstOrNull()
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
    var currentPage by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                coroutineScope {
                    launch {
                        detectTapGestures(
                            onLongPress = { onShowSettingsClicked() },
                            onDoubleTap = {
                                try {
                                    val intent = Intent("com.android.settings.action.CLEAR_GHOST")
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // Ignore error on standard devices
                                }
                            }
                        )
                    }
                    launch {
                        detectHorizontalDragGestures {
                            change, dragAmount ->
                            if (dragAmount < -50) { // Swipe Left
                                onShowNotificationsClicked()
                                change.consume()
                            }
                        }
                    }
                    launch {
                        detectVerticalDragGestures {
                            change, dragAmount ->
                            if (dragAmount < -50) { // Swipe Up
                                onShowAllAppsClicked()
                                change.consume()
                            }
                        }
                    }
                }
            }
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            DateText()
            if (notifications.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                NotificationIndicator(
                    count = notifications.size,
                    onClick = onShowNotificationsClicked
                )
            }
        }

        Row(
            modifier = Modifier.weight(1f).padding(vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (pageCount > 1) {
                Column(
                    modifier = Modifier.padding(end = 8.dp).fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    repeat(pageCount) { pageIndex ->
                        Box(
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .size(8.dp)
                                .background(
                                    if (currentPage == pageIndex) Color.Black else Color.Gray,
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .pointerInput(pageCount) { // Re-trigger if pageCount changes
                        detectVerticalDragGestures {
                            change, dragAmount ->
                            if (dragAmount < -50) { // Swipe Up
                                currentPage = (currentPage + 1) % pageCount
                                change.consume()
                            }
                            if (dragAmount > 50) { // Swipe Down
                                currentPage = (currentPage - 1 + pageCount) % pageCount
                                change.consume()
                            }
                        }
                    }
            ) {
                Column {
                    val pageStart = currentPage * 4
                    val pageEnd = minOf((currentPage + 1) * 4, favoriteCount)

                    for (i in pageStart until pageEnd) {
                        val app = favoriteApps.getOrNull(i)
                        Box(modifier = Modifier.pointerInput(Unit) {
                            detectTapGestures(onLongPress = { onEditFavorite(i) })
                        }) {
                            if (app != null) {
                                val appNotifications = notifications.filter { it.packageName == app.packageName }
                                FavoriteAppItem(app = app, notifications = appNotifications) {
                                    appNotifications.forEach { sbn ->
                                        NotificationListener.instance?.dismissNotification(sbn.key)
                                    }
                                    val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName)
                                    context.startActivity(launchIntent)
                                }
                            } else {
                                Text(
                                    text = "[ + ]",
                                    modifier = Modifier
                                        .padding(16.dp),
                                    textAlign = TextAlign.Center,
                                    fontSize = 24.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        val showMediaControls = playbackState?.state in listOf(PlaybackState.STATE_PLAYING, PlaybackState.STATE_PAUSED)
        if (showMediaControls) {
            MediaControlsWidget(
                metadata = mediaMetadata,
                playbackState = playbackState,
                onPlayPause = { mediaController?.transportControls?.run { if (playbackState?.state == PlaybackState.STATE_PLAYING) pause() else play() } },
                onSkipPrevious = { mediaController?.transportControls?.skipToPrevious() },
                onSkipNext = { mediaController?.transportControls?.skipToNext() }
            )
        }
    }
}

@Composable
fun NotificationIndicator(count: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() }
            .border(BorderStroke(1.dp, Color.Black))
            .padding(16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("New notifications: $count")
    }
}

@Composable
fun MediaControlsWidget(
    metadata: MediaMetadata?,
    playbackState: PlaybackState?,
    onPlayPause: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit
) {
    val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: ""
    val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
    val isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(BorderStroke(1.dp, Color.Gray))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (title.isNotBlank()) {
            Text(text = title, fontWeight = FontWeight.Bold, maxLines = 1)
        }
        if (artist.isNotBlank()) {
            Text(text = artist, fontSize = 14.sp, maxLines = 1)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onSkipPrevious) { Text("PREV") }
            Button(onClick = onPlayPause) { Text(if (isPlaying) "PAUSE" else "PLAY") }
            Button(onClick = onSkipNext) { Text("NEXT") }
        }
    }
}


@Composable
fun DateText() {
    val date = LocalDate.now()
    val dayOfWeekFormatter = remember { DateTimeFormatter.ofPattern("EEEE d") }
    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM") }
    val yearFormatter = remember { DateTimeFormatter.ofPattern("yyyy") }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = date.format(dayOfWeekFormatter), fontSize = 20.sp)
        Text(text = date.format(monthFormatter), fontSize = 20.sp)
        Text(text = date.format(yearFormatter), fontSize = 20.sp)
    }
}

@Composable
fun FavoriteAppItem(app: AppInfo, notifications: List<StatusBarNotification>, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Column(
            modifier = Modifier.clickable { onClick() },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = app.name, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                if (notifications.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "(${notifications.size})", fontSize = 16.sp)
                }
            }
            notifications.firstOrNull()?.notification?.extras?.let {
                val title = it.getString("android.title")
                if (title != null) {
                    Text(text = title, fontSize = 14.sp, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

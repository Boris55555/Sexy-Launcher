package com.boris55555.sexylauncher

import android.app.Activity
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.net.Uri
import android.app.Notification
import android.service.notification.StatusBarNotification
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@Composable
fun AppListScreen(
    isPickerMode: Boolean = false,
    onAppSelected: ((AppInfo?) -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    onShowSettingsClicked: (() -> Unit)? = null,
    favoritesRepository: FavoritesRepository,
    usageRepository: UsageRepository? = null,
    onLockedLetterChanged: (Char?) -> Unit,
    lockedLetter: Char?,
    onAppLaunched: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    val notifications by NotificationListener.notifications.collectAsState()
    val customNames by favoritesRepository.customNames.collectAsState()
    val hideLauncherFromAppView by favoritesRepository.hideLauncherFromAppView.collectAsState()
    val showAppIcons by favoritesRepository.showAppIcons.collectAsState()
    val fontSizeAllApps by favoritesRepository.fontSizeAllApps.collectAsState()
    val preferredAppList by favoritesRepository.preferredAppList.collectAsState()
    val usageMap by (usageRepository?.usageMap ?: MutableStateFlow<Map<String, Int>>(emptyMap())).collectAsState()
    val favorites by favoritesRepository.favorites.collectAsState()

    val pagerState = rememberPagerState(
        initialPage = if (preferredAppList == "Top 10" && !isPickerMode) 1 else 0,
        pageCount = { if (isPickerMode) 1 else 2 }
    )
    val coroutineScope = rememberCoroutineScope()
    val selectedTab = if (pagerState.currentPage == 0) "All Apps" else "Top 10"

    val fontSizeAdjustment = when (fontSizeAllApps) {
        "Small" -> -2
        "Big" -> 2
        else -> 0
    }

    var appToEdit by remember { mutableStateOf<AppInfo?>(null) }
    var refreshKey by remember { mutableStateOf(0) } // State to trigger refresh

    val uninstallLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            refreshKey++ // Trigger recomposition
        }
    }

    val apps = remember(isPickerMode, customNames, refreshKey, hideLauncherFromAppView) {
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfoList: List<ResolveInfo> = packageManager.queryIntentActivities(intent, 0)
        val hiddenPackages = setOf(
            "com.android.calendar",
            "com.android.deskclock",
            "com.android.fmradio",
            "com.android.documentsui",
            "com.android.gallery3d",
            "com.mediatek.gnss.nonframeworklbs",
            "com.phdtaui.mainactivity",
            "com.android.stk",
            "com.android.quicksearchbox",
            "com.android.settings",
            "org.chromium.webview_shell"
        )

        resolveInfoList.mapNotNull { resolveInfo ->
            try {
                val appInfo = packageManager.getApplicationInfo(resolveInfo.activityInfo.packageName, 0)
                val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                        resolveInfo.activityInfo.packageName.startsWith("com.mudita")

                val originalName = resolveInfo.loadLabel(packageManager).toString()
                val customName = customNames[resolveInfo.activityInfo.packageName]
                AppInfo(
                    name = customName ?: originalName,
                    packageName = resolveInfo.activityInfo.packageName,
                    customName = customName,
                    isSystemApp = isSystemApp
                )
            } catch (e: Exception) {
                null
            }
        }.filter {
            val isLauncher = it.packageName == context.packageName
            val shouldHideLauncher = hideLauncherFromAppView && isLauncher
            it.packageName !in hiddenPackages && !shouldHideLauncher &&
            // Don't show the launcher itself in picker mode
            (!isPickerMode || !isLauncher)
        }.sortedBy { it.name }
    }

    if (appToEdit != null) {
        val app = appToEdit!!
        RenameAppDialog(
            appInfo = app,
            onDismiss = { appToEdit = null },
            onRename = { newName ->
                favoritesRepository.saveCustomName(app.packageName, newName)
                appToEdit = null
            },
            onUninstall = if (!app.isSystemApp) {
                {
                    appToEdit = null // Dismiss the dialog immediately
                    val intent = Intent(Intent.ACTION_DELETE)
                    intent.data = Uri.parse("package:${app.packageName}")
                    intent.putExtra(Intent.EXTRA_RETURN_RESULT, true)
                    uninstallLauncher.launch(intent)
                }
            } else null,
            showAppIcons = showAppIcons
        )
    }

    val alphabet = remember(apps) {
        apps.groupBy { it.name.first().uppercaseChar() }.keys.sorted()
    }

    val allAppsListState = rememberLazyListState()
    val top10ListState = rememberLazyListState()

    LaunchedEffect(lockedLetter) {
        allAppsListState.scrollToItem(0)
    }

    val indexMap = remember(apps, lockedLetter, pagerState.currentPage) {
        val map = mutableMapOf<Char, Int>()
        if (lockedLetter == null && pagerState.currentPage == 0) {
            val items = mutableListOf<Any>()
            val grouped = apps.groupBy { it.name.first().uppercaseChar() }
            grouped.toSortedMap().forEach { (letter, appsInGroup) ->
                items.add(letter)
                items.addAll(appsInGroup)
            }
            items.forEachIndexed { index, item ->
                if (item is Char) {
                    map[item] = index
                }
            }
        }
        map
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .pointerInput(lockedLetter) {
            if (lockedLetter != null) {
                detectTapGestures(onTap = { onLockedLetterChanged(null) })
            }
        }) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (isPickerMode) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Button(onClick = { onAppSelected?.invoke(null) }) { Text("Clear") }
                    Button(onClick = { onDismiss?.invoke() }) { Text("Cancel") }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "All Apps",
                        modifier = Modifier
                            .clickable {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(0)
                                }
                            }
                            .padding(horizontal = 8.dp),
                        textAlign = TextAlign.Center,
                        fontSize = (if (selectedTab == "All Apps") 24 else 18).sp,
                        fontWeight = if (selectedTab == "All Apps") FontWeight.Bold else FontWeight.Normal,
                        color = Color.Black
                    )
                    Text(
                        text = "|",
                        fontSize = 24.sp,
                        color = Color.Black,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Text(
                        text = "Top 10",
                        modifier = Modifier
                            .clickable {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(1)
                                }
                            }
                            .padding(horizontal = 8.dp),
                        textAlign = TextAlign.Center,
                        fontSize = (if (selectedTab == "Top 10") 24 else 18).sp,
                        fontWeight = if (selectedTab == "Top 10") FontWeight.Bold else FontWeight.Normal,
                        color = Color.Black
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = lockedLetter == null && !isPickerMode
            ) { page ->
                val currentTabForPage = if (page == 0) "All Apps" else "Top 10"
                val listItems = remember(apps, lockedLetter, usageMap, isPickerMode, currentTabForPage, favorites) {
                    val items = mutableListOf<Any>()

                    if (isPickerMode) {
                        val grouped = apps.groupBy { it.name.first().uppercaseChar() }
                        grouped.toSortedMap().forEach { (letter, appsInGroup) ->
                            items.add(letter)
                            items.addAll(appsInGroup)
                        }
                        return@remember items
                    }

                    if (currentTabForPage == "Top 10" && lockedLetter == null) {
                        val topApps = usageMap.entries
                            .filter { it.key !in favorites }
                            .sortedByDescending { it.value }
                            .take(10)
                            .mapNotNull { entry -> apps.find { it.packageName == entry.key } }

                        items.addAll(topApps)
                    } else {
                        if (lockedLetter != null) {
                            val filteredApps = apps.filter { it.name.first().uppercaseChar() == lockedLetter }
                            if (filteredApps.isNotEmpty()) {
                                items.add(lockedLetter!!)
                                items.addAll(filteredApps)
                            }
                        } else {
                            val grouped = apps.groupBy { it.name.first().uppercaseChar() }
                            grouped.toSortedMap().forEach { (letter, appsInGroup) ->
                                items.add(letter)
                                items.addAll(appsInGroup)
                            }
                        }
                    }
                    items
                }

                LazyColumn(
                    state = if (page == 0) allAppsListState else top10ListState,
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.Start
                ) {
                    items(listItems) { item ->
                        when (item) {
                            is Char -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                                ) {
                                    Text(
                                        text = item.toString(),
                                        modifier = Modifier
                                            .clickable { onLockedLetterChanged(item) },
                                        fontSize = (20 + fontSizeAdjustment).sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            is AppInfo -> {
                                AppListItem(
                                    app = item,
                                    showAppIcons = showAppIcons,
                                    notifications = notifications,
                                    onLongClick = { appToEdit = item },
                                    onClick = {
                                        if (isPickerMode) {
                                            onAppSelected?.invoke(item)
                                        } else {
                                            if (item.packageName == context.packageName) {
                                                onShowSettingsClicked?.invoke()
                                            } else {
                                                onAppLaunched?.invoke(item.packageName)
                                                notifications
                                                    .filter { it.packageName == item.packageName }
                                                    .forEach { sbn ->
                                                        NotificationListener.instance?.dismissNotification(sbn.key)
                                                    }
                                                val launchIntent =
                                                    packageManager.getLaunchIntentForPackage(item.packageName)
                                                if (launchIntent != null) {
                                                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    context.startActivity(launchIntent)
                                                }
                                            }
                                        }
                                    },
                                    fontSizeAdjustment = fontSizeAdjustment
                                )
                            }
                        }
                    }
                }
            }
        }

        AlphabetScroller(
            modifier = Modifier.align(Alignment.CenterEnd),
            alphabet = if (selectedTab == "All Apps" || lockedLetter != null) alphabet else emptyList(),
            onLetterSelected = { letter ->
                if (lockedLetter == null) {
                    val index = indexMap[letter]
                    if (index != null) {
                        coroutineScope.launch {
                            allAppsListState.scrollToItem(index)
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun AlphabetScroller(
    modifier: Modifier = Modifier,
    alphabet: List<Char>,
    onLetterSelected: (Char) -> Unit
) {
    var selectedLetter by remember { mutableStateOf<Char?>(null) }
    var columnSize by remember { mutableStateOf(IntSize.Zero) }

    fun updateSelectedLetter(y: Float) {
        if (columnSize.height <= 0 || alphabet.isEmpty()) return
        val letterHeight = columnSize.height.toFloat() / alphabet.size
        val index = (y / letterHeight).toInt().coerceIn(0, alphabet.lastIndex)
        val letter = alphabet.getOrNull(index)
        if (letter != null && letter != selectedLetter) {
            selectedLetter = letter
            onLetterSelected(letter)
        }
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .onSizeChanged { columnSize = it }
                .pointerInput(alphabet) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        updateSelectedLetter(down.position.y)

                        do {
                            val event = awaitPointerEvent()
                            event.changes.forEach {
                                updateSelectedLetter(it.position.y)
                                it.consume()
                            }
                        } while (event.changes.any { it.pressed })

                        selectedLetter = null
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            alphabet.forEach { letter ->
                val isSelected = selectedLetter == letter
                Text(
                    text = letter.toString(),
                    modifier = Modifier
                        .padding(4.dp)
                        .offset(x = if (isSelected) (-24).dp else 0.dp),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppListItem(
    app: AppInfo,
    showAppIcons: Boolean,
    notifications: List<StatusBarNotification>,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    fontSizeAdjustment: Int = 0
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    val missedCallsCount by NotificationListener.missedCallsCount.collectAsState()

    val appIcon: Drawable? = if (showAppIcons) {
        try {
            packageManager.getApplicationIcon(app.packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    } else {
        null
    }

    // Determine if this is the phone app to include missed calls count
    val isPhoneApp = app.packageName == "com.mudita.dial" || 
                     app.packageName.contains("dialer") || 
                     app.packageName.contains("telecom")

    val appNotifications = notifications.filter { 
        it.packageName.equals(app.packageName, ignoreCase = true) 
    }
    val totalCount = if (isPhoneApp) {
        val otherCallNotificationsCount = appNotifications.filter { it.notification.category != Notification.CATEGORY_MISSED_CALL }.sumOf { getNotificationCount(it) }
        otherCallNotificationsCount + missedCallsCount
    } else {
        appNotifications.sumOf { getNotificationCount(it) }
    }

    Row(
        modifier = Modifier
            .padding(start = 64.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Row(
            modifier = Modifier
                .combinedClickable(onClick = onClick, onLongClick = onLongClick),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showAppIcons) {
                Box(modifier = Modifier.size(40.dp)) {
                    if (appIcon != null) {
                        Image(
                            painter = rememberDrawablePainter(drawable = appIcon),
                            contentDescription = "${app.name} icon",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
            }
            Text(
                text = app.name,
                fontSize = (24 + fontSizeAdjustment).sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            if (totalCount > 0) {
                Box(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .border(BorderStroke(3.dp, Color.Black), shape = CircleShape)
                        .background(color = Color.White, shape = CircleShape)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (totalCount > 99) "99+" else totalCount.toString(),
                        color = Color.Black,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

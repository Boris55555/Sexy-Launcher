package com.example.sexylauncher

import android.app.Activity
import android.content.Intent
import android.content.pm.ResolveInfo
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun AppListScreen(
    isPickerMode: Boolean = false,
    onAppSelected: ((AppInfo?) -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    onShowSettingsClicked: (() -> Unit)? = null,
    favoritesRepository: FavoritesRepository
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    val notifications by NotificationListener.notifications.collectAsState()
    val customNames by favoritesRepository.customNames.collectAsState()
    var appToEdit by remember { mutableStateOf<AppInfo?>(null) }
    var refreshKey by remember { mutableStateOf(0) } // State to trigger refresh

    val uninstallLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            refreshKey++ // Trigger recomposition
        }
    }

    val apps = remember(isPickerMode, customNames, refreshKey) {
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

        resolveInfoList.map {
            val originalName = it.loadLabel(packageManager).toString()
            val customName = customNames[it.activityInfo.packageName]
            AppInfo(
                name = customName ?: originalName,
                packageName = it.activityInfo.packageName,
                customName = customName
            )
        }.filter {
            it.packageName !in hiddenPackages &&
            // Don't show the launcher itself in picker mode
            (!isPickerMode || it.packageName != context.packageName)
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
            onUninstall = {
                appToEdit = null // Dismiss the dialog immediately
                val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE)
                intent.data = Uri.parse("package:${app.packageName}")
                intent.putExtra(Intent.EXTRA_RETURN_RESULT, true)
                uninstallLauncher.launch(intent)
            }
        )
    }

    val groupedApps = remember(apps) {
        apps.groupBy { it.name.first().uppercaseChar() }
    }
    val listItems = remember(groupedApps) {
        val items = mutableListOf<Any>()
        groupedApps.toSortedMap().forEach { (letter, apps) ->
            items.add(letter)
            items.addAll(apps)
        }
        items
    }
    val alphabet = remember(groupedApps) {
        groupedApps.keys.sorted()
    }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val indexMap = remember(listItems) {
        val map = mutableMapOf<Char, Int>()
        listItems.forEachIndexed { index, item ->
            if (item is Char) {
                map[item] = index
            }
        }
        map
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                Text(
                    text = "All Apps",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    textAlign = TextAlign.Center,
                    fontSize = 24.sp
                )
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(listItems) { item ->
                    when (item) {
                        is Char -> {
                            Text(
                                text = item.toString(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
                                fontSize = 20.sp,
                                textAlign = TextAlign.Start
                            )
                        }
                        is AppInfo -> {
                            AppListItem(
                                app = item, 
                                onLongClick = { appToEdit = item },
                                onClick = {
                                    if (isPickerMode) {
                                        onAppSelected?.invoke(item)
                                    } else {
                                        if (item.packageName == context.packageName) {
                                            onShowSettingsClicked?.invoke()
                                        } else {
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
                                }
                            )
                        }
                    }
                }
            }
        }

        AlphabetScroller(
            modifier = Modifier.align(Alignment.CenterEnd),
            alphabet = alphabet,
            onLetterSelected = { letter ->
                val index = indexMap[letter]
                if (index != null) {
                    coroutineScope.launch {
                        listState.scrollToItem(index)
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

    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(horizontal = 8.dp)
            .onSizeChanged { columnSize = it }
            .pointerInput(alphabet) {
                detectVerticalDragGestures(
                    onDragEnd = { selectedLetter = null },
                    onDragCancel = { selectedLetter = null }
                ) { change, dragAmount ->
                    val y = change.position.y.coerceIn(0f, columnSize.height.toFloat())
                    if (columnSize.height > 0) {
                        val letterHeight = columnSize.height.toFloat() / alphabet.size
                        val index = (y / letterHeight)
                            .toInt()
                            .coerceIn(0, alphabet.lastIndex)
                        val letter = alphabet[index]

                        if (letter != selectedLetter) {
                            selectedLetter = letter
                            onLetterSelected(letter)
                        }
                    }
                    change.consume()
                }
            },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        alphabet.forEach { letter ->
            val isSelected = selectedLetter == letter
            Text(
                text = letter.toString(),
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isSelected) Color.Black else Color.Transparent)
                    .clickable { onLetterSelected(letter) }
                    .padding(4.dp),
                color = if (isSelected) Color.White else Color.Black
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppListItem(app: AppInfo, onClick: () -> Unit, onLongClick: () -> Unit) {
    Text(
        text = app.name,
        modifier = Modifier
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(8.dp),
        textAlign = TextAlign.Center,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold
    )
}

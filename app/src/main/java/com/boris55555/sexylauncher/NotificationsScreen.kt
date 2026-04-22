package com.boris55555.sexylauncher

import android.app.Notification
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.CallLog
import android.service.notification.StatusBarNotification
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boris55555.sexylauncher.reminders.RemindersRepository

data class MissedCallInfo(
    val id: Long,
    val number: String,
    val date: Long,
    val name: String?
)

@Composable
fun NotificationsScreen(
    remindersRepository: RemindersRepository,
    favoritesRepository: FavoritesRepository,
    onDismiss: () -> Unit
) {
    val notifications by NotificationListener.notifications.collectAsState()
    val context = LocalContext.current
    val fontSizeNotifications by favoritesRepository.fontSizeNotifications.collectAsState()

    val fontSizeAdjustment = when (fontSizeNotifications) {
        "Small" -> -2
        "Big" -> 2
        else -> 0
    }

    
    var missedCallsFromLog by remember { mutableStateOf<List<MissedCallInfo>>(emptyList()) }

    LaunchedEffect(Unit) {
        val calls = mutableListOf<MissedCallInfo>()
        try {
            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls._ID, CallLog.Calls.NUMBER, CallLog.Calls.DATE, CallLog.Calls.CACHED_NAME),
                "${CallLog.Calls.TYPE} = ${CallLog.Calls.MISSED_TYPE} AND (${CallLog.Calls.NEW} = 1 OR ${CallLog.Calls.IS_READ} = 0)",
                null,
                "${CallLog.Calls.DATE} DESC"
            )
            cursor?.use {
                val idIdx = it.getColumnIndex(CallLog.Calls._ID)
                val numIdx = it.getColumnIndex(CallLog.Calls.NUMBER)
                val dateIdx = it.getColumnIndex(CallLog.Calls.DATE)
                val nameIdx = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
                while (it.moveToNext()) {
                    calls.add(MissedCallInfo(
                        it.getLong(idIdx),
                        it.getString(numIdx),
                        it.getLong(dateIdx),
                        it.getString(nameIdx)
                    ))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        missedCallsFromLog = calls
    }

    val combinedItems = remember(notifications, missedCallsFromLog) {
        val items = mutableListOf<Any>()
        items.addAll(notifications)
        
        // Only add missed calls that don't already have a notification
        val existingMissedCallPackages = notifications.filter { 
            it.notification.category == Notification.CATEGORY_MISSED_CALL || 
            it.packageName.contains("dialer") || 
            it.packageName.contains("phone") 
        }.map { it.packageName }.toSet()

        if (existingMissedCallPackages.isEmpty()) {
            items.addAll(missedCallsFromLog)
        }
        
        items.sortedWith { a, b ->
            val timeA = if (a is StatusBarNotification) a.postTime else (a as MissedCallInfo).date
            val timeB = if (b is StatusBarNotification) b.postTime else (b as MissedCallInfo).date
            timeB.compareTo(timeA)
        }
    }

    LaunchedEffect(combinedItems) {
        if (combinedItems.isEmpty()) {
            onDismiss()
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .pointerInput(Unit) {
            detectHorizontalDragGestures { change, dragAmount ->
                if (dragAmount > 50) { // Swipe Right
                    onDismiss()
                    change.consume()
                }
            }
        }) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Notifications",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = (28 + fontSizeAdjustment).sp),
                modifier = Modifier.padding(16.dp),
                color = Color.Black
            )
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(combinedItems, key = { 
                    if (it is StatusBarNotification) it.key else "call_${(it as MissedCallInfo).id}" 
                }) { item ->
                    if (item is StatusBarNotification) {
                        NotificationItem(
                            sbn = item,
                            onClick = {
                                try {
                                    item.notification.contentIntent.send()
                                } catch (e: Exception) {
                                    val pm = context.packageManager
                                    var launchIntent = pm.getLaunchIntentForPackage(item.packageName)
                                    if (launchIntent == null) {
                                        val intent = Intent(Intent.ACTION_MAIN, null).apply {
                                            addCategory(Intent.CATEGORY_LAUNCHER)
                                            `package` = item.packageName
                                        }
                                        val resolveInfo = pm.queryIntentActivities(intent, 0)
                                        if (resolveInfo.isNotEmpty()) {
                                            val activityInfo = resolveInfo[0].activityInfo
                                            launchIntent = Intent(Intent.ACTION_MAIN).apply {
                                                addCategory(Intent.CATEGORY_LAUNCHER)
                                                component = ComponentName(activityInfo.packageName, activityInfo.name)
                                            }
                                        }
                                    }

                                    launchIntent?.let {
                                        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(it)
                                    }
                                }
                                if (item.notification.category in setOf(Notification.CATEGORY_MESSAGE, Notification.CATEGORY_CALL, Notification.CATEGORY_SOCIAL)) {
                                    NotificationListener.instance?.dismissNotification(item.key)
                                }
                            },
                            onDismiss = { NotificationListener.instance?.dismissNotification(item.key) },
                            fontSizeAdjustment = fontSizeAdjustment
                        )
                    } else if (item is MissedCallInfo) {
                        MissedCallItem(
                            info = item,
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    type = CallLog.Calls.CONTENT_TYPE
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                                // Mark as read/new=0 when clicked
                                try {
                                    val values = android.content.ContentValues()
                                    values.put(CallLog.Calls.NEW, 0)
                                    values.put(CallLog.Calls.IS_READ, 1)
                                    context.contentResolver.update(
                                        CallLog.Calls.CONTENT_URI,
                                        values,
                                        "${CallLog.Calls._ID} = ${item.id}",
                                        null
                                    )
                                    missedCallsFromLog = missedCallsFromLog.filter { it.id != item.id }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            },
                            onDismiss = {
                                try {
                                    val values = android.content.ContentValues()
                                    values.put(CallLog.Calls.NEW, 0)
                                    values.put(CallLog.Calls.IS_READ, 1)
                                    context.contentResolver.update(
                                        CallLog.Calls.CONTENT_URI,
                                        values,
                                        "${CallLog.Calls._ID} = ${item.id}",
                                        null
                                    )
                                    missedCallsFromLog = missedCallsFromLog.filter { it.id != item.id }
                                    NotificationListener.instance?.requestRefresh()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            },
                            fontSizeAdjustment = fontSizeAdjustment
                        )
                    }
                    Divider(color = Color.Black, modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

@Composable
fun NotificationItem(
    sbn: StatusBarNotification,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    fontSizeAdjustment: Int = 0
) {
    val context = LocalContext.current
    val packageManager = context.packageManager

    val appName = try {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(sbn.packageName, 0)).toString()
    } catch (e: Exception) {
        sbn.packageName // Fallback to package name
    }

    val extras = sbn.notification.extras
    val title = extras.getString("android.title") ?: extras.getString("android.conversationTitle")
    var text: CharSequence? = extras.getCharSequence("android.text") ?: extras.getCharSequence("android.bigText")

    // Handle MessagingStyle notifications
    val messages = extras.getParcelableArray("android.messages")
    if (messages != null && messages.isNotEmpty()) {
        val lastMessage = messages.last()
        if (lastMessage is Bundle) {
            text = lastMessage.getCharSequence("text") ?: text
        }
    }

    // Handle InboxStyle notifications
    if (text.isNullOrBlank()) {
        val lines = extras.getCharSequenceArray("android.text.lines")
        if (lines != null && lines.isNotEmpty()) {
            text = lines.last()
        }
    }

    val isReminder = sbn.packageName == context.packageName

    val category = getNotificationCategory(sbn, context)
    val isMessage = category == NotificationCategory.MESSAGES
    val isCall = category == NotificationCategory.CALLS

    Row(
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (isReminder) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Reminder",
                tint = Color.Black,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        } else if (isMessage) {
            Icon(
                imageVector = Icons.Default.Email,
                contentDescription = "Message",
                tint = Color.Black,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        } else if (isCall) {
            Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = "Call",
                tint = Color.Black,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(text = appName, fontWeight = FontWeight.Bold, fontSize = (18 + fontSizeAdjustment).sp, color = Color.Black)
            if (!title.isNullOrBlank()) {
                Text(text = title, fontSize = (16 + fontSizeAdjustment).sp, color = Color.Black)
            }
            if (!text.isNullOrBlank()) {
                Text(text = text.toString(), fontSize = (14 + fontSizeAdjustment).sp, color = Color.Black)
            }
        }
        if (sbn.isClearable) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss Notification", tint = Color.Black)
            }
        }
    }
}

@Composable
fun MissedCallItem(
    info: MissedCallInfo,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    fontSizeAdjustment: Int = 0
) {
    Row(
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(
            imageVector = Icons.Default.Phone,
            contentDescription = "Missed Call",
            tint = Color.Black,
            modifier = Modifier.padding(top = 4.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Missed Call", fontWeight = FontWeight.Bold, fontSize = (18 + fontSizeAdjustment).sp, color = Color.Black)
            Text(text = info.name ?: info.number, fontSize = (16 + fontSizeAdjustment).sp, color = Color.Black)
            val time = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(info.date))
            Text(text = time, fontSize = (14 + fontSizeAdjustment).sp, color = Color.Black)
        }
        IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color.Black)
        }
    }
}

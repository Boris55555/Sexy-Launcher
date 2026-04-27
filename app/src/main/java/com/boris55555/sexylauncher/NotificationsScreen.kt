package com.boris55555.sexylauncher

import android.app.Notification
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.CallLog
import android.service.notification.StatusBarNotification
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.foundation.shape.RoundedCornerShape

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
    val activeCallInfo by NotificationListener.activeCall.collectAsState()
    val context = LocalContext.current
    val fontSizeNotifications by favoritesRepository.fontSizeNotifications.collectAsState()

    val fontSizeAdjustment = when (fontSizeNotifications) {
        "Small" -> -2
        "Big" -> 2
        else -> 0
    }

    var missedCallsFromLog by remember { mutableStateOf<List<MissedCallInfo>>(emptyList()) }

    var previousActiveCallInfo by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(activeCallInfo) {
        // Auto-dismiss if a call was answered (transition from Incoming/Calling to Call)
        val wasPending = previousActiveCallInfo?.startsWith("Incoming:") == true || 
                        previousActiveCallInfo?.startsWith("Calling:") == true
        val isNowActive = activeCallInfo?.startsWith("Call:") == true
        
        if (wasPending && isNowActive) {
            onDismiss()
        }
        previousActiveCallInfo = activeCallInfo
    }

    LaunchedEffect(Unit) {
        // Force a refresh of notifications and active call info when screen opens
        NotificationListener.instance?.requestRefresh()
        NotificationListener.instance?.refreshCallInfo()
    }

    LaunchedEffect(notifications) {
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

    val otherItems = remember(notifications, missedCallsFromLog) {
        val list = mutableListOf<Any>()
        // Filter out ongoing call notifications from the main list as they are shown in ActiveCallItem
        val filteredNotifications = notifications.filter { sbn ->
            val isOngoing = (sbn.notification.flags and Notification.FLAG_ONGOING_EVENT) != 0
            val pkg = sbn.packageName.lowercase()
            val isCall = sbn.notification.category == Notification.CATEGORY_CALL || 
                         pkg.contains("dialer") || pkg.contains("telecom") || 
                         pkg.contains("phone") || pkg.contains("mudita.dial") || pkg.contains("incallui")
            
            !(isOngoing && isCall)
        }
        list.addAll(filteredNotifications)
        list.addAll(missedCallsFromLog)
        list.sortWith { a, b ->
            val timeA = if (a is StatusBarNotification) a.postTime else (a as MissedCallInfo).date
            val timeB = if (b is StatusBarNotification) b.postTime else (b as MissedCallInfo).date
            timeB.compareTo(timeA)
        }
        list
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.White)
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

            // ACTIVE CALL
            if (activeCallInfo != null) {
                ActiveCallItem(
                    info = activeCallInfo!!,
                    onClick = {
                        val telecomManager = context.getSystemService(android.telecom.TelecomManager::class.java)
                        var success = false
                        try {
                            telecomManager?.showInCallScreen(false)
                            success = true
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        if (!success) {
                            // Fallback to finding the notification intent
                            val callNotif = notifications.find { sbn ->
                                val pkg = sbn.packageName.lowercase()
                                val cat = sbn.notification.category
                                val isOngoing = (sbn.notification.flags and Notification.FLAG_ONGOING_EVENT) != 0
                                
                                (cat == Notification.CATEGORY_CALL || 
                                 pkg.contains("dialer") || pkg.contains("telecom") || 
                                 pkg.contains("phone") || pkg.contains("mudita.dial") || pkg.contains("incallui")) && 
                                isOngoing
                            }

                            try {
                                if (callNotif != null) {
                                    val intent = callNotif.notification.fullScreenIntent ?: callNotif.notification.contentIntent
                                    intent?.send()
                                } else {
                                    val intent = Intent(Intent.ACTION_DIAL)
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                                    context.startActivity(intent)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        onDismiss()
                    },
                    fontSizeAdjustment = fontSizeAdjustment
                )
            }

            if (otherItems.isEmpty() && activeCallInfo == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "No notifications", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(otherItems, key = { 
                        if (it is StatusBarNotification) it.key else "call_${(it as MissedCallInfo).id}"
                    }) { item ->
                        when (item) {
                            is StatusBarNotification -> {
                                NotificationItem(
                                    sbn = item,
                                    onClick = {
                                        val pkg = item.packageName.lowercase()
                                        val isPhoneApp = pkg.contains("dialer") || pkg.contains("telecom") || 
                                                         pkg.contains("phone") || pkg.contains("mudita.dial")
                                        
                                        val telephonyManager = context.getSystemService(android.telephony.TelephonyManager::class.java)
                                        val isCallActive = telephonyManager?.callState != android.telephony.TelephonyManager.CALL_STATE_IDLE

                                        if (isPhoneApp && isCallActive) {
                                            val telecomManager = context.getSystemService(android.telecom.TelecomManager::class.java)
                                            try {
                                                telecomManager?.showInCallScreen(false)
                                            } catch (e: Exception) {
                                                try {
                                                    val intent = item.notification.fullScreenIntent ?: item.notification.contentIntent
                                                    intent?.send()
                                                } catch (e2: Exception) {
                                                    val launchIntent = context.packageManager.getLaunchIntentForPackage(item.packageName)
                                                    launchIntent?.let {
                                                        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                        context.startActivity(it)
                                                    }
                                                }
                                            }
                                        } else {
                                            try {
                                                val intent = item.notification.fullScreenIntent ?: item.notification.contentIntent
                                                intent?.send()
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
                                        }

                                        if (item.notification.category in setOf(Notification.CATEGORY_MESSAGE, Notification.CATEGORY_CALL, Notification.CATEGORY_SOCIAL)) {
                                            NotificationListener.instance?.dismissNotification(item.key)
                                        }
                                        onDismiss() // Sulje ilmoitusnäkymä kun viesti avataan
                                    },
                                    onDismiss = { NotificationListener.instance?.dismissNotification(item.key) },
                                    fontSizeAdjustment = fontSizeAdjustment
                                )
                            }
                            is MissedCallInfo -> {
                                MissedCallItem(
                                    info = item,
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            type = CallLog.Calls.CONTENT_TYPE
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
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
                        }
                        HorizontalDivider(color = Color.Black, modifier = Modifier.padding(horizontal = 16.dp))
                    }
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
        sbn.packageName
    }

    val extras = sbn.notification.extras
    val title = extras.getString("android.title") ?: extras.getString("android.conversationTitle")
    
    val allMessages = mutableListOf<String>()
    val messages = extras.getParcelableArray("android.messages")
    if (messages != null && messages.isNotEmpty()) {
        for (i in messages.indices.reversed()) {
            val m = messages[i]
            if (m is Bundle) {
                val mText = m.getCharSequence("text")
                val mSender = m.getCharSequence("sender")
                if (!mText.isNullOrBlank()) {
                    if (!mSender.isNullOrBlank() && mSender != title) {
                        allMessages.add("$mSender: $mText")
                    } else {
                        allMessages.add(mText.toString())
                    }
                }
            }
        }
    }

    var text: CharSequence? = if (allMessages.isNotEmpty()) {
        null
    } else {
        extras.getCharSequence("android.text") ?: extras.getCharSequence("android.bigText")
    }

    val lines = extras.getCharSequenceArray("android.text.lines")
    if (text.isNullOrBlank() && allMessages.isEmpty() && lines != null && lines.isNotEmpty()) {
        for (i in lines.indices.reversed()) {
            allMessages.add(lines[i].toString())
        }
    }

    val category = getNotificationCategory(sbn, context)

    Row(
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(
            imageVector = category.icon,
            contentDescription = category.name,
            tint = Color.Black,
            modifier = Modifier.padding(top = 4.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(text = appName, fontWeight = FontWeight.Bold, fontSize = (18 + fontSizeAdjustment).sp, color = Color.Black)
            if (!title.isNullOrBlank()) {
                Text(text = title, fontSize = (16 + fontSizeAdjustment).sp, color = Color.Black)
            }
            if (allMessages.isNotEmpty()) {
                allMessages.forEach { msg ->
                    Text(text = msg, fontSize = (14 + fontSizeAdjustment).sp, color = Color.Black, modifier = Modifier.padding(top = 2.dp))
                }
            } else if (!text.isNullOrBlank()) {
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

@Composable
fun ActiveCallItem(
    info: String,
    onClick: () -> Unit,
    fontSizeAdjustment: Int = 0
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .border(BorderStroke(2.dp, Color.Black), shape = RoundedCornerShape(8.dp))
            .background(Color.White)
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = "Active Call",
                tint = Color.Black,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = info, 
                fontSize = (20 + fontSizeAdjustment).sp, 
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

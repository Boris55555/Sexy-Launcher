package com.example.sexylauncher

import android.app.Notification
import android.content.ComponentName
import android.content.Intent
import android.service.notification.StatusBarNotification
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NotificationsScreen(onDismiss: () -> Unit) {
    val notifications by NotificationListener.notifications.collectAsState()
    val sortedNotifications = remember(notifications) {
        notifications.sortedByDescending { it.postTime }
    }
    val context = LocalContext.current

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
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(16.dp),
                color = Color.Black
            )
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(sortedNotifications, key = { it.key }) { sbn ->
                    NotificationItem(
                        sbn = sbn,
                        onClick = {
                            try {
                                sbn.notification.contentIntent.send()
                            } catch (e: Exception) {
                                val pm = context.packageManager
                                var launchIntent = pm.getLaunchIntentForPackage(sbn.packageName)
                                if (launchIntent == null) {
                                    // A more robust way to find the launch intent if the first one fails
                                    val intent = Intent(Intent.ACTION_MAIN, null).apply {
                                        addCategory(Intent.CATEGORY_LAUNCHER)
                                        `package` = sbn.packageName
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
                            // Special handling for Mudita Messages
                            if (sbn.packageName == "com.mudita.messages") {
                                NotificationListener.instance?.dismissNotification(sbn.key)
                            }
                        },
                        onDismiss = { NotificationListener.instance?.dismissNotification(sbn.key) }
                    )
                    Divider(color = Color.Black, modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

@Composable
fun NotificationItem(sbn: StatusBarNotification, onClick: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val packageManager = context.packageManager

    val appName = try {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(sbn.packageName, 0)).toString()
    } catch (e: Exception) {
        sbn.packageName // Fallback to package name
    }

    val extras = sbn.notification.extras
    val title = extras.getString("android.title")
    val text = extras.getString("android.text")
    
    val isCommunication = when (sbn.notification.category) {
        Notification.CATEGORY_MESSAGE,
        Notification.CATEGORY_CALL,
        Notification.CATEGORY_SOCIAL -> true
        else -> false
    }

    Row(
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(text = appName, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
            if (!title.isNullOrBlank()) {
                Text(text = title, fontSize = 16.sp, color = Color.Black)
            }
            if (!text.isNullOrBlank()) {
                Text(text = text, fontSize = 14.sp, color = Color.Black)
            }
        }
        if (sbn.isClearable && !isCommunication) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss Notification")
            }
        }
    }
}

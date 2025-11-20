package com.example.sexylauncher

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
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

    Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
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
                modifier = Modifier.padding(16.dp)
            )
            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(sortedNotifications) { index, sbn ->
                    if (index > 0 && sbn.packageName != sortedNotifications[index - 1].packageName) {
                        Divider(color = Color.Black, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    }
                    NotificationItem(
                        sbn = sbn,
                        onDismiss = { NotificationListener.instance?.dismissNotification(sbn.key) },
                        onClick = {
                            try {
                                sbn.notification.contentIntent.send()
                            } catch (e: Exception) {
                                // Could not send pending intent
                            }
                            NotificationListener.instance?.dismissNotification(sbn.key)
                        }
                    )
                }
            }
            Button(
                onClick = { NotificationListener.instance?.dismissAllNotifications() },
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Text("Clear all")
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

    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable { onClick() }
        ) {
            Text(text = appName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            if (!title.isNullOrBlank()) {
                Text(text = title, fontSize = 16.sp)
            }
            if (!text.isNullOrBlank()) {
                Text(text = text, fontSize = 14.sp)
            }
        }
        Button(onClick = onDismiss) {
            Text("X")
        }
    }
}

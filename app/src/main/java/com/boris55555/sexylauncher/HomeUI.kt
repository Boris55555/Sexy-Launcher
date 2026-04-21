package com.boris55555.sexylauncher

import android.app.Notification
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.provider.CallLog
import android.service.notification.StatusBarNotification
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun DateText(favoritesRepository: FavoritesRepository) {
    val weekStartsOnSunday by favoritesRepository.weekStartsOnSunday.collectAsState()
    val dateThemeLight by favoritesRepository.dateThemeLight.collectAsState()

    val calendar = Calendar.getInstance()
    val englishLocale = Locale.ENGLISH

    if (weekStartsOnSunday) {
        calendar.firstDayOfWeek = Calendar.SUNDAY
        calendar.minimalDaysInFirstWeek = 1
    } else {
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.minimalDaysInFirstWeek = 4
    }

    val dayOfWeek = SimpleDateFormat("EEEE", englishLocale).format(calendar.time).uppercase()
    val weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR)
    val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
    val monthName = SimpleDateFormat("MMMM", englishLocale).format(calendar.time)
    val year = calendar.get(Calendar.YEAR)

    val backgroundColor = if (dateThemeLight) Color.White else Color.Black
    val textColor = if (dateThemeLight) Color.Black else Color.White

    Box(
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .border(BorderStroke(2.dp, Color.Black), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$dayOfWeek, WK $weekOfYear",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
            Text(
                text = "$dayOfMonth $monthName $year",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

enum class NotificationCategory(val icon: ImageVector) {
    EMAIL(Icons.Default.AlternateEmail),
    MESSAGES(Icons.AutoMirrored.Filled.Message),
    CALLS(Icons.Default.Phone),
    CALENDAR(Icons.Default.CalendarToday),
    REMINDERS(Icons.Default.Notifications),
    OTHER(Icons.Default.Android)
}

fun getNotificationCategory(sbn: StatusBarNotification, context: Context): NotificationCategory {
    val packageName = sbn.packageName.lowercase(Locale.getDefault())
    val extras = sbn.notification.extras
    val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.lowercase(Locale.getDefault()) ?: ""
    val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.lowercase(Locale.getDefault()) ?: ""
    val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.lowercase(Locale.getDefault()) ?: ""
    val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.lowercase(Locale.getDefault()) ?: ""
    val fullContent = "$title $text $subText $bigText"

    return when {
        sbn.packageName == context.packageName -> NotificationCategory.REMINDERS
        
        // 1. Calls (Priority check)
        sbn.notification.category == Notification.CATEGORY_CALL || 
        sbn.notification.category == Notification.CATEGORY_MISSED_CALL ||
        fullContent.contains("missed call") ||
        fullContent.contains("vastaamaton") ||
        fullContent.contains("puhelu") ||
        fullContent.contains("huti") ||
        packageName.contains("dialer") ||
        packageName.contains("telecom") ||
        packageName.contains("phone") ||
        packageName == "com.mudita.dial"
            -> NotificationCategory.CALLS

        // 2. Emails
        sbn.notification.category == Notification.CATEGORY_EMAIL ||
        packageName.contains("mail") ||
        packageName.contains("gmail") ||
        packageName.contains("outlook") ||
        packageName.contains("thunderbird")
            -> NotificationCategory.EMAIL
            
        // 3. Messages
        sbn.notification.category == Notification.CATEGORY_MESSAGE ||
        packageName.contains("messaging") ||
        packageName.contains("sms") ||
        packageName.contains("chat") ||
        packageName.contains("messenger") ||
        packageName.contains("whatsapp") ||
        packageName.contains("telegram") ||
        packageName.contains("threema") ||
        packageName.contains("viber") ||
        packageName.contains("discord") ||
        packageName.contains("slack") ||
        packageName.contains("matrix") ||
        packageName.contains("element") ||
        packageName.contains("fluffychat") ||
        packageName.contains("sunup") ||
        packageName == "org.mlm.mages" ||
        packageName == "com.mudita.messages" ||
        fullContent.contains("viesti") ||
        fullContent.contains("message") ||
        fullContent.contains("chat")
            -> NotificationCategory.MESSAGES
            
        // 4. Calendar
        sbn.notification.category == Notification.CATEGORY_EVENT ||
        packageName.contains("calendar")
            -> NotificationCategory.CALENDAR
            
        else -> NotificationCategory.OTHER
    }
}

@Composable
fun NotificationIndicator(notifications: List<StatusBarNotification>, onClick: () -> Unit) {
    val context = LocalContext.current
    val missedCallsCount by NotificationListener.missedCallsCount.collectAsState()

    val groupedNotifications = remember(notifications, missedCallsCount) {
        val result = mutableMapOf<NotificationCategory, Int>()
        
        notifications.forEach { sbn ->
            val category = getNotificationCategory(sbn, context)
            if (category != NotificationCategory.CALLS) {
                result[category] = (result[category] ?: 0) + 1
            }
        }

        // Special handling for CALLS to sync with missedCallsCount and avoid double counting
        val callNotifications = notifications.filter { getNotificationCategory(it, context) == NotificationCategory.CALLS }
        val missedCallNotificationsCount = callNotifications.count { it.notification.category == Notification.CATEGORY_MISSED_CALL }
        val otherCallNotificationsCount = callNotifications.size - missedCallNotificationsCount
        
        // Final call count = (other call notifications like Voicemail) + (actual missed calls from log)
        val finalCallCount = otherCallNotificationsCount + missedCallsCount
        if (finalCallCount > 0) {
            result[NotificationCategory.CALLS] = finalCallCount
        }

        result
    }

    if (groupedNotifications.isNotEmpty()) {
        Row(
            modifier = Modifier
                .border(BorderStroke(2.dp, Color.Black), shape = RoundedCornerShape(8.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            groupedNotifications.entries.forEachIndexed { index, entry ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = entry.key.icon,
                        contentDescription = entry.key.name,
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = entry.value.toString(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
                if (index < groupedNotifications.size - 1) {
                    Spacer(modifier = Modifier.width(16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FavoriteAppItem(
    app: AppInfo, 
    notifications: List<StatusBarNotification>, 
    showAppIcons: Boolean,
    onLongClick: () -> Unit, 
    onClick: () -> Unit
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

    // Determine if this is the phone app
    val isPhoneApp = app.packageName == "com.mudita.dial" || 
                     app.packageName.contains("dialer") || 
                     app.packageName.contains("telecom")

    val totalCount = if (isPhoneApp) {
        val missedCallNotificationsCount = notifications.count { it.notification.category == Notification.CATEGORY_MISSED_CALL }
        (notifications.size - missedCallNotificationsCount) + missedCallsCount
    } else {
        notifications.size
    }

    Column(
        modifier = Modifier
            .padding(8.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            if (appIcon != null) {
                Image(
                    painter = rememberDrawablePainter(drawable = appIcon),
                    contentDescription = "${app.name} icon",
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = app.name,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color.Black
            )
            if (totalCount > 0) {
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .border(BorderStroke(2.dp, Color.Black), shape = CircleShape)
                        .background(color = Color.White, shape = CircleShape)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (totalCount > 99) "99+" else totalCount.toString(),
                        color = Color.Black,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        var previewText: String? = null
        
        if (isPhoneApp && missedCallsCount > 0) {
            // Priority: Show missed call info if it's the phone app and we have missed calls
            try {
                val cursor = context.contentResolver.query(
                    CallLog.Calls.CONTENT_URI,
                    arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME),
                    "${CallLog.Calls.TYPE} = ${CallLog.Calls.MISSED_TYPE} AND (${CallLog.Calls.NEW} = 1 OR ${CallLog.Calls.IS_READ} = 0)",
                    null,
                    "${CallLog.Calls.DATE} DESC"
                )
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIdx = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
                        val numIdx = it.getColumnIndex(CallLog.Calls.NUMBER)
                        
                        val name = if (nameIdx != -1) it.getString(nameIdx) else null
                        val number = if (numIdx != -1) it.getString(numIdx) else null
                        
                        val displayInfo = if (!name.isNullOrBlank()) name else number
                        if (!displayInfo.isNullOrBlank()) {
                            previewText = "Missed: $displayInfo"
                        }
                    }
                }
            } catch (e: Exception) {
                // Fallback will be handled below
            }
            if (previewText == null) {
                previewText = "Missed call"
            }
        } 
        
        // If we still don't have a preview (or it's not a phone app/no missed calls), check notifications
        if (previewText == null && notifications.isNotEmpty()) {
            val firstNotification = notifications.first().notification
            val extras = firstNotification.extras
            val title = extras.getString("android.title") ?: extras.getString(Notification.EXTRA_TITLE)
            val text = extras.getCharSequence("android.text") ?: extras.getCharSequence(Notification.EXTRA_TEXT)

            previewText = when {
                !title.isNullOrBlank() && !text.isNullOrBlank() -> "$title: $text"
                !text.isNullOrBlank() -> text.toString()
                !title.isNullOrBlank() -> title
                else -> null
            }
        }

        if (!previewText.isNullOrBlank()) {
            Text(
                text = previewText!!,
                fontSize = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
                color = Color.Black
            )
        }
    }
}

@Composable
fun RenameAppDialog(
    appInfo: AppInfo,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
    onUninstall: (() -> Unit)? = null,
    showAppIcons: Boolean
) {
    var newName by remember { mutableStateOf(appInfo.customName ?: appInfo.name) }
    val context = LocalContext.current
    val packageManager = context.packageManager

    val appIcon: Drawable? = if (showAppIcons) {
        try {
            packageManager.getApplicationIcon(appInfo.packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    } else {
        null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (appIcon != null) {
                        Image(
                            painter = rememberDrawablePainter(drawable = appIcon),
                            contentDescription = "${appInfo.name} icon",
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = "Edit ${appInfo.name}",
                        color = Color.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                if (onUninstall != null) {
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
                horizontalArrangement = Arrangement.End
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
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { onRename(newName) },
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White
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

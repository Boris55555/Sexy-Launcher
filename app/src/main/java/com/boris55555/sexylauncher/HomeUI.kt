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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun StickyNote(
    title: String? = null,
    text: String,
    modifier: Modifier = Modifier,
    fontSizeAdjustment: Int = 0
) {
    if (text.isBlank() && title.isNullOrBlank()) return

    Box(
        modifier = modifier
            .padding(8.dp)
            .background(Color.White, shape = RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            if (!title.isNullOrBlank()) {
                Text(
                    text = title,
                    fontSize = (20 + fontSizeAdjustment).sp,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                HorizontalDivider(color = Color.Black, thickness = 1.dp, modifier = Modifier.padding(bottom = 8.dp))
            }
            Text(
                text = text,
                fontSize = (18 + fontSizeAdjustment).sp,
                color = Color.Black,
                fontWeight = FontWeight.Normal,
                lineHeight = (22 + fontSizeAdjustment).sp
            )
        }
    }
}

@Composable
fun DateText() {
    val date = remember { Calendar.getInstance().time }
    val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
    val dateFormat = SimpleDateFormat("d. MMMM yyyy", Locale.getDefault())

    val dayText = dayFormat.format(date)
    val dateText = dateFormat.format(date)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = dayText.uppercase(),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Text(
            text = dateText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold, // Changed to Bold
            color = Color.Black
        )
    }
}

enum class NotificationCategory(val icon: ImageVector) {
    EMAIL(Icons.Default.AlternateEmail),
    MESSAGES(Icons.AutoMirrored.Filled.Message),
    CALLS(Icons.Default.Phone),
    CALENDAR(Icons.Default.CalendarToday),
    REMINDERS(Icons.Default.Notifications),
    AUDIO(Icons.Default.MusicNote),
    BLUETOOTH(Icons.Filled.BluetoothConnected),
    SECURITY(Icons.Default.Key),
    NAVIGATION(Icons.Default.Map),
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

        packageName.contains("keepass") -> NotificationCategory.SECURITY

        packageName.contains("maps") || 
        packageName.contains("navigation") || 
        packageName.contains("waze") || 
        sbn.notification.category == Notification.CATEGORY_NAVIGATION
            -> NotificationCategory.NAVIGATION

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
        packageName.contains("incallui") ||
        packageName == "com.mudita.dial"
            -> {
                // Threema Libre push service often uses CATEGORY_CALL to stay alive, 
                // but it's not a real call unless it contains call-related words.
                if (packageName.contains("threema") && !fullContent.contains("soittaa") && !fullContent.contains("calling")) {
                    NotificationCategory.MESSAGES
                } else {
                    NotificationCategory.CALLS
                }
            }

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
        packageName.contains("vector") ||
        packageName.contains("fluffychat") ||
        packageName.contains("sunup") ||
        packageName == "org.mlm.mages" ||
        packageName == "com.mudita.messages" ||
        fullContent.contains("viesti") ||
        fullContent.contains("message") ||
        fullContent.contains("chat")
            -> NotificationCategory.MESSAGES

        // 3.5 Audio / Media
        sbn.notification.category == Notification.CATEGORY_TRANSPORT ||
        sbn.notification.category == Notification.CATEGORY_SERVICE ||
        packageName.contains("audio") ||
        packageName.contains("music") ||
        packageName.contains("player") ||
        packageName.contains("calmcast") ||
        packageName.contains("tubular") ||
        packageName.contains("newpipe") ||
        packageName.contains("antennapod") ||
        packageName.contains("spotify") ||
        packageName.contains("podcast") ||
        packageName == "com.mudita.audio.player" ||
        fullContent.contains("playing") ||
        fullContent.contains("soittaa") ||
        fullContent.contains("toistetaan")
            -> NotificationCategory.AUDIO
        fullContent.contains("chat")
            -> NotificationCategory.MESSAGES
            
        // 4. Calendar
        sbn.notification.category == Notification.CATEGORY_EVENT ||
        packageName.contains("calendar")
            -> NotificationCategory.CALENDAR
            
        else -> NotificationCategory.OTHER
    }
}

fun getNotificationCount(sbn: StatusBarNotification): Int {
    val extras = sbn.notification.extras
    
    // 1. Check for MessagingStyle messages
    val messages = extras.getParcelableArray("android.messages")
    if (messages != null && messages.isNotEmpty()) {
        return messages.size
    }

    // 2. Check for badge icon intent or specific count extra
    val badgeCount = extras.getInt("android.notification.count", 0)
    if (badgeCount > 0) return badgeCount

    return 1 // Default to 1 if no specific count found
}

fun isLikelySummary(sbn: StatusBarNotification): Boolean {
    val extras = sbn.notification.extras
    
    // A group summary is the most common form of "count-only" notification
    if ((sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return true
    
    // If it has multiple messages, it's NOT a summary, it's a collection
    if (extras.containsKey(Notification.EXTRA_MESSAGES)) return false

    // Look for indicators that this is a placeholder/count notification
    val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.lowercase() ?: ""
    val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.lowercase() ?: ""
    val combined = "$title $text"

    // Keywords that strongly suggest a summary/count notification
    val summaryKeywords = listOf(
        "new messages", "notifications", "viestiä", "ilmoitusta", 
        "missed calls", "vastaamatonta", "uutta viestiä"
    )

    // Check if it has a list of lines but no main text body
    if (extras.containsKey(Notification.EXTRA_TEXT_LINES)) {
        if (!extras.containsKey(Notification.EXTRA_CONVERSATION_TITLE)) return true
    }

    // If there's a number and summary keywords, it's likely a summary (e.g., \"2 messages\")
    val hasDigit = combined.any { it.isDigit() }
    if (hasDigit && summaryKeywords.any { combined.contains(it) }) return true

    return false
}

fun getSmartNotificationCount(notifications: List<StatusBarNotification>): Int {
    if (notifications.isEmpty()) return 0
    
    // Group by package to handle each app's summaries separately
    val packageGroups = notifications.groupBy { it.packageName }
    
    return packageGroups.values.sumOf { pkgNotifs ->
        val individuals = pkgNotifs.filter { !isLikelySummary(it) }
        val summaries = pkgNotifs.filter { isLikelySummary(it) }
        
        when {
            individuals.isNotEmpty() -> {
                // If we have actual messages, ignore any "summary" notifications for this app
                individuals.sumOf { getNotificationCount(it) }
            }
            summaries.isNotEmpty() -> {
                // If only summaries exist, use the count from the largest summary
                summaries.maxOf { getNotificationCount(it) }
            }
            else -> 0
        }
    }
}

@Composable
fun NotificationIndicator(
    notifications: List<StatusBarNotification>,
    bluetoothState: MainActivity.BluetoothState = MainActivity.BluetoothState.Disabled,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val missedCallsCount by NotificationListener.missedCallsCount.collectAsState()
    val unreadSmsCount by NotificationListener.unreadSmsCount.collectAsState()
    val activeCall by NotificationListener.activeCall.collectAsState()

    val groupedNotifications = remember(notifications, missedCallsCount, unreadSmsCount, activeCall, bluetoothState) {
        val result = mutableMapOf<NotificationCategory, Int>()
        
        notifications.forEach { sbn ->
            val category = getNotificationCategory(sbn, context)
            if (category != NotificationCategory.CALLS && category != NotificationCategory.MESSAGES) {
                val count = getNotificationCount(sbn)
                result[category] = (result[category] ?: 0) + count
            }
        }

        // Bluetooth connected state
        if (bluetoothState is MainActivity.BluetoothState.Connected) {
            result[NotificationCategory.BLUETOOTH] = 1
        }

        // Special handling for CALLS to sync with missedCallsCount and avoid double counting
        val callNotifications = notifications.filter { getNotificationCategory(it, context) == NotificationCategory.CALLS }
        
        // Count actual notifications that are not the missed call notifications we track via log
        val otherCallNotificationsCount = callNotifications.filter { it.notification.category != Notification.CATEGORY_MISSED_CALL }.sumOf { getNotificationCount(it) }
        
        // Check if we already have an ongoing call notification in the list
        val hasOngoingCallNotification = callNotifications.any { (it.notification.flags and Notification.FLAG_ONGOING_EVENT) != 0 }

        // Final call count = (other call notifications like Voicemail) + (actual missed calls from log) + (1 if call is active and NOT already in notifications)
        var finalCallCount = otherCallNotificationsCount + missedCallsCount
        if (activeCall != null && !hasOngoingCallNotification) {
            finalCallCount += 1
        }

        if (finalCallCount > 0) {
            result[NotificationCategory.CALLS] = finalCallCount
        }

        // Special handling for MESSAGES to sync with unreadSmsCount
        val messageNotifications = notifications.filter { getNotificationCategory(it, context) == NotificationCategory.MESSAGES }
        
        // Group notifications by package to handle SMS apps separately
        val messageNotificationsByApp = messageNotifications.groupBy { it.packageName }
        
        val smsAppPackages = listOf("com.mudita.messages", "com.android.messaging", "com.google.android.apps.messaging")
        
        var smsAppNotificationsCount = 0
        var otherMessageNotificationsCount = 0
        
        messageNotificationsByApp.forEach { (pkg, notifs) ->
            val count = getSmartNotificationCount(notifs)
            if (pkg in smsAppPackages) {
                smsAppNotificationsCount += count
            } else {
                otherMessageNotificationsCount += count
            }
        }
        
        val finalMessageCount = otherMessageNotificationsCount + maxOf(unreadSmsCount, smsAppNotificationsCount)
        if (finalMessageCount > 0) {
            result[NotificationCategory.MESSAGES] = finalMessageCount
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
                    if (entry.key != NotificationCategory.BLUETOOTH) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = entry.value.toString(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
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
    modifier: Modifier = Modifier,
    showNotificationPreviews: Boolean = true,
    sexyMode: Boolean = false,
    sexyAlignment: String = "left",
    onLongClick: () -> Unit, 
    onClick: () -> Unit,
    fontSizeAdjustment: Int = 0
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    val missedCallsCount by NotificationListener.missedCallsCount.collectAsState()
    val unreadSmsCount by NotificationListener.unreadSmsCount.collectAsState()

    val appIcon: Drawable? = if (showAppIcons || sexyMode) {
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
    
    val isSmsApp = app.packageName == "com.mudita.messages" ||
                   app.packageName == "com.android.messaging" ||
                   app.packageName == "com.google.android.apps.messaging" ||
                   app.packageName.contains("messaging") ||
                   app.packageName.contains("sms")

    val totalCount = when {
        isPhoneApp -> {
            // Phone app: Missed calls from DB + any other non-missed-call notifications from dialer
            val otherCallNotificationsCount = notifications.filter { 
                it.packageName == app.packageName &&
                getNotificationCategory(it, context) == NotificationCategory.CALLS && 
                it.notification.category != Notification.CATEGORY_MISSED_CALL 
            }.sumOf { getNotificationCount(it) }
            otherCallNotificationsCount + missedCallsCount
        }
        isSmsApp -> {
            // SMS app: Max of (Unread SMS from DB) and (Active notifications)
            val knownSmsApps = listOf("com.mudita.messages", "com.android.messaging", "com.google.android.apps.messaging")
            val appNotifications = notifications.filter { it.packageName == app.packageName }
            val notificationCount = getSmartNotificationCount(appNotifications)
            if (app.packageName in knownSmsApps) {
                maxOf(unreadSmsCount, notificationCount)
            } else {
                notificationCount
            }
        }
        else -> {
            // Other apps: Only their own notifications
            notifications.filter { it.packageName == app.packageName }.sumOf { getNotificationCount(it) }
        }
    }
    
    val activeCallInfo by MainActivity.activeCallInfo.collectAsState()

    val itemModifier = if (sexyMode) {
        Modifier
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    } else {
        Modifier
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    }

    Column(
        modifier = modifier
            .then(if (sexyMode && sexyAlignment == "bottom") Modifier.wrapContentSize() 
                  else if (sexyMode) Modifier.fillMaxWidth().wrapContentHeight() 
                  else Modifier.fillMaxWidth().height((60 + fontSizeAdjustment * 2).dp))
            .padding(horizontal = if (sexyMode) 0.dp else 8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = if (sexyMode && sexyAlignment == "bottom") Alignment.CenterHorizontally else Alignment.Start
    ) {
        val previewText = remember(showNotificationPreviews, notifications, missedCallsCount, activeCallInfo, sexyAlignment, sexyMode) {
            if (!showNotificationPreviews || sexyMode) return@remember null
            
            var text: String? = null
            if (isPhoneApp && activeCallInfo != null) {
                text = activeCallInfo
            } else if (isPhoneApp && missedCallsCount > 0) {
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
                            text = if (!name.isNullOrBlank()) "Missed: $name" else "Missed: $number"
                        }
                    }
                } catch (e: Exception) {}
                if (text == null) text = "Missed call"
            }
            
            if (text == null) {
                val appNotifications = notifications.filter { it.packageName == app.packageName }
                if (appNotifications.isNotEmpty()) {
                    val extras = appNotifications.first().notification.extras
                    val title = extras.getString("android.title") ?: extras.getString(Notification.EXTRA_TITLE)
                    val body = extras.getCharSequence("android.text") ?: extras.getCharSequence(Notification.EXTRA_TEXT)
                    text = when {
                        !title.isNullOrBlank() && !body.isNullOrBlank() -> "$title: $body"
                        !body.isNullOrBlank() -> body.toString()
                        !title.isNullOrBlank() -> title
                        else -> null
                    }
                }
            }
            text
        }

        if (sexyMode) {
            if (sexyAlignment == "bottom") {
                // Bottom alignment: Just the icon and name
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(72.dp).combinedClickable(onClick = onClick, onLongClick = onLongClick)
                ) {
                    Box(
                        modifier = itemModifier,
                        contentAlignment = Alignment.Center
                    ) {
                        if (appIcon != null) {
                            Box(contentAlignment = Alignment.TopEnd) {
                                Image(
                                    painter = rememberDrawablePainter(drawable = appIcon),
                                    contentDescription = "${app.name} icon",
                                    modifier = Modifier.size(64.dp)
                                )
                                if (totalCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .offset(x = 4.dp, y = (-4).dp)
                                            .size(20.dp)
                                            .background(Color.Black, shape = CircleShape)
                                            .border(1.dp, Color.White, shape = CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (totalCount > 9) "!" else totalCount.toString(),
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        } else {
                            if (app.packageName.isEmpty()) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add app",
                                    tint = Color.Black,
                                    modifier = Modifier.size(32.dp)
                                )
                            } else {
                                Text(text = app.name.take(1).uppercase(), fontSize = 32.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    val displayName = if (app.name.length > 10) app.name.take(7) + "..." else app.name
                    Text(
                        text = displayName,
                        fontSize = (11 + fontSizeAdjustment).sp,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.offset(y = (-2).dp)
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp) 
                        .padding(horizontal = 8.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(72.dp)
                    ) {
                        Box(
                            modifier = itemModifier,
                            contentAlignment = Alignment.Center
                        ) {
                            if (appIcon != null) {
                                Box(contentAlignment = Alignment.TopEnd) {
                                    Image(
                                        painter = rememberDrawablePainter(drawable = appIcon),
                                        contentDescription = "${app.name} icon",
                                        modifier = Modifier.size(64.dp)
                                    )
                                    if (totalCount > 0) {
                                        Box(
                                            modifier = Modifier
                                                .offset(x = 4.dp, y = (-4).dp)
                                                .size(20.dp)
                                                .background(Color.Black, shape = CircleShape)
                                                .border(1.dp, Color.White, shape = CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (totalCount > 9) "!" else totalCount.toString(),
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            } else {
                                if (app.packageName.isEmpty()) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add app",
                                        tint = Color.Black,
                                        modifier = Modifier.size(32.dp)
                                    )
                                } else {
                                    Text(text = app.name.take(1).uppercase(), fontSize = 32.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        val displayName = if (app.name.length > 10) app.name.take(7) + "..." else app.name
                        Text(
                            text = displayName,
                            fontSize = (11 + fontSizeAdjustment).sp,
                            maxLines = 1,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.offset(y = (-2).dp)
                        )
                    }
                    
                    if (!previewText.isNullOrBlank()) {
                        Text(
                            text = previewText,
                            fontSize = (14 + fontSizeAdjustment).sp,
                            color = Color.Gray,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .padding(start = 12.dp, end = 16.dp)
                                .weight(1f)
                                .clickable { onClick() }
                        )
                    }
                }
            }
        } else {
            // Standard Layout... (keeping original logic for non-sexy mode)
            Row(
                modifier = Modifier
                    .wrapContentWidth()
                    .combinedClickable(onClick = onClick, onLongClick = onLongClick),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                if (appIcon != null) {
                    Box(contentAlignment = Alignment.TopEnd) {
                        Image(
                            painter = rememberDrawablePainter(drawable = appIcon),
                            contentDescription = "${app.name} icon",
                            modifier = Modifier.size(40.dp)
                        )
                        if (totalCount > 0) {
                            Box(
                                modifier = Modifier.offset(x = 4.dp, y = (-4).dp).size(16.dp).background(Color.Black, shape = CircleShape).border(1.dp, Color.White, shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = if (totalCount > 9) "!" else totalCount.toString(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = app.name,
                    fontSize = (32 + fontSizeAdjustment).sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.Black,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (totalCount > 0 && appIcon == null) {
                    Box(
                        modifier = Modifier.padding(start = 8.dp).border(BorderStroke(2.dp, Color.Black), shape = CircleShape).background(color = Color.White, shape = CircleShape).padding(horizontal = 4.dp, vertical = 1.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = if (totalCount > 99) "99+" else totalCount.toString(), color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
            if (!previewText.isNullOrBlank()) {
                Text(
                    text = previewText,
                    fontSize = (14 + fontSizeAdjustment).sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = if (appIcon != null) 56.dp else 8.dp, bottom = 8.dp)
                )
            }
        }
    }
}

@Composable
fun RenameAppDialog(
    appInfo: AppInfo,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
    onUninstall: (() -> Unit)? = null,
    showAppIcons: Boolean = false,
    isHiddenFromTop10: Boolean = false,
    onToggleHideFromTop10: (() -> Unit)? = null
) {
    var newName by remember { mutableStateOf(appInfo.customName ?: appInfo.name) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showAppIcons) {
                    val icon = try {
                        context.packageManager.getApplicationIcon(appInfo.packageName)
                    } catch (e: Exception) {
                        null
                    }
                    if (icon != null) {
                        Image(
                            painter = rememberDrawablePainter(drawable = icon),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                }
                Text("Rename App", color = Color.Black)
            }
        },
        text = {
            Column {
                TextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedIndicatorColor = Color.Black,
                        unfocusedIndicatorColor = Color.Black
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onRename(newName) }),
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (onToggleHideFromTop10 != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleHideFromTop10() }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isHiddenFromTop10) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = Color.Black
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isHiddenFromTop10) "Show in Top 10" else "Hide from Top 10",
                            color = Color.Black
                        )
                    }
                }
            }
        },
        confirmButton = {
            EInkButton(onClick = { onRename(newName) }) {
                Text("OK")
            }
        },
        dismissButton = {
            Row {
                if (onUninstall != null) {
                    EInkButton(
                        onClick = onUninstall
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Uninstall", tint = Color.Black)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Uninstall")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                EInkButton(
                    onClick = onDismiss
                ) {
                    Text("Cancel")
                }
            }
        },
        containerColor = Color.White
    )
}

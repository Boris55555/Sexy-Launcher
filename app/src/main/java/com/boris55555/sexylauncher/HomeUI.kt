package com.boris55555.sexylauncher

import android.app.Notification
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
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
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Message
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
    MESSAGES(Icons.Default.Message),
    CALLS(Icons.Default.Phone),
    CALENDAR(Icons.Default.CalendarToday),
    REMINDERS(Icons.Default.Notifications),
    OTHER(Icons.Default.Android)
}

@Composable
fun NotificationIndicator(notifications: List<StatusBarNotification>, onClick: () -> Unit) {
    val context = LocalContext.current

    val groupedNotifications = remember(notifications) {
        notifications.groupingBy {
            val packageName = it.packageName.lowercase(Locale.getDefault())
            when {
                it.packageName == context.packageName -> NotificationCategory.REMINDERS
                it.notification.category == Notification.CATEGORY_EMAIL ||
                packageName.contains("mail") ||
                packageName.contains("gmail") ||
                packageName.contains("outlook") ||
                packageName.contains("thunderbird")
                    -> NotificationCategory.EMAIL
                it.notification.category == Notification.CATEGORY_MESSAGE
                    -> NotificationCategory.MESSAGES
                it.notification.category == Notification.CATEGORY_CALL -> NotificationCategory.CALLS
                it.notification.category == Notification.CATEGORY_EVENT -> NotificationCategory.CALENDAR
                else -> NotificationCategory.OTHER
            }
        }.eachCount()
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

    val appIcon: Drawable? = if (showAppIcons) {
        try {
            packageManager.getApplicationIcon(app.packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    } else {
        null
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

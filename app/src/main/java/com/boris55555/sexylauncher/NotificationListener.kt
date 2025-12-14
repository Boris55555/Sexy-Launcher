package com.boris55555.sexylauncher

import android.app.Notification
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class NotificationListener : NotificationListenerService() {

    companion object {
        private val _notifications = MutableStateFlow<List<StatusBarNotification>>(emptyList())
        val notifications = _notifications.asStateFlow()

        var instance: NotificationListener? = null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        updateNotifications()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        updateNotifications()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        updateNotifications()
    }

    private fun updateNotifications() {
        val prefs = applicationContext.getSharedPreferences("SexyLauncherPrefs", Context.MODE_PRIVATE)
        val disableDuraSpeed = prefs.getBoolean("disable_duraspeed_notifications", false)

        _notifications.value = (activeNotifications ?: emptyArray()).filter {
            isNotificationRelevant(it, disableDuraSpeed)
        }.sortedByDescending { it.postTime }
    }

    private fun isNotificationRelevant(sbn: StatusBarNotification, disableDuraSpeed: Boolean): Boolean {
        if (disableDuraSpeed && sbn.packageName.startsWith("com.duraspeed")) {
            return false
        }
        // Filter out ongoing media notifications
        if (sbn.notification.category == Notification.CATEGORY_TRANSPORT) {
            return false
        }
        // Filter out group summary notifications
        if ((sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) {
            return false
        }
        // Filter out low-priority ONGOING service and system notifications, but allow others (like calls)
        val isOngoing = (sbn.notification.flags and Notification.FLAG_ONGOING_EVENT) != 0
        val isServiceOrSystem = sbn.notification.category == Notification.CATEGORY_SERVICE || sbn.notification.category == Notification.CATEGORY_SYSTEM
        if (isOngoing && isServiceOrSystem) {
            return false
        }
        // Filter out summary notifications that might not be flagged as FLAG_GROUP_SUMMARY
        if (sbn.notification.extras.containsKey(Notification.EXTRA_SUMMARY_TEXT)) {
             val summaryText = sbn.notification.extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString()
             if (summaryText != null && summaryText.matches(Regex(".* new messages?.*\\d+.*chats?.*|\\d+.*messages?.*from.*\\d+.*chats?.*|2 new messages"))){
                return false
             }
        }
        return true
    }

    fun dismissNotification(key: String) {
        cancelNotification(key)
        // The list will auto-update via onNotificationRemoved -> updateNotifications()
    }

    fun requestRefresh() {
        onListenerConnected()
    }
}

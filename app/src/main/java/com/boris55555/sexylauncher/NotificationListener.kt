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
        // Absolute exception for Mudita dialer
        if (sbn.packageName == "com.mudita.dial") {
            return true
        }

        val text = sbn.notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()

        if (disableDuraSpeed && (sbn.packageName.startsWith("com.duraspeed") || text?.contains("DuraSpeed", ignoreCase = true) == true)) {
            return false
        }

        if (sbn.notification.category == Notification.CATEGORY_TRANSPORT) {
            return false
        }

        val isCallRelated = sbn.notification.category == Notification.CATEGORY_MISSED_CALL || sbn.notification.category == Notification.CATEGORY_CALL || text?.contains("missed call", ignoreCase = true) == true

        val isGroupSummary = (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0
        if (isGroupSummary && !isCallRelated) {
            return false
        }

        val isOngoing = (sbn.notification.flags and Notification.FLAG_ONGOING_EVENT) != 0
        val isServiceOrSystem = sbn.notification.category == Notification.CATEGORY_SERVICE || sbn.notification.category == Notification.CATEGORY_SYSTEM
        if (isOngoing && isServiceOrSystem && !isCallRelated) {
            return false
        }

//        if (sbn.notification.extras.containsKey(Notification.EXTRA_SUMMARY_TEXT) && !isCallRelated) {
//            val summaryText = sbn.notification.extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString()
//            if (summaryText != null && summaryText.matches(Regex(".* new messages?.*|.*\d+.*chats?.*"))){
//                return false
//            }
//        }

        return true
    }

    fun dismissNotification(key: String) {
        cancelNotification(key)
    }

    fun requestRefresh() {
        onListenerConnected()
    }
}

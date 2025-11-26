package com.example.sexylauncher

import android.app.Notification
import android.content.ComponentName
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
        _notifications.value = activeNotifications?.filter { isNotificationRelevant(it) }?.sortedByDescending { it.postTime } ?: emptyList()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        if (isNotificationRelevant(sbn)) {
            val currentList = _notifications.value.toMutableList()
            currentList.removeAll { it.key == sbn.key }
            currentList.add(sbn)
            _notifications.value = currentList.sortedByDescending { it.postTime }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn ?: return
        _notifications.value = _notifications.value.filterNot { it.key == sbn.key }
    }

    private fun isNotificationRelevant(sbn: StatusBarNotification): Boolean {
        // Filter out ongoing media notifications
        if (sbn.notification.category == Notification.CATEGORY_TRANSPORT) {
            return false
        }
        // Filter out group summary notifications
        if ((sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) {
            return false
        }
        // Filter out low-priority ONGOING service and system notifications
        val isOngoingServiceOrSystem = (sbn.notification.flags and Notification.FLAG_ONGOING_EVENT) != 0 &&
                (sbn.notification.category == Notification.CATEGORY_SERVICE || sbn.notification.category == Notification.CATEGORY_SYSTEM)
        if (isOngoingServiceOrSystem) {
            return false
        }
        return true
    }

    fun dismissNotification(key: String) {
        // Remove from our local list immediately for instant UI feedback
        _notifications.value = _notifications.value.filterNot { it.key == key }
        // Ask the system to cancel the notification
        cancelNotification(key)
    }

    fun requestRefresh() {
        onListenerConnected()
    }
}

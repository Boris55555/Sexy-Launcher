package com.example.sexylauncher

import android.app.Notification
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
        _notifications.value = activeNotifications?.filter { isNotificationRelevant(it) } ?: emptyList()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        super.onNotificationPosted(sbn)
        if (isNotificationRelevant(sbn)) {
            val currentList = _notifications.value.filterNot { it.key == sbn.key }.toMutableList()
            currentList.add(0, sbn) 
            _notifications.value = currentList.sortedByDescending { it.postTime }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn ?: return
        super.onNotificationRemoved(sbn)
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
        return true
    }

    fun dismissNotification(key: String) {
        cancelNotification(key)
    }
}

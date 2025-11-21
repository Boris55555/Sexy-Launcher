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
        updateNotifications()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        updateNotifications()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        updateNotifications()
    }

    private fun updateNotifications() {
        _notifications.value = activeNotifications.filter { isNotificationRelevant(it) }
    }

    private fun isNotificationRelevant(sbn: StatusBarNotification): Boolean {
        // Filter out ongoing media notifications
        return sbn.notification.category != Notification.CATEGORY_TRANSPORT
    }

    fun dismissNotification(key: String) {
        _notifications.value = _notifications.value.filterNot { it.key == key }
        cancelNotification(key)
    }

    fun dismissAllNotifications() {
        _notifications.value.forEach { sbn ->
            cancelNotification(sbn.key)
        }
        _notifications.value = emptyList()
    }
}

package com.example.sexylauncher

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
        _notifications.value = activeNotifications.toList()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        _notifications.value = activeNotifications.toList()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        _notifications.value = activeNotifications.toList()
    }

    fun dismissNotification(key: String) {
        _notifications.value = _notifications.value.filterNot { it.key == key }
        cancelNotification(key)
    }

    fun dismissAllNotifications() {
        _notifications.value = emptyList()
        cancelAllNotifications()
    }
}

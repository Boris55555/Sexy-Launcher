package com.boris55555.sexylauncher

import android.app.Notification
import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.CallLog
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class NotificationListener : NotificationListenerService() {

    private var callLogObserver: ContentObserver? = null

    companion object {
        private val _notifications = MutableStateFlow<List<StatusBarNotification>>(emptyList())
        val notifications = _notifications.asStateFlow()

        private val _missedCallsCount = MutableStateFlow(0)
        val missedCallsCount = _missedCallsCount.asStateFlow()

        var instance: NotificationListener? = null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Register observer for call log changes
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                updateNotifications()
            }
        }
        try {
            contentResolver.registerContentObserver(CallLog.Calls.CONTENT_URI, true, observer)
            callLogObserver = observer
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        updateMissedCallsCount()
    }

    override fun onDestroy() {
        super.onDestroy()
        callLogObserver?.let {
            contentResolver.unregisterContentObserver(it)
        }
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

        updateMissedCallsCount()
    }

    private fun updateMissedCallsCount() {
        try {
            val contentResolver = applicationContext.contentResolver
            val cursor = contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                null,
                "${CallLog.Calls.TYPE} = ${CallLog.Calls.MISSED_TYPE} AND (${CallLog.Calls.NEW} = 1 OR ${CallLog.Calls.IS_READ} = 0)",
                null,
                null
            )
            _missedCallsCount.value = cursor?.count ?: 0
            cursor?.close()
        } catch (e: Exception) {
            _missedCallsCount.value = 0
        }
    }

    private fun isNotificationRelevant(sbn: StatusBarNotification, disableDuraSpeed: Boolean): Boolean {
        val packageName = sbn.packageName.lowercase(Locale.getDefault())
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.lowercase(Locale.getDefault()) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.lowercase(Locale.getDefault()) ?: ""
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.lowercase(Locale.getDefault()) ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.lowercase(Locale.getDefault()) ?: ""
        val fullContent = "$title $text $subText $bigText"

        val isOngoing = (sbn.notification.flags and Notification.FLAG_ONGOING_EVENT) != 0
        val isGroupSummary = (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0
        
        // Progress info
        val hasProgressKey = extras.containsKey(Notification.EXTRA_PROGRESS)
        val isIndeterminate = extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE)
        val progressMax = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
        val progressCurrent = extras.getInt(Notification.EXTRA_PROGRESS, 0)
        val hasActiveProgress = isIndeterminate || (progressMax > 0 && progressCurrent < progressMax)

        // 1. Call related (Always show)
        val isCallRelated = sbn.notification.category == Notification.CATEGORY_MISSED_CALL ||
                sbn.notification.category == Notification.CATEGORY_CALL ||
                fullContent.contains("missed call") ||
                packageName.contains("dialer") ||
                packageName.contains("telecom") ||
                packageName.contains("phone") ||
                packageName == "com.mudita.dial"

        if (isCallRelated) return true

        // 1.5 Message related (Always show)
        val isMessageRelated = sbn.notification.category == Notification.CATEGORY_MESSAGE ||
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

        if (isMessageRelated) return true

        // 2. Specific Whitelist for Mudita Maps
        if (packageName.contains("mudita.maps")) {
            if (isGroupSummary) return false
            
            // Special handling for Mudita Maps to hide the idle "in progress" notification
            if (isOngoing && !hasActiveProgress) {
                // If it's the downloader service but not currently downloading, hide it
                if (fullContent.contains("downloader") || fullContent.contains("in progress")) {
                    return false
                }
            }
            return true
        }

        // 3. Group summaries are usually noise
        if (isGroupSummary) return false

        // 4. General Download/Podcast/Media related
        val isDownloadRelated = sbn.notification.category == Notification.CATEGORY_PROGRESS ||
                fullContent.contains("download") ||
                fullContent.contains("ladataan") ||
                fullContent.contains("lataus") ||
                fullContent.contains("podcast") ||
                fullContent.contains("kartat") ||
                fullContent.contains("maps") ||
                fullContent.contains("transfer") ||
                fullContent.contains("siirretään") ||
                fullContent.contains("upload") ||
                hasProgressKey

        if (isDownloadRelated) {
            if (isOngoing) {
                // Skip idle/finished maps downloader
                if (fullContent.contains("maps downloader") && !hasActiveProgress) return false
                
                // If finished (100%), hide
                if (hasProgressKey && !isIndeterminate && progressMax > 0 && progressCurrent >= progressMax) return false
                
                return true
            }
            return true
        }

        // 5. Filtering for other types
        if (disableDuraSpeed && (packageName.startsWith("com.duraspeed") || fullContent.contains("duraspeed"))) {
            return false
        }

        val isServiceOrSystem = sbn.notification.category == Notification.CATEGORY_SERVICE || 
                               sbn.notification.category == Notification.CATEGORY_SYSTEM ||
                               packageName.contains("android.system") ||
                               packageName.contains("settings")

        if (isOngoing && isServiceOrSystem) {
            return false
        }

        return true
    }

    fun dismissNotification(key: String) {
        cancelNotification(key)
        updateNotifications()
    }

    fun requestRefresh() {
        updateNotifications()
    }
}

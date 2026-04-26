package com.boris55555.sexylauncher

import android.Manifest
import android.app.Notification
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.CallLog
import android.provider.ContactsContract
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class NotificationListener : NotificationListenerService() {

    private var callLogObserver: ContentObserver? = null
    private var telephonyManager: TelephonyManager? = null
    private var telephonyCallback: Any? = null

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
                checkLastCallInfo()
            }
        }
        try {
            contentResolver.registerContentObserver(CallLog.Calls.CONTENT_URI, true, observer)
            callLogObserver = observer
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        updateMissedCallsCount()
        listenToCallState()
    }

    override fun onDestroy() {
        super.onDestroy()
        callLogObserver?.let {
            contentResolver.unregisterContentObserver(it)
        }
        unregisterTelephonyListener()
        instance = null
    }

    private fun listenToCallState() {
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) {
                    updateCallInfo(state, null)
                }
            }
            telephonyManager?.registerTelephonyCallback(mainExecutor, callback)
            telephonyCallback = callback
        } else {
            val listener = object : android.telephony.PhoneStateListener() {
                @Deprecated("Deprecated in Java")
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    updateCallInfo(state, phoneNumber)
                }
            }
            @Suppress("DEPRECATION")
            telephonyManager?.listen(listener, android.telephony.PhoneStateListener.LISTEN_CALL_STATE)
            telephonyCallback = listener
        }
    }

    private fun unregisterTelephonyListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (telephonyCallback as? TelephonyCallback)?.let {
                telephonyManager?.unregisterTelephonyCallback(it)
            }
        } else {
            @Suppress("DEPRECATION")
            telephonyManager?.listen(telephonyCallback as? android.telephony.PhoneStateListener, android.telephony.PhoneStateListener.LISTEN_NONE)
        }
    }

    private fun checkLastCallInfo() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        if (telephonyManager?.callState == TelephonyManager.CALL_STATE_IDLE) {
            return
        }

        val cursor = contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.TYPE, CallLog.Calls.CACHED_NAME),
            null,
            null,
            CallLog.Calls.DATE + " DESC LIMIT 1"
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val type = it.getInt(it.getColumnIndexOrThrow(CallLog.Calls.TYPE))
                val number = it.getString(it.getColumnIndexOrThrow(CallLog.Calls.NUMBER))
                val cachedName = it.getString(it.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME))
                val name = cachedName ?: getContactName(number)
                val displayName = name ?: number
                
                val currentState = telephonyManager?.callState
                when {
                    currentState == TelephonyManager.CALL_STATE_RINGING -> {
                        MainActivity.updateActiveCallInfo("Incoming: $displayName")
                    }
                    type == CallLog.Calls.OUTGOING_TYPE && currentState == TelephonyManager.CALL_STATE_OFFHOOK -> {
                        MainActivity.updateActiveCallInfo("Calling: $displayName")
                    }
                    currentState == TelephonyManager.CALL_STATE_OFFHOOK -> {
                        MainActivity.updateActiveCallInfo("On a call: $displayName")
                    }
                }
            }
        }
    }

    private fun updateCallInfo(state: Int, phoneNumber: String?) {
        if (state == TelephonyManager.CALL_STATE_IDLE) {
            MainActivity.updateActiveCallInfo(null)
            return
        }

        // Try to get contact name if we have a number
        val name = phoneNumber?.let { getContactName(it) }
        val displayName = name ?: phoneNumber

        val info = when (state) {
            TelephonyManager.CALL_STATE_RINGING -> if (displayName != null) "Incoming: $displayName" else "Incoming call"
            TelephonyManager.CALL_STATE_OFFHOOK -> if (displayName != null) "On a call: $displayName" else "On a call"
            else -> null
        }
        
        if (info != null) {
            // If we have a generic "On a call" or "Incoming call", try to enrich it from CallLog
            if (!info.contains(":")) {
                MainActivity.updateActiveCallInfo(info)
                checkLastCallInfo()
            } else {
                MainActivity.updateActiveCallInfo(info)
            }
        }
    }

    private fun getContactName(phoneNumber: String): String? {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return null
        }
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
        return try {
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0)
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        updateNotifications()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        updateNotifications()
        updateCallStatus(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        updateNotifications()
        // If a call notification is removed, we might want to clear the active call info
        // but TelephonyManager usually handles the idle state better.
    }

    private fun updateCallStatus(sbn: StatusBarNotification?) {
        if (sbn == null) return
        val packageName = sbn.packageName.lowercase(Locale.getDefault())
        if (packageName.contains("dialer") || packageName.contains("telecom") || packageName.contains("phone") || packageName == "com.mudita.dial") {
            val extras = sbn.notification.extras
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            
            val fullContent = "$title $text".lowercase(Locale.getDefault())
            
            // Check for outgoing call
            if (fullContent.contains("calling") || fullContent.contains("soitetaan") || fullContent.contains("valitsee")) {
                MainActivity.updateActiveCallInfo("Calling: $title")
            } else if (fullContent.contains("on a call") || fullContent.contains("puhelu käynnissä") || fullContent.contains("active call")) {
                MainActivity.updateActiveCallInfo("On a call: $title")
            }
        }
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

        // 1.5 Message related (Always show, but filter out summaries)
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

        if (isMessageRelated) {
            // Filter out redundant summaries for messaging apps (like "Element Classic: 1 notification")
            if (isGroupSummary) return false
            
            // If the content is just the app name + "notification", it's likely a duplicate/summary
            if (text.contains("notification") || text.contains("ilmoitus")) {
                val appName = try {
                    applicationContext.packageManager.getApplicationLabel(
                        applicationContext.packageManager.getApplicationInfo(sbn.packageName, 0)
                    ).toString().lowercase(Locale.getDefault())
                } catch (e: Exception) { "" }
                
                if (title.contains(appName) && (text.contains("1") || text.isEmpty())) {
                    return false
                }
            }
            
            return true
        }

        // 1.7 Audio/Media (Keep playing media notifications)
        val isAudioRelated = sbn.notification.category == Notification.CATEGORY_TRANSPORT ||
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

        if (isAudioRelated && isOngoing) return true

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

        // 3. Group summaries are usually noise, but keep them for messages/calls if they are the main content
        if (isGroupSummary && !isCallRelated && !isMessageRelated) return false

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

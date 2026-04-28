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
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class NotificationListener : NotificationListenerService() {

    private var callLogObserver: ContentObserver? = null
    private var smsObserver: ContentObserver? = null
    private var telephonyManager: TelephonyManager? = null
    private var telephonyCallback: Any? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var pollingJob: Job? = null
    private var currentCallStartTime: Long = 0L

    companion object {
        private const val TAG = "SexyNotificationListener"
        private val _notifications = MutableStateFlow<List<StatusBarNotification>>(emptyList())
        val notifications = _notifications.asStateFlow()

        private val _missedCallsCount = MutableStateFlow(0)
        val missedCallsCount = _missedCallsCount.asStateFlow()

        private val _unreadSmsCount = MutableStateFlow(0)
        val unreadSmsCount = _unreadSmsCount.asStateFlow()

        private val _activeCall = MutableStateFlow<String?>(null)
        val activeCall = _activeCall.asStateFlow()

        var instance: NotificationListener? = null
        var lastKnownNumber: String? = null
        var isIncomingCall: Boolean = false
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        updateMissedCallsCount()
        updateUnreadSmsCount()
        tryRegisterObservers()
    }

    private fun tryRegisterObservers() {
        Log.d(TAG, "Attempting to register observers...")
        // Register observer for call log changes if permission granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "READ_CALL_LOG permission granted")
            if (callLogObserver == null) {
                val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                    override fun onChange(selfChange: Boolean) {
                        Log.d(TAG, "Call log changed")
                        updateNotifications()
                        checkLastCallInfo()
                    }
                }
                try {
                    contentResolver.registerContentObserver(CallLog.Calls.CONTENT_URI, true, observer)
                    callLogObserver = observer
                    Log.d(TAG, "Call log observer registered")
                } catch (e: Exception) {
                    Log.e(TAG, "Error registering call log observer", e)
                }
            }
        } else {
            Log.w(TAG, "READ_CALL_LOG permission NOT granted")
        }

        // Register observer for SMS changes if permission granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "READ_SMS permission granted")
            if (smsObserver == null) {
                val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                    override fun onChange(selfChange: Boolean) {
                        Log.d(TAG, "SMS database changed")
                        updateUnreadSmsCount()
                        updateNotifications()
                    }
                }
                try {
                    contentResolver.registerContentObserver(Uri.parse("content://sms"), true, observer)
                    smsObserver = observer
                    Log.d(TAG, "SMS observer registered")
                } catch (e: Exception) {
                    Log.e(TAG, "Error registering SMS observer", e)
                }
            }
        } else {
            Log.w(TAG, "READ_SMS permission NOT granted")
        }
        
        if (telephonyCallback == null) {
            listenToCallState()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        callLogObserver?.let {
            contentResolver.unregisterContentObserver(it)
        }
        smsObserver?.let {
            contentResolver.unregisterContentObserver(it)
        }
        unregisterTelephonyListener()
        instance = null
    }

    private fun listenToCallState() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "READ_PHONE_STATE permission NOT granted")
            return
        }

        try {
            Log.d(TAG, "Registering telephony callback")
            telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            
            // Check initial state
            val initialState = telephonyManager?.callState ?: TelephonyManager.CALL_STATE_IDLE
            if (initialState != TelephonyManager.CALL_STATE_IDLE) {
                Log.d(TAG, "Initial call state: $initialState")
                updateCallInfo(initialState, null)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                    override fun onCallStateChanged(state: Int) {
                        Log.d(TAG, "Call state changed (S+): $state")
                        updateCallInfo(state, null)
                    }
                }
                telephonyManager?.registerTelephonyCallback(mainExecutor, callback)
                telephonyCallback = callback
            } else {
                val listener = object : android.telephony.PhoneStateListener() {
                    @Deprecated("Deprecated in Java")
                    override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                        Log.d(TAG, "Call state changed (legacy): $state, number: ${phoneNumber?.take(4)}...")
                        updateCallInfo(state, phoneNumber)
                    }
                }
                @Suppress("DEPRECATION")
                telephonyManager?.listen(listener, android.telephony.PhoneStateListener.LISTEN_CALL_STATE)
                telephonyCallback = listener
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in listenToCallState", e)
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
            Log.w(TAG, "Cannot check call info: READ_CALL_LOG missing")
            return
        }

        try {
            val currentState = telephonyManager?.callState
            Log.d(TAG, "checkLastCallInfo, current state: $currentState, startTime: $currentCallStartTime")
            
            if (currentState == TelephonyManager.CALL_STATE_IDLE) {
                safeUpdateCallInfo(null)
                currentCallStartTime = 0L
                return
            }

            val cursor = contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.TYPE, CallLog.Calls.CACHED_NAME, CallLog.Calls.DATE),
                null,
                null,
                CallLog.Calls.DATE + " DESC"
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    val typeIdx = it.getColumnIndex(CallLog.Calls.TYPE)
                    val numberIdx = it.getColumnIndex(CallLog.Calls.NUMBER)
                    val nameIdx = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
                    val dateIdx = it.getColumnIndex(CallLog.Calls.DATE)
                    
                    val type = if (typeIdx != -1) it.getInt(typeIdx) else -1
                    val number = if (numberIdx != -1) it.getString(numberIdx) else null
                    val cachedName = if (nameIdx != -1) it.getString(nameIdx) else null
                    val callDate = if (dateIdx != -1) it.getLong(dateIdx) else 0L
                    
                    val name = cachedName ?: getContactName(number)
                    val displayName = name ?: number ?: "Unknown"
                    
                    Log.d(TAG, "Call log check: name=$displayName, date=$callDate, start=$currentCallStartTime")

                    // CRITICAL: Only trust call log entries that started AFTER our current call began
                    // Use a 5-second buffer (5000ms) in case of system clock slight desync
                    if (currentCallStartTime > 0 && callDate < (currentCallStartTime - 5000)) {
                        Log.d(TAG, "Ignoring old call log entry from previous call. Using lastKnownNumber if available.")
                        
                        // Fallback: use lastKnownNumber if log is not yet updated
                        val fallbackNumber = lastKnownNumber
                        if (fallbackNumber != null) {
                            val fallbackName = getContactName(fallbackNumber) ?: fallbackNumber
                            updateUiWithState(currentState ?: TelephonyManager.CALL_STATE_OFFHOOK, fallbackName, type)
                        } else {
                            // If we don't even have a lastKnownNumber, we can't do much but use the log's name
                            // OR just say "Call" if it's clearly old. But usually lastKnownNumber should be set.
                            updateUiWithState(currentState ?: TelephonyManager.CALL_STATE_OFFHOOK, displayName, type)
                        }
                        return@use
                    }

                    updateUiWithState(currentState ?: TelephonyManager.CALL_STATE_OFFHOOK, displayName, type)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in checkLastCallInfo", e)
        }
    }

    private fun updateUiWithState(state: Int, displayName: String, logType: Int) {
        if (state != TelephonyManager.CALL_STATE_IDLE) {
            val prefix = when {
                state == TelephonyManager.CALL_STATE_RINGING -> "Incoming: "
                state == TelephonyManager.CALL_STATE_OFFHOOK && !isIncomingCall -> "Calling: "
                else -> "Call: "
            }
            safeUpdateCallInfo("$prefix$displayName")
        }
    }

    private fun safeUpdateCallInfo(newInfo: String?) {
        val current = _activeCall.value
        
        // Don't overwrite a specific status with a more generic one
        // Priority: "Calling: Name" / "Incoming: Name" > "Call: Name" > "Call" > null
        if (newInfo != null && current != null) {
            val currentIsSpecific = current.contains(":")
            val newIsSpecific = newInfo.contains(":")
            
            if (currentIsSpecific && !newIsSpecific) {
                return // Keep the more specific one
            }
        }

        if (current != newInfo) {
            _activeCall.value = newInfo
            MainActivity.updateActiveCallInfo(newInfo)
            // Immediately refresh notifications to show/update the ActiveCallItem
            updateNotifications()
        }
    }

    fun refreshCallInfo() {
        val state = telephonyManager?.callState ?: return
        if (state != TelephonyManager.CALL_STATE_IDLE) {
            Log.d(TAG, "Refreshing call info due to new number discovery")
            updateCallInfo(state, null)
        }
    }

    private fun updateCallInfo(state: Int, phoneNumber: String?) {
        if (!phoneNumber.isNullOrEmpty()) {
            lastKnownNumber = phoneNumber
        }
        val finalNumber = phoneNumber ?: lastKnownNumber
        Log.d(TAG, "updateCallInfo: state=$state, hasNumber=${phoneNumber != null}, lastKnown=${lastKnownNumber != null}")
        
        if (state == TelephonyManager.CALL_STATE_IDLE) {
            pollingJob?.cancel()
            currentCallStartTime = 0L
            lastKnownNumber = null
            isIncomingCall = false
            safeUpdateCallInfo(null)
            return
        }

        // Record the start time of the call if we don't have it yet
        if (currentCallStartTime == 0L) {
            currentCallStartTime = System.currentTimeMillis()
            isIncomingCall = (state == TelephonyManager.CALL_STATE_RINGING)
            Log.d(TAG, "Call started at: $currentCallStartTime, incoming: $isIncomingCall")
        } else if (state == TelephonyManager.CALL_STATE_RINGING) {
            isIncomingCall = true
        }

        // Try to get contact name if we have a number
        val name = finalNumber?.let { getContactName(it) }
        val displayName = name ?: finalNumber

        val prefix = when {
            state == TelephonyManager.CALL_STATE_RINGING -> "Incoming: "
            state == TelephonyManager.CALL_STATE_OFFHOOK && !isIncomingCall -> "Calling: "
            else -> "Call: "
        }
        
        val info = if (displayName != null) "$prefix$displayName" else "Call"
        safeUpdateCallInfo(info)

        // Poll CallLog multiple times to catch the entry as soon as it's written
        pollingJob?.cancel()
        pollingJob = serviceScope.launch {
            repeat(10) { // Try for 10 seconds
                delay(1000)
                if (telephonyManager?.callState != TelephonyManager.CALL_STATE_IDLE) {
                    checkLastCallInfo()
                }
            }
        }
    }

    private fun getContactName(phoneNumber: String?): String? {
        if (phoneNumber.isNullOrBlank()) return null
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "READ_CONTACTS permission NOT granted")
            return null
        }
        
        // Clean the number (remove spaces, dashes etc) but keep + for international
        val cleanedNumber = phoneNumber.filter { it.isDigit() || it == '+' }
        
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(cleanedNumber))
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
        return try {
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val name = cursor.getString(0)
                    Log.d(TAG, "Contact found for $phoneNumber: $name")
                    name
                } else {
                    Log.d(TAG, "No contact found for $phoneNumber")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying contacts", e)
            null
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        tryRegisterObservers()
        updateNotifications()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        updateCallStatus(sbn)
        updateNotifications()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        updateNotifications()
        // If a call notification is removed, we might want to clear the active call info
        // but TelephonyManager usually handles the idle state better.
    }

    private fun updateCallStatus(sbn: StatusBarNotification?) {
        if (sbn == null) return
        val packageName = sbn.packageName.lowercase(Locale.getDefault())
        val category = sbn.notification.category
        val isOngoing = (sbn.notification.flags and Notification.FLAG_ONGOING_EVENT) != 0
        
        val isCallRelated = category == Notification.CATEGORY_CALL ||
                category == Notification.CATEGORY_MISSED_CALL ||
                packageName.contains("dialer") ||
                packageName.contains("telecom") ||
                packageName.contains("phone") ||
                packageName.contains("incallui") ||
                packageName.contains("mudita.dial") ||
                packageName == "com.mudita.dial"

        if (!isCallRelated) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        
        // Try to get number from hidden extra field
        val extraNumber = extras.getString("android.phone.number")
        if (!extraNumber.isNullOrEmpty()) {
            lastKnownNumber = extraNumber
        }
        
        Log.d(TAG, "Call notification: pkg=$packageName, title=$title, text=$text, extraNum=$extraNumber")
        
        val fullContent = "$title $text".lowercase(Locale.getDefault())
        
        // Try to resolve name from multiple sources
        var resolvedName: String? = extraNumber?.let { getContactName(it) }
        
        if (resolvedName == null && title.isNotEmpty() && 
            !title.lowercase().contains("dialer") && 
            !title.lowercase().contains("phone") &&
            !title.lowercase().contains("calling")) {
            // If title has numbers, try to extract them
            val numberInTitle = title.filter { it.isDigit() || it == '+' }
            resolvedName = if (numberInTitle.length > 3) getContactName(numberInTitle) ?: title else title
        }
        
        if (resolvedName == null && text.isNotEmpty() && !text.contains(":") && text.length > 2) {
            val numberInText = text.filter { it.isDigit() || it == '+' }
            resolvedName = if (numberInText.length > 3) getContactName(numberInText) ?: text else text
        }
        
        val finalName = resolvedName ?: ""
        val isGeneric = finalName.isEmpty()

        // For any ongoing or call-category notification, treat as active call
        if (isOngoing || category == Notification.CATEGORY_CALL || 
            fullContent.contains("ongoing") || fullContent.contains("active") || 
            fullContent.contains("calling") || fullContent.contains("dialing")) {
            
            val status = if (isGeneric) "Call" else "Call: $finalName"
            safeUpdateCallInfo(status)
        }
    }

    private fun updateNotifications() {
        val prefs = applicationContext.getSharedPreferences("SexyLauncherPrefs", Context.MODE_PRIVATE)
        val disableDuraSpeed = prefs.getBoolean("disable_duraspeed_notifications", false)

        val activeNotifs = activeNotifications ?: emptyArray()
        
        // Ensure active call info is set if there's an active call notification
        // but we haven't detected it via telephony state yet
        if (_activeCall.value == null) {
            activeNotifs.forEach { sbn ->
                val pkg = sbn.packageName.lowercase(Locale.getDefault())
                val category = sbn.notification.category
                if (category == Notification.CATEGORY_CALL || 
                    pkg.contains("dialer") || pkg.contains("telecom") || 
                    pkg.contains("phone") || pkg.contains("mudita.dial")) {
                    updateCallStatus(sbn)
                }
            }
        }

        _notifications.value = activeNotifs.filter {
            isNotificationRelevant(it, disableDuraSpeed)
        }.sortedByDescending { it.postTime }

        updateMissedCallsCount()
        updateUnreadSmsCount()
    }

    private fun updateUnreadSmsCount() {
        try {
            val contentResolver = applicationContext.contentResolver
            val cursor = contentResolver.query(
                Uri.parse("content://sms/inbox"),
                null,
                "read = 0",
                null,
                null
            )
            _unreadSmsCount.value = cursor?.count ?: 0
            cursor?.close()
        } catch (e: Exception) {
            _unreadSmsCount.value = 0
        }
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
                packageName.contains("incallui") ||
                packageName.contains("mudita.dial") ||
                packageName == "com.mudita.dial"

        if (isCallRelated) {
            return true
        }

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
            if (text.contains("notification")) {
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
                fullContent.contains("playing")

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
                fullContent.contains("podcast") ||
                fullContent.contains("maps") ||
                fullContent.contains("transfer") ||
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
        if (disableDuraSpeed && (packageName.contains("duraspeed") || 
            packageName.contains("duraspeen") ||
            fullContent.contains("duraspeed") ||
            fullContent.contains("duraspeen"))) {
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
        tryRegisterObservers()
        updateNotifications()
    }
}

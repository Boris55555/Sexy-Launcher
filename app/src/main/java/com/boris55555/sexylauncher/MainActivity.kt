package com.boris55555.sexylauncher

import android.Manifest
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.boris55555.sexylauncher.birthdays.BirthdaysRepository
import com.boris55555.sexylauncher.birthdays.BirthdaysScreen
import com.boris55555.sexylauncher.reminders.RemindersRepository
import com.boris55555.sexylauncher.reminders.RemindersScreen
import com.boris55555.sexylauncher.ui.theme.SexyLauncherTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.telephony.TelephonyManager

private const val PREFS_NAME = "SexyLauncherPrefs"
private const val KEY_FAVORITES = "favorite_apps"
private const val KEY_FAVORITE_COUNT = "favorite_app_count"
private const val KEY_ALARM_APP = "alarm_app_package"
private const val KEY_CALENDAR_APP = "calendar_app_package"
private const val KEY_HOME_LOCKED = "home_locked"
private const val KEY_CUSTOM_NAMES = "custom_app_names_set"
private const val KEY_WEEK_STARTS_ON_SUNDAY = "week_starts_on_sunday"
private const val KEY_HIDE_LAUNCHER_FROM_APP_VIEW = "hide_launcher_from_app_view"
private const val KEY_GESTURES_ENABLED = "gestures_enabled"
private const val KEY_KEEP_ALL_APPS_BUTTON = "keep_all_apps_button"
private const val KEY_SWIPE_LEFT_ACTION = "swipe_left_action"
private const val KEY_SWIPE_RIGHT_ACTION = "swipe_right_action"
private const val KEY_CAT_ICON_ACTION = "cat_icon_action"
private const val KEY_DISABLE_DURASPEED_NOTIFICATIONS = "disable_duraspeed_notifications"
private const val KEY_DATE_THEME_LIGHT = "date_theme_light"
private const val KEY_SHOW_APP_ICONS = "show_app_icons"
private const val KEY_BATTERY_THRESHOLD = "battery_threshold"
private const val KEY_SELECTED_FONT = "selected_font"
private const val KEY_FONT_SIZE_HOME = "font_size_home"
private const val KEY_FONT_SIZE_ALL_APPS = "font_size_all_apps"
private const val KEY_FONT_SIZE_NOTIFICATIONS = "font_size_notifications"
private const val KEY_HIDE_STATUS_BAR = "hide_status_bar"
private const val KEY_USE_24H_FORMAT = "use_24h_format"
private const val KEY_INITIAL_SETUP_COMPLETE = "initial_setup_complete"
private const val KEY_ASKED_NOTIFICATION_ACCESS = "asked_notification_access"
private const val KEY_ASKED_EXACT_ALARM = "asked_exact_alarm"
private const val KEY_ASKED_RUNTIME_PERMISSIONS = "asked_runtime_permissions"
private const val KEY_SHOW_NOTIFICATION_PREVIEWS = "show_notification_previews"
private const val KEY_NOTIFICATION_MAX_CHARACTERS = "notification_max_characters"
private const val KEY_PREFERRED_APP_LIST = "preferred_app_list"
private const val KEY_HIDDEN_FROM_TOP10 = "hidden_from_top10"
private const val DEFAULT_FAVORITE_COUNT = 4
private const val DEFAULT_BATTERY_THRESHOLD = 50
private const val DEFAULT_FONT = "Sans Serif"
private const val DEFAULT_NOTIFICATION_MAX_CHARACTERS = 30

class FavoritesRepository(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _favoriteCount = MutableStateFlow(prefs.getInt(KEY_FAVORITE_COUNT, DEFAULT_FAVORITE_COUNT))
    val favoriteCount = _favoriteCount.asStateFlow()

    private val _batteryThreshold = MutableStateFlow(prefs.getInt(KEY_BATTERY_THRESHOLD, DEFAULT_BATTERY_THRESHOLD))
    val batteryThreshold = _batteryThreshold.asStateFlow()

    private val _favorites = MutableStateFlow(getFavoritesList())
    val favorites = _favorites.asStateFlow()

    private val _alarmAppPackage = MutableStateFlow(prefs.getString(KEY_ALARM_APP, null))
    val alarmAppPackage = _alarmAppPackage.asStateFlow()

    private val _calendarAppPackage = MutableStateFlow(prefs.getString(KEY_CALENDAR_APP, null))
    val calendarAppPackage = _calendarAppPackage.asStateFlow()

    private val _isHomeLocked = MutableStateFlow(prefs.getBoolean(KEY_HOME_LOCKED, false))
    val isHomeLocked = _isHomeLocked.asStateFlow()

    private val _hideLauncherFromAppView = MutableStateFlow(prefs.getBoolean(KEY_HIDE_LAUNCHER_FROM_APP_VIEW, true))
    val hideLauncherFromAppView = _hideLauncherFromAppView.asStateFlow()

    private val _customNames = MutableStateFlow(getCustomNamesMap())
    val customNames = _customNames.asStateFlow()

    private val _weekStartsOnSunday = MutableStateFlow(prefs.getBoolean(KEY_WEEK_STARTS_ON_SUNDAY, false))
    val weekStartsOnSunday = _weekStartsOnSunday.asStateFlow()

    private val _gesturesEnabled = MutableStateFlow(prefs.getBoolean(KEY_GESTURES_ENABLED, false))
    val gesturesEnabled = _gesturesEnabled.asStateFlow()

    private val _keepAllAppsButton = MutableStateFlow(prefs.getBoolean(KEY_KEEP_ALL_APPS_BUTTON, false))
    val keepAllAppsButton = _keepAllAppsButton.asStateFlow()

    private val _swipeLeftAction = MutableStateFlow(prefs.getString(KEY_SWIPE_LEFT_ACTION, "none") ?: "none")
    val swipeLeftAction = _swipeLeftAction.asStateFlow()

    private val _swipeRightAction = MutableStateFlow(prefs.getString(KEY_SWIPE_RIGHT_ACTION, "none") ?: "none")
    val swipeRightAction = _swipeRightAction.asStateFlow()

    private val _catIconAction = MutableStateFlow(prefs.getString(KEY_CAT_ICON_ACTION, "double_touch") ?: "double_touch")
    val catIconAction = _catIconAction.asStateFlow()

    private val _disableDuraSpeedNotifications = MutableStateFlow(prefs.getBoolean(KEY_DISABLE_DURASPEED_NOTIFICATIONS, false))
    val disableDuraSpeedNotifications = _disableDuraSpeedNotifications.asStateFlow()

    private val _dateThemeLight = MutableStateFlow(prefs.getBoolean(KEY_DATE_THEME_LIGHT, false))
    val dateThemeLight = _dateThemeLight.asStateFlow()

    private val _use24hFormat = MutableStateFlow(prefs.getBoolean(KEY_USE_24H_FORMAT, true))
    val use24hFormat = _use24hFormat.asStateFlow()

    private val _showAppIcons = MutableStateFlow(prefs.getBoolean(KEY_SHOW_APP_ICONS, false))
    val showAppIcons = _showAppIcons.asStateFlow()

    private val _showNotificationPreviews = MutableStateFlow(prefs.getBoolean(KEY_SHOW_NOTIFICATION_PREVIEWS, true))
    val showNotificationPreviews = _showNotificationPreviews.asStateFlow()

    private val _notificationMaxCharacters = MutableStateFlow(prefs.getInt(KEY_NOTIFICATION_MAX_CHARACTERS, DEFAULT_NOTIFICATION_MAX_CHARACTERS))
    val notificationMaxCharacters = _notificationMaxCharacters.asStateFlow()

    private val _selectedFont = MutableStateFlow(prefs.getString(KEY_SELECTED_FONT, DEFAULT_FONT) ?: DEFAULT_FONT)
    val selectedFont = _selectedFont.asStateFlow()

    private val _fontSizeHome = MutableStateFlow(prefs.getString(KEY_FONT_SIZE_HOME, "Normal") ?: "Normal")
    val fontSizeHome = _fontSizeHome.asStateFlow()

    private val _fontSizeAllApps = MutableStateFlow(prefs.getString(KEY_FONT_SIZE_ALL_APPS, "Normal") ?: "Normal")
    val fontSizeAllApps = _fontSizeAllApps.asStateFlow()

    private val _fontSizeNotifications = MutableStateFlow(prefs.getString(KEY_FONT_SIZE_NOTIFICATIONS, "Normal") ?: "Normal")
    val fontSizeNotifications = _fontSizeNotifications.asStateFlow()

    private val _hideStatusBar = MutableStateFlow(prefs.getBoolean(KEY_HIDE_STATUS_BAR, true))
    val hideStatusBar = _hideStatusBar.asStateFlow()

    private val _preferredAppList = MutableStateFlow(prefs.getString(KEY_PREFERRED_APP_LIST, "All Apps") ?: "All Apps")
    val preferredAppList = _preferredAppList.asStateFlow()

    private val _hiddenFromTop10 = MutableStateFlow(prefs.getStringSet(KEY_HIDDEN_FROM_TOP10, emptySet()) ?: emptySet())
    val hiddenFromTop10 = _hiddenFromTop10.asStateFlow()

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            KEY_FAVORITE_COUNT, KEY_FAVORITES -> {
                _favoriteCount.value = prefs.getInt(KEY_FAVORITE_COUNT, DEFAULT_FAVORITE_COUNT)
                _favorites.value = getFavoritesList()
            }
            KEY_ALARM_APP -> {
                _alarmAppPackage.value = prefs.getString(KEY_ALARM_APP, null)
            }
            KEY_CALENDAR_APP -> {
                _calendarAppPackage.value = prefs.getString(KEY_CALENDAR_APP, null)
            }
            KEY_HOME_LOCKED -> {
                _isHomeLocked.value = prefs.getBoolean(KEY_HOME_LOCKED, false)
            }
            KEY_HIDE_LAUNCHER_FROM_APP_VIEW -> {
                _hideLauncherFromAppView.value = prefs.getBoolean(KEY_HIDE_LAUNCHER_FROM_APP_VIEW, true)
            }
            KEY_CUSTOM_NAMES -> {
                _customNames.value = getCustomNamesMap()
            }
            KEY_WEEK_STARTS_ON_SUNDAY -> {
                _weekStartsOnSunday.value = prefs.getBoolean(KEY_WEEK_STARTS_ON_SUNDAY, false)
            }
            KEY_GESTURES_ENABLED -> {
                _gesturesEnabled.value = prefs.getBoolean(KEY_GESTURES_ENABLED, false)
            }
            KEY_KEEP_ALL_APPS_BUTTON -> {
                _keepAllAppsButton.value = prefs.getBoolean(KEY_KEEP_ALL_APPS_BUTTON, false)
            }
            KEY_SWIPE_LEFT_ACTION -> {
                val action = prefs.getString(KEY_SWIPE_LEFT_ACTION, "none") ?: "none"
                if (action == "notifications") {
                    saveSwipeLeftAction("none")
                } else {
                    _swipeLeftAction.value = action
                }
            }
            KEY_SWIPE_RIGHT_ACTION -> {
                val action = prefs.getString(KEY_SWIPE_RIGHT_ACTION, "none") ?: "none"
                if (action == "notifications") {
                    saveSwipeRightAction("none")
                } else {
                    _swipeRightAction.value = action
                }
            }
            KEY_CAT_ICON_ACTION -> {
                _catIconAction.value = prefs.getString(KEY_CAT_ICON_ACTION, "double_touch") ?: "double_touch"
            }
            KEY_DISABLE_DURASPEED_NOTIFICATIONS -> {
                _disableDuraSpeedNotifications.value = prefs.getBoolean(KEY_DISABLE_DURASPEED_NOTIFICATIONS, false)
            }
            KEY_DATE_THEME_LIGHT -> {
                _dateThemeLight.value = prefs.getBoolean(KEY_DATE_THEME_LIGHT, false)
            }
            KEY_USE_24H_FORMAT -> {
                _use24hFormat.value = prefs.getBoolean(KEY_USE_24H_FORMAT, true)
            }
            KEY_SHOW_APP_ICONS -> {
                _showAppIcons.value = prefs.getBoolean(KEY_SHOW_APP_ICONS, false)
            }
            KEY_SHOW_NOTIFICATION_PREVIEWS -> {
                _showNotificationPreviews.value = prefs.getBoolean(KEY_SHOW_NOTIFICATION_PREVIEWS, true)
            }
            KEY_NOTIFICATION_MAX_CHARACTERS -> {
                _notificationMaxCharacters.value = prefs.getInt(KEY_NOTIFICATION_MAX_CHARACTERS, DEFAULT_NOTIFICATION_MAX_CHARACTERS)
            }
            KEY_BATTERY_THRESHOLD -> {
                _batteryThreshold.value = prefs.getInt(KEY_BATTERY_THRESHOLD, DEFAULT_BATTERY_THRESHOLD)
            }
            KEY_SELECTED_FONT -> {
                _selectedFont.value = prefs.getString(KEY_SELECTED_FONT, DEFAULT_FONT) ?: DEFAULT_FONT
            }
            KEY_FONT_SIZE_HOME -> {
                _fontSizeHome.value = prefs.getString(KEY_FONT_SIZE_HOME, "Normal") ?: "Normal"
            }
            KEY_FONT_SIZE_ALL_APPS -> {
                _fontSizeAllApps.value = prefs.getString(KEY_FONT_SIZE_ALL_APPS, "Normal") ?: "Normal"
            }
            KEY_FONT_SIZE_NOTIFICATIONS -> {
                _fontSizeNotifications.value = prefs.getString(KEY_FONT_SIZE_NOTIFICATIONS, "Normal") ?: "Normal"
            }
            KEY_HIDE_STATUS_BAR -> {
                _hideStatusBar.value = prefs.getBoolean(KEY_HIDE_STATUS_BAR, false)
            }
            KEY_PREFERRED_APP_LIST -> {
                _preferredAppList.value = prefs.getString(KEY_PREFERRED_APP_LIST, "All Apps") ?: "All Apps"
            }
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(listener)
        
        // Initial check to clean up old values
        val leftAction = prefs.getString(KEY_SWIPE_LEFT_ACTION, "none") ?: "none"
        if (leftAction == "notifications") {
            saveSwipeLeftAction("none")
        }
        val rightAction = prefs.getString(KEY_SWIPE_RIGHT_ACTION, "none") ?: "none"
        if (rightAction == "notifications") {
            saveSwipeRightAction("none")
        }

        // Mudita Kompakt defaults: auto-set Alarm and Calendar if they exist and no user choice has been made
        if (prefs.getString(KEY_ALARM_APP, null) == null) {
            val muditaAlarm = "com.mudita.alarm"
            if (isAppInstalled(muditaAlarm)) {
                saveAlarmApp(muditaAlarm)
            }
        }
        if (prefs.getString(KEY_CALENDAR_APP, null) == null) {
            val muditaCalendar = "com.mudita.calendar"
            if (isAppInstalled(muditaCalendar)) {
                saveCalendarApp(muditaCalendar)
            }
        }
    }

    private fun isAppInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun cleanup() {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    private fun getFavoritesList(): List<String?> {
        val count = prefs.getInt(KEY_FAVORITE_COUNT, DEFAULT_FAVORITE_COUNT)
        val stored = prefs.getString(KEY_FAVORITES, null) ?: return List(count) { null }
        val list = stored.split(",").map { if (it == "null") null else it }.toMutableList()
        while (list.size < count) list.add(null)
        while (list.size > count) list.removeAt(list.size - 1)
        return list
    }

    private fun getCustomNamesMap(): Map<String, String> {
        val storedSet = prefs.getStringSet(KEY_CUSTOM_NAMES, emptySet()) ?: emptySet()
        return storedSet.mapNotNull {
            val parts = it.split(":", limit = 2)
            if (parts.size == 2) parts[0] to parts[1] else null
        }.toMap()
    }

    fun saveCustomName(packageName: String, customName: String?) {
        val currentNames = getCustomNamesMap().toMutableMap()
        if (customName.isNullOrBlank()) {
            currentNames.remove(packageName)
        } else {
            currentNames[packageName] = customName
        }

        val newSet = currentNames.map { "${it.key}:${it.value}" }.toSet()
        prefs.edit().putStringSet(KEY_CUSTOM_NAMES, newSet).apply()
    }

    fun saveFavoriteCount(count: Int) {
        val currentFavorites = _favorites.value.toMutableList()
        val currentSize = currentFavorites.size
        if (count < currentSize) {
            while (currentFavorites.size > count) {
                currentFavorites.removeAt(currentFavorites.size - 1)
            }
        } else if (count > currentSize) {
            repeat(count - currentSize) {
                currentFavorites.add(null)
            }
        }
        val toStore = currentFavorites.joinToString(separator = ",") { it ?: "null" }
        prefs.edit()
            .putInt(KEY_FAVORITE_COUNT, count)
            .putString(KEY_FAVORITES, toStore)
            .apply()
    }

    fun saveFavorite(index: Int, packageName: String?) {
        val currentFavorites = _favorites.value.toMutableList()
        if (index in 0 until _favoriteCount.value) {
            currentFavorites[index] = packageName
            _favorites.value = currentFavorites
            val toStore = currentFavorites.joinToString(separator = ",") { it ?: "null" }
            prefs.edit().putString(KEY_FAVORITES, toStore).apply()
        }
    }

    fun saveAlarmApp(packageName: String?) {
        prefs.edit().putString(KEY_ALARM_APP, packageName).apply()
    }

    fun saveCalendarApp(packageName: String?) {
        prefs.edit().putString(KEY_CALENDAR_APP, packageName).apply()
    }

    fun saveHomeLocked(isLocked: Boolean) {
        prefs.edit().putBoolean(KEY_HOME_LOCKED, isLocked).apply()
    }

    fun saveHideLauncherFromAppView(hide: Boolean) {
        prefs.edit().putBoolean(KEY_HIDE_LAUNCHER_FROM_APP_VIEW, hide).apply()
    }

    fun saveWeekStartsOnSunday(isSunday: Boolean) {
        prefs.edit().putBoolean(KEY_WEEK_STARTS_ON_SUNDAY, isSunday).apply()
    }

    fun saveGesturesEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GESTURES_ENABLED, enabled).apply()
    }

    fun saveKeepAllAppsButton(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_KEEP_ALL_APPS_BUTTON, enabled).apply()
    }

    fun saveSwipeLeftAction(action: String) {
        prefs.edit().putString(KEY_SWIPE_LEFT_ACTION, action).apply()
    }

    fun saveSwipeRightAction(action: String) {
        prefs.edit().putString(KEY_SWIPE_RIGHT_ACTION, action).apply()
    }

    fun saveCatIconAction(action: String) {
        prefs.edit().putString(KEY_CAT_ICON_ACTION, action).apply()
    }

    fun saveDisableDuraSpeedNotifications(disabled: Boolean) {
        prefs.edit().putBoolean(KEY_DISABLE_DURASPEED_NOTIFICATIONS, disabled).apply()
    }

    fun saveDateThemeLight(isLight: Boolean) {
        prefs.edit().putBoolean(KEY_DATE_THEME_LIGHT, isLight).apply()
    }

    fun saveUse24hFormat(use24h: Boolean) {
        prefs.edit().putBoolean(KEY_USE_24H_FORMAT, use24h).apply()
    }

    fun saveShowAppIcons(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_APP_ICONS, show).apply()
    }

    fun saveShowNotificationPreviews(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_NOTIFICATION_PREVIEWS, show).apply()
    }

    fun saveNotificationMaxCharacters(max: Int) {
        prefs.edit().putInt(KEY_NOTIFICATION_MAX_CHARACTERS, max).apply()
    }

    fun saveBatteryThreshold(threshold: Int) {
        prefs.edit().putInt(KEY_BATTERY_THRESHOLD, threshold).apply()
    }

    fun saveSelectedFont(font: String) {
        prefs.edit().putString(KEY_SELECTED_FONT, font).apply()
    }

    fun saveFontSizeHome(size: String) {
        prefs.edit().putString(KEY_FONT_SIZE_HOME, size).apply()
    }

    fun saveFontSizeAllApps(size: String) {
        prefs.edit().putString(KEY_FONT_SIZE_ALL_APPS, size).apply()
    }

    fun saveFontSizeNotifications(size: String) {
        prefs.edit().putString(KEY_FONT_SIZE_NOTIFICATIONS, size).apply()
    }

    fun saveHideStatusBar(hide: Boolean) {
        prefs.edit().putBoolean(KEY_HIDE_STATUS_BAR, hide).apply()
    }

    fun savePreferredAppList(list: String) {
        prefs.edit().putString(KEY_PREFERRED_APP_LIST, list).apply()
    }

    fun toggleHiddenFromTop10(packageName: String) {
        val current = _hiddenFromTop10.value.toMutableSet()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        prefs.edit().putStringSet(KEY_HIDDEN_FROM_TOP10, current).apply()
        _hiddenFromTop10.value = current
    }
}

enum class Screen {
    Home,
    AppDrawer,
    Notifications,
    Settings,
    Birthdays,
    Reminders
}

class MainActivity : ComponentActivity() {
    private lateinit var favoritesRepository: FavoritesRepository
    private lateinit var usageRepository: UsageRepository
    private lateinit var birthdaysRepository: BirthdaysRepository
    private lateinit var remindersRepository: RemindersRepository

    private val _bluetoothState = MutableStateFlow<BluetoothState>(BluetoothState.Disabled)
    val bluetoothState: StateFlow<BluetoothState> = _bluetoothState.asStateFlow()

    sealed class BluetoothState {
        object Disabled : BluetoothState()
        object Enabled : BluetoothState()
        data class Connected(val deviceName: String) : BluetoothState()
    }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            }

            Log.d("MainActivity", "Bluetooth Receiver: action=$action, device=${device?.name}")

            when (action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        val name = device?.name ?: "Connected Device"
                        _bluetoothState.value = BluetoothState.Connected(name)
                    } else {
                        updateBluetoothStatus()
                    }
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    updateBluetoothStatus()
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    updateBluetoothStatus()
                }
                BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED,
                BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_DISCONNECTED)
                    if (state == BluetoothProfile.STATE_CONNECTED) {
                        if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                            val name = device?.name ?: "Connected Device"
                            _bluetoothState.value = BluetoothState.Connected(name)
                        } else {
                            _bluetoothState.value = BluetoothState.Connected("Connected Device")
                        }
                    } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                        updateBluetoothStatus()
                    }
                }
                else -> updateBluetoothStatus()
            }
        }
    }

    private val _currentScreen = MutableStateFlow(Screen.Home)
    private var lastBackPressTime = 0L
    private val _currentPage = MutableStateFlow(0)
    private var previousScreen: Screen? = null

    private val screenOffReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            if (intent?.action == android.content.Intent.ACTION_SCREEN_OFF) {
                _currentScreen.value = Screen.Home
                _currentPage.value = 0
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateBluetoothStatus()
        runPermissionSequence()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothReceiver)
        unregisterReceiver(screenOffReceiver)
        favoritesRepository.cleanup()
        birthdaysRepository.cleanup()
        remindersRepository.cleanup()
    }

    private fun hasBluetoothConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun updateBluetoothStatus() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter
        
        if (adapter == null || !adapter.isEnabled) {
            _bluetoothState.value = BluetoothState.Disabled
            return
        }

        if (!hasBluetoothConnectPermission()) {
            _bluetoothState.value = BluetoothState.Enabled
            return
        }

        // 1. Check GATT (Low Energy devices like watches, sensors)
        try {
            val connectedGattDevices = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT)
            if (connectedGattDevices.isNotEmpty()) {
                val device = connectedGattDevices.first()
                _bluetoothState.value = BluetoothState.Connected(device.name ?: "Connected Device")
                return
            }
        } catch (e: SecurityException) {
            Log.e("MainActivity", "SecurityException checking GATT devices", e)
        }

        // 2. Check A2DP and Headset (Headphones, speakers)
        val isA2dpConnected = adapter.getProfileConnectionState(BluetoothProfile.A2DP) == BluetoothAdapter.STATE_CONNECTED
        val isHeadsetConnected = adapter.getProfileConnectionState(BluetoothProfile.HEADSET) == BluetoothAdapter.STATE_CONNECTED

        if (isA2dpConnected || isHeadsetConnected) {
            // We know something is connected, set a placeholder and try to get the real name
            if (_bluetoothState.value !is BluetoothState.Connected) {
                _bluetoothState.value = BluetoothState.Connected("Connected Device")
            }

            val profileToQuery = if (isA2dpConnected) BluetoothProfile.A2DP else BluetoothProfile.HEADSET
            try {
                adapter.getProfileProxy(this, object : BluetoothProfile.ServiceListener {
                    override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                        if (hasBluetoothConnectPermission()) {
                            val devices = proxy.connectedDevices
                            if (devices.isNotEmpty()) {
                                val name = devices.first().name ?: "Connected Device"
                                _bluetoothState.value = BluetoothState.Connected(name)
                            }
                        }
                        adapter.closeProfileProxy(profile, proxy)
                    }
                    override fun onServiceDisconnected(profile: Int) {}
                }, profileToQuery)
            } catch (e: Exception) {
                Log.e("MainActivity", "Error getting profile proxy", e)
            }
        } else {
            _bluetoothState.value = BluetoothState.Enabled
        }
    }

    companion object {
        private val _activeCallInfo = MutableStateFlow<String?>(null)
        val activeCallInfo = _activeCallInfo.asStateFlow()

        fun updateActiveCallInfo(info: String?) {
            _activeCallInfo.value = info
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        updateBluetoothStatus()
        NotificationListener.instance?.requestRefresh()
        runPermissionSequence()
    }

    private fun navigateTo(screen: Screen) {
        previousScreen = _currentScreen.value
        _currentScreen.value = screen
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        favoritesRepository = FavoritesRepository(this)
        usageRepository = UsageRepository(this)
        birthdaysRepository = BirthdaysRepository(this)
        remindersRepository = RemindersRepository(this)

        updateBluetoothStatus()
        val bluetoothFilter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
        }
        registerReceiver(bluetoothReceiver, bluetoothFilter)

        val filter = android.content.IntentFilter(android.content.Intent.ACTION_SCREEN_OFF)
        registerReceiver(screenOffReceiver, filter)

        runPermissionSequence()

        setContent {
            val hideStatusBar by favoritesRepository.hideStatusBar.collectAsState()

            LaunchedEffect(hideStatusBar) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val controller = window.insetsController
                    if (controller != null) {
                        if (hideStatusBar) {
                            controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                            controller.systemBarsBehavior =
                                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        } else {
                            controller.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                        }
                    }
                }
            }

            val currentScreen by _currentScreen.collectAsState()
            var showPickerForIndex by remember { mutableStateOf<Int?>(null) }
            var showAlarmAppPicker by remember { mutableStateOf(false) }
            var showCalendarAppPicker by remember { mutableStateOf(false) }
            var showSwipeLeftAppPicker by remember { mutableStateOf(false) }
            var showSwipeRightAppPicker by remember { mutableStateOf(false) }
            val currentPage by _currentPage.collectAsState()
            val bluetoothState by _bluetoothState.collectAsState()
            var lockedLetter by remember { mutableStateOf<Char?>(null) }

            // Centralized Back Handler
            BackHandler(enabled = true) {
                val currentTime = System.currentTimeMillis()
                when {
                    showPickerForIndex != null -> {
                        showPickerForIndex = null
                    }
                    showAlarmAppPicker -> {
                        showAlarmAppPicker = false
                    }
                    showCalendarAppPicker -> {
                        showCalendarAppPicker = false
                    }
                    showSwipeLeftAppPicker -> {
                        showSwipeLeftAppPicker = false
                    }
                    showSwipeRightAppPicker -> {
                        showSwipeRightAppPicker = false
                    }
                    currentScreen == Screen.AppDrawer && lockedLetter != null -> {
                        lockedLetter = null
                    }
                    currentScreen == Screen.Birthdays || currentScreen == Screen.Reminders -> {
                        _currentScreen.value = previousScreen ?: Screen.Home
                    }
                    currentScreen != Screen.Home -> {
                        _currentScreen.value = Screen.Home
                    }
                    currentTime - lastBackPressTime < 500 -> {
                        try {
                            val intent = Intent("com.android.settings.action.CLEAR_GHOST")
                            startActivity(intent)
                        } catch (e: Exception) {
                            // Ignore
                        }
                    }
                    else -> lastBackPressTime = currentTime
                }
            }

            SexyLauncherTheme(selectedFontName = favoritesRepository.selectedFont.collectAsState().value) {
                if (showPickerForIndex != null) {
                    AppListScreen(
                        isPickerMode = true,
                        onAppSelected = {
                            favoritesRepository.saveFavorite(showPickerForIndex!!, it?.packageName)
                            showPickerForIndex = null
                        },
                        onDismiss = { showPickerForIndex = null },
                        favoritesRepository = favoritesRepository,
                        onLockedLetterChanged = { lockedLetter = it },
                        lockedLetter = lockedLetter
                    )
                } else if (showAlarmAppPicker) {
                    AppListScreen(
                        isPickerMode = true,
                        onAppSelected = {
                            favoritesRepository.saveAlarmApp(it?.packageName)
                            showAlarmAppPicker = false
                        },
                        onDismiss = { showAlarmAppPicker = false },
                        favoritesRepository = favoritesRepository,
                        onLockedLetterChanged = { lockedLetter = it },
                        lockedLetter = lockedLetter
                    )
                } else if (showCalendarAppPicker) {
                    AppListScreen(
                        isPickerMode = true,
                        onAppSelected = {
                            favoritesRepository.saveCalendarApp(it?.packageName)
                            showCalendarAppPicker = false
                        },
                        onDismiss = { showCalendarAppPicker = false },
                        favoritesRepository = favoritesRepository,
                        onLockedLetterChanged = { lockedLetter = it },
                        lockedLetter = lockedLetter
                    )
                } else if (showSwipeLeftAppPicker) {
                    AppListScreen(
                        isPickerMode = true,
                        onAppSelected = {
                            favoritesRepository.saveSwipeLeftAction(it?.packageName?.let { pkg -> "app:$pkg" } ?: "none")
                            showSwipeLeftAppPicker = false
                        },
                        onDismiss = { showSwipeLeftAppPicker = false },
                        favoritesRepository = favoritesRepository,
                        onLockedLetterChanged = { lockedLetter = it },
                        lockedLetter = lockedLetter
                    )
                } else if (showSwipeRightAppPicker) {
                    AppListScreen(
                        isPickerMode = true,
                        onAppSelected = {
                            favoritesRepository.saveSwipeRightAction(it?.packageName?.let { pkg -> "app:$pkg" } ?: "none")
                            showSwipeRightAppPicker = false
                        },
                        onDismiss = { showSwipeRightAppPicker = false },
                        favoritesRepository = favoritesRepository,
                        onLockedLetterChanged = { lockedLetter = it },
                        lockedLetter = lockedLetter
                    )
                } else {
                    when (currentScreen) {
                        Screen.Home -> HomeScreen(
                            favoritesRepository = favoritesRepository,
                            birthdaysRepository = birthdaysRepository,
                            onShowAllAppsClicked = { navigateTo(Screen.AppDrawer) },
                            onShowNotificationsClicked = { navigateTo(Screen.Notifications) },
                            onShowBirthdaysClicked = { navigateTo(Screen.Birthdays) },
                            onShowRemindersClicked = { navigateTo(Screen.Reminders) },
                            onLaunchAppClicked = { packageName ->
                                usageRepository.incrementUsage(packageName)
                                val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                                if (launchIntent != null) {
                                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    startActivity(launchIntent)
                                }
                            },
                            onShowSettingsClicked = { navigateTo(Screen.Settings) },
                            onEditFavorite = { index -> showPickerForIndex = index },
                            currentPage = currentPage,
                            onCurrentPageChanged = { _currentPage.value = it },
                            bluetoothState = bluetoothState
                        )
                        Screen.AppDrawer -> AppListScreen(
                            onDismiss = { _currentScreen.value = Screen.Home },
                            onShowSettingsClicked = { navigateTo(Screen.Settings) },
                            favoritesRepository = favoritesRepository,
                            usageRepository = usageRepository,
                            onLockedLetterChanged = { lockedLetter = it },
                            lockedLetter = lockedLetter,
                            onAppLaunched = { pkg -> usageRepository.incrementUsage(pkg) }
                        )
                        Screen.Notifications -> NotificationsScreen(
                            remindersRepository = remindersRepository,
                            favoritesRepository = favoritesRepository,
                            onDismiss = { _currentScreen.value = Screen.Home },
                            bluetoothState = bluetoothState
                        )
                        Screen.Settings -> SettingsScreen(
                            favoritesRepository = favoritesRepository,
                            onChooseAlarmAppClicked = { showAlarmAppPicker = true },
                            onChooseCalendarAppClicked = { showCalendarAppPicker = true },
                            onChooseSwipeLeftAppClicked = { showSwipeLeftAppPicker = true },
                            onChooseSwipeRightAppClicked = { showSwipeRightAppPicker = true },
                            onBirthdaysClicked = { navigateTo(Screen.Birthdays) },
                            onRemindersClicked = { navigateTo(Screen.Reminders) }
                        )
                        Screen.Birthdays -> BirthdaysScreen(repository = birthdaysRepository)
                        Screen.Reminders -> RemindersScreen(repository = remindersRepository)
                    }
                }
            }
        }
    }

    private fun runPermissionSequence() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_INITIAL_SETUP_COMPLETE, false)) return

        val runtimePermissions = mutableListOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.READ_SMS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runtimePermissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missingRuntime = runtimePermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingRuntime.isNotEmpty() && !prefs.getBoolean(KEY_ASKED_RUNTIME_PERMISSIONS, false)) {
            prefs.edit().putBoolean(KEY_ASKED_RUNTIME_PERMISSIONS, true).apply()
            requestPermissionLauncher.launch(missingRuntime.toTypedArray())
            return
        }

        // Special: Notification Access
        if (!isNotificationServiceEnabled(this) && !prefs.getBoolean(KEY_ASKED_NOTIFICATION_ACCESS, false)) {
            prefs.edit().putBoolean(KEY_ASKED_NOTIFICATION_ACCESS, true).apply()
            try {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            } catch (e: Exception) {
                Log.e("MainActivity", "Error opening notification listener settings", e)
            }
            return
        }

        // Special: Exact Alarms
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            if (!alarmManager.canScheduleExactAlarms() && !prefs.getBoolean(KEY_ASKED_EXACT_ALARM, false)) {
                prefs.edit().putBoolean(KEY_ASKED_EXACT_ALARM, true).apply()
                try {
                    startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error opening exact alarm settings", e)
                }
                return
            }
        }
        
        // All permissions asked (not necessarily granted, but asked aluksi)
        prefs.edit().putBoolean(KEY_INITIAL_SETUP_COMPLETE, true).apply()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_HOME) {
            event.startTracking()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            _currentScreen.value = Screen.Home
            _currentPage.value = 0
        }
    }
}

data class AppInfo(
    val name: String,
    val packageName: String,
    val customName: String? = null,
    val isSystemApp: Boolean
)

fun isNotificationServiceEnabled(context: Context): Boolean {
    val enabledListeners = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return enabledListeners?.contains(context.packageName) == true
}

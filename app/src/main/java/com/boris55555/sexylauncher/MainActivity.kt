package com.boris55555.sexylauncher

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
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
import kotlinx.coroutines.flow.asStateFlow

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
private const val KEY_SWIPE_LEFT_ACTION = "swipe_left_action"
private const val KEY_SWIPE_RIGHT_ACTION = "swipe_right_action"
private const val KEY_CAT_ICON_ACTION = "cat_icon_action"
private const val KEY_DISABLE_DURASPEED_NOTIFICATIONS = "disable_duraspeed_notifications"
private const val KEY_DATE_THEME_LIGHT = "date_theme_light"
private const val KEY_SHOW_APP_ICONS = "show_app_icons"
private const val DEFAULT_FAVORITE_COUNT = 4

class FavoritesRepository(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _favoriteCount = MutableStateFlow(prefs.getInt(KEY_FAVORITE_COUNT, DEFAULT_FAVORITE_COUNT))
    val favoriteCount = _favoriteCount.asStateFlow()

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

    private val _showAppIcons = MutableStateFlow(prefs.getBoolean(KEY_SHOW_APP_ICONS, false))
    val showAppIcons = _showAppIcons.asStateFlow()

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
            KEY_SHOW_APP_ICONS -> {
                _showAppIcons.value = prefs.getBoolean(KEY_SHOW_APP_ICONS, false)
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

    fun saveShowAppIcons(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_APP_ICONS, show).apply()
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
    private lateinit var birthdaysRepository: BirthdaysRepository
    private lateinit var remindersRepository: RemindersRepository
    private val _currentScreen = MutableStateFlow(Screen.Home)
    private var lastBackPressTime = 0L
    private val _currentPage = MutableStateFlow(0)
    private var previousScreen: Screen? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _: Boolean ->
        // Handle permission result if needed
    }

    private fun navigateTo(screen: Screen) {
        previousScreen = _currentScreen.value
        _currentScreen.value = screen
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        favoritesRepository = FavoritesRepository(this)
        birthdaysRepository = BirthdaysRepository(this)
        remindersRepository = RemindersRepository(this)

        requestPermissions()

        setContent {
            val currentScreen by _currentScreen.collectAsState()
            var showPickerForIndex by remember { mutableStateOf<Int?>(null) }
            var showAlarmAppPicker by remember { mutableStateOf(false) }
            var showCalendarAppPicker by remember { mutableStateOf(false) }
            var showSwipeLeftAppPicker by remember { mutableStateOf(false) }
            var showSwipeRightAppPicker by remember { mutableStateOf(false) }
            val currentPage by _currentPage.collectAsState()
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

            SexyLauncherTheme {
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
                                val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                                if (launchIntent != null) {
                                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    startActivity(launchIntent)
                                }
                            },
                            onShowSettingsClicked = { navigateTo(Screen.Settings) },
                            onEditFavorite = { index -> showPickerForIndex = index },
                            currentPage = currentPage,
                            onCurrentPageChanged = { _currentPage.value = it }
                        )
                        Screen.AppDrawer -> AppListScreen(
                            onDismiss = { _currentScreen.value = Screen.Home },
                            onShowSettingsClicked = { navigateTo(Screen.Settings) },
                            favoritesRepository = favoritesRepository,
                            onLockedLetterChanged = { lockedLetter = it },
                            lockedLetter = lockedLetter
                        )
                        Screen.Notifications -> NotificationsScreen(
                            remindersRepository = remindersRepository,
                            onDismiss = { _currentScreen.value = Screen.Home }
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

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
            }
        }
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

    override fun onDestroy() {
        super.onDestroy()
        favoritesRepository.cleanup()
        birthdaysRepository.cleanup()
        remindersRepository.cleanup()
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

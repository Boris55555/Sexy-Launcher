package com.example.sexylauncher

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.example.sexylauncher.ui.theme.SexyLauncherTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val PREFS_NAME = "SexyLauncherPrefs"
private const val KEY_FAVORITES = "favorite_apps"
private const val KEY_FAVORITE_COUNT = "favorite_app_count"
private const val KEY_ALARM_APP = "alarm_app_package"
private const val KEY_CALENDAR_APP = "calendar_app_package"
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
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(listener)
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
}

enum class Screen {
    Home,
    AppDrawer,
    Notifications,
    Settings
}

class MainActivity : ComponentActivity() {
    private lateinit var favoritesRepository: FavoritesRepository
    private val _currentScreen = MutableStateFlow(Screen.Home)
    private var lastBackPressTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        favoritesRepository = FavoritesRepository(this)

        setContent {
            val currentScreen by _currentScreen.collectAsState()
            var showPickerForIndex by remember { mutableStateOf<Int?>(null) }
            var showAlarmAppPicker by remember { mutableStateOf(false) }
            var showCalendarAppPicker by remember { mutableStateOf(false) }

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
                        onDismiss = { showPickerForIndex = null }
                    )
                } else if (showAlarmAppPicker) {
                    AppListScreen(
                        isPickerMode = true,
                        onAppSelected = {
                            favoritesRepository.saveAlarmApp(it?.packageName)
                            showAlarmAppPicker = false
                        },
                        onDismiss = { showAlarmAppPicker = false }
                    )
                } else if (showCalendarAppPicker) {
                    AppListScreen(
                        isPickerMode = true,
                        onAppSelected = {
                            favoritesRepository.saveCalendarApp(it?.packageName)
                            showCalendarAppPicker = false
                        },
                        onDismiss = { showCalendarAppPicker = false }
                    )
                } else {
                    when (currentScreen) {
                        Screen.Home -> HomeScreen(
                            favoritesRepository = favoritesRepository,
                            onShowAllAppsClicked = { _currentScreen.value = Screen.AppDrawer },
                            onShowNotificationsClicked = { _currentScreen.value = Screen.Notifications },
                            onShowSettingsClicked = { _currentScreen.value = Screen.Settings },
                            onEditFavorite = { index -> showPickerForIndex = index }
                        )
                        Screen.AppDrawer -> AppListScreen(
                            onDismiss = { _currentScreen.value = Screen.Home },
                            onShowSettingsClicked = { _currentScreen.value = Screen.Settings }
                        )
                        Screen.Notifications -> NotificationsScreen(onDismiss = { _currentScreen.value = Screen.Home })
                        Screen.Settings -> SettingsScreen(
                            onDismiss = { _currentScreen.value = Screen.Home },
                            favoritesRepository = favoritesRepository,
                            onChooseAlarmAppClicked = { showAlarmAppPicker = true },
                            onChooseCalendarAppClicked = { showCalendarAppPicker = true }
                        )
                    }
                }
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
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        favoritesRepository.cleanup()
    }
}

data class AppInfo(
    val name: String,
    val packageName: String,
)

fun isNotificationServiceEnabled(context: Context): Boolean {
    val enabledListeners = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return enabledListeners?.contains(context.packageName) == true
}

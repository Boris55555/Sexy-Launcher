package com.boris55555.sexylauncher.data

import android.content.Context
import android.content.SharedPreferences

class BrightnessPrefs(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var brightnessLevel: Int
        get() = prefs.getInt(BRIGHTNESS_LEVEL, 128) // Default to mid brightness
        set(value) = prefs.edit().putInt(BRIGHTNESS_LEVEL, value.coerceIn(0, 255)).apply()

    /**
     * Stores the last non-zero brightness level before turning off.
     * This is used to restore brightness when turning it back on.
     */
    var lastBrightnessLevel: Int
        get() = prefs.getInt(LAST_BRIGHTNESS_LEVEL, 128) // Default to mid brightness
        set(value) = prefs.edit().putInt(LAST_BRIGHTNESS_LEVEL, value.coerceIn(1, 255)).apply()

    companion object {
        private const val PREFS_NAME = "brightness_prefs"
        private const val BRIGHTNESS_LEVEL = "BRIGHTNESS_LEVEL"
        private const val LAST_BRIGHTNESS_LEVEL = "LAST_BRIGHTNESS_LEVEL"
    }
}

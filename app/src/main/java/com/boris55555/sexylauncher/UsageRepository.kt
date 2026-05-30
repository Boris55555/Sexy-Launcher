package com.boris55555.sexylauncher

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val PREFS_NAME = "SexyLauncherUsagePrefs"
private const val KEY_USAGE_DATA = "app_usage_data"

class UsageRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    private val _usageMap = MutableStateFlow(getStoredUsage())
    val usageMap = _usageMap.asStateFlow()

    private fun getStoredUsage(): Map<String, Int> {
        val storedSet = prefs.getStringSet(KEY_USAGE_DATA, emptySet()) ?: emptySet()
        return storedSet.mapNotNull {
            val parts = it.split(":", limit = 2)
            if (parts.size == 2) {
                parts[0] to (parts[1].toIntOrNull() ?: 0)
            } else null
        }.toMap()
    }

    fun incrementUsage(packageName: String) {
        val currentUsage = _usageMap.value.toMutableMap()
        val count = currentUsage.getOrDefault(packageName, 0)
        currentUsage[packageName] = count + 1
        
        _usageMap.value = currentUsage
        saveUsage(currentUsage)
    }

    private fun saveUsage(usage: Map<String, Int>) {
        val set = usage.map { "${it.key}:${it.value}" }.toSet()
        prefs.edit().putStringSet(KEY_USAGE_DATA, set).apply()
    }
}

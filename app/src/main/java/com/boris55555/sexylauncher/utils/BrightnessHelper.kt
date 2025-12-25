package com.boris55555.sexylauncher.utils

import android.content.Context
import android.widget.Toast
import com.boris55555.sexylauncher.data.BrightnessPrefs
import androidx.core.net.toUri

object BrightnessHelper {
    fun toggleBrightness(context: Context, window: android.view.Window) {
        val prefs = BrightnessPrefs(context)
        // Check if we have permission to modify system settings
        if (!android.provider.Settings.System.canWrite(context)) {
            val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS)
            intent.data = ("package:${context.packageName}").toUri()
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Toast.makeText(
                context,
                "Please enable 'Modify system settings' for the brightness gesture.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val contentResolver = context.contentResolver

        try {
            // Get current system brightness (0-255)
            val currentSystemBrightness = android.provider.Settings.System.getInt(
                contentResolver,
                android.provider.Settings.System.SCREEN_BRIGHTNESS
            )

            // Determine if we're currently dimmed (brightness is 0 or very low)
            val isDimmed = currentSystemBrightness <= 1

            if (isDimmed) {
                // Restore brightness to the last saved value
                val savedBrightness = prefs.lastBrightnessLevel.coerceIn(1, 255)

                // Set system brightness
                android.provider.Settings.System.putInt(
                    contentResolver,
                    android.provider.Settings.System.SCREEN_BRIGHTNESS,
                    savedBrightness
                )

                // Update prefs to reflect restored brightness
                prefs.brightnessLevel = savedBrightness

                Toast.makeText(context, "Brightness restored to $savedBrightness", Toast.LENGTH_SHORT).show()
            } else {
                // ALWAYS save current brightness before dimming
                if (currentSystemBrightness > 0) {
                    prefs.lastBrightnessLevel = currentSystemBrightness
                }
                prefs.brightnessLevel = 0  // Mark as off in prefs

                // Dim the screen to minimum
                android.provider.Settings.System.putInt(
                    contentResolver,
                    android.provider.Settings.System.SCREEN_BRIGHTNESS,
                    1  // Minimum brightness (almost off but still visible)
                )

                Toast.makeText(context, "Brightness dimmed (saved: $currentSystemBrightness)", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Brightness control not available", Toast.LENGTH_SHORT).show()
        }
    }
}

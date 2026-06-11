package com.example.chromefinder.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.example.chromefinder.service.BrowserAutomationService

/** Проверка и переход к настройкам нашего Accessibility-сервиса. */
object AccessibilityHelper {

    /** Включён ли пользователем наш [BrowserAutomationService]. */
    fun isServiceEnabled(context: Context): Boolean {
        val expected =
            ComponentName(context, BrowserAutomationService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
    }

    /** Открыть системный экран «Спец. возможности», чтобы включить службу. */
    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

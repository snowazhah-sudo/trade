package com.example.chromefinder.browser

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log

/** Открывает URL в Google Chrome (с фолбэком на браузер по умолчанию). */
object ChromeLauncher {

    private const val TAG = "ChromeLauncher"
    const val CHROME_PACKAGE = "com.android.chrome"

    /** Результат попытки запуска. */
    enum class LaunchResult {
        /** Открылось в Chrome. */
        CHROME,

        /** Chrome не найден — открылось в другом браузере. */
        FALLBACK_BROWSER,

        /** Не удалось открыть вообще. */
        FAILED,
    }

    /** Установлен ли Google Chrome. */
    fun isChromeInstalled(context: Context): Boolean = try {
        context.packageManager.getPackageInfo(CHROME_PACKAGE, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    /**
     * Открыть [url] (уже нормализованный) в Chrome. Если Chrome нет —
     * пробуем дефолтный браузер.
     */
    fun open(context: Context, url: String): LaunchResult {
        val uri = Uri.parse(url)

        // 1. Пытаемся явно в Chrome.
        val chromeIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage(CHROME_PACKAGE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (isChromeInstalled(context)) {
            try {
                context.startActivity(chromeIntent)
                return LaunchResult.CHROME
            } catch (e: ActivityNotFoundException) {
                Log.w(TAG, "Chrome установлен, но не открылся, пробуем фолбэк", e)
            }
        }

        // 2. Фолбэк: любой браузер.
        val genericIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(genericIntent)
            LaunchResult.FALLBACK_BROWSER
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "Не удалось открыть URL ни в одном браузере", e)
            LaunchResult.FAILED
        }
    }
}

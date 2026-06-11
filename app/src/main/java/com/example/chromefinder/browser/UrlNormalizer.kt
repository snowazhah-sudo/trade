package com.example.chromefinder.browser

import android.util.Patterns

/** Приведение пользовательского ввода к корректному URL. */
object UrlNormalizer {

    /**
     * Добавляет схему `https://`, если её нет, и обрезает пробелы.
     * Возвращает null, если результат не похож на веб-адрес.
     */
    fun normalize(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null

        val withScheme = when {
            trimmed.startsWith("http://", ignoreCase = true) -> trimmed
            trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            else -> "https://$trimmed"
        }

        return if (Patterns.WEB_URL.matcher(withScheme).matches()) withScheme else null
    }
}

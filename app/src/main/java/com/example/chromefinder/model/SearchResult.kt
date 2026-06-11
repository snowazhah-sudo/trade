package com.example.chromefinder.model

/**
 * Один найденный на странице фрагмент.
 *
 * @param snippet   короткий фрагмент с подсветкой контекста вокруг совпадения
 * @param fullText  полный текст узла, в котором нашлось совпадение (может быть null)
 * @param timestamp когда найдено (epoch ms)
 */
data class SearchResult(
    val snippet: String,
    val fullText: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
)

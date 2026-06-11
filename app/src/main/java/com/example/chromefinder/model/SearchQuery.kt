package com.example.chromefinder.model

/**
 * Что и где искать.
 *
 * @param url     адрес страницы, которую надо открыть в Chrome
 * @param keyword искомое слово/фраза (для CONTAINS) или шаблон (для REGEX)
 * @param mode    режим сопоставления
 */
data class SearchQuery(
    val url: String,
    val keyword: String,
    val mode: MatchMode = MatchMode.CONTAINS,
)

/** Режим поиска совпадений в тексте страницы. */
enum class MatchMode {
    /** Простое вхождение подстроки без учёта регистра. */
    CONTAINS,

    /** Поиск по регулярному выражению. */
    REGEX,
}

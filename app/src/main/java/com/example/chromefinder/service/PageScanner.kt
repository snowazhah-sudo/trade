package com.example.chromefinder.service

import android.view.accessibility.AccessibilityNodeInfo
import com.example.chromefinder.model.MatchMode
import com.example.chromefinder.model.SearchQuery
import com.example.chromefinder.model.SearchResult

/**
 * Читает дерево узлов открытой страницы и ищет в собранном тексте совпадения
 * с [SearchQuery]. Не зависит от Android-сервиса — легко тестируется отдельно
 * (методы [search]/[buildSnippet] работают с обычными строками).
 */
object PageScanner {

    /** Сколько символов контекста показывать слева и справа от совпадения. */
    private const val CONTEXT_CHARS = 60

    /** Защита от слишком глубоких/огромных деревьев. */
    private const val MAX_DEPTH = 200
    private const val MAX_NODES = 5000

    /**
     * Рекурсивно собирает весь видимый текст со страницы в порядке обхода.
     * Дубликаты подряд (один и тот же текст у вложенных узлов) схлопываются.
     */
    fun collectTexts(root: AccessibilityNodeInfo?): List<String> {
        if (root == null) return emptyList()
        val out = ArrayList<String>()
        val counter = intArrayOf(0)
        traverse(root, 0, out, counter)
        // Уберём подряд идущие повторы.
        return out.filterIndexed { i, s -> i == 0 || s != out[i - 1] }
    }

    private fun traverse(
        node: AccessibilityNodeInfo?,
        depth: Int,
        out: MutableList<String>,
        counter: IntArray,
    ) {
        if (node == null || depth > MAX_DEPTH || counter[0] > MAX_NODES) return
        counter[0]++

        val text = node.text?.toString()?.trim()
        if (!text.isNullOrEmpty()) out.add(text)

        // Иногда полезная информация лежит в contentDescription.
        val desc = node.contentDescription?.toString()?.trim()
        if (!desc.isNullOrEmpty() && desc != text) out.add(desc)

        for (i in 0 until node.childCount) {
            traverse(node.getChild(i), depth + 1, out, counter)
        }
    }

    /** Применяет запрос к собранным строкам и возвращает фрагменты-совпадения. */
    fun search(texts: List<String>, query: SearchQuery): List<SearchResult> {
        if (query.keyword.isBlank()) return emptyList()
        val now = System.currentTimeMillis()
        val results = LinkedHashMap<String, SearchResult>() // дедуп по snippet

        when (query.mode) {
            MatchMode.CONTAINS -> {
                for (text in texts) {
                    val idx = text.indexOf(query.keyword, ignoreCase = true)
                    if (idx >= 0) {
                        val snippet = buildSnippet(text, idx, query.keyword.length)
                        results.putIfAbsent(
                            snippet,
                            SearchResult(snippet = snippet, fullText = text, timestamp = now),
                        )
                    }
                }
            }

            MatchMode.REGEX -> {
                val regex = runCatching {
                    Regex(query.keyword, RegexOption.IGNORE_CASE)
                }.getOrNull() ?: return emptyList()
                for (text in texts) {
                    for (match in regex.findAll(text)) {
                        val snippet = buildSnippet(text, match.range.first, match.value.length)
                        results.putIfAbsent(
                            snippet,
                            SearchResult(snippet = snippet, fullText = text, timestamp = now),
                        )
                    }
                }
            }
        }
        return results.values.toList()
    }

    /** Вырезает фрагмент с контекстом вокруг совпадения и добавляет «…» по краям. */
    fun buildSnippet(text: String, matchStart: Int, matchLength: Int): String {
        val start = (matchStart - CONTEXT_CHARS).coerceAtLeast(0)
        val end = (matchStart + matchLength + CONTEXT_CHARS).coerceAtMost(text.length)
        val core = text.substring(start, end).trim()
        val prefix = if (start > 0) "…" else ""
        val suffix = if (end < text.length) "…" else ""
        return "$prefix$core$suffix"
    }
}

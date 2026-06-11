package com.example.chromefinder.data

import com.example.chromefinder.model.SearchQuery
import com.example.chromefinder.model.SearchResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Текущее состояние поиска для отображения в UI. */
enum class ScanStatus {
    /** Ничего не запущено. */
    IDLE,

    /** Запрос отправлен, ждём пока сервис прочитает страницу. */
    SEARCHING,

    /** Поиск завершён, есть результаты. */
    DONE,

    /** Поиск завершён, совпадений нет. */
    NO_MATCH,

    /** Ошибка (Chrome не открылся, сервис выключен и т.п.). */
    ERROR,
}

/**
 * Общее состояние между UI ([com.example.chromefinder.ui.MainActivity]) и
 * accessibility-сервисом ([com.example.chromefinder.service.BrowserAutomationService]).
 *
 * Оба работают в одном процессе приложения, поэтому singleton-объект с
 * [StateFlow] — простой и надёжный канал связи без IPC.
 */
object SearchRepository {

    private val _query = MutableStateFlow<SearchQuery?>(null)
    /** Активный запрос, который сервис должен искать на странице. */
    val query: StateFlow<SearchQuery?> = _query.asStateFlow()

    private val _results = MutableStateFlow<List<SearchResult>>(emptyList())
    /** Найденные фрагменты. */
    val results: StateFlow<List<SearchResult>> = _results.asStateFlow()

    private val _status = MutableStateFlow(ScanStatus.IDLE)
    /** Текущий статус для UI. */
    val status: StateFlow<ScanStatus> = _status.asStateFlow()

    /** UI вызывает перед открытием Chrome: фиксирует запрос и сбрасывает прошлый результат. */
    fun startSearch(newQuery: SearchQuery) {
        _query.value = newQuery
        _results.value = emptyList()
        _status.value = ScanStatus.SEARCHING
    }

    /** Сервис вызывает, когда прочитал страницу и собрал совпадения. */
    fun publishResults(found: List<SearchResult>) {
        _results.value = found
        _status.value = if (found.isEmpty()) ScanStatus.NO_MATCH else ScanStatus.DONE
    }

    /** Сервис/UI сообщает об ошибке. */
    fun reportError() {
        _status.value = ScanStatus.ERROR
    }

    /** Полный сброс (например, по кнопке «очистить»). */
    fun clear() {
        _query.value = null
        _results.value = emptyList()
        _status.value = ScanStatus.IDLE
    }
}

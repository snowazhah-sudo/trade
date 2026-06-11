package com.example.chromefinder.service

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.chromefinder.browser.ChromeLauncher
import com.example.chromefinder.data.ScanStatus
import com.example.chromefinder.data.SearchRepository
import com.example.chromefinder.model.SearchQuery

/**
 * Читает содержимое страницы, открытой в Google Chrome, и ищет в нём то, что
 * задал пользователь. Реагирует только когда статус в [SearchRepository] —
 * [ScanStatus.SEARCHING].
 */
class BrowserAutomationService : AccessibilityService() {

    private companion object {
        const val TAG = "BrowserAutomation"

        /** Тишина в событиях, после которой считаем страницу «устаканившейся». */
        const val DEBOUNCE_MS = 800L

        /** Сколько раз прокрутить страницу в поисках текста ниже видимой области. */
        const val MAX_SCROLLS = 6
    }

    private val handler = Handler(Looper.getMainLooper())
    private val scanRunnable = Runnable { performScan() }

    /** Запрос, который сейчас обрабатываем (для сброса счётчиков при новом поиске). */
    private var activeQuery: SearchQuery? = null
    private var scrollCount = 0

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Accessibility-сервис подключён")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        // 3.2 — работаем только с Chrome.
        if (event.packageName != ChromeLauncher.CHROME_PACKAGE) return

        // Реагируем, только если пользователь запустил поиск.
        if (SearchRepository.status.value != ScanStatus.SEARCHING) return

        val current = SearchRepository.query.value ?: return

        // Новый запрос — сбрасываем состояние прокрутки.
        if (current != activeQuery) {
            activeQuery = current
            scrollCount = 0
        }

        // 3.3 — дебаунс: ждём, пока поток событий стихнет, и тогда сканируем.
        handler.removeCallbacks(scanRunnable)
        handler.postDelayed(scanRunnable, DEBOUNCE_MS)
    }

    override fun onInterrupt() {
        handler.removeCallbacks(scanRunnable)
    }

    private fun performScan() {
        val query = activeQuery ?: return
        if (SearchRepository.status.value != ScanStatus.SEARCHING) return

        val root: AccessibilityNodeInfo? = rootInActiveWindow
        if (root == null) {
            Log.w(TAG, "rootInActiveWindow == null, повторим позже")
            return
        }

        // 3.4 + 3.5 — собрать текст и найти совпадения.
        val texts = PageScanner.collectTexts(root)
        val results = PageScanner.search(texts, query)
        Log.d(TAG, "Просканировано узлов: ${texts.size}, найдено: ${results.size}")

        if (results.isNotEmpty()) {
            // 3.6 — отдать результат в UI.
            SearchRepository.publishResults(results)
            reset()
            return
        }

        // 3.7 — пока ничего: пробуем прокрутить вниз и просканировать снова.
        if (scrollCount < MAX_SCROLLS && scrollForward(root)) {
            scrollCount++
            handler.postDelayed(scanRunnable, DEBOUNCE_MS)
        } else {
            // Дальше скроллить некуда — совпадений на странице нет.
            SearchRepository.publishResults(emptyList())
            reset()
        }
    }

    /** Находит прокручиваемый узел и листает его вперёд. true — если получилось. */
    private fun scrollForward(root: AccessibilityNodeInfo): Boolean {
        val scrollable = findScrollable(root) ?: return false
        return scrollable.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
    }

    private fun findScrollable(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            val found = findScrollable(node.getChild(i))
            if (found != null) return found
        }
        return null
    }

    private fun reset() {
        activeQuery = null
        scrollCount = 0
        handler.removeCallbacks(scanRunnable)
    }
}

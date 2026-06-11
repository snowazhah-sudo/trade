# TODO — ChromeFinder (Android)

Чек-лист разработки. Каждая задача — атомарная и проверяемая.

## Протокол выполнения (для /loop)

На каждой итерации:
1. Открой этот файл и найди **первую** незавершённую задачу `[ ]` (сверху вниз).
2. Выполни **только её** (если задача большая — выполни её целиком, но не хватай следующие).
3. Отметь её как `[x]` и кратко допиши под ней, что сделано (1 строка).
4. Сделай **отдельный коммит** с понятным сообщением и запушь в ветку `claude/mobile-app-plan-1g45r1`.
5. Если все задачи `[x]` — ничего не делай, сообщи что всё готово.

Правила: не ломай уже сделанное; код должен быть синтаксически корректным; если
чего-то не хватает для сборки в контейнере — это нормально, главное чтобы код
был правильным под Android (отметь в коммите «проверено на устройстве требуется»).

---

## Этап 0. Каркас проекта

- [x] **0.1** Создать структуру Gradle-проекта: `settings.gradle.kts`,
  корневой `build.gradle.kts`, `gradle.properties`, обёртку Gradle
  (`gradlew`, `gradle/wrapper/...`).
  > Сделано: settings/build/gradle.properties, gradlew(+.bat), wrapper.properties,
  > .gitignore. Бинарный gradle-wrapper.jar генерируется командой `gradle wrapper`
  > (см. gradle/wrapper/README.md) — проверить на устройстве/в Android Studio.
- [x] **0.2** Создать модуль `app/` с `app/build.gradle.kts` (Kotlin, Compose,
  minSdk 26, targetSdk актуальный), подключить нужные зависимости.
  > Сделано: app/build.gradle.kts (compileSdk 34, minSdk 26, Compose BOM,
  > material3, icons-extended), proguard-rules.pro.
- [x] **0.3** Создать `app/src/main/AndroidManifest.xml`: `MainActivity`,
  объявление `BrowserAutomationService` как accessibility-сервиса, нужные
  права (`QUERY_ALL_PACKAGES`/intent для запуска Chrome).
  > Сделано: манифест с MainActivity (LAUNCHER), сервисом + meta-data,
  > <queries> для видимости Chrome.
- [x] **0.4** Базовые ресурсы: `res/values/strings.xml`, `themes.xml`,
  иконка-заглушка, `res/xml/accessibility_service_config.xml`.
  > Сделано: strings, themes, colors, adaptive-иконка (лупа),
  > accessibility_service_config.xml (слушает только Chrome).

## Этап 1. Модели и хранилище

- [x] **1.1** Модель `SearchQuery` (url: String, keyword: String,
  matchMode: enum CONTAINS/REGEX).
  > Сделано: model/SearchQuery.kt + enum MatchMode.
- [x] **1.2** Модель `SearchResult` (snippet: String, fullText: String?,
  timestamp: Long).
  > Сделано: model/SearchResult.kt.
- [x] **1.3** `SearchRepository` — синглтон с `StateFlow<SearchQuery?>` и
  `StateFlow<List<SearchResult>>` для связи UI ⇄ сервиса.
  > Сделано: data/SearchRepository.kt + enum ScanStatus (IDLE/SEARCHING/DONE/NO_MATCH/ERROR).

## Этап 2. Запуск Chrome

- [x] **2.1** `ChromeLauncher` — функция открыть URL именно в Google Chrome
  через `Intent` (`com.android.chrome`), с фолбэком на дефолтный браузер.
  > Сделано: browser/ChromeLauncher.kt (isChromeInstalled, open → enum LaunchResult).
- [x] **2.2** Нормализация URL (добавить `https://` если схемы нет, валидация).
  > Сделано: browser/UrlNormalizer.kt (Patterns.WEB_URL).

## Этап 3. Accessibility Service (ядро)

- [x] **3.1** `BrowserAutomationService extends AccessibilityService` —
  заготовка: `onServiceConnected`, `onAccessibilityEvent`, `onInterrupt`.
  > Сделано: service/BrowserAutomationService.kt.
- [x] **3.2** Фильтрация: реагировать только когда на переднем плане Chrome
  (`packageName == com.android.chrome`).
  > Сделано: проверка packageName + статус SEARCHING.
- [x] **3.3** Определение «страница загрузилась»: дебаунс событий
  `TYPE_WINDOW_CONTENT_CHANGED` (например, тишина 800 мс).
  > Сделано: Handler.postDelayed с DEBOUNCE_MS=800.
- [x] **3.4** `PageScanner` — рекурсивный обход `rootInActiveWindow`/
  `AccessibilityNodeInfo`, сбор всего видимого текста в список с сохранением
  порядка.
  > Сделано: PageScanner.collectTexts (text + contentDescription, защита по глубине/кол-ву).
- [x] **3.5** Поиск: применить `SearchQuery` к собранному тексту (CONTAINS и
  REGEX), вырезать фрагмент-контекст вокруг совпадения.
  > Сделано: PageScanner.search + buildSnippet (контекст ±60 симв., дедуп).
- [x] **3.6** Отдать результаты в `SearchRepository` (обновить StateFlow).
  > Сделано: publishResults из performScan.
- [x] **3.7** (Опц.) Автопрокрутка страницы (`ACTION_SCROLL_FORWARD`) чтобы
  прочитать контент ниже видимой области, с защитой от зацикливания.
  > Сделано: findScrollable + scrollForward, лимит MAX_SCROLLS=6.

## Этап 4. UI

- [x] **4.1** `MainActivity` + Compose-экран: поля URL и «что искать»,
  переключатель режима, кнопка «Запустить».
  > Сделано: ui/MainActivity.kt (Compose, поля, FilterChip CONTAINS/REGEX).
- [x] **4.2** Проверка, включён ли наш Accessibility Service; если нет — баннер
  и кнопка «Открыть настройки спец. возможностей».
  > Сделано: AccessibilityHelper + ServiceOffBanner, перепроверка на ON_RESUME.
- [x] **4.3** Кнопка «Запустить»: сохранить запрос в репозиторий → запустить
  Chrome через `ChromeLauncher`.
  > Сделано: onRun() с валидацией URL/запроса и startSearch → ChromeLauncher.open.
- [x] **4.4** Экран результатов: список найденных фрагментов (collectAsState из
  репозитория), кнопка «Открыть в Chrome», пустое состояние.
  > Сделано: ResultsList (LazyColumn) + StatusLine (searching/no_match/error).

## Этап 5. Надёжность и финал

- [ ] **5.1** Обработка ошибок: Chrome не установлен, страница без текста,
  совпадений нет, сервис выключен.
- [ ] **5.2** Логирование (`Log.d` с тегом) ключевых шагов для отладки.
- [ ] **5.3** Инструкция `INSTALL.md`: как собрать APK, установить, включить
  спец. возможности и пользоваться.
- [ ] **5.4** Финальная проверка: пройтись по сценарию из README, обновить
  README если что-то поменялось.

---

## Открытые вопросы (решить по ходу)

- Compose или XML-верстка? (по умолчанию — **Compose**)
- Нужна ли автопрокрутка (задача 3.7) для целевого сайта?
- Какой конкретно сайт и что именно искать? (нужно от пользователя для теста)

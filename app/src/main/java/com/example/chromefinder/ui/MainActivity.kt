package com.example.chromefinder.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.text.KeyboardOptions
import com.example.chromefinder.R
import com.example.chromefinder.browser.ChromeLauncher
import com.example.chromefinder.browser.UrlNormalizer
import com.example.chromefinder.data.ScanStatus
import com.example.chromefinder.data.SearchRepository
import com.example.chromefinder.model.MatchMode
import com.example.chromefinder.model.SearchQuery

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen() {
    val context = LocalContext.current

    var url by remember { mutableStateOf("") }
    var keyword by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(MatchMode.CONTAINS) }

    // Пересчитываем доступность службы при каждом возврате на экран.
    var serviceEnabled by remember { mutableStateOf(AccessibilityHelper.isServiceEnabled(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                serviceEnabled = AccessibilityHelper.isServiceEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val status by SearchRepository.status.collectAsState()
    val results by SearchRepository.results.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResourceCompat(context, R.string.app_name)) }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 4.2 — баннер, если служба выключена.
            if (!serviceEnabled) {
                ServiceOffBanner(onOpenSettings = {
                    AccessibilityHelper.openAccessibilitySettings(context)
                })
            }

            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text(stringResourceCompat(context, R.string.url_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = keyword,
                onValueChange = { keyword = it },
                label = { Text(stringResourceCompat(context, R.string.query_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = mode == MatchMode.CONTAINS,
                    onClick = { mode = MatchMode.CONTAINS },
                    label = { Text(stringResourceCompat(context, R.string.mode_contains)) },
                )
                FilterChip(
                    selected = mode == MatchMode.REGEX,
                    onClick = { mode = MatchMode.REGEX },
                    label = { Text(stringResourceCompat(context, R.string.mode_regex)) },
                )
            }

            Button(
                onClick = {
                    onRun(
                        context = context,
                        rawUrl = url,
                        keyword = keyword,
                        mode = mode,
                        serviceEnabled = serviceEnabled,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResourceCompat(context, R.string.run))
            }

            StatusLine(status, context)

            ResultsList(results, context, lastUrl = url)
        }
    }
}

/** Реакция на кнопку «Запустить»: валидация → запуск Chrome. */
private fun onRun(
    context: android.content.Context,
    rawUrl: String,
    keyword: String,
    mode: MatchMode,
    serviceEnabled: Boolean,
) {
    val normalized = UrlNormalizer.normalize(rawUrl)
    when {
        normalized == null -> toast(context, R.string.empty_url)
        keyword.isBlank() -> toast(context, R.string.empty_query)
        !serviceEnabled -> AccessibilityHelper.openAccessibilitySettings(context)
        else -> {
            SearchRepository.startSearch(SearchQuery(normalized, keyword.trim(), mode))
            when (ChromeLauncher.open(context, normalized)) {
                ChromeLauncher.LaunchResult.FAILED -> {
                    SearchRepository.reportError()
                    toast(context, R.string.chrome_not_installed)
                }
                else -> Unit
            }
        }
    }
}

@Composable
private fun ServiceOffBanner(onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResourceCompat(context, R.string.service_off_banner))
            OutlinedButton(onClick = onOpenSettings) {
                Text(stringResourceCompat(context, R.string.open_accessibility_settings))
            }
        }
    }
}

@Composable
private fun StatusLine(status: ScanStatus, context: android.content.Context) {
    val text = when (status) {
        ScanStatus.SEARCHING -> stringResourceCompat(context, R.string.searching)
        ScanStatus.NO_MATCH -> stringResourceCompat(context, R.string.no_results)
        ScanStatus.ERROR -> stringResourceCompat(context, R.string.chrome_not_installed)
        else -> null
    }
    if (text != null) Text(text, style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun ResultsList(
    results: List<com.example.chromefinder.model.SearchResult>,
    context: android.content.Context,
    lastUrl: String,
) {
    if (results.isEmpty()) return
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(results) { r ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(r.snippet, style = MaterialTheme.typography.bodyLarge)
                    OutlinedButton(onClick = {
                        UrlNormalizer.normalize(lastUrl)?.let { ChromeLauncher.open(context, it) }
                    }) {
                        Text(stringResourceCompat(context, R.string.open_in_chrome))
                    }
                }
            }
        }
    }
}

private fun toast(context: android.content.Context, resId: Int) {
    Toast.makeText(context, context.getString(resId), Toast.LENGTH_SHORT).show()
}

/** Маленький помощник: строка из ресурсов без @Composable-обёртки. */
private fun stringResourceCompat(context: android.content.Context, resId: Int): String =
    context.getString(resId)

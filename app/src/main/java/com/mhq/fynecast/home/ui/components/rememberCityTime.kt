package com.mhq.fynecast.home.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mhq.fynecast.core.ui.format.DateTimeFormats
import com.mhq.fynecast.core.ui.globalcomponents.currentLocale
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.time.Duration.Companion.milliseconds

/** Live clock for the selected city.
 * Falls back to [fallback] when the timezone is unknown. */
@Composable
fun rememberCityTime(timeZoneId: String?, fallback: String): String {
    val locale = currentLocale()
    val zone = remember(timeZoneId) {
        timeZoneId?.takeIf { it.isNotBlank() }
            ?.let { runCatching { ZoneId.of(it) }.getOrNull() }
    }
    var now by remember(zone) { mutableStateOf(zone?.let { LocalDateTime.now(it) }) }

    LaunchedEffect(zone) {
        if (zone == null) return@LaunchedEffect
        while (true) {
            now = LocalDateTime.now(zone)
            delay((60_000L - System.currentTimeMillis() % 60_000L).milliseconds)
        }
    }

    return now?.let { DateTimeFormats.lastUpdated(it, locale) } ?: fallback
}
package com.mhq.fynecast.core.ui.format

import android.text.format.DateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateTimeFormats {
    private fun formatter(
        locale: Locale,
        skeleton: String
    ) = DateTimeFormatter.ofPattern(
        DateFormat.getBestDateTimePattern(locale, skeleton), locale
    )

    fun hour(time: LocalDateTime, locale: Locale): String =
        time.format(formatter(locale, "j"))

    fun lastUpdated(time: LocalDateTime, locale: Locale): String =
        time.format(formatter(locale, "jm"))

    fun dayName(date: LocalDate, locale: Locale): String =
        date.format(formatter(locale, "EEE"))

    fun monthDay(date: LocalDate, locale: Locale): String =
        date.format(formatter(locale, "MMMd"))
}
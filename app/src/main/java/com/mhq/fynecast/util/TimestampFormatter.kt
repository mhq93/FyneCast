package com.mhq.fynecast.util

import android.os.Build
import androidx.annotation.RequiresApi
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale


object TimestampFormatter {
    
    fun getFormattedHour(rawDateTime: String): String {
        return try {
            val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            val dateTime = LocalDateTime.parse(rawDateTime, inputFormatter)
            val outputFormatter = DateTimeFormatter.ofPattern("h a", Locale.US)
            dateTime.format(outputFormatter)
        } catch (e: Exception) {
            rawDateTime
        }
    }

    fun getFormattedDateAndTime(dateString: String): Pair<String, String> {
        return try {
            val sdfInput = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdfInput.parse(dateString) ?: return Pair("N/A", "N/A")
            val dayName = SimpleDateFormat("EEE", Locale.getDefault()).format(date)
            val monthDay = SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
            Pair(dayName, monthDay)
        } catch (e: Exception) {
            Pair("N/A", "N/A")
        }
    }
}
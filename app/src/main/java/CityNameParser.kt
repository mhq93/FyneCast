//package com.mhq.fynecast.util
//
//object CityNameParser {
//    data class ParsedCityName(val cityName: String, val countryName: String)
//
//    /**
//     * Parses a "City, Country" display string into its parts. Falls back to the
//     * provided defaults for whichever part is missing or blank — callers pass in
//     * the weather API's own cityName/country as that fallback, since it's the
//     * most reliable structured source available when the display string is short.
//     */
//    fun parse(
//        fullName: String,
//        fallbackCityName: String = "",
//        fallbackCountryName: String = ""
//    ): ParsedCityName {
//        val parts = fullName.split(",")
//        val cityName = parts.firstOrNull()?.trim()?.takeIf { it.isNotBlank() } ?: fallbackCityName
//        val countryName = parts.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() } ?: fallbackCountryName
//        return ParsedCityName(cityName, countryName)
//    }
//}
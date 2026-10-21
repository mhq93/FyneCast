package com.mhq.fynecast.home.data.repoimpl.location

interface PlaceNameSource {
    /** The city and country at these coordinates,
     * written in [langCode], or null. */
    suspend fun nameAt(
        latitude: Double,
        longitude: Double,
        langCode: String
    ): String?

    /**
     * [name] rewritten in [langCode],
     * for the place within about 50 km of the coordinates.
     * Null when nothing close enough is found,
     * so callers keep the name they already have.
     */
    suspend fun localize(
        name: String,
        latitude: Double,
        longitude: Double,
        langCode: String
    ): String?
}
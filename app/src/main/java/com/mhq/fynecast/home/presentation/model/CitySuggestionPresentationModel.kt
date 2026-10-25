package com.mhq.fynecast.home.presentation.model

/** One row of the search dropdown. The UI sends [id] back when the user picks it. */
data class CitySuggestionPresentationModel(
    val id: String,
    val name: String
)

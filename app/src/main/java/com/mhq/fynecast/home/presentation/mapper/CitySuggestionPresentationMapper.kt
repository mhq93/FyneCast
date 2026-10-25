package com.mhq.fynecast.home.presentation.mapper

import com.mhq.fynecast.core.domain.location.CitySuggestionModel
import com.mhq.fynecast.home.presentation.model.CitySuggestionPresentationModel

/** The id is the place's coordinates: two suggestions at the same spot are the same place. */
fun CitySuggestionModel.toPresentation(): CitySuggestionPresentationModel =
    CitySuggestionPresentationModel(
        id = "$latitude,$longitude",
        name = fullName
    )

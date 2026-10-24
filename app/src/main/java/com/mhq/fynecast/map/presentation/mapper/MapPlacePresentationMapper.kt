package com.mhq.fynecast.map.presentation.mapper

import com.mhq.fynecast.core.domain.location.CitySuggestionModel
import com.mhq.fynecast.map.presentation.model.MapPlacePresentationModel

fun CitySuggestionModel.toPresentation(): MapPlacePresentationModel =
    MapPlacePresentationModel(name = fullName)

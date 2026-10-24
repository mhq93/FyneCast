package com.mhq.fynecast.alerts.presentation.mapper

import com.mhq.fynecast.alerts.presentation.model.AlertPresentationModel
import com.mhq.fynecast.alerts.presentation.model.AlertSeverity
import com.mhq.fynecast.core.domain.weather.AlertDomainModel

fun AlertDomainModel.toPresentation(): AlertPresentationModel = AlertPresentationModel(
    id = id,
    event = event,
    headline = headline,
    description = description,
    instruction = instruction,
    severity = severity.toAlertSeverity(),
    severityLabel = severity
)

internal fun String.toAlertSeverity(): AlertSeverity = when (trim().lowercase()) {
    "extreme" -> AlertSeverity.Extreme
    "severe" -> AlertSeverity.Severe
    "moderate" -> AlertSeverity.Moderate
    else -> AlertSeverity.Other
}
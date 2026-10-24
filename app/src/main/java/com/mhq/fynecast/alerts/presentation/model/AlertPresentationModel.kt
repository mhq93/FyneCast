package com.mhq.fynecast.alerts.presentation.model

/** What the Alerts screen needs to show one alert. The UI never sees the domain model. */
data class AlertPresentationModel(
    val id: String,
    val event: String,
    val headline: String,
    val description: String,
    val instruction: String,
    val severity: AlertSeverity,
    /** The severity text as reported, for the badge. */
    val severityLabel: String
)

enum class AlertSeverity {
    Extreme,
    Severe,
    Moderate,

    /** Minor, unknown or anything not recognised. */
    Other
}
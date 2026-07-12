package com.mhq.fynecast.alerts.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AlertsDto(
    @SerialName("alert")
    val alert: List<AlertDto> = emptyList()
)
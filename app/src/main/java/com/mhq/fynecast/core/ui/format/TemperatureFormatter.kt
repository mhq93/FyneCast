package com.mhq.fynecast.core.ui.format

import kotlin.math.roundToInt

object TemperatureFormatter {
    fun format(value: Double): String = "${value.roundToInt()}°"
}
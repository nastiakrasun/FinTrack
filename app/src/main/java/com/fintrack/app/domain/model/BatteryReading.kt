package com.fintrack.app.domain.model

data class BatteryReading(
    val temperatureCelsius: Float?,
    val voltageMillivolts: Int?,
    val currentMicroamps: Int?,
    val levelPercent: Int?,
    val isCharging: Boolean
)

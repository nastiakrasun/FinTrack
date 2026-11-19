package com.fintrack.app.domain.repository

import com.fintrack.app.domain.model.BatteryReading
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface BatteryMonitorRepository {
    fun observeBattery(): Flow<BatteryReading>
}

object EmptyBatteryMonitorRepository : BatteryMonitorRepository {
    override fun observeBattery(): Flow<BatteryReading> = flowOf(
        BatteryReading(
            temperatureCelsius = null,
            voltageMillivolts = null,
            currentMicroamps = null,
            levelPercent = null,
            isCharging = false
        )
    )
}

interface BatteryAlertNotifier {
    fun notifyHighTemperature(temperatureCelsius: Float)
}

object NoOpBatteryAlertNotifier : BatteryAlertNotifier {
    override fun notifyHighTemperature(temperatureCelsius: Float) = Unit
}

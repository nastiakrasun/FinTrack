package com.fintrack.app.domain.repository

import com.fintrack.app.domain.model.DeviceResourceReading
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface DeviceResourceMonitor {
    fun observeResources(): Flow<DeviceResourceReading>
}

object EmptyDeviceResourceMonitor : DeviceResourceMonitor {
    override fun observeResources(): Flow<DeviceResourceReading> = flowOf(
        DeviceResourceReading(null, null, null, null, null)
    )
}

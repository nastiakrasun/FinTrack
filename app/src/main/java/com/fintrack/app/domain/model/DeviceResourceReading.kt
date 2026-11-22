package com.fintrack.app.domain.model

data class DeviceResourceReading(
    val processCpuPercent: Float?,
    val memoryUsedMegabytes: Long?,
    val memoryTotalMegabytes: Long?,
    val networkReceivedBytesPerSecond: Long?,
    val networkSentBytesPerSecond: Long?
)

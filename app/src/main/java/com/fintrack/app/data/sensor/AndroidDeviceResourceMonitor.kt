package com.fintrack.app.data.sensor

import android.app.ActivityManager
import android.content.Context
import android.net.TrafficStats
import android.os.Process
import android.os.SystemClock
import com.fintrack.app.domain.model.DeviceResourceReading
import com.fintrack.app.domain.repository.DeviceResourceMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class AndroidDeviceResourceMonitor(context: Context) : DeviceResourceMonitor {
    private val applicationContext = context.applicationContext
    private val activityManager =
        applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    override fun observeResources(): Flow<DeviceResourceReading> = flow {
        var previousCpuMillis = Process.getElapsedCpuTime()
        var previousWallMillis = SystemClock.elapsedRealtime()
        var previousRxBytes = TrafficStats.getTotalRxBytes()
        var previousTxBytes = TrafficStats.getTotalTxBytes()

        while (true) {
            val nowCpuMillis = Process.getElapsedCpuTime()
            val nowWallMillis = SystemClock.elapsedRealtime()
            val nowRxBytes = TrafficStats.getTotalRxBytes()
            val nowTxBytes = TrafficStats.getTotalTxBytes()
            val memory = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memory)

            val wallDelta = (nowWallMillis - previousWallMillis).coerceAtLeast(1L)
            val cpuPercent = ((nowCpuMillis - previousCpuMillis) * 100f / wallDelta)
                .coerceIn(0f, 100f)
            val rxRate = byteRate(previousRxBytes, nowRxBytes, wallDelta)
            val txRate = byteRate(previousTxBytes, nowTxBytes, wallDelta)
            val totalMegabytes = memory.totalMem / BYTES_PER_MEGABYTE
            val availableMegabytes = memory.availMem / BYTES_PER_MEGABYTE

            emit(
                DeviceResourceReading(
                    processCpuPercent = cpuPercent,
                    memoryUsedMegabytes = (totalMegabytes - availableMegabytes).coerceAtLeast(0L),
                    memoryTotalMegabytes = totalMegabytes,
                    networkReceivedBytesPerSecond = rxRate,
                    networkSentBytesPerSecond = txRate
                )
            )

            previousCpuMillis = nowCpuMillis
            previousWallMillis = nowWallMillis
            previousRxBytes = nowRxBytes
            previousTxBytes = nowTxBytes
            delay(SAMPLE_INTERVAL_MILLIS)
        }
    }.flowOn(Dispatchers.IO)

    private fun byteRate(previous: Long, current: Long, elapsedMillis: Long): Long? {
        if (previous < 0 || current < previous) return null
        return (current - previous) * 1_000L / elapsedMillis.coerceAtLeast(1L)
    }

    private companion object {
        const val SAMPLE_INTERVAL_MILLIS = 2_000L
        const val BYTES_PER_MEGABYTE = 1024L * 1024L
    }
}

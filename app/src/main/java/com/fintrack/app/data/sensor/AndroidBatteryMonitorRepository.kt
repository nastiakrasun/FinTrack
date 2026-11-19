package com.fintrack.app.data.sensor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.fintrack.app.domain.model.BatteryReading
import com.fintrack.app.domain.repository.BatteryMonitorRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class AndroidBatteryMonitorRepository(context: Context) : BatteryMonitorRepository {
    private val applicationContext = context.applicationContext
    private val batteryManager =
        applicationContext.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

    override fun observeBattery(): Flow<BatteryReading> = callbackFlow {
        fun publish(intent: Intent?) {
            if (intent == null) return
            val temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
            val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, Int.MIN_VALUE)
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val current = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
                    .takeUnless { it == Int.MIN_VALUE }
            } else {
                null
            }

            trySend(
                BatteryReading(
                    temperatureCelsius = temperature
                        .takeUnless { it == Int.MIN_VALUE }
                        ?.div(10f),
                    voltageMillivolts = voltage.takeUnless { it == Int.MIN_VALUE || it <= 0 },
                    currentMicroamps = current,
                    levelPercent = if (level >= 0 && scale > 0) {
                        (level * 100 / scale).coerceIn(0, 100)
                    } else {
                        null
                    },
                    isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
                )
            )
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) = publish(intent)
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val initialReading = ContextCompat.registerReceiver(
            applicationContext,
            receiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
        publish(initialReading)

        awaitClose { applicationContext.unregisterReceiver(receiver) }
    }
}

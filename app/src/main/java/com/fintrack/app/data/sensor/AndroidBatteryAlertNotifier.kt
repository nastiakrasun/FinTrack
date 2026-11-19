package com.fintrack.app.data.sensor

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.fintrack.app.R
import com.fintrack.app.domain.repository.BatteryAlertNotifier
import java.util.Locale

class AndroidBatteryAlertNotifier(context: Context) : BatteryAlertNotifier {
    private val applicationContext = context.applicationContext

    override fun notifyHighTemperature(temperatureCelsius: Float) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
            as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Battery alerts",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = "Alerts when device battery temperature is high" }
            )
        }

        val temperature = String.format(Locale.getDefault(), "%.1f °C", temperatureCelsius)
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("FinTrack device temperature alert")
            .setContentText("Battery temperature is $temperature. Consider letting the device cool.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
    }

    private companion object {
        const val CHANNEL_ID = "battery_temperature_alerts"
        const val NOTIFICATION_ID = 1301
    }
}

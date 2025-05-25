package com.example.mqttwearable.health

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.mqttwearable.R

class HealthForegroundService : Service() {
    private lateinit var wakeLock: PowerManager.WakeLock

    override fun onCreate() {
        super.onCreate()
        // 1. Adquire o WakeLock (permite CPU ativo mesmo com tela apagada)
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "MqttHealth:WakeLockTag"
        )
        wakeLock.acquire()

        // 2. Cria a notificação de foreground
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("HealthPublisher ativo")
            .setContentText("Coletando dados em segundo plano…")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()

        startForeground(ONGOING_NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Aqui você pode inicializar seu HealthPublisher e/ou MqttHandler
        // healthPublisher.startPassive(), etc.
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        wakeLock.release()
    }

    override fun onBind(intent: Intent?) = null

    companion object {
        const val CHANNEL_ID = "health_foreground_channel"
        const val ONGOING_NOTIFICATION_ID = 1
    }
}
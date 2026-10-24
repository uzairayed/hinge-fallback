package com.hingefallback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat

class HingeFallbackService : Service(), SensorEventListener {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val worker = java.util.concurrent.Executors.newSingleThreadExecutor()
    private var sensorManager: SensorManager? = null
    private var hall0: Sensor? = null
    private var hall1: Sensor? = null
    @Volatile private var desiredClosed: Boolean? = null
    @Volatile private var appliedClosed: Boolean? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private val applyRunnable = Runnable { applyDesired() }

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(SensorManager::class.java)
        createChannel()
        startForeground(NOTIFICATION_ID, notification("Starting"))
        val powerManager = getSystemService(PowerManager::class.java)
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "hingefallback:apply").apply {
            setReferenceCounted(false)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISABLE -> {
                disable()
                return START_NOT_STICKY
            }
            ACTION_RESET -> {
                Prefs.setEnabled(this, false)
                disable()
                return START_NOT_STICKY
            }
        }
        if (!Prefs.isEnabled(this)) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        startWatching()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        mainHandler.removeCallbacks(applyRunnable)
        sensorManager?.unregisterListener(this)
        worker.shutdownNow()
        wakeLock?.let { if (it.isHeld) it.release() }
        super.onDestroy()
    }

    override fun onSensorChanged(event: SensorEvent) {
        val raw = event.values.firstOrNull() ?: return
        when (event.sensor.name) {
            HallSensors.NAME_0 -> {
                StatusStore.hall0 = formatHall(raw)
                desiredClosed = raw >= 0.5f
                Log.i(TAG, "hall0=$raw closed=$desiredClosed")
                mainHandler.removeCallbacks(applyRunnable)
                mainHandler.postDelayed(applyRunnable, 300)
            }
            HallSensors.NAME_1 -> {
                StatusStore.hall1 = trimNumber(raw)
                Log.i(TAG, "hall1=$raw")
            }
        }
        mainHandler.post { refreshNotification() }
        StatusStore.publish()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun startWatching() {
        val manager = sensorManager ?: return
        if (hall0 != null) return
        val (sensor0, sensor1) = HallSensors.find(manager)
        hall0 = sensor0
        hall1 = sensor1
        if (sensor0 == null) {
            StatusStore.hall0 = "Not available"
            StatusStore.lastError = "Hall sensor 0 is not visible to this app."
            refreshNotification()
            StatusStore.publish()
            Log.e(TAG, "hall sensor 0 missing")
            return
        }
        manager.registerListener(this, sensor0, SensorManager.SENSOR_DELAY_NORMAL)
        sensor1?.let { manager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        Log.i(TAG, "watching ${sensor0.name} and ${sensor1?.name}")
    }

    private fun applyDesired() {
        val closed = desiredClosed ?: return
        if (closed == appliedClosed) return
        worker.execute {
            if (!ShizukuShell.isGranted()) {
                StatusStore.lastError = "Shizuku is not allowed yet."
                mainHandler.post {
                    refreshNotification()
                    StatusStore.publish()
                }
                return@execute
            }
            val lock = wakeLock
            lock?.acquire(5_000)
            try {
                val result = if (closed) {
                    ShizukuShell.baseState("0")
                } else {
                    ShizukuShell.baseState("reset")
                }
                if (!result.ok) {
                    StatusStore.lastError = result.stderr.ifBlank { "device_state failed (${result.code})" }
                    Log.e(TAG, "base-state failed: ${result.stderr} ${result.stdout}")
                } else {
                    appliedClosed = closed
                    StatusStore.lastError = ""
                    val printed = ShizukuShell.printState()
                    StatusStore.deviceState = deviceStateName(printed.stdout)
                    Log.i(TAG, "applied closed=$closed state=${printed.stdout}")
                }
            } catch (error: Throwable) {
                StatusStore.lastError = error.message ?: "Shell command failed"
                Log.e(TAG, "apply failed", error)
            } finally {
                if (lock?.isHeld == true) lock.release()
                mainHandler.post {
                    refreshNotification()
                    StatusStore.publish()
                }
            }
        }
    }

    private fun disable() {
        mainHandler.removeCallbacks(applyRunnable)
        sensorManager?.unregisterListener(this)
        hall0 = null
        hall1 = null
        desiredClosed = null
        appliedClosed = null
        worker.execute {
            if (ShizukuShell.isGranted()) {
                val result = ShizukuShell.baseState("reset")
                Log.i(TAG, "reset on disable ok=${result.ok} ${result.stdout} ${result.stderr}")
            }
            mainHandler.post {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                StatusStore.publish()
            }
        }
    }

    private fun refreshNotification() {
        val text = when (desiredClosed) {
            true -> "Closed · cover screen"
            false -> "Open · inner screen"
            null -> StatusStore.hall0
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification(text))
    }

    private fun notification(text: String): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val reset = PendingIntent.getForegroundService(
            this,
            1,
            Intent(this, HingeFallbackService::class.java).setAction(ACTION_RESET),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_hinge)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(open)
            .addAction(0, getString(R.string.turn_off), reset)
            .build()
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_LOW,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_DISABLE = "com.hingefallback.DISABLE"
        const val ACTION_RESET = "com.hingefallback.RESET"
        private const val TAG = "HingeFallback"
        private const val CHANNEL_ID = "hinge"
        private const val NOTIFICATION_ID = 41
    }
}

private fun formatHall(value: Float): String {
    val meaning = if (value >= 0.5f) "closed" else "open"
    return "${trimNumber(value)} · $meaning"
}

private fun trimNumber(value: Float): String =
    if (value % 1f == 0f) value.toInt().toString() else String.format("%.2f", value)

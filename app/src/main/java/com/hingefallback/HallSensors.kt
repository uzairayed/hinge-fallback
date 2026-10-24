package com.hingefallback

import android.hardware.Sensor
import android.hardware.SensorManager

object HallSensors {
    const val TYPE = 65547
    const val NAME_0 = "DRV5032 Hall Effect Sensor 0 (wake-up)"
    const val NAME_1 = "DRV5032 Hall Effect Sensor 1 (wake-up)"

    fun find(sensorManager: SensorManager): Pair<Sensor?, Sensor?> {
        val sensors = sensorManager.getSensorList(TYPE).ifEmpty {
            sensorManager.getSensorList(Sensor.TYPE_ALL).filter { sensor ->
                sensor.stringType == "com.google.sensor.hall_effect" || sensor.name.contains("Hall Effect")
            }
        }
        return sensors.firstOrNull { it.name == NAME_0 } to sensors.firstOrNull { it.name == NAME_1 }
    }
}

fun deviceStateName(code: String): String = when (code.trim()) {
    "0" -> "Closed"
    "1" -> "Half open"
    "2" -> "Open"
    else -> code.ifBlank { "Unknown" }
}

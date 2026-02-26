package com.example.mc_project

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import androidx.compose.runtime.rememberUpdatedState


// Code from online source "developer" has been used as a base here
// AI was used to debug how to get the change in dark and light mode to work
// not in use for final projectS
@Composable
fun StartLightSensorFeed(
    vm: ProfileViewModel,
    currentDarkMode: Boolean,
) {
    val context = LocalContext.current

    // always up-to-date (changes after you click the notification too)
    val latestAppliedDark = rememberUpdatedState(currentDarkMode)

    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    val lightSensor = remember { sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) }
    var latestLux by remember { mutableStateOf<Float?>(null) }

    val darkBelowLux = 5000f
    val lightAboveLux = 10000f

    DisposableEffect(lightSensor) {
        if (lightSensor == null) return@DisposableEffect onDispose { }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val lux = event.values.firstOrNull() ?: return
                if (lux < 0f || lux > 40_000f) return
                latestLux = lux
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        sensorManager.registerListener(listener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
        onDispose { sensorManager.unregisterListener(listener) }
    }

    LaunchedEffect(Unit) {
        var lastSuggestedDark: Boolean? = null

        while (true) {
            val lux = latestLux
            if (lux != null) {
                val suggestedDark = when {
                    lux <= darkBelowLux -> true
                    lux >= lightAboveLux -> false
                    else -> lastSuggestedDark
                }

                val appliedNow = latestAppliedDark.value

                // notify only when suggestion changes AND differs from current applied theme
                if (suggestedDark != null &&
                    suggestedDark != lastSuggestedDark &&
                    suggestedDark != appliedNow
                ) {
                    lastSuggestedDark = suggestedDark
                    NotificationHelper.showThemeChanged(context, suggestedDark)
                }
            }

            delay(10_000)
        }
    }
}
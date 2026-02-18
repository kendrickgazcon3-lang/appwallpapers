package com.wallpaper.pro

import android.content.Context
import android.hardware.*
import android.os.*
import android.service.wallpaper.WallpaperService
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.webkit.WebView

class MyWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = HapticEngine()

    inner class HapticEngine : Engine(), SensorEventListener {
        private var webView: WebView? = null
        private var sensorManager: SensorManager? = null
        private var vibrator: Vibrator? = null

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            
            // Sensores: Acelerómetro y Luz
            val accel = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            val light = sensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT)
            sensorManager?.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME)
            sensorManager?.registerListener(this, light, SensorManager.SENSOR_DELAY_UI)

            webView = WebView(applicationContext).apply {
                settings.javaScriptEnabled = true
                // Importante: Esto permite que el HTML sepa que hay una interfaz Android
                loadUrl("file:///android_asset/wallpaper.html")
            }
        }

        override fun onTouchEvent(event: MotionEvent) {
            if (event.action == MotionEvent.ACTION_MOVE || event.action == MotionEvent.ACTION_DOWN) {
                // Pasamos coordenadas al JS
                webView?.evaluateJavascript("updateTouch(${event.x}, ${event.y})", null)
                // Feedback háptico de colisión con el dedo
                triggerHaptic(30, 180) 
            }
        }

        override fun onSensorChanged(event: SensorEvent) {
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    // X e Y para gravedad, Z para "sacudida"
                    webView?.evaluateJavascript("updatePhys(${event.values[0]}, ${event.values[1]})", null)
                }
                Sensor.TYPE_LIGHT -> {
                    webView?.evaluateJavascript("updateLight(${event.values[0]})", null)
                }
            }
        }

        private fun triggerHaptic(ms: Long, amp: Int) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(ms, amp))
            } else {
                vibrator?.vibrate(ms)
            }
        }

        override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        override fun onDestroy() {
            sensorManager?.unregisterListener(this)
            super.onDestroy()
        }
    }
}
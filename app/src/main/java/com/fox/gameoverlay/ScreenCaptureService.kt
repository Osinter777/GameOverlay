package com.fox.gameoverlay

import android.content.Context
import kotlinx.coroutines.*

class ScreenCaptureService(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var isRunning = false

    fun start() {
        isRunning = true
        scope.launch {
            while (isRunning) {
                try {
                    delay(33)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun stop() {
        isRunning = false
        scope.cancel()
    }

    companion object {
        const val REQUEST_CODE_SCREEN_CAPTURE = 200
    }
}

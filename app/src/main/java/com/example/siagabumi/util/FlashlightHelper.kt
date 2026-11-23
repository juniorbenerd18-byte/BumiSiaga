package com.example.siagabumi.util

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

object FlashlightHelper {

    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn: StateFlow<Boolean> = _isTorchOn

    private val _isSosActive = MutableStateFlow(false)
    val isSosActive: StateFlow<Boolean> = _isSosActive

    private var sosJob: Job? = null

    fun isFlashAvailable(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
    }

    fun toggleTorch(context: Context) {
        if (_isSosActive.value) {
            stopSos(context)
        }
        val newState = !_isTorchOn.value
        setTorchMode(context, newState)
        _isTorchOn.value = newState
    }

    fun toggleSos(context: Context, scope: CoroutineScope) {
        if (_isSosActive.value) {
            stopSos(context)
        } else {
            startSos(context, scope)
        }
    }

    private fun setTorchMode(context: Context, enabled: Boolean) {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return
        try {
            for (id in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    cameraManager.setTorchMode(id, enabled)
                    break
                }
            }
        } catch (_: CameraAccessException) {
            // Flash not accessible or camera in use
        } catch (_: Exception) {
            // Other exceptions
        }
    }

    private fun startSos(context: Context, scope: CoroutineScope) {
        _isSosActive.value = true
        _isTorchOn.value = false
        sosJob?.cancel()
        sosJob = scope.launch(Dispatchers.Default) {
            // S-O-S Morse Code: ... --- ...
            // dot: 150ms, dash: 450ms, intra-element gap: 150ms, inter-letter gap: 400ms, word gap: 1000ms
            val dot = 150L
            val dash = 450L
            val gap = 150L

            try {
                while (_isSosActive.value) {
                    // S: 3 dots
                    repeat(3) {
                        setTorchMode(context, true)
                        delay(dot)
                        setTorchMode(context, false)
                        delay(gap)
                    }
                    delay(300L)

                    // O: 3 dashes
                    repeat(3) {
                        setTorchMode(context, true)
                        delay(dash)
                        setTorchMode(context, false)
                        delay(gap)
                    }
                    delay(300L)

                    // S: 3 dots
                    repeat(3) {
                        setTorchMode(context, true)
                        delay(dot)
                        setTorchMode(context, false)
                        delay(gap)
                    }
                    delay(1200L)
                }
            } finally {
                setTorchMode(context, false)
            }
        }
    }

    fun stopSos(context: Context) {
        _isSosActive.value = false
        sosJob?.cancel()
        sosJob = null
        setTorchMode(context, false)
        _isTorchOn.value = false
    }
}

package com.miesposa.sadia.features.phone

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager

class FlashlightController(context: Context) {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val torchCameraId: String? by lazy { findTorchCapableCamera() }

    fun turnOn(): Boolean = setTorch(true)
    fun turnOff(): Boolean = setTorch(false)

    private fun setTorch(enabled: Boolean): Boolean {
        val manager = cameraManager ?: return false
        val id = torchCameraId ?: return false
        return try {
            manager.setTorchMode(id, enabled)
            true
        } catch (e: Exception) {
            // Some OEM cameras (esp. on older Android 12 devices) throw here while the
            // Camera app itself is active — fail safely rather than crash.
            false
        }
    }

    private fun findTorchCapableCamera(): String? {
        val manager = cameraManager ?: return null
        return try {
            manager.cameraIdList.firstOrNull { id ->
                manager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: Exception) {
            null
        }
    }
}

package com.fox.gameoverlay

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {
    companion object {
        private const val REQUEST_CODE_DRAW_OVERLAY = 100
        private const val REQUEST_CODE_PERMISSIONS = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkAndRequestPermissions()

        if (!Settings.canDrawOverlays(this)) {
            startActivityForResult(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION),
                REQUEST_CODE_DRAW_OVERLAY
            )
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.FOREGROUND_SERVICE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.FOREGROUND_SERVICE_CAMERA)
        }

        val requiredPermissions = permissions.filter {
            ActivityCompat.checkSelfPermission(this, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (requiredPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, requiredPermissions, REQUEST_CODE_PERMISSIONS)
        }
    }

    fun startOverlay() {
        val intent = Intent(this, OverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    fun stopOverlay() {
        stopService(Intent(this, OverlayService::class.java))
    }

    fun openSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }
}

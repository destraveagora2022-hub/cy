package com.cryx

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkOverlayPermission()

        fun sw(id: Int, action: (Boolean) -> Unit) =
            findViewById<Switch>(id).setOnCheckedChangeListener { _, v -> action(v) }

        sw(R.id.switchEsp)      { OverlayService.espEnabled      = it }
        sw(R.id.switchSkeleton) { OverlayService.skeletonEnabled  = it }
        sw(R.id.switchHp)       { OverlayService.hpEnabled        = it }
        sw(R.id.switchAimbot)   { OverlayService.aimbotEnabled    = it }

        findViewById<Button>(R.id.btnLaunch).setOnClickListener {
            startForegroundService(Intent(this, OverlayService::class.java))
        }
        findViewById<Button>(R.id.btnStop).setOnClickListener {
            stopService(Intent(this, OverlayService::class.java))
        }
    }

    private fun checkOverlayPermission() {
        if (!Settings.canDrawOverlays(this))
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")))
    }
}

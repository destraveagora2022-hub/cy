package com.cryx

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.IBinder
import android.view.*
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class OverlayService : Service() {

    companion object {
        const val CH_ID = "cryx_ch"; const val NOTIF_ID = 1
        @Volatile var espEnabled      = true
        @Volatile var skeletonEnabled = true
        @Volatile var hpEnabled       = true
        @Volatile var aimbotEnabled   = false
    }

    private lateinit var wm: WindowManager
    private lateinit var overlay: OverlayView
    private val mem   = MemoryReader()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIF_ID, buildNotif())
        val dm = resources.displayMetrics
        val params = WindowManager.LayoutParams(
            dm.widthPixels, dm.heightPixels,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START }
        overlay = OverlayView(this, dm.widthPixels, dm.heightPixels, mem)
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        wm.addView(overlay, params)
        scope.launch {
            mem.init()
            while (isActive) { overlay.postInvalidate(); delay(16L) }
        }
    }

    override fun onDestroy() { scope.cancel(); wm.removeView(overlay); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() =
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(NotificationChannel(CH_ID, "cryX", NotificationManager.IMPORTANCE_LOW))

    private fun buildNotif() = NotificationCompat.Builder(this, CH_ID)
        .setContentTitle("cryX").setContentText("Rodando")
        .setSmallIcon(android.R.drawable.ic_menu_view).build()
}

class OverlayView(
    context: Context,
    private val sw: Int, private val sh: Int,
    private val mem: MemoryReader
) : View(context) {

    private val renderer = ESPRenderer(sw, sh)
    private val aimbot   = AimbotHelper(sw, sh)

    override fun onDraw(canvas: Canvas) {
        if (!mem.attached) return
        val vm       = mem.readViewMatrix()
        val localPos = mem.readLocalPos()
        val enemies  = mem.readEnemies(0)

        for (e in enemies) {
            val headScr = e.bones?.let { renderer.worldToScreen(vm, it.head) }
                          ?: renderer.worldToScreen(vm, e.position) ?: continue
            val feetScr = renderer.worldToScreen(vm, e.position) ?: continue

            if (OverlayService.espEnabled) {
                renderer.drawBox(canvas, headScr, feetScr)
                renderer.drawDistance(canvas, headScr, localPos.distanceTo(e.position))
            }
            if (OverlayService.skeletonEnabled && e.bones != null)
                renderer.drawSkeleton(canvas, vm, e.bones)
            if (OverlayService.hpEnabled)
                renderer.drawHP(canvas, headScr, e.hp, e.maxHp)
        }

        if (OverlayService.aimbotEnabled)
            aimbot.findTarget(enemies, vm, renderer)?.let { (_, pt) ->
                renderer.drawAimbotDot(canvas, pt)
            }
    }
}

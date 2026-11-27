package com.cryx

import android.graphics.PointF
import kotlin.math.sqrt

class AimbotHelper(screenW: Int, screenH: Int) {

    var fovRadius = 150f
    var smoothing = 0.12f

    private val centerX = screenW / 2f
    private val centerY = screenH / 2f

    fun findTarget(enemies: List<Enemy>, viewMatrix: FloatArray, renderer: ESPRenderer): Pair<Enemy, PointF>? {
        var best: Pair<Enemy, PointF>? = null
        var minDist = Float.MAX_VALUE
        for (enemy in enemies) {
            if (!enemy.isAlive || enemy.hp <= 0) continue
            val target = enemy.bones?.neck ?: enemy.position
            val screen = renderer.worldToScreen(viewMatrix, target) ?: continue
            val dx = screen.x - centerX; val dy = screen.y - centerY
            val dist = sqrt(dx*dx + dy*dy)
            if (dist < fovRadius && dist < minDist) { minDist = dist; best = enemy to screen }
        }
        return best
    }

    fun smooth(target: PointF, cx: Float, cy: Float) = PointF(
        cx + (target.x - cx) * smoothing,
        cy + (target.y - cy) * smoothing
    )
}

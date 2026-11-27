package com.cryx

import android.graphics.*

class ESPRenderer(private val screenW: Int, private val screenH: Int) {

    private val paintSkel = Paint().apply {
        color = Color.RED; strokeWidth = 2.5f; style = Paint.Style.STROKE; isAntiAlias = true
    }
    private val paintBox = Paint().apply {
        color = Color.argb(220, 220, 30, 30); strokeWidth = 1.8f; style = Paint.Style.STROKE; isAntiAlias = true
    }
    private val paintHpBg = Paint().apply { color = Color.argb(180, 50, 0, 0); style = Paint.Style.FILL }
    private val paintHp   = Paint().apply { color = Color.RED; style = Paint.Style.FILL }
    private val paintTxt  = Paint().apply {
        color = Color.WHITE; textSize = 24f; typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true; setShadowLayer(3f, 1f, 1f, Color.BLACK)
    }
    private val paintDot  = Paint().apply { color = Color.RED; style = Paint.Style.FILL }

    fun worldToScreen(viewMatrix: FloatArray, world: Vec3): PointF? {
        val w = viewMatrix[12]*world.x + viewMatrix[13]*world.y + viewMatrix[14]*world.z + viewMatrix[15]
        if (w < 0.01f) return null
        val x = viewMatrix[0]*world.x + viewMatrix[1]*world.y + viewMatrix[2]*world.z + viewMatrix[3]
        val y = viewMatrix[4]*world.x + viewMatrix[5]*world.y + viewMatrix[6]*world.z + viewMatrix[7]
        return PointF((1f + x/w)*screenW/2f, (1f - y/w)*screenH/2f)
    }

    private fun line(c: Canvas, a: PointF?, b: PointF?) {
        if (a != null && b != null) c.drawLine(a.x, a.y, b.x, b.y, paintSkel)
    }

    fun drawSkeleton(canvas: Canvas, vm: FloatArray, b: BoneSet) {
        val head=worldToScreen(vm,b.head); val neck=worldToScreen(vm,b.neck)
        val chest=worldToScreen(vm,b.chest); val pelvis=worldToScreen(vm,b.pelvis)
        val lS=worldToScreen(vm,b.lShoulder); val rS=worldToScreen(vm,b.rShoulder)
        val lE=worldToScreen(vm,b.lElbow); val rE=worldToScreen(vm,b.rElbow)
        val lW=worldToScreen(vm,b.lWrist); val rW=worldToScreen(vm,b.rWrist)
        val lK=worldToScreen(vm,b.lKnee); val rK=worldToScreen(vm,b.rKnee)
        val lF=worldToScreen(vm,b.lFoot); val rF=worldToScreen(vm,b.rFoot)
        line(canvas,head,neck); line(canvas,neck,chest); line(canvas,chest,pelvis)
        line(canvas,chest,lS); line(canvas,chest,rS)
        line(canvas,lS,lE); line(canvas,lE,lW); line(canvas,rS,rE); line(canvas,rE,rW)
        line(canvas,pelvis,lK); line(canvas,lK,lF); line(canvas,pelvis,rK); line(canvas,rK,rF)
        head?.let { canvas.drawCircle(it.x, it.y, 5f, paintDot) }
    }

    fun drawHP(canvas: Canvas, pos: PointF, hp: Int, maxHp: Int) {
        val barW=40f; val barH=5f; val left=pos.x-barW/2f; val top=pos.y-60f
        val fill=barW*(hp.toFloat()/maxHp.coerceAtLeast(1))
        canvas.drawRect(left,top,left+barW,top+barH,paintHpBg)
        canvas.drawRect(left,top,left+fill,top+barH,paintHp)
        canvas.drawText("$hp/$maxHp",left,top-4f,paintTxt)
    }

    fun drawBox(canvas: Canvas, head: PointF, feet: PointF) {
        val h=Math.abs(feet.y-head.y); val w=h*0.4f; val cx=(head.x+feet.x)/2f
        canvas.drawRect(cx-w,head.y-10f,cx+w,feet.y+10f,paintBox)
    }

    fun drawDistance(canvas: Canvas, pos: PointF, dist: Float) {
        canvas.drawText("%.0fm".format(dist),pos.x+8f,pos.y,paintTxt)
    }

    fun drawAimbotDot(canvas: Canvas, target: PointF) {
        paintDot.color = Color.argb(220,255,0,60)
        canvas.drawCircle(target.x,target.y,8f,paintDot)
        paintDot.color = Color.RED
    }
}

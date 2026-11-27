package com.cryx

import kotlin.math.sqrt

data class Vec3(val x: Float, val y: Float, val z: Float) {
    fun distanceTo(other: Vec3) = sqrt(
        (x - other.x).let { it * it } +
        (y - other.y).let { it * it } +
        (z - other.z).let { it * it }
    )
}

data class BoneSet(
    val head: Vec3,      val neck: Vec3,
    val chest: Vec3,     val pelvis: Vec3,
    val lShoulder: Vec3, val rShoulder: Vec3,
    val lElbow: Vec3,    val rElbow: Vec3,
    val lWrist: Vec3,    val rWrist: Vec3,
    val lKnee: Vec3,     val rKnee: Vec3,
    val lFoot: Vec3,     val rFoot: Vec3
)

data class Enemy(
    val address:  Long,
    val position: Vec3,
    val bones:    BoneSet?,
    val hp:       Int,
    val maxHp:    Int,
    val isAlive:  Boolean,
    val teamId:   Int
)

object Offsets {
    // libil2cpp.so — DERIVAR POR VERSAO com IDA/Ghidra
    const val ENTITY_LIST     = 0x0L
    const val ENTITY_COUNT    = 0x0L
    const val ENTITY_STRIDE   = 0x8L
    const val OFF_POSITION_X  = 0x0L
    const val OFF_POSITION_Y  = 0x4L
    const val OFF_POSITION_Z  = 0x8L
    const val OFF_HP          = 0x0L
    const val OFF_MAX_HP      = 0x4L
    const val OFF_TEAM_ID     = 0x0L
    const val OFF_IS_ALIVE    = 0x0L
    const val OFF_BONE_PTR    = 0x0L
    const val BONE_HEAD       = 0
    const val BONE_NECK       = 1
    const val BONE_CHEST      = 2
    const val BONE_PELVIS     = 3
    const val BONE_L_SHOULDER = 4
    const val BONE_R_SHOULDER = 5
    const val BONE_L_ELBOW    = 6
    const val BONE_R_ELBOW    = 7
    const val BONE_L_WRIST    = 8
    const val BONE_R_WRIST    = 9
    const val BONE_L_KNEE     = 10
    const val BONE_R_KNEE     = 11
    const val BONE_L_FOOT     = 12
    const val BONE_R_FOOT     = 13
    const val VIEW_MATRIX     = 0x0L
    const val LOCAL_PLAYER    = 0x0L
}

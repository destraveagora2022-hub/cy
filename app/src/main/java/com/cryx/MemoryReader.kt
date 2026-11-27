package com.cryx

import android.util.Log

class MemoryReader {

    companion object {
        init { System.loadLibrary("cryx_mem") }
        private const val FF_PKG = "com.dts.freefireth"
        private const val FF_LIB = "libil2cpp.so"
    }

    external fun findPid(pkg: String): Int
    external fun attachPid(pid: Int): Boolean
    external fun getModuleBase(mod: String): Long
    external fun readFloat(addr: Long): Float
    external fun readInt(addr: Long): Int
    external fun readLong(addr: Long): Long
    external fun writeFloat(addr: Long, value: Float): Boolean
    external fun writeInt(addr: Long, value: Int): Boolean
    external fun walkChain(base: Long, offsets: LongArray): Long

    var libBase: Long = 0L
    var attached = false

    fun init(): Boolean {
        val pid = findPid(FF_PKG)
        if (pid <= 0) { Log.e("cryX", "Free Fire nao encontrado"); return false }
        if (!attachPid(pid)) { Log.e("cryX", "Falha ao anexar PID $pid"); return false }
        libBase = getModuleBase(FF_LIB)
        if (libBase == 0L) { Log.e("cryX", "Base do modulo nao encontrada"); return false }
        attached = true
        Log.i("cryX", "OK — PID=$pid base=0x${libBase.toString(16)}")
        return true
    }

    private fun readVec3(base: Long, offX: Long, offY: Long, offZ: Long) = Vec3(
        readFloat(base + offX),
        readFloat(base + offY),
        readFloat(base + offZ)
    )

    private fun readBone(bonePtr: Long, index: Int): Vec3 {
        val base = bonePtr + index * 12L
        return Vec3(readFloat(base), readFloat(base + 4), readFloat(base + 8))
    }

    fun readEnemies(localTeam: Int): List<Enemy> {
        if (!attached) return emptyList()
        val listAddr = libBase + Offsets.ENTITY_LIST
        val count    = readInt(libBase + Offsets.ENTITY_COUNT).coerceIn(0, 100)
        val enemies  = mutableListOf<Enemy>()

        for (i in 0 until count) {
            val ptr = readLong(listAddr + i * Offsets.ENTITY_STRIDE)
            if (ptr == 0L) continue
            val hp    = readInt(ptr + Offsets.OFF_HP)
            val maxHp = readInt(ptr + Offsets.OFF_MAX_HP)
            val alive = readInt(ptr + Offsets.OFF_IS_ALIVE) != 0
            val team  = readInt(ptr + Offsets.OFF_TEAM_ID)
            if (!alive || team == localTeam || hp <= 0) continue
            val pos = readVec3(ptr, Offsets.OFF_POSITION_X, Offsets.OFF_POSITION_Y, Offsets.OFF_POSITION_Z)
            val bonePtr = readLong(ptr + Offsets.OFF_BONE_PTR)
            val bones = if (bonePtr != 0L) BoneSet(
                readBone(bonePtr, Offsets.BONE_HEAD),   readBone(bonePtr, Offsets.BONE_NECK),
                readBone(bonePtr, Offsets.BONE_CHEST),  readBone(bonePtr, Offsets.BONE_PELVIS),
                readBone(bonePtr, Offsets.BONE_L_SHOULDER), readBone(bonePtr, Offsets.BONE_R_SHOULDER),
                readBone(bonePtr, Offsets.BONE_L_ELBOW),    readBone(bonePtr, Offsets.BONE_R_ELBOW),
                readBone(bonePtr, Offsets.BONE_L_WRIST),    readBone(bonePtr, Offsets.BONE_R_WRIST),
                readBone(bonePtr, Offsets.BONE_L_KNEE),     readBone(bonePtr, Offsets.BONE_R_KNEE),
                readBone(bonePtr, Offsets.BONE_L_FOOT),     readBone(bonePtr, Offsets.BONE_R_FOOT)
            ) else null
            enemies.add(Enemy(ptr, pos, bones, hp, maxHp, alive, team))
        }
        return enemies
    }

    fun readLocalPos(): Vec3 {
        val p = readLong(libBase + Offsets.LOCAL_PLAYER)
        if (p == 0L) return Vec3(0f, 0f, 0f)
        return readVec3(p, Offsets.OFF_POSITION_X, Offsets.OFF_POSITION_Y, Offsets.OFF_POSITION_Z)
    }

    fun readViewMatrix(): FloatArray {
        val mat = FloatArray(16)
        val base = libBase + Offsets.VIEW_MATRIX
        for (i in 0..15) mat[i] = readFloat(base + i * 4L)
        return mat
    }
}

package com.zayk.engine

class InputState {
    @Volatile var moveX = 0f
    @Volatile var moveY = 0f
    @Volatile var vert = 0f

    private var lookX = 0f
    private var lookY = 0f
    private val lock = Any()

    fun addLook(dx: Float, dy: Float) {
        synchronized(lock) {
            lookX += dx
            lookY += dy
        }
    }

    fun consumeLook(): Pair<Float, Float> {
        synchronized(lock) {
            val r = Pair(lookX, lookY)
            lookX = 0f
            lookY = 0f
            return r
        }
    }
}
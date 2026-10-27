package com.zayk.engine

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import kotlin.math.hypot

/** Joystick virtual na metade esquerda (mover) e arrastar na direita (olhar). */
class ZaykGLView(context: Context, val renderer: ZaykRenderer) : GLSurfaceView(context) {

    private val density = resources.displayMetrics.density
    private val radius = 50f * density
    private var joyId = -1
    private var joyX0 = 0f
    private var joyY0 = 0f
    private var lookId = -1
    private var lookX = 0f
    private var lookY = 0f

    init {
        setEGLContextClientVersion(3)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val i = e.actionIndex
                val id = e.getPointerId(i)
                val x = e.getX(i)
                val y = e.getY(i)
                if (x < width / 2f && joyId == -1) {
                    joyId = id; joyX0 = x; joyY0 = y
                } else if (x >= width / 2f && lookId == -1) {
                    lookId = id; lookX = x; lookY = y
                }
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until e.pointerCount) {
                    val id = e.getPointerId(i)
                    if (id == joyId) {
                        var dx = e.getX(i) - joyX0
                        var dy = e.getY(i) - joyY0
                        val len = hypot(dx, dy)
                        if (len > radius) {
                            dx = dx / len * radius
                            dy = dy / len * radius
                        }
                        renderer.input.moveX = dx / radius
                        renderer.input.moveY = -dy / radius
                    } else if (id == lookId) {
                        val x = e.getX(i)
                        val y = e.getY(i)
                        renderer.input.addLook((x - lookX) / density, (y - lookY) / density)
                        lookX = x
                        lookY = y
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val id = e.getPointerId(e.actionIndex)
                if (id == joyId) {
                    joyId = -1
                    renderer.input.moveX = 0f
                    renderer.input.moveY = 0f
                }
                if (id == lookId) lookId = -1
            }
            MotionEvent.ACTION_CANCEL -> {
                joyId = -1
                lookId = -1
                renderer.input.moveX = 0f
                renderer.input.moveY = 0f
            }
        }
        return true
    }
}
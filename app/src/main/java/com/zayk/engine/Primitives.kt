package com.zayk.engine

import android.opengl.GLES30 as GL
import kotlin.math.cos
import kotlin.math.sin

object Primitives {

    fun cube(): Mesh {
        // cada face: normal(3), eixo u(3), eixo w(3)
        val faces = arrayOf(
            floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f),
            floatArrayOf(-1f, 0f, 0f, 0f, 0f, 1f, 0f, 1f, 0f),
            floatArrayOf(0f, 1f, 0f, 0f, 0f, 1f, 1f, 0f, 0f),
            floatArrayOf(0f, -1f, 0f, 1f, 0f, 0f, 0f, 0f, 1f),
            floatArrayOf(0f, 0f, 1f, 1f, 0f, 0f, 0f, 1f, 0f),
            floatArrayOf(0f, 0f, -1f, 0f, 1f, 0f, 1f, 0f, 0f),
        )
        val signs = arrayOf(
            floatArrayOf(-1f, -1f), floatArrayOf(1f, -1f),
            floatArrayOf(1f, 1f), floatArrayOf(-1f, 1f),
        )
        val v = ArrayList<Float>()
        val idx = ArrayList<Short>()
        faces.forEachIndexed { f, a ->
            for (s in signs) {
                for (i in 0..2) v.add(0.5f * (a[i] + s[0] * a[3 + i] + s[1] * a[6 + i]))
                v.add(a[0]); v.add(a[1]); v.add(a[2])
            }
            val o = f * 4
            for (k in intArrayOf(0, 1, 2, 0, 2, 3)) idx.add((o + k).toShort())
        }
        return Mesh(v.toFloatArray(), idx.toShortArray())
    }

    fun plane(): Mesh {
        val v = floatArrayOf(
            -0.5f, 0f, 0.5f, 0f, 1f, 0f,
            0.5f, 0f, 0.5f, 0f, 1f, 0f,
            0.5f, 0f, -0.5f, 0f, 1f, 0f,
            -0.5f, 0f, -0.5f, 0f, 1f, 0f,
        )
        return Mesh(v, shortArrayOf(0, 1, 2, 0, 2, 3))
    }

    fun sphere(stacks: Int = 16, slices: Int = 24): Mesh {
        val v = ArrayList<Float>()
        val idx = ArrayList<Short>()
        for (i in 0..stacks) {
            val th = i * Math.PI.toFloat() / stacks
            for (j in 0..slices) {
                val ph = j * 2f * Math.PI.toFloat() / slices
                val x = sin(th) * cos(ph)
                val y = cos(th)
                val z = sin(th) * sin(ph)
                v.add(x * 0.5f); v.add(y * 0.5f); v.add(z * 0.5f)
                v.add(x); v.add(y); v.add(z)
            }
        }
        for (i in 0 until stacks) {
            for (j in 0 until slices) {
                val a = i * (slices + 1) + j
                val b = a + slices + 1
                for (k in intArrayOf(a, a + 1, b, a + 1, b + 1, b)) idx.add(k.toShort())
            }
        }
        return Mesh(v.toFloatArray(), idx.toShortArray())
    }

    /** Grid estilo Unity: linhas menores, linhas maiores a cada 10 e eixos coloridos. */
    fun grid(half: Int = 100): Mesh {
        val minor = floatArrayOf(0.33f, 0.33f, 0.36f)
        val major = floatArrayOf(0.52f, 0.52f, 0.55f)
        val v = ArrayList<Float>()
        fun line(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float, c: FloatArray) {
            v.add(x1); v.add(y1); v.add(z1); v.add(c[0]); v.add(c[1]); v.add(c[2])
            v.add(x2); v.add(y2); v.add(z2); v.add(c[0]); v.add(c[1]); v.add(c[2])
        }
        val h = half.toFloat()
        for (i in -half..half) {
            if (i == 0) continue
            val c = if (i % 10 == 0) major else minor
            val f = i.toFloat()
            line(f, 0f, -h, f, 0f, h, c)
            line(-h, 0f, f, h, 0f, f, c)
        }
        line(-h, 0f, 0f, h, 0f, 0f, floatArrayOf(0.9f, 0.25f, 0.25f))  // eixo X
        line(0f, 0f, -h, 0f, 0f, h, floatArrayOf(0.3f, 0.45f, 0.98f))  // eixo Z
        line(0f, 0f, 0f, 0f, 5f, 0f, floatArrayOf(0.3f, 0.85f, 0.35f)) // eixo Y
        val n = v.size / 6
        return Mesh(v.toFloatArray(), ShortArray(n) { it.toShort() }, GL.GL_LINES)
    }
}
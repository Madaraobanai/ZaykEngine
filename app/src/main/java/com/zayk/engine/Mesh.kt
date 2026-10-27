package com.zayk.engine

import android.opengl.GLES30 as GL
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** vertices: [x,y,z, a,b,c] intercalado (normal ou cor); indices em Short. */
class Mesh(vertices: FloatArray, indices: ShortArray, private val mode: Int = GL.GL_TRIANGLES) {
    private val vao: Int
    private val count = indices.size

    init {
        val vaoArr = IntArray(1)
        GL.glGenVertexArrays(1, vaoArr, 0)
        vao = vaoArr[0]
        val bufs = IntArray(2)
        GL.glGenBuffers(2, bufs, 0)

        GL.glBindVertexArray(vao)

        val vb = ByteBuffer.allocateDirect(vertices.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        vb.put(vertices).position(0)
        GL.glBindBuffer(GL.GL_ARRAY_BUFFER, bufs[0])
        GL.glBufferData(GL.GL_ARRAY_BUFFER, vertices.size * 4, vb, GL.GL_STATIC_DRAW)
        GL.glEnableVertexAttribArray(0)
        GL.glVertexAttribPointer(0, 3, GL.GL_FLOAT, false, 24, 0)
        GL.glEnableVertexAttribArray(1)
        GL.glVertexAttribPointer(1, 3, GL.GL_FLOAT, false, 24, 12)

        val ib = ByteBuffer.allocateDirect(indices.size * 2).order(ByteOrder.nativeOrder()).asShortBuffer()
        ib.put(indices).position(0)
        GL.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, bufs[1])
        GL.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, indices.size * 2, ib, GL.GL_STATIC_DRAW)

        GL.glBindVertexArray(0)
    }

    fun draw() {
        GL.glBindVertexArray(vao)
        GL.glDrawElements(mode, count, GL.GL_UNSIGNED_SHORT, 0)
    }
}
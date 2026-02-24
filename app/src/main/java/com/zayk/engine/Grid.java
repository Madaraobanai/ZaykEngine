package com.zayk.engine;

import android.opengl.GLES20;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class Grid {
    private FloatBuffer vertexBuffer;
    private int mProgram;
    private float[] gridCoords;
    private int vertexCount;

    public Grid() {
        int size = 40;
        gridCoords = new float[(size + 1) * 4 * 3];
        int index = 0;
        for (int i = -size / 2; i <= size / 2; i++) {
            gridCoords[index++] = i; gridCoords[index++] = 0; gridCoords[index++] = -size / 2f;
            gridCoords[index++] = i; gridCoords[index++] = 0; gridCoords[index++] = size / 2f;
            gridCoords[index++] = -size / 2f; gridCoords[index++] = 0; gridCoords[index++] = i;
            gridCoords[index++] = size / 2f;  gridCoords[index++] = 0; gridCoords[index++] = i;
        }
        vertexCount = gridCoords.length / 3;
    }

    public void setup() {
        ByteBuffer bb = ByteBuffer.allocateDirect(gridCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(gridCoords);
        vertexBuffer.position(0);

        // Compilação simples de shader para a grade
        int vs = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        GLES20.glShaderSource(vs, "uniform mat4 uVPMatrix; attribute vec4 vPosition; void main() { gl_Position = uVPMatrix * vPosition; }");
        GLES20.glCompileShader(vs);

        int fs = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        GLES20.glShaderSource(fs, "precision mediump float; void main() { gl_FragColor = vec4(0.5, 0.5, 0.5, 1.0); }");
        GLES20.glCompileShader(fs);

        mProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgram, vs);
        GLES20.glAttachShader(mProgram, fs);
        GLES20.glLinkProgram(mProgram);
    }

    public void draw(float[] mvpMatrix) {
        GLES20.glUseProgram(mProgram);
        int matrixHandle = GLES20.glGetUniformLocation(mProgram, "uVPMatrix");
        GLES20.glUniformMatrix4fv(matrixHandle, 1, false, mvpMatrix, 0);

        int posHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        GLES20.glEnableVertexAttribArray(posHandle);
        GLES20.glVertexAttribPointer(posHandle, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer);

        GLES20.glDrawArrays(GLES20.GL_LINES, 0, vertexCount);
        GLES20.glDisableVertexAttribArray(posHandle);
    }

    public void addBlock(int x, int y, int z, int type) {}
    public void clear() {}
}

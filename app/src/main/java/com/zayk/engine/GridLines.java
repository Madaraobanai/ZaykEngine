package com.zayk.engine;

import android.opengl.GLES20;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class GridLines {
    private FloatBuffer vertexBuffer;
    private int vertexCount;

    public GridLines(int size, int step) {
        int numLines = (size / step) * 2 + 1;
        int totalLines = numLines * 2; // Eixo X e Eixo Z
        vertexCount = totalLines * 2; // 2 pontos por linha
        
        float[] coords = new float[vertexCount * 3];
        int index = 0;

        for (int i = -size; i <= size; i += step) {
            // Linhas verticais (Z)
            coords[index++] = i; coords[index++] = 0; coords[index++] = -size;
            coords[index++] = i; coords[index++] = 0; coords[index++] = size;

            // Linhas horizontais (X)
            coords[index++] = -size; coords[index++] = 0; coords[index++] = i;
            coords[index++] = size;  coords[index++] = 0; coords[index++] = i;
        }

        ByteBuffer bb = ByteBuffer.allocateDirect(coords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(coords);
        vertexBuffer.position(0);
    }

    public void draw(int positionHandle, int colorHandle) {
        // Cor cinza para a grade
        GLES20.glUniform4f(colorHandle, 0.4f, 0.4f, 0.4f, 1.0f);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, vertexCount);
    }
}

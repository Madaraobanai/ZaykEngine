package com.zayk.engine;

import android.opengl.GLES20;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class Grid {
    private FloatBuffer lineBuffer, floorBuffer;
    private int mProgram;
    private int lineCount;

    public Grid() {
        int size = 120;
        float[] lines = new float[(size + 1) * 4 * 3];
        int idx = 0;
        for (int i = -size / 2; i <= size / 2; i++) {
            lines[idx++] = i; lines[idx++] = 0; lines[idx++] = -size / 2f;
            lines[idx++] = i; lines[idx++] = 0; lines[idx++] = size / 2f;
            lines[idx++] = -size / 2f; lines[idx++] = 0; lines[idx++] = i;
            lines[idx++] = size / 2f;  lines[idx++] = 0; lines[idx++] = i;
        }
        lineCount = lines.length / 3;
        lineBuffer = createBuffer(lines);

        float s = size / 2f;
        float[] floor = {-s,-0.05f,-s, s,-0.05f,-s, -s,-0.05f,s, -s,-0.05f,s, s,-0.05f,-s, s,-0.05f,s};
        floorBuffer = createBuffer(floor);
    }

    private FloatBuffer createBuffer(float[] data) {
        ByteBuffer bb = ByteBuffer.allocateDirect(data.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(data); fb.position(0);
        return fb;
    }

    public void setup() {
        String vs = 
            "uniform mat4 uVPMatrix; attribute vec4 vPosition; varying float vDist; " +
            "void main() { gl_Position = uVPMatrix * vPosition; vDist = gl_Position.z; }";
        
        String fs = 
            "precision mediump float; uniform vec4 uColor; uniform vec4 uFogColor; " +
            "uniform float uFogStart; uniform float uFogEnd; uniform int uFogEnabled; " +
            "varying float vDist; " +
            "void main() { " +
            "  vec4 finalColor = uColor; " +
            "  if(uFogEnabled == 1) { " +
            "    float fogFactor = clamp((vDist - uFogStart) / (uFogEnd - uFogStart), 0.0, 1.0); " +
            "    finalColor = mix(uColor, uFogColor, fogFactor); " +
            "  } " +
            "  gl_FragColor = finalColor; " +
            "}";

        int vS = loadShader(GLES20.GL_VERTEX_SHADER, vs);
        int fS = loadShader(GLES20.GL_FRAGMENT_SHADER, fs);
        mProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgram, vS); GLES20.glAttachShader(mProgram, fS);
        GLES20.glLinkProgram(mProgram);
    }

    public void draw(float[] mvpMatrix, Fog fogSystem) {
        GLES20.glUseProgram(mProgram);
        int mH = GLES20.glGetUniformLocation(mProgram, "uVPMatrix");
        int cH = GLES20.glGetUniformLocation(mProgram, "uColor");
        int pH = GLES20.glGetAttribLocation(mProgram, "vPosition");

        GLES20.glUniformMatrix4fv(mH, 1, false, mvpMatrix, 0);
        
        // Aplica o sistema de neblina separado
        fogSystem.apply(mProgram);

        GLES20.glEnableVertexAttribArray(pH);
        GLES20.glVertexAttribPointer(pH, 3, GLES20.GL_FLOAT, false, 0, floorBuffer);
        GLES20.glUniform4f(cH, 0.25f, 0.25f, 0.25f, 1.0f); 
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);

        GLES20.glVertexAttribPointer(pH, 3, GLES20.GL_FLOAT, false, 0, lineBuffer);
        GLES20.glUniform4f(cH, 0.45f, 0.45f, 0.45f, 1.0f); 
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, lineCount);
    }

    private int loadShader(int type, String code) {
        int s = GLES20.glCreateShader(type);
        GLES20.glShaderSource(s, code); GLES20.glCompileShader(s);
        return s;
    }
}

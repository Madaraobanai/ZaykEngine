package com.zayk.engine;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class ZaykRenderer implements GLSurfaceView.Renderer {
    public static float camX=0, camY=2.0f, camZ=5.0f, camPitch=0, camYaw=0;
    private LuaManager luaManager;
    private Grid grid;
    private GridLines gridVisual;
    private final float[] vPMatrix = new float[16], projectionMatrix = new float[16], viewMatrix = new float[16], modelMatrix = new float[16];
    private FloatBuffer cubeBuffer, groundBuffer;
    private ShortBuffer drawListBuffer;
    private int mProgram;
    private final float[] skyColor = {0.44f, 0.54f, 0.65f, 1.0f}, groundColor = {0.22f, 0.22f, 0.22f, 1.0f};

    public ZaykRenderer(Context context, Grid grid) { this.grid = grid; }
    public void setLuaManager(LuaManager lm) { this.luaManager = lm; }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        gridVisual = new GridLines(400, 1);
        initShapes();
        String v = "uniform mat4 uMVPMatrix; attribute vec4 vPosition; varying float vDist; void main() { gl_Position = uMVPMatrix * vPosition; vDist = gl_Position.z; }";
        String f = "precision mediump float; uniform vec4 uColor; uniform vec4 uFogColor; varying float vDist; void main() { float fog = clamp((vDist-10.0)/85.0, 0.0, 1.0); gl_FragColor = mix(uColor, uFogColor, fog); }";
        int vs = loadShader(GLES20.GL_VERTEX_SHADER, v);
        int fs = loadShader(GLES20.GL_FRAGMENT_SHADER, f);
        mProgram = GLES20.glCreateProgram(); GLES20.glAttachShader(mProgram, vs); GLES20.glAttachShader(mProgram, fs); GLES20.glLinkProgram(mProgram);
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        if (luaManager != null) luaManager.callUpdate();
        GLES20.glClearColor(skyColor[0], skyColor[1], skyColor[2], skyColor[3]);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        Matrix.setIdentityM(viewMatrix, 0);
        Matrix.rotateM(viewMatrix, 0, camPitch, 1, 0, 0);
        Matrix.rotateM(viewMatrix, 0, camYaw, 0, 1, 0);
        Matrix.translateM(viewMatrix, 0, -camX, -camY, -camZ);
        Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, viewMatrix, 0);

        GLES20.glUseProgram(mProgram);
        int posH = GLES20.glGetAttribLocation(mProgram, "vPosition");
        int colH = GLES20.glGetUniformLocation(mProgram, "uColor");
        int fogH = GLES20.glGetUniformLocation(mProgram, "uFogColor");
        int mvpH = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        GLES20.glEnableVertexAttribArray(posH);
        GLES20.glUniform4fv(fogH, 1, skyColor, 0);

        // Ground Plane (Chão infinito cinza)
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, camX, -0.01f, camZ);
        draw(groundBuffer, groundColor, 4, GLES20.GL_TRIANGLE_FAN, posH, colH, mvpH, false);

        // Grid
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, (float)Math.floor(camX), 0, (float)Math.floor(camZ));
        float[] gMVP = new float[16]; Matrix.multiplyMM(gMVP, 0, vPMatrix, 0, modelMatrix, 0);
        GLES20.glUniformMatrix4fv(mvpH, 1, false, gMVP, 0);
        GLES20.glUniform4f(colH, 0.5f, 0.5f, 0.5f, 1.0f);
        if(gridVisual != null) gridVisual.draw(posH, colH);

        // Blocos
        synchronized(grid.getBlocks()) {
            for(Grid.Block b : grid.getBlocks()) {
                Matrix.setIdentityM(modelMatrix, 0);
                Matrix.translateM(modelMatrix, 0, b.x, b.y, b.z);
                draw(cubeBuffer, new float[]{0.9f, 0.9f, 0.9f, 1.0f}, 36, GLES20.GL_TRIANGLES, posH, colH, mvpH, true);
            }
        }
    }

    private void draw(FloatBuffer b, float[] c, int count, int mode, int p, int cl, int m, boolean isCube) {
        float[] mvp = new float[16]; Matrix.multiplyMM(mvp, 0, vPMatrix, 0, modelMatrix, 0);
        GLES20.glUniformMatrix4fv(m, 1, false, mvp, 0);
        GLES20.glUniform4fv(cl, 1, c, 0);
        GLES20.glVertexAttribPointer(p, 3, GLES20.GL_FLOAT, false, 12, b);
        if(isCube) GLES20.glDrawElements(mode, count, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);
        else GLES20.glDrawArrays(mode, 0, count);
    }

    private void initShapes() {
        float[] coords = {-0.5f,0.5f,0.5f, -0.5f,-0.5f,0.5f, 0.5f,-0.5f,0.5f, 0.5f,0.5f,0.5f, -0.5f,0.5f,-0.5f, -0.5f,-0.5f,-0.5f, 0.5f,-0.5f,-0.5f, 0.5f,0.5f,-0.5f};
        short[] order = {0,1,2,0,2,3, 4,5,6,4,6,7, 0,4,7,0,7,3, 1,5,6,1,6,2, 0,4,5,0,5,1, 3,7,6,3,6,2};
        cubeBuffer = createFloatBuffer(coords);
        drawListBuffer = ByteBuffer.allocateDirect(order.length*2).order(ByteOrder.nativeOrder()).asShortBuffer().put(order);
        drawListBuffer.position(0);
        float s = 500f; float[] g = {-s,0,s, -s,0,-s, s,0,-s, s,0,s};
        groundBuffer = createFloatBuffer(g);
    }

    private FloatBuffer createFloatBuffer(float[] arr) {
        FloatBuffer b = ByteBuffer.allocateDirect(arr.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(arr);
        b.position(0); return b;
    }

    private int loadShader(int type, String code) {
        int s = GLES20.glCreateShader(type); GLES20.glShaderSource(s, code); GLES20.glCompileShader(s); return s;
    }

    @Override public void onSurfaceChanged(GL10 u, int w, int h) {
        GLES20.glViewport(0,0,w,h);
        Matrix.perspectiveM(projectionMatrix, 0, 60, (float)w/h, 0.1f, 1000f);
    }
}

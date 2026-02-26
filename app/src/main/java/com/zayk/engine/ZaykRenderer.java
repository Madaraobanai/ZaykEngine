package com.zayk.engine;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.view.MotionEvent;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class ZaykRenderer implements GLSurfaceView.Renderer {

    private final Grid mGrid;
    private final LuaManager luaManager;
    private final Fog fogSystem = new Fog(); 

    public static float camX = 0, camY = 5, camZ = 15;
    public static float camPitch = 0, camYaw = 0;
    public static float touchDX = 0, touchDY = 0, moveDX = 0, moveDY = 0;   
    public static boolean isLeftActive = false, isRightActive = false;

    private int leftPointerId = -1, rightPointerId = -1;
    private float startLeftX, startLeftY, lastRightX, lastRightY;

    private float skyR = 0.44f;
    private float skyG = 0.57f;
    private float skyB = 0.75f;

    private final float[] vPMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private int viewportWidth, viewportHeight;

    public ZaykRenderer(Context context, Grid grid) {
        this.mGrid = grid;
        this.luaManager = new LuaManager(context, this);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        if (mGrid != null) mGrid.setup();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        this.viewportWidth = width; 
        this.viewportHeight = height;
        float ratio = (float) width / height;
        Matrix.perspectiveM(projectionMatrix, 0, 45, ratio, 0.1f, 1000.0f);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClearColor(skyR, skyG, skyB, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        fogSystem.color[0] = skyR;
        fogSystem.color[1] = skyG;
        fogSystem.color[2] = skyB;

        if (luaManager != null) luaManager.update();

        Matrix.setIdentityM(viewMatrix, 0);
        Matrix.rotateM(viewMatrix, 0, camPitch, 1, 0, 0);
        Matrix.rotateM(viewMatrix, 0, camYaw, 0, 1, 0);
        Matrix.translateM(viewMatrix, 0, -camX, -camY, -camZ);

        Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
        
        if (mGrid != null) mGrid.draw(vPMatrix, fogSystem);

        touchDX *= 0.6f; 
        touchDY *= 0.6f;
    }

    public void setSkyColor(float r, float g, float b) {
        this.skyR = r;
        this.skyG = g;
        this.skyB = b;
    }

    public void setFogParams(float start, float end, boolean enabled) {
        fogSystem.start = start;
        fogSystem.end = end;
        fogSystem.enabled = enabled;
    }

    // REMOVIDO O @Override DAQUI:
    public void onTouchEvent(MotionEvent e) {
        int action = e.getActionMasked();
        int index = e.getActionIndex();
        int id = e.getPointerId(index);
        float x = e.getX(index);
        float y = e.getY(index);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                if (x < viewportWidth / 2f && leftPointerId == -1) {
                    leftPointerId = id;
                    startLeftX = x;
                    startLeftY = y;
                    isLeftActive = true;
                } else if (x >= viewportWidth / 2f && rightPointerId == -1) {
                    rightPointerId = id;
                    lastRightX = x;
                    lastRightY = y;
                    isRightActive = true;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < e.getPointerCount(); i++) {
                    int pId = e.getPointerId(i);
                    float px = e.getX(i);
                    float py = e.getY(i);

                    if (pId == leftPointerId) {
                        moveDX = px - startLeftX;
                        moveDY = py - startLeftY;
                    } else if (pId == rightPointerId) {
                        touchDX = px - lastRightX;
                        touchDY = py - lastRightY;
                        lastRightX = px;
                        lastRightY = py;
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
                if (id == leftPointerId) {
                    leftPointerId = -1;
                    isLeftActive = false;
                    moveDX = 0; moveDY = 0;
                } else if (id == rightPointerId) {
                    rightPointerId = -1;
                    isRightActive = false;
                    touchDX = 0; touchDY = 0;
                }
                break;
        }
    }
}

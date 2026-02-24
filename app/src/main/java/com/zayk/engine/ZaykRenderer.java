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

    // Variáveis da Câmara (Sincronizadas com Lua)
    public static float camX = 0, camY = 5, camZ = 15;
    public static float camPitch = 0, camYaw = 0;

    // Inputs para o Lua
    public static float touchDX = 0, touchDY = 0; // Delta para Olhar (Direita)
    public static float moveDX = 0, moveDY = 0;   // Vetor constante para Andar (Esquerda)
    public static boolean isLeftActive = false;
    public static boolean isRightActive = false;

    // Controle de Multitoque e Joystick
    private int leftPointerId = -1;
    private int rightPointerId = -1;
    private float startLeftX, startLeftY;
    private float lastRightX, lastRightY;

    // Matrizes de Projeção
    private final float[] vPMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];

    private int screenWidth;

    public ZaykRenderer(Context context, Grid grid) {
        this.mGrid = grid;
        this.luaManager = new LuaManager(context);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.1f, 0.1f, 0.12f, 1.0f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        if (mGrid != null) mGrid.setup();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        this.screenWidth = width;
        float ratio = (float) width / height;
        Matrix.perspectiveM(projectionMatrix, 0, 45, ratio, 0.1f, 1000.0f);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // 1. O Lua processa a lógica de voo contínuo
        if (luaManager != null) {
            luaManager.update();
        }

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // 2. Montagem da Câmera (Ordem correta para Drone/FPS)
        Matrix.setIdentityM(viewMatrix, 0);
        Matrix.rotateM(viewMatrix, 0, camPitch, 1, 0, 0);
        Matrix.rotateM(viewMatrix, 0, camYaw, 0, 1, 0);
        Matrix.translateM(viewMatrix, 0, -camX, -camY, -camZ);

        Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
        
        if (mGrid != null) {
            mGrid.draw(vPMatrix);
        }

        // 3. Reset suave do Delta de rotação (apenas para o olhar não ficar infinito)
        // Nota: moveDX/DY NÃO são resetados aqui para manter o movimento contínuo
        touchDX *= 0.5f;
        touchDY *= 0.5f;
    }

    public void onTouchEvent(MotionEvent e) {
        int action = e.getActionMasked();
        int index = e.getActionIndex();
        int id = e.getPointerId(index);
        float x = e.getX(index);
        float y = e.getY(index);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                if (x < screenWidth / 2f && leftPointerId == -1) {
                    leftPointerId = id;
                    startLeftX = x; // Define o centro do Joystick
                    startLeftY = y;
                    isLeftActive = true;
                } else if (x >= screenWidth / 2f && rightPointerId == -1) {
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
                        // Calcula a distância do centro (Joystick Virtual)
                        moveDX = px - startLeftX;
                        moveDY = py - startLeftY;
                    } else if (pId == rightPointerId) {
                        // Calcula o deslocamento frame-a-frame (Olhar)
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
                    moveDX = 0; moveDY = 0; // Para o drone ao soltar
                } else if (id == rightPointerId) {
                    rightPointerId = -1;
                    isRightActive = false;
                    touchDX = 0; touchDY = 0;
                }
                break;
        }
    }
}

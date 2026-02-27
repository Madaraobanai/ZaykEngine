package com.zayk.engine;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.view.MotionEvent;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.util.ArrayList;
import java.util.List;

public class ZaykRenderer implements GLSurfaceView.Renderer {

    private final Grid mGrid;
    private final LuaManager luaManager;
    private final Fog fogSystem = new Fog(); 
    private final Context context;

    // Lista de modelos já carregados
    private final List<ModelData> modelList = new ArrayList<>();
    
    // FILA DE CARREGAMENTO: Para evitar erros de Thread entre UI e OpenGL
    private final List<String[]> pendingModels = new ArrayList<>();

    // Variáveis da Câmera
    public static float camX = 0, camY = 5, camZ = 15;
    public static float camPitch = 0, camYaw = 0;
    
    // Variáveis de Input
    public static float touchDX = 0, touchDY = 0, moveDX = 0, moveDY = 0;   
    public static boolean isLeftActive = false, isRightActive = false;

    private int leftPointerId = -1, rightPointerId = -1;
    private float startLeftX, startLeftY, lastRightX, lastRightY;

    // Ambiente
    private float skyR = 0.44f;
    private float skyG = 0.57f;
    private float skyB = 0.75f;

    private final float[] vPMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private int viewportWidth, viewportHeight;

    private static class ModelData {
        float x, y, z;
        Model3D model;
        ModelData(float x, float y, float z, Model3D m) {
            this.x = x; this.y = y; this.z = z; this.model = m;
        }
    }

    public ZaykRenderer(Context context, Grid grid) {
        this.context = context;
        this.mGrid = grid;
        this.luaManager = new LuaManager(context, this);
    }

    /**
     * Converte o toque da tela em coordenadas 3D no plano do chão (Y=0)
     */
    public float[] screenToWorld(float screenX, float screenY) {
        float[] viewProjMatrix = new float[16];
        float[] invViewProjMatrix = new float[16];
        
        Matrix.multiplyMM(viewProjMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
        Matrix.invertM(invViewProjMatrix, 0, viewProjMatrix, 0);

        float x = (2.0f * screenX) / viewportWidth - 1.0f;
        float y = 1.0f - (2.0f * screenY) / viewportHeight;

        float[] nearPoint = {x, y, -1.0f, 1.0f};
        float[] farPoint = {x, y, 1.0f, 1.0f};

        float[] nearResult = new float[4];
        float[] farResult = new float[4];

        Matrix.multiplyMV(nearResult, 0, invViewProjMatrix, 0, nearPoint, 0);
        Matrix.multiplyMV(farResult, 0, invViewProjMatrix, 0, farPoint, 0);

        for(int i=0; i<3; i++) {
            nearResult[i] /= nearResult[3];
            farResult[i] /= farResult[3];
        }

        float t = -nearResult[1] / (farResult[1] - nearResult[1]);
        float worldX = nearResult[0] + t * (farResult[0] - nearResult[0]);
        float worldZ = nearResult[2] + t * (farResult[2] - nearResult[2]);

        return new float[]{worldX, 0, worldZ};
    }

    /**
     * Adiciona o modelo na fila para ser carregado pelo onDrawFrame
     */
    public void spawnModel(String path, float x, float y, float z) {
        synchronized (pendingModels) {
            pendingModels.add(new String[]{path, String.valueOf(x), String.valueOf(y), String.valueOf(z)});
        }
    }

    public void clearMap() {
        synchronized (modelList) {
            modelList.clear();
        }
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
        // --- PROCESSAR CARREGAMENTOS PENDENTES (Dentro da GLThread) ---
        synchronized (pendingModels) {
            if (!pendingModels.isEmpty()) {
                for (String[] data : pendingModels) {
                    try {
                        String path = data[0];
                        float x = Float.parseFloat(data[1]);
                        float y = Float.parseFloat(data[2]);
                        float z = Float.parseFloat(data[3]);
                        
                        Model3D newModel = new Model3D(path); 
                        synchronized (modelList) {
                            modelList.add(new ModelData(x, y, z, newModel));
                        }
                    } catch (Exception e) {
                        android.util.Log.e("ZaykEngine", "Erro ao carregar modelo: " + e.getMessage());
                    }
                }
                pendingModels.clear();
            }
        }

        // --- RENDERIZAÇÃO ---
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

        synchronized (modelList) {
            for (ModelData d : modelList) {
                float[] mMatrix = new float[16];
                Matrix.setIdentityM(mMatrix, 0);
                Matrix.translateM(mMatrix, 0, d.x, d.y, d.z);
                
                float[] mvp = new float[16];
                Matrix.multiplyMM(mvp, 0, vPMatrix, 0, mMatrix, 0);
                d.model.draw(mvp, new float[]{0.9f, 0.9f, 0.9f, 1.0f});
            }
        }

        touchDX *= 0.6f; 
        touchDY *= 0.6f;
    }

    public void setSkyColor(float r, float g, float b) {
        this.skyR = r; this.skyG = g; this.skyB = b;
    }

    public void setFogParams(float start, float end, boolean enabled) {
        fogSystem.start = start; fogSystem.end = end; fogSystem.enabled = enabled;
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
                if (x < viewportWidth / 2f && leftPointerId == -1) {
                    leftPointerId = id; startLeftX = x; startLeftY = y; isLeftActive = true;
                } else if (x >= viewportWidth / 2f && rightPointerId == -1) {
                    rightPointerId = id; lastRightX = x; lastRightY = y; isRightActive = true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < e.getPointerCount(); i++) {
                    int pId = e.getPointerId(i);
                    float px = e.getX(i); float py = e.getY(i);
                    if (pId == leftPointerId) { moveDX = px - startLeftX; moveDY = py - startLeftY; }
                    else if (pId == rightPointerId) { touchDX = px - lastRightX; touchDY = py - lastRightY; lastRightX = px; lastRightY = py; }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
                if (id == leftPointerId) { leftPointerId = -1; isLeftActive = false; moveDX = 0; moveDY = 0; }
                else if (id == rightPointerId) { rightPointerId = -1; isRightActive = false; touchDX = 0; touchDY = 0; }
                break;
        }
    }
}

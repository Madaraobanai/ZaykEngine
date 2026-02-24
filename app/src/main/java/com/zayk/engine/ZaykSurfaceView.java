package com.zayk.engine;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * ZaykSurfaceView - O Viewport principal do Editor.
 * Gerencia o contexto OpenGL e repassa eventos de toque para o Renderer.
 */
public class ZaykSurfaceView extends GLSurfaceView {

    private ZaykRenderer mRenderer;

    // Construtor para criação via código
    public ZaykSurfaceView(Context context) {
        super(context);
        init(context);
    }

    // CONSTRUTOR ESSENCIAL: Permite que o XML encontre esta View
    public ZaykSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        // 1. Configura o contexto para OpenGL ES 2.0
        setEGLContextClientVersion(2);

        // 2. Inicializa o Grid (Chão do Editor)
        Grid grid = new Grid();
        
        // 3. Define o Renderer (Cérebro gráfico)
        mRenderer = new ZaykRenderer(context, grid);
        setRenderer(mRenderer);

        // 4. Configura para renderizar apenas quando houver mudanças (economiza bateria)
        // Se preferir fluidez total de drone, mude para RENDERMODE_CONTINUOUSLY
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        // Repassa os eventos de toque para a lógica de multitoque do Renderer
        if (mRenderer != null) {
            mRenderer.onTouchEvent(e);
        }
        return true;
    }

    public ZaykRenderer getRenderer() {
        return mRenderer;
    }
}

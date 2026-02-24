package com.zayk.engine;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class ZaykSurfaceView extends GLSurfaceView {

    private ZaykRenderer mRenderer;

    public ZaykSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // Define a versão do OpenGL antes de qualquer coisa
        setEGLContextClientVersion(2);
    }

    // Método explícito para configurar o renderer vindo da Activity
    public void setZaykRenderer(ZaykRenderer renderer) {
        this.mRenderer = renderer;
        setRenderer(mRenderer);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mRenderer != null) {
            mRenderer.onTouchEvent(event);
        }
        return true;
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }
}

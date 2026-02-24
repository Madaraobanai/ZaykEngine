package com.zayk.engine;

import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

public class MainActivity extends Activity {

    private ZaykSurfaceView mGLView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Deixa a tela em modo Fullscreen e remove o título
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
                           WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        // 1. Inicializa a View do Motor
        mGLView = (ZaykSurfaceView) findViewById(R.id.zayk_surface_view);
        
        // 2. Cria o Grid e o Renderer (Passando 'this' como Contexto para o LuaManager)
        Grid grid = new Grid();
        ZaykRenderer renderer = new ZaykRenderer(this, grid);
        
        // 3. Conecta o Renderer à nossa View customizada
        mGLView.setZaykRenderer(renderer);

        // --- Lógica das Abas Laterais (Resizers) ---
        final View leftPanel = findViewById(R.id.left_panel);
        final View rightPanel = findViewById(R.id.right_panel);
        View leftResizer = findViewById(R.id.left_resizer);
        View rightResizer = findViewById(R.id.right_resizer);

        if (leftResizer != null) {
            leftResizer.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    int x = (int) event.getRawX();
                    // Limita o tamanho da aba para não cobrir a tela toda
                    if (x > 50 && x < getResources().getDisplayMetrics().widthPixels * 0.4) {
                        leftPanel.getLayoutParams().width = x;
                        leftPanel.requestLayout();
                    }
                }
                return true;
            });
        }

        if (rightResizer != null) {
            rightResizer.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    int screenWidth = getResources().getDisplayMetrics().widthPixels;
                    int x = screenWidth - (int) event.getRawX();
                    if (x > 50 && x < screenWidth * 0.4) {
                        rightPanel.getLayoutParams().width = x;
                        rightPanel.requestLayout();
                    }
                }
                return true;
            });
        }
    }

    // Gerenciamento de ciclo de vida para evitar travamento do OpenGL
    @Override
    protected void onPause() {
        super.onPause();
        if (mGLView != null) {
            mGLView.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mGLView != null) {
            mGLView.onResume();
        }
    }
}

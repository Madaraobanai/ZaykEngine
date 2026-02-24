package com.zayk.engine;

import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

public class MainActivity extends Activity {

    private ZaykSurfaceView mGLView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Configuração de tela cheia estilo editor
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        setContentView(R.layout.activity_main);

        mGLView = (ZaykSurfaceView) findViewById(R.id.zayk_surface_view);

        final View leftPanel = findViewById(R.id.left_panel);
        final View rightPanel = findViewById(R.id.right_panel);
        final View leftResizer = findViewById(R.id.left_resizer);
        final View rightResizer = findViewById(R.id.right_resizer);

        // --- RESIZE DINÂMICO ESQUERDO ---
        leftResizer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setBackgroundColor(0x33FFFFFF); // Brilho ao tocar
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float newX = event.getRawX();
                        // Limites: mínimo 40dp, máximo 40% da tela
                        if (newX > 100 && newX < getResources().getDisplayMetrics().widthPixels * 0.4) {
                            ViewGroup.LayoutParams params = leftPanel.getLayoutParams();
                            params.width = (int) newX;
                            leftPanel.setLayoutParams(params);
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.setBackgroundColor(0x00000000); // Remove brilho
                        return true;
                }
                return false;
            }
        });

        // --- RESIZE DINÂMICO DIREITO ---
        rightResizer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setBackgroundColor(0x33FFFFFF);
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float screenWidth = getResources().getDisplayMetrics().widthPixels;
                        float newWidth = screenWidth - event.getRawX();
                        // Limites: mínimo 40dp, máximo 40% da tela
                        if (newWidth > 100 && newWidth < screenWidth * 0.4) {
                            ViewGroup.LayoutParams params = rightPanel.getLayoutParams();
                            params.width = (int) newWidth;
                            rightPanel.setLayoutParams(params);
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.setBackgroundColor(0x00000000);
                        return true;
                }
                return false;
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGLView != null) mGLView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mGLView != null) mGLView.onResume();
    }
}

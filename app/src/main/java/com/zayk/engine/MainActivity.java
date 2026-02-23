package com.zayk.engine;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.view.MotionEvent;

public class MainActivity extends Activity {
    private GLSurfaceView gLView;
    private LuaManager luaManager;
    private Grid worldGrid;
    private int screenWidth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        screenWidth = dm.widthPixels;

        worldGrid = new Grid();
        luaManager = new LuaManager(worldGrid);

        gLView = new GLSurfaceView(this);
        gLView.setEGLContextClientVersion(2);

        ZaykRenderer renderer = new ZaykRenderer(this, worldGrid);
        renderer.setLuaManager(luaManager);

        gLView.setRenderer(renderer);
        setContentView(gLView);

        luaManager.runFile(Environment.getExternalStorageDirectory().getPath() + "/ZaykEngine/main.lua");
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        InputManager.handleInput(event, screenWidth);
        return true; // PRECISA SER TRUE
    }
}

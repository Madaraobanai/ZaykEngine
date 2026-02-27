package com.zayk.engine;

import android.content.Context;
import android.os.Environment;
import java.io.File;
import java.io.FileInputStream;
import java.util.Scanner;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.*;

public class LuaManager {
    private Globals globals;
    private LuaValue updateFunc;
    private Context context;
    private ZaykRenderer renderer;
    
    // Caminho para o script no armazenamento externo
    private String scriptPath = Environment.getExternalStorageDirectory().getPath() + "/ZaykEngine/main.lua";

    public LuaManager(Context context, ZaykRenderer renderer) {
        this.context = context;
        this.renderer = renderer;
        setupLua();
    }

    private void setupLua() {
        globals = JsePlatform.standardGlobals();
        LuaValue zaykLib = LuaValue.tableOf();
        
        // --- Zayk.getInput() ---
        zaykLib.set("getInput", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                LuaValue input = LuaValue.tableOf();
                input.set("touchDX", LuaValue.valueOf(ZaykRenderer.touchDX));
                input.set("touchDY", LuaValue.valueOf(ZaykRenderer.touchDY));
                input.set("isRightActive", LuaValue.valueOf(ZaykRenderer.isRightActive));
                input.set("moveDX", LuaValue.valueOf(ZaykRenderer.moveDX));
                input.set("moveDY", LuaValue.valueOf(ZaykRenderer.moveDY));
                input.set("isLeftActive", LuaValue.valueOf(ZaykRenderer.isLeftActive));
                return input;
            }
        });

        // --- Zayk.setCamera(x, y, z, pitch, yaw) ---
        zaykLib.set("setCamera", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                ZaykRenderer.camX = (float) args.checkdouble(1);
                ZaykRenderer.camY = (float) args.checkdouble(2);
                ZaykRenderer.camZ = (float) args.checkdouble(3);
                ZaykRenderer.camPitch = (float) args.checkdouble(4);
                ZaykRenderer.camYaw = (float) args.checkdouble(5);
                return LuaValue.NIL;
            }
        });

        // --- Zayk.setSkyColor(r, g, b) ---
        zaykLib.set("setSkyColor", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                if (renderer != null) {
                    float r = (float) args.checkdouble(1);
                    float g = (float) args.checkdouble(2);
                    float b = (float) args.checkdouble(3);
                    renderer.setSkyColor(r, g, b);
                }
                return LuaValue.NIL;
            }
        });

        // --- Zayk.spawnModel("modelo.obj", x, y, z) ---
        zaykLib.set("spawnModel", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                if (renderer != null) {
                    // CORREÇÃO AQUI: checkjstring em vez de checkjsstring
                    String path = args.checkjstring(1).toString();
                    float x = (float) args.checkdouble(2);
                    float y = (float) args.checkdouble(3);
                    float z = (float) args.checkdouble(4);
                    renderer.spawnModel(path, x, y, z);
                }
                return LuaValue.NIL;
            }
        });

        // --- Zayk.clearMap() ---
        zaykLib.set("clearMap", new ZeroArgFunction() { 
            @Override 
            public LuaValue call() { 
                if (renderer != null) renderer.clearMap();
                return NIL; 
            } 
        });

        // Funções vazias para evitar erros em scripts antigos
        zaykLib.set("setBlock", new VarArgFunction() { @Override public Varargs invoke(Varargs a) { return NIL; } });

        globals.set("Zayk", zaykLib);
        loadScript();
    }

    public void loadScript() {
        try {
            File file = new File(scriptPath);
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                android.util.Log.e("ZaykLua", "Script não encontrado em: " + scriptPath);
                return;
            }
            FileInputStream fis = new FileInputStream(file);
            Scanner s = new Scanner(fis).useDelimiter("\\A");
            String code = s.hasNext() ? s.next() : "";
            fis.close();
            
            globals.load(code).call();
            updateFunc = globals.get("update");
        } catch (Exception e) {
            android.util.Log.e("ZaykLua", "Erro ao carregar main.lua: " + e.getMessage());
        }
    }

    public void update() {
        if (updateFunc != null && !updateFunc.isnil()) {
            try {
                updateFunc.call();
            } catch (LuaError e) {
                android.util.Log.e("ZaykLua", "Erro no loop update: " + e.getMessage());
            }
        }
    }
}

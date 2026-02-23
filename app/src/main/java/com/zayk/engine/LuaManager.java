package com.zayk.engine;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.luaj.vm2.lib.LibFunction;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.Varargs;

public class LuaManager {
    private Globals globals;
    private Grid grid;

    public LuaManager(Grid grid) {
        this.grid = grid;
        globals = JsePlatform.standardGlobals();
        LuaValue zayk = LuaValue.tableOf();

        // setBlock(x, y, z, type)
        zayk.set("setBlock", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                grid.addBlock(args.checkint(1), args.checkint(2), args.checkint(3), args.checkint(4));
                return NONE;
            }
        });

        // setCamera(x, y, z, pitch, yaw) - Corrigido para VarArg
        zayk.set("setCamera", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                ZaykRenderer.camX = (float)args.checkdouble(1);
                ZaykRenderer.camY = (float)args.checkdouble(2);
                ZaykRenderer.camZ = (float)args.checkdouble(3);
                ZaykRenderer.camPitch = (float)args.checkdouble(4);
                ZaykRenderer.camYaw = (float)args.checkdouble(5);
                return NONE;
            }
        });

        // getInput() - Corrigido os símbolos joyX/joyY
        zayk.set("getInput", new LibFunction() {
            @Override
            public LuaValue call() {
                LuaValue t = LuaValue.tableOf();
                // Referenciando a classe InputManager explicitamente
                t.set("joyX", LuaValue.valueOf(InputManager.joyX));
                t.set("joyY", LuaValue.valueOf(InputManager.joyY));
                t.set("lookDX", LuaValue.valueOf(InputManager.lookDX));
                t.set("lookDY", LuaValue.valueOf(InputManager.lookDY));
                t.set("isMoving", LuaValue.valueOf(InputManager.touchingLeft));
                t.set("isLooking", LuaValue.valueOf(InputManager.touchingRight));
                
                // Limpa o delta de rotação após a leitura para não girar infinitamente
                InputManager.lookDX = 0; 
                InputManager.lookDY = 0;
                return t;
            }
        });

        zayk.set("clearMap", new LibFunction() {
            @Override
            public LuaValue call() {
                grid.clear();
                return NIL;
            }
        });

        globals.set("Zayk", zayk);
    }

    public void callUpdate() {
        LuaValue update = globals.get("update");
        if (update.isfunction()) {
            update.call();
        }
    }

    public void runFile(String path) {
        try {
            globals.loadfile(path).call();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

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
    
    // Caminho para o seu script externo (Live Coding)
    private String scriptPath = Environment.getExternalStorageDirectory().getPath() + "/ZaykEngine/main.lua";

    public LuaManager(Context context) {
        this.context = context;
        setupLua();
    }

    private void setupLua() {
        // Inicializa o ambiente padrão da Luaj
        globals = JsePlatform.standardGlobals();
        
        // Cria a biblioteca principal da Engine
        LuaValue zaykLib = LuaValue.tableOf();
        
        // --- Registro: Zayk.getInput() ---
        // Retorna todos os estados de toque e movimento para o Lua
        zaykLib.set("getInput", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                LuaValue input = LuaValue.tableOf();
                // Lado Direito (Olhar/Câmera)
                input.set("touchDX", LuaValue.valueOf(ZaykRenderer.touchDX));
                input.set("touchDY", LuaValue.valueOf(ZaykRenderer.touchDY));
                input.set("isRightActive", LuaValue.valueOf(ZaykRenderer.isRightActive));
                
                // Lado Esquerdo (Movimento/Voo)
                input.set("moveDX", LuaValue.valueOf(ZaykRenderer.moveDX));
                input.set("moveDY", LuaValue.valueOf(ZaykRenderer.moveDY));
                input.set("isLeftActive", LuaValue.valueOf(ZaykRenderer.isLeftActive));
                
                return input;
            }
        });

        // --- Registro: Zayk.setCamera(x, y, z, pitch, yaw) ---
        // Recebe os cálculos processados pelo Lua e aplica nas variáveis do Renderer
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

        // Funções de utilidade para o mapa
        zaykLib.set("clearMap", new ZeroArgFunction() { @Override public LuaValue call() { return NIL; } });
        zaykLib.set("setBlock", new VarArgFunction() { @Override public Varargs invoke(Varargs a) { return NIL; } });

        // Define o objeto global "Zayk" no ambiente Lua
        globals.set("Zayk", zaykLib);
        
        loadScript();
    }

    // Carrega ou recarrega o script externo
    public void loadScript() {
        try {
            File file = new File(scriptPath);
            if (!file.exists()) {
                // Cria diretório se não existir
                file.getParentFile().mkdirs();
                return;
            }
            
            FileInputStream fis = new FileInputStream(file);
            Scanner s = new Scanner(fis).useDelimiter("\\A");
            String code = s.hasNext() ? s.next() : "";
            fis.close();
            
            // Compila e executa o código global do script
            globals.load(code).call();
            
            // Busca a função de loop
            updateFunc = globals.get("update");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Chamado 60 vezes por segundo pelo ZaykRenderer.onDrawFrame()
    public void update() {
        if (updateFunc != null && !updateFunc.isnil()) {
            try {
                updateFunc.call();
            } catch (LuaError e) {
                // Log de erro para não travar a Engine durante o Live Coding
                e.printStackTrace();
            }
        }
    }
}

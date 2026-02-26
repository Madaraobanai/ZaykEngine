package com.zayk.engine;

import android.opengl.GLES20;

public class Fog {
    // Parâmetros que poderão ser alterados via Lua no futuro
    public float start = 10.0f;
    public float end = 60.0f;
    public float[] color = {0.44f, 0.57f, 0.75f, 1.0f};
    public boolean enabled = true;

    public void apply(int program) {
        int startHandle = GLES20.glGetUniformLocation(program, "uFogStart");
        int endHandle = GLES20.glGetUniformLocation(program, "uFogEnd");
        int colorHandle = GLES20.glGetUniformLocation(program, "uFogColor");
        int enabledHandle = GLES20.glGetUniformLocation(program, "uFogEnabled");

        GLES20.glUniform1f(startHandle, start);
        GLES20.glUniform1f(endHandle, end);
        GLES20.glUniform4fv(colorHandle, 1, color, 0);
        GLES20.glUniform1i(enabledHandle, enabled ? 1 : 0);
    }
}

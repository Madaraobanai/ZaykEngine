package com.zayk.engine;

import android.opengl.GLES20;
import android.os.Environment;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

public class Model3D {
    private FloatBuffer vertexBuffer;
    private int vertexCount;
    private int mProgram;

    // Shaders básicos
    private final String vertexShaderCode =
        "uniform mat4 uMVPMatrix; attribute vec4 vPosition; void main() { gl_Position = uMVPMatrix * vPosition; }";
    private final String fragmentShaderCode =
        "precision mediump float; uniform vec4 vColor; void main() { gl_FragColor = vColor; }";

    public Model3D(String fileName) {
        loadFromExternalStorage(fileName);
        setupShaders();
    }

    private void loadFromExternalStorage(String fileName) {
        List<Float> vertices = new ArrayList<>();
        List<Float> resultVertices = new ArrayList<>();
        
        try {
            // Caminho: /sdcard/ZaykEngine/seu_modelo.obj
            File file = new File(Environment.getExternalStorageDirectory(), "ZaykEngine/" + fileName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length == 0) continue;

                if (parts[0].equals("v")) {
                    vertices.add(Float.parseFloat(parts[1]));
                    vertices.add(Float.parseFloat(parts[2]));
                    vertices.add(Float.parseFloat(parts[3]));
                } else if (parts[0].equals("f")) {
                    for (int i = 1; i < parts.length; i++) {
                        // Ignora dados de textura/normal por enquanto para não crashar
                        int index = Integer.parseInt(parts[i].split("/")[0]) - 1;
                        resultVertices.add(vertices.get(index * 3));
                        resultVertices.add(vertices.get(index * 3 + 1));
                        resultVertices.add(vertices.get(index * 3 + 2));
                    }
                }
            }
            reader.close();
        } catch (Exception e) {
            android.util.Log.e("ZaykEngine", "Falha ao carrergar OBJ: " + e.getMessage());
        }

        vertexCount = resultVertices.size() / 3;
        ByteBuffer bb = ByteBuffer.allocateDirect(resultVertices.size() * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        for (Float f : resultVertices) vertexBuffer.put(f);
        vertexBuffer.position(0);
    }

    private void setupShaders() {
        int vShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
        mProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgram, vShader);
        GLES20.glAttachShader(mProgram, fShader);
        GLES20.glLinkProgram(mProgram);
    }

    public void draw(float[] mvpMatrix, float[] color) {
        GLES20.glUseProgram(mProgram);
        int posHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        GLES20.glEnableVertexAttribArray(posHandle);
        GLES20.glVertexAttribPointer(posHandle, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer);
        
        GLES20.glUniform4fv(GLES20.glGetUniformLocation(mProgram, "vColor"), 1, color, 0);
        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(mProgram, "uMVPMatrix"), 1, false, mvpMatrix, 0);
        
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);
    }

    private int loadShader(int type, String code) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, code);
        GLES20.glCompileShader(shader);
        return shader;
    }
}

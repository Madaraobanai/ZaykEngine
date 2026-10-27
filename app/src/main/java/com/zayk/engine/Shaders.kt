package com.zayk.engine

import android.opengl.GLES30 as GL

class ShaderProgram(vs: String, fs: String) {
    val id: Int
    private val locs = HashMap<String, Int>()

    init {
        val v = compile(GL.GL_VERTEX_SHADER, vs)
        val f = compile(GL.GL_FRAGMENT_SHADER, fs)
        id = GL.glCreateProgram()
        GL.glAttachShader(id, v)
        GL.glAttachShader(id, f)
        GL.glLinkProgram(id)
        val ok = IntArray(1)
        GL.glGetProgramiv(id, GL.GL_LINK_STATUS, ok, 0)
        if (ok[0] == 0) throw RuntimeException("Link: " + GL.glGetProgramInfoLog(id))
    }

    private fun compile(type: Int, src: String): Int {
        val s = GL.glCreateShader(type)
        GL.glShaderSource(s, src)
        GL.glCompileShader(s)
        val ok = IntArray(1)
        GL.glGetShaderiv(s, GL.GL_COMPILE_STATUS, ok, 0)
        if (ok[0] == 0) throw RuntimeException("Shader: " + GL.glGetShaderInfoLog(s))
        return s
    }

    fun use() = GL.glUseProgram(id)
    fun loc(name: String): Int = locs.getOrPut(name) { GL.glGetUniformLocation(id, name) }
}

object Shaders {
    const val LIT_VERT = """#version 300 es
layout(location=0) in vec3 aPos;
layout(location=1) in vec3 aNormal;
uniform mat4 uVP;
uniform mat4 uModel;
out vec3 vN;
out vec3 vW;
void main(){
  vec4 w = uModel * vec4(aPos, 1.0);
  vW = w.xyz;
  vN = mat3(uModel) * aNormal;
  gl_Position = uVP * w;
}"""

    const val LIT_FRAG = """#version 300 es
precision mediump float;
in vec3 vN;
in vec3 vW;
uniform vec3 uColor;
uniform vec3 uLightDir;
uniform vec3 uCamPos;
uniform float uAmbient;
out vec4 outColor;
void main(){
  vec3 n = normalize(vN);
  vec3 l = -normalize(uLightDir);
  float diff = max(dot(n, l), 0.0);
  vec3 v = normalize(uCamPos - vW);
  vec3 h = normalize(l + v);
  float spec = pow(max(dot(n, h), 0.0), 32.0) * 0.25;
  outColor = vec4(uColor * (uAmbient + diff) + spec, 1.0);
}"""

    const val LINE_VERT = """#version 300 es
layout(location=0) in vec3 aPos;
layout(location=1) in vec3 aCol;
uniform mat4 uVP;
out vec3 vC;
out vec3 vP;
void main(){
  vC = aCol;
  vP = aPos;
  gl_Position = uVP * vec4(aPos, 1.0);
}"""

    const val LINE_FRAG = """#version 300 es
precision highp float;
in vec3 vC;
in vec3 vP;
uniform vec3 uCam;
out vec4 outColor;
void main(){
  float a = 1.0 - clamp(distance(vP.xz, uCam.xz) / 70.0, 0.0, 1.0);
  outColor = vec4(vC, a);
}"""
}
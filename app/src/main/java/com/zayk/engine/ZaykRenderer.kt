package com.zayk.engine

import android.opengl.GLES30 as GL
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.sin

class ZaykRenderer(private val onFps: (Int) -> Unit) : GLSurfaceView.Renderer {

    val input = InputState()
    val camera = Camera()
    val entities = ArrayList<Entity>() // acessar somente na thread GL (use queueEvent)

    private var counter = 0
    private lateinit var lit: ShaderProgram
    private lateinit var lineProg: ShaderProgram
    private lateinit var grid: Mesh
    private val meshes = HashMap<MeshKind, Mesh>()

    private val proj = FloatArray(16)
    private val view = FloatArray(16)
    private val vp = FloatArray(16)
    private val model = FloatArray(16)
    private var aspect = 1f
    private var last = System.nanoTime()
    private var frames = 0
    private var acc = 0f

    companion object {
        const val SPEED = 5f
        const val SENS = 0.005f
        private fun deg(rad: Float) = rad * 57.29578f
        private fun snap(v: Float) = round(v * 2f) / 2f
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        lit = ShaderProgram(Shaders.LIT_VERT, Shaders.LIT_FRAG)
        lineProg = ShaderProgram(Shaders.LINE_VERT, Shaders.LINE_FRAG)
        grid = Primitives.grid()
        meshes[MeshKind.CUBE] = Primitives.cube()
        meshes[MeshKind.SPHERE] = Primitives.sphere()
        meshes[MeshKind.PLANE] = Primitives.plane()
        // contexto GL novo: modelos importados precisam ser recriados na GPU
        for (e in entities) e.gpu = null
        GL.glEnable(GL.GL_DEPTH_TEST)
        GL.glEnable(GL.GL_CULL_FACE)
        last = System.nanoTime()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GL.glViewport(0, 0, width, height)
        aspect = width.toFloat() / max(height, 1)
    }

    private fun placeInFront(e: Entity, y: Float) {
        e.position[0] = snap(camera.pos[0] - sin(camera.yaw) * 6f)
        e.position[1] = y
        e.position[2] = snap(camera.pos[2] - cos(camera.yaw) * 6f)
    }

    /** Chamar via glView.queueEvent { renderer.spawn(...) } */
    fun spawn(kind: MeshKind) {
        val name = when (kind) {
            MeshKind.CUBE -> "Cubo"
            MeshKind.SPHERE -> "Esfera"
            MeshKind.PLANE -> "Plano"
            MeshKind.MODEL -> "Modelo"
        }
        counter++
        val e = Entity("$name $counter", kind)
        placeInFront(e, if (kind == MeshKind.PLANE) 0.01f else 0.5f)
        if (kind == MeshKind.PLANE) {
            e.scale[0] = 4f
            e.scale[2] = 4f
        }
        entities.add(e)
    }

    /** Chamar via glView.queueEvent { renderer.spawnModel(...) } */
    fun spawnModel(name: String, parts: List<MeshData>) {
        counter++
        val e = Entity("$name $counter", MeshKind.MODEL, parts)
        e.color = floatArrayOf(1f, 1f, 1f)
        placeInFront(e, 0f)
        entities.add(e)
    }

    private fun updateCamera(dt: Float) {
        val (lx, ly) = input.consumeLook()
        camera.yaw -= lx * SENS
        camera.pitch = (camera.pitch - ly * SENS).coerceIn(-1.5f, 1.5f)
        val mx = input.moveX
        val my = input.moveY
        val fx = -sin(camera.yaw)
        val fz = -cos(camera.yaw)
        val rx = cos(camera.yaw)
        val rz = -sin(camera.yaw)
        camera.pos[0] += (fx * my + rx * mx) * SPEED * dt
        camera.pos[2] += (fz * my + rz * mx) * SPEED * dt
        camera.pos[1] += input.vert * SPEED * dt
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        val dt = min((now - last) / 1_000_000_000f, 0.1f)
        last = now

        updateCamera(dt)

        frames++
        acc += dt
        if (acc >= 0.5f) {
            onFps((frames / acc).toInt())
            frames = 0
            acc = 0f
        }

        GL.glClearColor(0.2f, 0.21f, 0.23f, 1f)
        GL.glClear(GL.GL_COLOR_BUFFER_BIT or GL.GL_DEPTH_BUFFER_BIT)

        Matrix.perspectiveM(proj, 0, 60f, aspect, 0.1f, 300f)
        Matrix.setIdentityM(view, 0)
        Matrix.rotateM(view, 0, deg(-camera.pitch), 1f, 0f, 0f)
        Matrix.rotateM(view, 0, deg(-camera.yaw), 0f, 1f, 0f)
        Matrix.translateM(view, 0, -camera.pos[0], -camera.pos[1], -camera.pos[2])
        Matrix.multiplyMM(vp, 0, proj, 0, view, 0)

        // Objetos
        lit.use()
        GL.glUniformMatrix4fv(lit.loc("uVP"), 1, false, vp, 0)
        GL.glUniform3f(lit.loc("uLightDir"), -0.5f, -1f, -0.4f)
        GL.glUniform1f(lit.loc("uAmbient"), 0.3f)
        GL.glUniform3f(lit.loc("uCamPos"), camera.pos[0], camera.pos[1], camera.pos[2])
        for (e in entities) {
            Matrix.setIdentityM(model, 0)
            Matrix.translateM(model, 0, e.position[0], e.position[1], e.position[2])
            Matrix.rotateM(model, 0, deg(e.rotation[1]), 0f, 1f, 0f)
            Matrix.rotateM(model, 0, deg(e.rotation[0]), 1f, 0f, 0f)
            Matrix.rotateM(model, 0, deg(e.rotation[2]), 0f, 0f, 1f)
            Matrix.scaleM(model, 0, e.scale[0], e.scale[1], e.scale[2])
            GL.glUniformMatrix4fv(lit.loc("uModel"), 1, false, model, 0)

            val parts = e.parts
            if (parts != null) {
                val gpu = e.gpu ?: parts.map { Mesh(it.vertices, it.indices) }.also { e.gpu = it }
                for (i in gpu.indices) {
                    val c = parts[i].color
                    GL.glUniform3f(lit.loc("uColor"), c[0] * e.color[0], c[1] * e.color[1], c[2] * e.color[2])
                    gpu[i].draw()
                }
            } else {
                GL.glUniform3f(lit.loc("uColor"), e.color[0], e.color[1], e.color[2])
                meshes[e.kind]?.draw()
            }
        }

        // Grid
        lineProg.use()
        GL.glUniformMatrix4fv(lineProg.loc("uVP"), 1, false, vp, 0)
        GL.glUniform3f(lineProg.loc("uCam"), camera.pos[0], camera.pos[1], camera.pos[2])
        GL.glEnable(GL.GL_BLEND)
        GL.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA)
        GL.glDepthMask(false)
        grid.draw()
        GL.glDepthMask(true)
        GL.glDisable(GL.GL_BLEND)
    }
}
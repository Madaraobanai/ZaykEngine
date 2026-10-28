package com.zayk.engine

enum class MeshKind { CUBE, SPHERE, PLANE, MODEL }

class Entity(var name: String, val kind: MeshKind, val parts: List<MeshData>? = null) {
    val position = floatArrayOf(0f, 0f, 0f)
    val rotation = floatArrayOf(0f, 0f, 0f) // radianos (X, Y, Z)
    val scale = floatArrayOf(1f, 1f, 1f)
    var color = floatArrayOf(0.8f, 0.8f, 0.85f)
    var gpu: List<Mesh>? = null // criado sob demanda na thread GL
}

class Camera {
    val pos = floatArrayOf(0f, 4f, 9f)
    var yaw = 0f
    var pitch = -0.35f
}
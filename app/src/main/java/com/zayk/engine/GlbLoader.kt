package com.zayk.engine

import android.opengl.Matrix
import android.util.Base64
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.net.URLDecoder
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipInputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class MeshData(val vertices: FloatArray, val indices: IntArray, val color: FloatArray)

object GlbLoader {

    private class Model(val json: JSONObject, val buffers: List<ByteBuffer>)

    private fun isZip(b: ByteArray) = b.size > 4 && b[0] == 0x50.toByte() && b[1] == 0x4B.toByte()

    private fun isGlb(b: ByteArray) = b.size >= 20 &&
        b[0] == 'g'.code.toByte() && b[1] == 'l'.code.toByte() &&
        b[2] == 'T'.code.toByte() && b[3] == 'F'.code.toByte()

    private fun isJson(b: ByteArray): Boolean {
        for (x in b) {
            val c = x.toInt().toChar()
            if (c == ' ' || c == '\n' || c == '\r' || c == '\t') continue
            return c == '{'
        }
        return false
    }

    private fun describe(files: Map<String, ByteArray>): String {
        val sb = StringBuilder("Formato não reconhecido.")
        for ((name, b) in files) {
            val head = String(CharArray(min(12, b.size)) {
                val c = b[it].toInt()
                if (c in 32..126) c.toChar() else '.'
            })
            sb.append("\n").append(name).append(" | ").append(b.size).append(" bytes | começa com: ").append(head)
        }
        return sb.toString()
    }

    /** files: nome -> bytes. Aceita .glb, .gltf (+ .bin) e .zip com esses arquivos. */
    fun load(input: Map<String, ByteArray>): List<MeshData> {
        val files = LinkedHashMap<String, ByteArray>()
        for ((name, bytes) in input) {
            if (isZip(bytes)) {
                ZipInputStream(ByteArrayInputStream(bytes)).use { z ->
                    while (true) {
                        val e = z.nextEntry ?: break
                        if (!e.isDirectory) files[e.name.substringAfterLast('/')] = z.readBytes()
                    }
                }
            } else {
                files[name.substringAfterLast('/')] = bytes
            }
        }

        val glbName = files.keys.firstOrNull { isGlb(files[it]!!) }
        if (glbName != null) return build(parseGlb(files[glbName]!!))

        val gltfName = files.keys.firstOrNull { it.endsWith(".gltf", true) && isJson(files[it]!!) }
            ?: files.keys.firstOrNull { isJson(files[it]!!) }
        if (gltfName != null) return build(parseGltf(files[gltfName]!!, files))

        throw IllegalArgumentException(describe(files))
    }

    private fun wrap(b: ByteArray, start: Int = 0, len: Int = b.size): ByteBuffer =
        ByteBuffer.wrap(b, start, len).slice().order(ByteOrder.LITTLE_ENDIAN)

    private fun parseGlb(bytes: ByteArray): Model {
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        var pos = 12
        var json: JSONObject? = null
        var bin: ByteBuffer? = null
        while (pos + 8 <= bytes.size) {
            val len = buf.getInt(pos)
            val type = buf.getInt(pos + 4)
            val start = pos + 8
            if (type == 0x4E4F534A) {
                json = JSONObject(String(bytes, start, len, Charsets.UTF_8))
            } else if (type == 0x004E4942 && bin == null) {
                bin = wrap(bytes, start, len)
            }
            pos = start + len
        }
        if (json == null) throw IllegalArgumentException("JSON ausente no .glb")
        return Model(json, if (bin != null) listOf(bin) else emptyList())
    }

    private fun parseGltf(jsonBytes: ByteArray, files: Map<String, ByteArray>): Model {
        val json = JSONObject(String(jsonBytes, Charsets.UTF_8))
        val list = ArrayList<ByteBuffer>()
        val arr = json.optJSONArray("buffers")
        if (arr != null) {
            for (i in 0 until arr.length()) {
                val uri = arr.getJSONObject(i).optString("uri", "")
                val data: ByteArray = if (uri.startsWith("data:")) {
                    Base64.decode(uri.substringAfter(","), Base64.DEFAULT)
                } else {
                    val name = URLDecoder.decode(uri, "UTF-8").substringAfterLast('/')
                    files[name]
                        ?: files.entries.firstOrNull { it.key.equals(name, true) }?.value
                        ?: throw IllegalArgumentException("Falta o arquivo \"$name\". Selecione o .gltf e o .bin juntos.")
                }
                list.add(wrap(data))
            }
        }
        return Model(json, list)
    }

    private fun build(m: Model): List<MeshData> {
        val ext = m.json.optJSONArray("extensionsRequired")
        if (ext != null) {
            for (i in 0 until ext.length()) {
                val e = ext.getString(i)
                if (e.contains("draco", true) || e.contains("meshopt", true) || e.contains("quantization", true)) {
                    throw IllegalArgumentException("Modelo usa extensão não suportada: $e")
                }
            }
        }
        val nodes = m.json.optJSONArray("nodes") ?: throw IllegalArgumentException("Modelo sem nós")
        val roots = ArrayList<Int>()
        val scenes = m.json.optJSONArray("scenes")
        if (scenes != null && scenes.length() > 0) {
            val sc = scenes.getJSONObject(m.json.optInt("scene", 0))
            val list = sc.optJSONArray("nodes")
            if (list != null) for (i in 0 until list.length()) roots.add(list.getInt(i))
        } else {
            val isChild = BooleanArray(nodes.length())
            for (n in 0 until nodes.length()) {
                val ch = nodes.getJSONObject(n).optJSONArray("children")
                if (ch != null) for (i in 0 until ch.length()) isChild[ch.getInt(i)] = true
            }
            for (n in 0 until nodes.length()) if (!isChild[n]) roots.add(n)
        }

        val identity = FloatArray(16)
        Matrix.setIdentityM(identity, 0)
        val parts = ArrayList<MeshData>()
        for (r in roots) traverse(m, r, identity, parts, 0)
        if (parts.isEmpty()) throw IllegalArgumentException("Nenhuma malha encontrada")
        return fit(parts)
    }

    private fun traverse(m: Model, idx: Int, parent: FloatArray, parts: ArrayList<MeshData>, depth: Int) {
        if (depth > 64) return
        val node = m.json.getJSONArray("nodes").getJSONObject(idx)
        val local = localMatrix(node)
        val world = FloatArray(16)
        Matrix.multiplyMM(world, 0, parent, 0, local, 0)
        if (node.has("mesh")) addMesh(m, node.getInt("mesh"), world, parts)
        val ch = node.optJSONArray("children")
        if (ch != null) for (i in 0 until ch.length()) traverse(m, ch.getInt(i), world, parts, depth + 1)
    }

    private fun localMatrix(node: JSONObject): FloatArray {
        val mtx = FloatArray(16)
        val mat = node.optJSONArray("matrix")
        if (mat != null && mat.length() == 16) {
            for (i in 0 until 16) mtx[i] = mat.getDouble(i).toFloat()
            return mtx
        }
        val t = node.optJSONArray("translation")
        val r = node.optJSONArray("rotation")
        val s = node.optJSONArray("scale")
        Matrix.setIdentityM(mtx, 0)
        if (t != null) Matrix.translateM(mtx, 0, t.getDouble(0).toFloat(), t.getDouble(1).toFloat(), t.getDouble(2).toFloat())
        val rot = FloatArray(16)
        Matrix.setIdentityM(rot, 0)
        if (r != null) {
            val x = r.getDouble(0).toFloat()
            val y = r.getDouble(1).toFloat()
            val z = r.getDouble(2).toFloat()
            val w = r.getDouble(3).toFloat()
            rot[0] = 1 - 2 * (y * y + z * z); rot[1] = 2 * (x * y + z * w); rot[2] = 2 * (x * z - y * w)
            rot[4] = 2 * (x * y - z * w); rot[5] = 1 - 2 * (x * x + z * z); rot[6] = 2 * (y * z + x * w)
            rot[8] = 2 * (x * z + y * w); rot[9] = 2 * (y * z - x * w); rot[10] = 1 - 2 * (x * x + y * y)
        }
        val out = FloatArray(16)
        Matrix.multiplyMM(out, 0, mtx, 0, rot, 0)
        if (s != null) Matrix.scaleM(out, 0, s.getDouble(0).toFloat(), s.getDouble(1).toFloat(), s.getDouble(2).toFloat())
        return out
    }

    private fun addMesh(m: Model, meshIdx: Int, world: FloatArray, parts: ArrayList<MeshData>) {
        val meshes = m.json.optJSONArray("meshes") ?: return
        val prims = meshes.getJSONObject(meshIdx).optJSONArray("primitives") ?: return
        for (p in 0 until prims.length()) {
            val prim = prims.getJSONObject(p)
            if (prim.optInt("mode", 4) != 4) continue
            val attrs = prim.getJSONObject("attributes")
            if (!attrs.has("POSITION")) continue

            val pos = readFloats(m, attrs.getInt("POSITION"), 3)
            val vcount = pos.size / 3
            val idx = if (prim.has("indices")) readIndices(m, prim.getInt("indices")) else IntArray(vcount) { it }
            val nor = if (attrs.has("NORMAL")) readFloats(m, attrs.getInt("NORMAL"), 3) else computeNormals(pos, idx)

            val out = FloatArray(vcount * 6)
            val v = FloatArray(4)
            val tmp = FloatArray(4)
            for (i in 0 until vcount) {
                v[0] = pos[i * 3]; v[1] = pos[i * 3 + 1]; v[2] = pos[i * 3 + 2]; v[3] = 1f
                Matrix.multiplyMV(tmp, 0, world, 0, v, 0)
                out[i * 6] = tmp[0]; out[i * 6 + 1] = tmp[1]; out[i * 6 + 2] = tmp[2]

                v[0] = nor[i * 3]; v[1] = nor[i * 3 + 1]; v[2] = nor[i * 3 + 2]; v[3] = 0f
                Matrix.multiplyMV(tmp, 0, world, 0, v, 0)
                var l = sqrt(tmp[0] * tmp[0] + tmp[1] * tmp[1] + tmp[2] * tmp[2])
                if (l == 0f) l = 1f
                out[i * 6 + 3] = tmp[0] / l; out[i * 6 + 4] = tmp[1] / l; out[i * 6 + 5] = tmp[2] / l
            }
            parts.add(MeshData(out, idx, materialColor(m, prim)))
        }
    }

    private fun materialColor(m: Model, prim: JSONObject): FloatArray {
        val mats = m.json.optJSONArray("materials")
        val mi = prim.optInt("material", -1)
        if (mats == null || mi < 0 || mi >= mats.length()) return floatArrayOf(0.8f, 0.8f, 0.8f)
        val c = floatArrayOf(1f, 1f, 1f)
        val f = mats.getJSONObject(mi).optJSONObject("pbrMetallicRoughness")?.optJSONArray("baseColorFactor")
        if (f != null && f.length() >= 3) for (i in 0..2) c[i] = f.getDouble(i).toFloat()
        return c
    }

    private fun readFloats(m: Model, accIdx: Int, comps: Int): FloatArray {
        val acc = m.json.getJSONArray("accessors").getJSONObject(accIdx)
        if (acc.getInt("componentType") != 5126) {
            throw IllegalArgumentException("Vértices comprimidos não são suportados")
        }
        val count = acc.getInt("count")
        val out = FloatArray(count * comps)
        if (!acc.has("bufferView")) return out
        val bv = m.json.getJSONArray("bufferViews").getJSONObject(acc.getInt("bufferView"))
        val buf = m.buffers[bv.optInt("buffer", 0)]
        val base = bv.optInt("byteOffset", 0) + acc.optInt("byteOffset", 0)
        val s = bv.optInt("byteStride", 0)
        val stride = if (s == 0) comps * 4 else s
        for (i in 0 until count) {
            for (c in 0 until comps) out[i * comps + c] = buf.getFloat(base + i * stride + c * 4)
        }
        return out
    }

    private fun readIndices(m: Model, accIdx: Int): IntArray {
        val acc = m.json.getJSONArray("accessors").getJSONObject(accIdx)
        val count = acc.getInt("count")
        val type = acc.getInt("componentType")
        val bv = m.json.getJSONArray("bufferViews").getJSONObject(acc.getInt("bufferView"))
        val buf = m.buffers[bv.optInt("buffer", 0)]
        val base = bv.optInt("byteOffset", 0) + acc.optInt("byteOffset", 0)
        val size = when (type) {
            5121 -> 1
            5123 -> 2
            else -> 4
        }
        val s = bv.optInt("byteStride", 0)
        val stride = if (s == 0) size else s
        val out = IntArray(count)
        for (i in 0 until count) {
            val p = base + i * stride
            out[i] = when (type) {
                5121 -> buf.get(p).toInt() and 0xFF
                5123 -> buf.getShort(p).toInt() and 0xFFFF
                else -> buf.getInt(p)
            }
        }
        return out
    }

    private fun computeNormals(pos: FloatArray, idx: IntArray): FloatArray {
        val n = FloatArray(pos.size)
        var i = 0
        while (i + 2 < idx.size) {
            val a = idx[i]; val b = idx[i + 1]; val c = idx[i + 2]
            val ux = pos[b * 3] - pos[a * 3]
            val uy = pos[b * 3 + 1] - pos[a * 3 + 1]
            val uz = pos[b * 3 + 2] - pos[a * 3 + 2]
            val vx = pos[c * 3] - pos[a * 3]
            val vy = pos[c * 3 + 1] - pos[a * 3 + 1]
            val vz = pos[c * 3 + 2] - pos[a * 3 + 2]
            val nx = uy * vz - uz * vy
            val ny = uz * vx - ux * vz
            val nz = ux * vy - uy * vx
            for (k in intArrayOf(a, b, c)) {
                n[k * 3] += nx; n[k * 3 + 1] += ny; n[k * 3 + 2] += nz
            }
            i += 3
        }
        for (k in 0 until n.size / 3) {
            var l = sqrt(n[k * 3] * n[k * 3] + n[k * 3 + 1] * n[k * 3 + 1] + n[k * 3 + 2] * n[k * 3 + 2])
            if (l == 0f) l = 1f
            n[k * 3] /= l; n[k * 3 + 1] /= l; n[k * 3 + 2] /= l
        }
        return n
    }

    /** Centraliza em X/Z, apoia no chão (Y=0) e escala para ~2 unidades. */
    private fun fit(parts: List<MeshData>): List<MeshData> {
        var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE; var minZ = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE; var maxY = -Float.MAX_VALUE; var maxZ = -Float.MAX_VALUE
        for (p in parts) {
            var i = 0
            while (i < p.vertices.size) {
                minX = min(minX, p.vertices[i]); maxX = max(maxX, p.vertices[i])
                minY = min(minY, p.vertices[i + 1]); maxY = max(maxY, p.vertices[i + 1])
                minZ = min(minZ, p.vertices[i + 2]); maxZ = max(maxZ, p.vertices[i + 2])
                i += 6
            }
        }
        val ext = max(max(maxX - minX, maxY - minY), maxZ - minZ)
        val s = if (ext > 1e-6f) 2f / ext else 1f
        val cx = (minX + maxX) / 2f
        val cz = (minZ + maxZ) / 2f
        for (p in parts) {
            var i = 0
            while (i < p.vertices.size) {
                p.vertices[i] = (p.vertices[i] - cx) * s
                p.vertices[i + 1] = (p.vertices[i + 1] - minY) * s
                p.vertices[i + 2] = (p.vertices[i + 2] - cz) * s
                i += 6
            }
        }
        return parts
    }
}
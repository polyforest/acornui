package com.acornui.gl.component.drawing

import com.acornui.math.MathUtils
import com.acornui.math.PI2
import com.acornui.math.Vector3

private val v1_3 = Vector3() // Careful for KT-14116, private package member names cannot clash.

/**
 * Creates a 3d cylinder mesh (only fills)
 *
 * The following gl properties should be set:
 * gl.enable(Gl20.CULL_FACE)
 * gl.frontFace(Gl20.CW)
 * gl.cullFace(Gl20.BACK)
 */
fun cylinder(width: Float, height: Float, depth: Float, segments: Int = 180, init: MeshData.() -> Unit = {}): MeshData {
	val m = meshData {
		val hW = width * 0.5f
		val hH = height * 0.5f
		texture = fillStyle.texture
		pushVertex(v1_3.set(hW, hH, 0f), fillStyle, Vector3.NEG_Z) // 0
		pushVertex(v1_3.set(hW, hH, depth), fillStyle, Vector3.Z) // 1
		var index = 2

		for (i in 0..segments) {
			val theta = i.toFloat() / segments * PI2
			val cos = MathUtils.cos(theta)
			val sin = MathUtils.sin(theta)
			val x = (cos + 1f) * hW
			val y = (sin + 1f) * hH

			// Top
			pushVertex(v1_3.set(x, y, 0f), fillStyle, Vector3.NEG_Z) // index - 4
			// Bottom
			pushVertex(v1_3.set(x, y, depth), fillStyle, Vector3.Z) // index - 3
			// Side top
			pushVertex(v1_3.set(x, y, 0f), fillStyle, Vector3(cos, sin)) // index - 2
			// Side bottom
			pushVertex(v1_3.set(x, y, depth), fillStyle, Vector3(cos, sin)) // index - 1
			index += 4
			if (i > 0) {
				// CW
				pushIndex(0)
				pushIndex(index - 8)
				pushIndex(index - 4)
				// CCW
				pushIndex(index - 3)
				pushIndex(index - 7)
				pushIndex(1)

				pushIndex(index - 1) // BR
				pushIndex(index - 2) // TR
				pushIndex(index - 6) // TL
				pushIndex(index - 6) // TL
				pushIndex(index - 5) // BL
				pushIndex(index - 1) // BR
			}
		}
	}
	m.init()
	return m
}

fun box(width: Float, height: Float, depth: Float, init: MeshData.() -> Unit = {}): MeshData {
	val m = meshData {
		texture = fillStyle.texture
		// Top face
		pushVertex(v1_3.set(0f, 0f, 0f), fillStyle, Vector3.NEG_Z)
		pushVertex(v1_3.set(width, 0f, 0f), fillStyle, Vector3.NEG_Z)
		pushVertex(v1_3.set(width, height, 0f), fillStyle, Vector3.NEG_Z)
		pushVertex(v1_3.set(0f, height, 0f), fillStyle, Vector3.NEG_Z)
		pushIndices(QUAD_INDICES)

		// Right face
		pushVertex(v1_3.set(width, height, 0f), fillStyle, Vector3.X)
		pushVertex(v1_3.set(width, 0f, 0f), fillStyle, Vector3.X)
		pushVertex(v1_3.set(width, 0f, depth), fillStyle, Vector3.X)
		pushVertex(v1_3.set(width, height, depth), fillStyle, Vector3.X)
		pushIndices(QUAD_INDICES)

		// Back face
		pushVertex(v1_3.set(width, 0f, 0f), fillStyle, Vector3.NEG_Y)
		pushVertex(v1_3.set(0f, 0f, 0f), fillStyle, Vector3.NEG_Y)
		pushVertex(v1_3.set(0f, 0f, depth), fillStyle, Vector3.NEG_Y)
		pushVertex(v1_3.set(width, 0f, depth), fillStyle, Vector3.NEG_Y)
		pushIndices(QUAD_INDICES)

		// Left face
		pushVertex(v1_3.set(0f, 0f, 0f), fillStyle, Vector3.NEG_X)
		pushVertex(v1_3.set(0f, height, 0f), fillStyle, Vector3.NEG_X)
		pushVertex(v1_3.set(0f, height, depth), fillStyle, Vector3.NEG_X)
		pushVertex(v1_3.set(0f, 0f, depth), fillStyle, Vector3.NEG_X)
		pushIndices(QUAD_INDICES)

		// Front face
		pushVertex(v1_3.set(0f, height, 0f), fillStyle, Vector3.Y)
		pushVertex(v1_3.set(width, height, 0f), fillStyle, Vector3.Y)
		pushVertex(v1_3.set(width, height, depth), fillStyle, Vector3.Y)
		pushVertex(v1_3.set(0f, height, depth), fillStyle, Vector3.Y)
		pushIndices(QUAD_INDICES)

		// Bottom face
		pushVertex(v1_3.set(0f, height, depth), fillStyle, Vector3.Z)
		pushVertex(v1_3.set(width, height, depth), fillStyle, Vector3.Z)
		pushVertex(v1_3.set(width, 0f, depth), fillStyle, Vector3.Z)
		pushVertex(v1_3.set(0f, 0f, depth), fillStyle, Vector3.Z)
		pushIndices(QUAD_INDICES)
	}
	m.init()
	return m
}
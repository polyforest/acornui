/*
 * Copyright 2019 Poly Forest, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.acornui.component.drawing

import com.acornui.gl.core.ShaderBatch
import com.acornui.gl.core.putIndex
import com.acornui.gl.core.putQuadIndices
import com.acornui.gl.core.putVertex
import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
import com.acornui.math.PI2
import com.acornui.math.Vector3
import kotlin.math.cos
import kotlin.math.sin

/**
 * Creates a 3d cylinder mesh.
 *
 * The following gl properties should be set:
 * gl.enable(Gl20.CULL_FACE)
 * gl.frontFace(Gl20.CW)
 * gl.cullFace(Gl20.BACK)
 */
fun ShaderBatch.cylinder(width: Float, height: Float, depth: Float, segments: Int = 180, fillColor: ColorRo = Color.WHITE, init: ShaderBatch.() -> Unit = {}) {
	val hW = width * 0.5f
	val hH = height * 0.5f
	putVertex(hW, hH, 0f, Vector3.NEG_Z, fillColor) // 0
	putVertex(hW, hH, depth, Vector3.Z, fillColor) // 1
	var index = 2

	val n = highestIndex + 1

	for (i in 0..segments) {
		val theta = i.toFloat() / segments * PI2
		val cos = cos(theta)
		val sin = sin(theta)
		val x = (cos + 1f) * hW
		val y = (sin + 1f) * hH

		// Top
		putVertex(x, y, 0f, Vector3.NEG_Z, fillColor) // index - 4
		// Bottom
		putVertex(x, y, depth, Vector3.Z, fillColor) // index - 3
		// Side top
		putVertex(x, y, 0f, Vector3(cos, sin), fillColor) // index - 2
		// Side bottom
		putVertex(x, y, depth, Vector3(cos, sin), fillColor) // index - 1
		index += 4
		if (i > 0) {
			// CW
			putIndex(n)
			putIndex(n + index - 8)
			putIndex(n + index - 4)
			// CCW
			putIndex(n + index - 3)
			putIndex(n + index - 7)
			putIndex(n + 1)

			putIndex(n + index - 1) // BR
			putIndex(n + index - 2) // TR
			putIndex(n + index - 6) // TL
			putIndex(n + index - 6) // TL
			putIndex(n + index - 5) // BL
			putIndex(n + index - 1) // BR
		}
	}
	init()
}

/**
 * Creates a 3d box.
 *
 * The following gl properties should be set:
 * gl.enable(Gl20.CULL_FACE)
 * gl.frontFace(Gl20.CW)
 * gl.cullFace(Gl20.BACK)
 */
fun ShaderBatch.box(width: Float, height: Float, depth: Float, fillColor: ColorRo = Color.WHITE, init: ShaderBatch.() -> Unit = {}) {
	// Top face
	putVertex(0f, 0f, 0f, Vector3.NEG_Z, fillColor)
	putVertex(width, 0f, 0f, Vector3.NEG_Z, fillColor)
	putVertex(width, height, 0f, Vector3.NEG_Z, fillColor)
	putVertex(0f, height, 0f, Vector3.NEG_Z, fillColor)
	putQuadIndices()

	// Right face
	putVertex(width, height, 0f, Vector3.X, fillColor)
	putVertex(width, 0f, 0f, Vector3.X, fillColor)
	putVertex(width, 0f, depth, Vector3.X, fillColor)
	putVertex(width, height, depth, Vector3.X, fillColor)
	putQuadIndices()

	// Back face
	putVertex(width, 0f, 0f, Vector3.NEG_Y, fillColor)
	putVertex(0f, 0f, 0f, Vector3.NEG_Y, fillColor)
	putVertex(0f, 0f, depth, Vector3.NEG_Y, fillColor)
	putVertex(width, 0f, depth, Vector3.NEG_Y, fillColor)
	putQuadIndices()

	// Left face
	putVertex(0f, 0f, 0f, Vector3.NEG_X, fillColor)
	putVertex(0f, height, 0f, Vector3.NEG_X, fillColor)
	putVertex(0f, height, depth, Vector3.NEG_X, fillColor)
	putVertex(0f, 0f, depth, Vector3.NEG_X, fillColor)
	putQuadIndices()

	// Front face
	putVertex(0f, height, 0f, Vector3.Y, fillColor)
	putVertex(width, height, 0f, Vector3.Y, fillColor)
	putVertex(width, height, depth, Vector3.Y, fillColor)
	putVertex(0f, height, depth, Vector3.Y, fillColor)
	putQuadIndices()

	// Bottom face
	putVertex(0f, height, depth, Vector3.Z, fillColor)
	putVertex(width, height, depth, Vector3.Z, fillColor)
	putVertex(width, 0f, depth, Vector3.Z, fillColor)
	putVertex(0f, 0f, depth, Vector3.Z, fillColor)
	putQuadIndices()
	init()
}

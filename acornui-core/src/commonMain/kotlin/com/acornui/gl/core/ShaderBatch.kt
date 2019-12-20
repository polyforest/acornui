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

package com.acornui.gl.core

import com.acornui.graphic.BlendMode
import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
import com.acornui.graphic.TextureRo
import com.acornui.io.ReadWriteBuffer
import com.acornui.math.Vector2Ro
import com.acornui.math.Vector3
import com.acornui.math.Vector3Ro
import com.acornui.recycle.Clearable

/**
 * A [ShaderBatch] writes to index and vertex buffers, handling when new draw calls need to be made.
 */
interface ShaderBatch : Clearable, VertexFeed, IndexFeed {

	/**
	 * True if this shader batch uploads, renders, and clears as it flushes.
	 */
	val isDynamic: Boolean

	val whitePixel: TextureRo

	/**
	 * Marks the beginning of a new batch. This will flush the batch if the buffers are past an internal threshold.
	 *
	 * @param drawMode Possible values are: [Gl20.POINTS], [Gl20.LINE_STRIP], [Gl20.LINE_LOOP], [Gl20.LINES],
	 * [Gl20.TRIANGLE_STRIP], [Gl20.TRIANGLE_FAN], or [Gl20.TRIANGLES].
	 */
	fun begin(texture: TextureRo = whitePixel, blendMode: BlendMode = BlendMode.NORMAL, premultipliedAlpha: Boolean = false, drawMode: Int = Gl20.TRIANGLES)

	/**
	 * Flushes this batch.
	 * Call this method when something has changed (such as setting a uniform or GL property) that would require a
	 * new draw.
	 */
	fun flush()

	/**
	 * A way to push a 'standard' UI vertex.
	 * This will be adapted to fit this batch's [vertexAttributes]
	 * Note that this will push a full vertex even if the standard vertex is missing components in [vertexAttributes].
	 * Those missing components will be pushed as 0f.
	 */
	fun putVertex(positionX: Float, positionY: Float, positionZ: Float, normalX: Float, normalY: Float, normalZ: Float, colorR: Float, colorG: Float, colorB: Float, colorA: Float, u: Float, v: Float)

	/**
	 * The indices written to this batch.
	 */
	val indices: ReadWriteBuffer<Short>

	/**
	 * The vertex components currently written to this batch.
	 */
	val vertexComponents: ReadWriteBuffer<Float>

	/**
	 * The draw calls to render.
	 */
	val drawCalls: MutableList<DrawElementsCall>

	fun delete()

	fun upload()

	fun render()

	companion object {

		/**
		 * Whenever a shader batch renders, the total number of draw calls is increased by the draw call count.
		 * This will be reset to zero at the beginning of every frame.
		 */
		var totalDrawCalls = 0
	}

}

// Utility methods for putting vertices.

fun ShaderBatch.putVertex(position: Vector2Ro, z: Float = 0f, normal: Vector3Ro = Vector3.NEG_Z, colorTint: ColorRo = Color.WHITE, u: Float = 0f, v: Float = 0f) {
	putVertex(position.x, position.y, z, normal.x, normal.y, normal.z, colorTint.r, colorTint.g, colorTint.b, colorTint.a, u, v)
}

fun ShaderBatch.putVertex(position: Vector3Ro, normal: Vector3Ro, colorTint: ColorRo) {
	putVertex(position.x, position.y, position.z, normal.x, normal.y, normal.z, colorTint.r, colorTint.g, colorTint.b, colorTint.a, 0f, 0f)
}

fun ShaderBatch.putVertex(position: Vector3Ro, normal: Vector3Ro = Vector3.NEG_Z, colorTint: ColorRo = Color.WHITE, u: Float = 0f, v: Float = 0f) {
	putVertex(position.x, position.y, position.z, normal.x, normal.y, normal.z, colorTint.r, colorTint.g, colorTint.b, colorTint.a, u, v)
}

fun ShaderBatch.putVertex(positionX: Float, positionY: Float, positionZ: Float, normal: Vector3Ro = Vector3.NEG_Z, colorTint: ColorRo = Color.WHITE, u: Float = 0f, v: Float = 0f) {
	putVertex(positionX, positionY, positionZ, normal.x, normal.y, normal.z, colorTint.r, colorTint.g, colorTint.b, colorTint.a, u, v)
}

inline fun ShaderBatch.iterateVertexAttribute(usage: Int, startPosition: Int = 0, endPosition: Int = vertexComponentsCount, inner: (ReadWriteBuffer<Float>) -> Unit) {
	val offset = vertexAttributes.getOffsetByUsage(usage) ?: return
	val previousPosition = vertexComponents.position
	var i = startPosition + offset
	while (i < endPosition) {
		vertexComponents.position = i
		inner(vertexComponents)
		i += vertexAttributes.vertexSize
	}
	vertexComponents.position = previousPosition
}

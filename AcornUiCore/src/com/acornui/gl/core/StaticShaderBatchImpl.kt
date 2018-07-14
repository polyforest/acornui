/*
 * Copyright 2017 Nicholas Bilyk
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

import com.acornui.collection.Clearable
import com.acornui.component.drawing.DrawElementsCall
import com.acornui.component.drawing.DrawElementsCallRo
import com.acornui.core.Disposable
import com.acornui.core.io.resizableFloatBuffer
import com.acornui.core.io.resizableShortBuffer

/**
 * A static shader batch will remember the draw calls, allowing the batch to be rendered without needing to buffer data
 * at a later time.
 * @author nbilyk
 */
class StaticShaderBatchImpl(
		private val gl: Gl20,
		val glState: GlState,
		override val vertexAttributes: VertexAttributes
) : StaticShaderBatch, Clearable, Disposable {

	private val vertexComponentsBuffer: GlBufferRef = gl.createBuffer()
	private val indicesBuffer: GlBufferRef = gl.createBuffer()

	private val _drawCalls = ArrayList<DrawElementsCall>()
	override val drawCalls: List<DrawElementsCallRo>
		get() = _drawCalls

	/**
	 * The draw mode if no indices are supplied.
	 * Possible values are: POINTS, LINE_STRIP, LINE_LOOP, LINES, TRIANGLE_STRIP, TRIANGLE_FAN, or TRIANGLES.
	 */
	private var drawMode: Int = Gl20.TRIANGLES

	override val indices = resizableShortBuffer()
	override val vertexComponents = resizableFloatBuffer()
	private var _highestIndex: Short = -1

	private var needsUpload = false

	override fun resetRenderCount() {
	}

	override val renderCount: Int
		get() {
			return _drawCalls.size
		}

	private fun bind() {
		gl.bindBuffer(Gl20.ARRAY_BUFFER, vertexComponentsBuffer)
		gl.bindBuffer(Gl20.ELEMENT_ARRAY_BUFFER, indicesBuffer)
		vertexAttributes.bind(gl, glState.shader!!)
	}

	private fun unbind() {
		vertexAttributes.unbind(gl, glState.shader!!)
		gl.bindBuffer(Gl20.ARRAY_BUFFER, null)
		gl.bindBuffer(Gl20.ELEMENT_ARRAY_BUFFER, null)
	}

	override fun begin(drawMode: Int) {
		if (this.drawMode == drawMode) {
			flush(false)
		} else {
			flush(true)
			this.drawMode = drawMode
		}
	}

	override fun clear() {
		// Recycle the draw calls.
		DrawElementsCall.freeAll(_drawCalls)
		_drawCalls.clear()
		indices.clear()
		vertexComponents.clear()
		_highestIndex = -1
	}

	override fun flush(force: Boolean) {
		if (!force) return
		val lastDrawCall = _drawCalls.lastOrNull()
		val offset = if (lastDrawCall == null) 0 else lastDrawCall.offset + lastDrawCall.count
		val count = indices.position - offset
		if (count <= 0) return // Nothing to draw.

		val drawCall = DrawElementsCall.obtain()
		drawCall.offset = offset
		drawCall.count = count
		drawCall.texture = glState.getTexture(0)
		drawCall.blendMode = glState.blendMode
		drawCall.premultipliedAlpha = glState.premultipliedAlpha
		drawCall.mode = drawMode
		_drawCalls.add(drawCall)
		needsUpload = true
	}

	override fun render() {
		if (_drawCalls.isEmpty()) return

		bind()
		if (needsUpload) {
			needsUpload = false

			indices.flip()
			gl.bufferData(Gl20.ELEMENT_ARRAY_BUFFER, indices.limit shl 1, Gl20.DYNAMIC_DRAW) // Allocate
			gl.bufferDatasv(Gl20.ELEMENT_ARRAY_BUFFER, indices, Gl20.DYNAMIC_DRAW) // Upload

			vertexComponents.flip()
			gl.bufferData(Gl20.ARRAY_BUFFER, vertexComponents.limit shl 2, Gl20.DYNAMIC_DRAW) // Allocate
			gl.bufferDatafv(Gl20.ARRAY_BUFFER, vertexComponents, Gl20.DYNAMIC_DRAW) // Upload
		}

		for (i in 0.._drawCalls.lastIndex) {
			val drawCall = _drawCalls[i]
			glState.setTexture(drawCall.texture ?: glState.whitePixel)
			glState.blendMode(drawCall.blendMode, drawCall.premultipliedAlpha)
			gl.drawElements(drawCall.mode, drawCall.count, Gl20.UNSIGNED_SHORT, drawCall.offset shl 1)
		}
		unbind()
	}

	override val highestIndex: Short
		get() {
			return _highestIndex
		}

	override fun putIndex(index: Short) {
		indices.put(index)
		if (index > _highestIndex) _highestIndex = index
	}

	override fun putVertexComponent(value: Float) {
		vertexComponents.put(value)
	}

	/**
	 * Converts the properties position, normal, colorTint, textureCoord into the expected vertex component order for
	 * this batch.
	 * @see vertexAttributes
	 */
	private val adapter = StandardAttributesAdapter(this)

	override fun putVertex(positionX: Float, positionY: Float, positionZ: Float, normalX: Float, normalY: Float, normalZ: Float, colorR: Float, colorG: Float, colorB: Float, colorA: Float, u: Float, v: Float) {
		adapter.putVertex(positionX, positionY, positionZ, normalX, normalY, normalZ, colorR, colorG, colorB, colorA, u, v)
	}

	override fun dispose() {
		gl.deleteBuffer(vertexComponentsBuffer)
		gl.deleteBuffer(indicesBuffer)
	}
}
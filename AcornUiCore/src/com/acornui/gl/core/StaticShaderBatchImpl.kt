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
import com.acornui.core.Disposable
import com.acornui.core.io.BufferFactory
import com.acornui.gl.component.drawing.DrawElementsCall
import com.acornui.graphics.ColorRo
import com.acornui.io.NativeBuffer
import com.acornui.math.Vector3Ro


/**
 * A static shader batch will remember the draw calls, allowing the batch to be rendered without needing to buffer data
 * at a later time.
 * @author nbilyk
 */
class StaticShaderBatchImpl(
		private val gl: Gl20,
		val glState: GlState,
		maxIndices: Int = 32767,
		maxVertexComponents: Int = 32767 * 16
) : ShaderBatch, Clearable, Disposable {

	var vertexAttributes: VertexAttributes = standardVertexAttributes

	private val vertexComponentsBuffer: GlBufferRef = gl.createBuffer()
	private val indicesBuffer: GlBufferRef = gl.createBuffer()

	private val drawCalls = ArrayList<DrawElementsCall>()

	/**
	 * The draw mode if no indices are supplied.
	 * Possible values are: POINTS, LINE_STRIP, LINE_LOOP, LINES, TRIANGLE_STRIP, TRIANGLE_FAN, or TRIANGLES.
	 */
	private var _drawMode: Int = Gl20.TRIANGLES

	private val indices: NativeBuffer<Short> = BufferFactory.instance.shortBuffer(maxIndices)
	private val vertexComponents: NativeBuffer<Float> = BufferFactory.instance.floatBuffer(maxVertexComponents)
	private var _highestIndex: Short = -1

	private var needsUpload = false

	override val currentDrawMode: Int
		get() = _drawMode

	override fun resetRenderCount() {
	}

	override val renderCount: Int
		get() {
			return drawCalls.size
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
		if (_drawMode == drawMode) {
			flush(false)
		} else {
			flush(true)
			_drawMode = drawMode
		}
	}

	override fun clear() {
		// Recycle the draw calls.
		for (i in 0..drawCalls.lastIndex) {
			drawCalls[i].free()
		}
		drawCalls.clear()
		indices.clear()
		vertexComponents.clear()
		_highestIndex = -1
	}

	override fun flush(force: Boolean) {
		if (!force) return
		val lastDrawCall = drawCalls.lastOrNull()
		val offset = if (lastDrawCall == null) 0 else lastDrawCall.offset + lastDrawCall.count
		val count = indices.position - offset
		if (count <= 0) return // Nothing to draw.

		val drawCall = DrawElementsCall.obtain()
		drawCall.offset = offset
		drawCall.count = count
		drawCall.texture = glState.getTexture(0)
		drawCall.blendMode = glState.blendMode
		drawCall.premultipliedAlpha = glState.premultipliedAlpha
		drawCall.mode = _drawMode
		drawCalls.add(drawCall)
		needsUpload = true
	}

	fun render() {
		if (drawCalls.isEmpty()) return

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

		for (i in 0..drawCalls.lastIndex) {
			val drawCall = drawCalls[i]
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

	override fun putVertex(position: Vector3Ro, normal: Vector3Ro, colorTint: ColorRo, u: Float, v: Float) {
		vertexAttributes.putVertex(vertexComponents, position, normal, colorTint, u, v)
	}

	override fun dispose() {
		gl.deleteBuffer(vertexComponentsBuffer)
		gl.deleteBuffer(indicesBuffer)
	}
}
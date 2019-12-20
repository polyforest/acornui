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

import com.acornui._assert
import com.acornui.assertionsEnabled
import com.acornui.Disposable
import com.acornui.di.Scoped
import com.acornui.di.inject
import com.acornui.graphic.BlendMode
import com.acornui.graphic.Color
import com.acornui.graphic.TextureRo
import com.acornui.graphic.rgbData
import com.acornui.io.resizableFloatBuffer
import com.acornui.io.resizableShortBuffer
import com.acornui.recycle.Clearable
import com.acornui.recycle.ClearableObjectPool
import com.acornui.recycle.freeAll

/**
 *
 * @author nbilyk
 */
open class ShaderBatchImpl(

		private val gl: CachedGl20,

		/**
		 * If true, when this batch is flushed, the data will be uploaded to the gpu, rendered, then cleared.
		 */
		var isDynamic: Boolean = true
) : ShaderBatch, Disposable {

	override val vertexAttributes: VertexAttributes = standardVertexAttributes

	/**
	 * One of [Gl20.DYNAMIC_DRAW], [Gl20.STATIC_DRAW], or [Gl20.STREAM_DRAW]
	 */
	var usage: Int = Gl20.DYNAMIC_DRAW

	override val vertexComponentsCount: Int
		get() = vertexComponents.position
	override val indicesCount: Int
		get() = indices.position

	private var vertexComponentsBuffer: GlBufferRef? = null
	private var indicesBuffer: GlBufferRef? = null

	private var drawCall: DrawElementsCall = DrawElementsCall.obtain()

	override val indices = resizableShortBuffer(2048)
	override val vertexComponents = resizableFloatBuffer(4096)
	override val drawCalls: MutableList<DrawElementsCall> = ArrayList()
	private var _highestIndex: Short = -1

	private val defaultWhitePixel by lazy {
		rgbTexture(gl, rgbData(1, 1, hasAlpha = true) { setPixel(0, 0, Color.WHITE) }) {
			filterMin = TextureMinFilter.NEAREST
			filterMag = TextureMagFilter.NEAREST
			refInc()
		}
	}

	override val whitePixel: TextureRo
		get() = if (drawCall.texture?.hasWhitePixel == true) drawCall.texture!! else defaultWhitePixel

	override fun putIndex(index: Short) {
		indices.put(index)
		if (index > _highestIndex) _highestIndex = index
	}

	private fun bind() {
		gl.bindBuffer(Gl20.ARRAY_BUFFER, vertexComponentsBuffer)
		gl.bindBuffer(Gl20.ELEMENT_ARRAY_BUFFER, indicesBuffer)
		vertexAttributes.bind(gl)
	}

	private fun unbind() {
		vertexAttributes.unbind(gl)
		gl.bindBuffer(Gl20.ARRAY_BUFFER, null)
		gl.bindBuffer(Gl20.ELEMENT_ARRAY_BUFFER, null)
	}

	override fun begin(texture: TextureRo, blendMode: BlendMode, premultipliedAlpha: Boolean, drawMode: Int) {
		if (drawCall.texture == texture &&
				drawCall.blendMode == blendMode &&
				drawCall.premultipiedAlpha == premultipliedAlpha &&
				drawCall.drawMode == drawMode) {
			// No change, flush only if we're nearing capacity
			if (!isDynamic || highestIndex < Short.MAX_VALUE * 0.75f) return
		}
		// Needs a flush
		flush()
		drawCall.texture = texture
		drawCall.blendMode = blendMode
		drawCall.premultipiedAlpha = premultipliedAlpha
		drawCall.drawMode = drawMode
	}

	override fun flush() {
		val vertexComponentsL = vertexComponents.position
		val indicesL = indices.position
		if (vertexComponentsL == 0) {
			check(indicesL == 0) { "Indices pushed, but no vertices" }
			return
		}
		if (assertionsEnabled) {
			// If assertions are enabled, check that we have rational vertex and index counts.
			val vertexSize = vertexAttributes.vertexSize
			_assert(vertexComponentsL % vertexSize == 0, "vertexData size $vertexComponentsL not evenly divisible by vertexSize value $vertexSize")
			if (drawCall.drawMode == Gl20.LINES) {
				_assert(indicesL % 2 == 0, "indices size $indicesL not evenly divisible by 2")
			} else if (drawCall.drawMode == Gl20.TRIANGLES) {
				_assert(indicesL % 3 == 0, "indices size $indicesL not evenly divisible by 3")
			}
		}
		val lastDrawCall = drawCalls.lastOrNull()
		val offset = if (lastDrawCall == null) 0 else lastDrawCall.offset + lastDrawCall.count
		val count = indices.position - offset
		if (count <= 0) return // Nothing to draw.
		require(highestIndex < count + offset)
		drawCall.offset = offset
		drawCall.count = count
		drawCalls.add(drawCall)
		drawCall = DrawElementsCall.obtain()

		if (isDynamic) {
			upload()
			render()
			clear()
		}
	}

	override val highestIndex: Short
		get() = _highestIndex

	override fun putVertexComponent(value: Float) {
		vertexComponents.put(value)
	}

	override fun putVertex(positionX: Float, positionY: Float, positionZ: Float, normalX: Float, normalY: Float, normalZ: Float, colorR: Float, colorG: Float, colorB: Float, colorA: Float, u: Float, v: Float) {
		vertexComponents.put(positionX)
		vertexComponents.put(positionY)
		vertexComponents.put(positionZ)
		vertexComponents.put(normalX)
		vertexComponents.put(normalY)
		vertexComponents.put(normalZ)
		vertexComponents.put(colorR)
		vertexComponents.put(colorG)
		vertexComponents.put(colorB)
		vertexComponents.put(colorA)
		vertexComponents.put(u)
		vertexComponents.put(v)
	}

	/**
	 * Creates the gpu buffers and uploads the static vertex and index data.
	 */
	override fun upload() {
		if (vertexComponentsBuffer == null) {
			vertexComponentsBuffer = gl.createBuffer()
			indicesBuffer = gl.createBuffer()
		}
		flush()
		gl.bindBuffer(Gl20.ARRAY_BUFFER, vertexComponentsBuffer)
		gl.bindBuffer(Gl20.ELEMENT_ARRAY_BUFFER, indicesBuffer)
		indices.flip()
		gl.bufferData(Gl20.ELEMENT_ARRAY_BUFFER, indices.limit shl 1, usage) // Allocate
		gl.bufferDatasv(Gl20.ELEMENT_ARRAY_BUFFER, indices, usage) // Upload

		vertexComponents.flip()
		gl.bufferData(Gl20.ARRAY_BUFFER, vertexComponents.limit shl 2, usage) // Allocate
		gl.bufferDatafv(Gl20.ARRAY_BUFFER, vertexComponents, usage) // Upload
		gl.bindBuffer(Gl20.ARRAY_BUFFER, null)
		gl.bindBuffer(Gl20.ELEMENT_ARRAY_BUFFER, null)
	}

	/**
	 * Deletes the buffers from the gpu.
	 */
	override fun delete() {
		if (vertexComponentsBuffer == null) return
		gl.deleteBuffer(vertexComponentsBuffer!!)
		gl.deleteBuffer(indicesBuffer!!)
		vertexComponentsBuffer = null
		indicesBuffer = null
	}

	override fun render() {
		require(vertexComponentsBuffer != null) { "StaticShaderBatch must be uploaded first." }
		if (drawCalls.isEmpty()) return
		gl.batch.flush()
		ShaderBatch.totalDrawCalls += drawCalls.size
		bind()
		gl.activeTexture()
		for (i in 0..drawCalls.lastIndex) {
			val drawCall = drawCalls[i]
			val texture = drawCall.texture!!
			gl.bindTexture(texture.target.value, texture.textureHandle!!)
			drawCall.blendMode.applyBlending(gl, drawCall.premultipiedAlpha)
			gl.drawElements(drawCall.drawMode, drawCall.count, Gl20.UNSIGNED_SHORT, drawCall.offset shl 1)
		}
		unbind()
	}

	override fun clear() {
		// Recycle the draw calls.
		DrawElementsCall.freeAll(drawCalls)
		drawCalls.clear()
		drawCall.clear()
		indices.clear()
		vertexComponents.clear()
		_highestIndex = -1
	}

	override fun dispose() {
		clear()
		delete()
	}
}

fun Scoped.shaderBatch(): ShaderBatchImpl {
	return ShaderBatchImpl(inject(CachedGl20))
}



class DrawElementsCall private constructor() : Clearable {

	var texture: TextureRo? = null
	var blendMode = BlendMode.NORMAL
	var premultipiedAlpha = false
	var drawMode = Gl20.TRIANGLES

	var count = 0
	var offset = 0

	override fun clear() {
		texture = null
		blendMode = BlendMode.NORMAL
		premultipiedAlpha = false
		drawMode = Gl20.TRIANGLES
		count = 0
		offset = 0
	}

	companion object {
		private val pool = ClearableObjectPool { DrawElementsCall() }

		fun obtain(): DrawElementsCall {
			return pool.obtain()
		}

		fun free(call: DrawElementsCall) {
			pool.free(call)
		}

		fun freeAll(calls: List<DrawElementsCall>) {
			pool.freeAll(calls)
		}
	}
}
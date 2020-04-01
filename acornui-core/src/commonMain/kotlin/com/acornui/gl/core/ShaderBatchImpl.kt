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

import com.acornui.Disposable
import com.acornui.assertionsEnabled
import com.acornui.di.Context
import com.acornui.gl.core.Gl20.Companion.LINES
import com.acornui.gl.core.Gl20.Companion.LINE_LOOP
import com.acornui.gl.core.Gl20.Companion.LINE_STRIP
import com.acornui.gl.core.Gl20.Companion.POINTS
import com.acornui.gl.core.Gl20.Companion.TRIANGLES
import com.acornui.gl.core.Gl20.Companion.TRIANGLE_FAN
import com.acornui.gl.core.Gl20.Companion.TRIANGLE_STRIP
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

		private val gl: Gl20,

		/**
		 * If true, when this batch is flushed, the data will be uploaded to the gpu, rendered, then cleared.
		 */
		override var isDynamic: Boolean = true
) : ShaderBatch, Disposable {

	/**
	 * Unwraps the cached gl [CachedGl20.wrapped]
	 */
	constructor(gl: CachedGl20, isDynamic: Boolean = true) : this(gl.wrapped, isDynamic)

	init {
		require(gl !is CachedGl20) { "ShaderBatchImpl requires either the unwrapped Gl20 instance or use the CachedGl20 constructor" }
	}

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

	private val defaultWhitePixel: RgbTexture = getWhitePixel(gl).also { it.refInc() }

	override val whitePixel: TextureRo
		get() = if (drawCall.texture?.hasWhitePixel == true) drawCall.texture!! else defaultWhitePixel

	override val currentDrawCall: DrawElementsCallRo
		get() = drawCall

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
		if (assertionsEnabled) {
			checkVertexComponents()
		}
		if (drawCall.texture == texture &&
				drawCall.blendMode == blendMode &&
				drawCall.premultipiedAlpha == premultipliedAlpha &&
				drawCall.drawMode == drawMode) {
			// No change, flush only if we're nearing capacity
			if (highestIndex < Short.MAX_VALUE * 0.75f) return
		}
		// Needs a flush
		flush()
		drawCall.texture = texture
		drawCall.blendMode = blendMode
		drawCall.premultipiedAlpha = premultipliedAlpha
		drawCall.drawMode = drawMode
	}

	override fun flush() {
		if (assertionsEnabled)
			checkVertexComponents()
		drawCall.indexCount = indicesCount - drawCall.indexOffset
		drawCall.vertexComponentsCount = vertexComponentsCount - drawCall.vertexComponentOffset
		if (drawCall.isEmpty) return // Nothing to draw

		val count = if (drawCall.hasElements) drawCall.indexCount else drawCall.vertexComponentsCount

		if (assertionsEnabled) {
			when (drawCall.drawMode) {
				LINES -> check(count % 2 == 0) { "count <$count> not evenly divisible by 2 (Gl20.LINES)" }
				TRIANGLES -> check(count % 3 == 0) { "count <$count> not evenly divisible by 3 (Gl20.TRIANGLES)" }
				TRIANGLE_STRIP, TRIANGLE_FAN -> check(count > 2) { "count must be at least 3" }
			}
		}
		require(highestIndex < drawCall.indexCount + drawCall.indexOffset)
		drawCalls.add(drawCall)
		drawCall = DrawElementsCall.obtain()
		drawCall.indexOffset = indicesCount
		drawCall.vertexComponentOffset = vertexComponentsCount

		if (isDynamic) {
			upload()
			render()
			clear()
		}
	}

	private fun checkVertexComponents() {
		// If assertions are enabled, check that we have rational vertex and index counts.
		val vertexComponentsL = vertexComponents.position
		val vertexSize = vertexAttributes.vertexSize
		check(vertexComponentsL % vertexSize == 0) { "vertexData size $vertexComponentsL not evenly divisible by vertexSize value $vertexSize" }
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
		val indicesP = indices.position
		indices.flip()
		gl.bindBuffer(Gl20.ELEMENT_ARRAY_BUFFER, indicesBuffer)
		gl.bufferData(Gl20.ELEMENT_ARRAY_BUFFER, indices.limit shl 1, usage) // Allocate
		gl.bufferDatasv(Gl20.ELEMENT_ARRAY_BUFFER, indices, usage) // Upload
		gl.bindBuffer(Gl20.ELEMENT_ARRAY_BUFFER, null)
		indices.position = indicesP

		val vertexComponentsP = vertexComponents.position
		vertexComponents.flip()
		gl.bindBuffer(Gl20.ARRAY_BUFFER, vertexComponentsBuffer)
		gl.bufferData(Gl20.ARRAY_BUFFER, vertexComponents.limit shl 2, usage) // Allocate
		gl.bufferDatafv(Gl20.ARRAY_BUFFER, vertexComponents, usage) // Upload
		gl.bindBuffer(Gl20.ARRAY_BUFFER, null)
		vertexComponents.position = vertexComponentsP
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
		flush()
		ShaderBatch.totalDrawCalls += drawCalls.size
		bind()
		gl.activeTexture()
		for (i in 0..drawCalls.lastIndex) {
			val drawCall = drawCalls[i]
			val texture = drawCall.texture!!
			gl.bindTexture(texture.target.value, texture.textureHandle!!)
			drawCall.blendMode.applyBlending(gl, drawCall.premultipiedAlpha)

			if (drawCall.hasElements)
				gl.drawElements(drawCall.drawMode, drawCall.indexCount, Gl20.UNSIGNED_SHORT, drawCall.indexOffset shl 1)
			else
				gl.drawArrays(drawCall.drawMode, drawCall.vertexComponentOffset / vertexAttributes.vertexSize, drawCall.vertexComponentsCount / vertexAttributes.vertexSize)
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
		defaultWhitePixel.refDec()
		clear()
		delete()
	}

	companion object {

		private val whitePixelCache = HashMap<Gl20, RgbTexture>()

		private fun getWhitePixel(gl: Gl20): RgbTexture {
			return whitePixelCache.getOrPut(gl) {
				val whitePixelData = rgbData(1, 1, hasAlpha = true) { setPixel(0, 0, Color.WHITE) }
				rgbTexture(gl, whitePixelData, "whitePixel") {
					filterMin = TextureMinFilter.NEAREST
					filterMag = TextureMagFilter.NEAREST
				}
			}
		}
	}
}

fun Context.shaderBatch(): ShaderBatchImpl {
	return ShaderBatchImpl(inject(CachedGl20).wrapped)
}

interface DrawElementsCallRo {

	/**
	 * The texture to set on unit 0.
	 * @see Gl20.activeTexture
	 */
	val texture: TextureRo?

	/**
	 * The blend mode to use for [Gl20.blendFunc]
	 * [BlendMode.applyBlending] will be invoked before the draw.
	 */
	val blendMode: BlendMode

	/**
	 * The premultiplied value to be passed to [BlendMode.applyBlending].
	 */
	val premultipiedAlpha: Boolean

	/**
	 * The draw mode for [Gl20.drawElements] or [Gl20.drawArrays]
	 * Must be one of [POINTS], [LINE_STRIP], [LINE_LOOP], [LINES], [TRIANGLE_STRIP], [TRIANGLE_FAN], or [TRIANGLES].
	 */
	val drawMode: Int

	/**
	 * The number of index elements pushed since the last draw call.
	 */
	val indexCount: Int

	/**
	 * The number of vertex components pushed since the last draw call.
	 */
	val vertexComponentsCount: Int

	/**
	 * The position offset in the index buffer.
	 * 
	 * NB: This is not a byte offset, to convert to the byte offset [Gl20.drawElements] is expecting, this should be 
	 * `shl 1` to represent the byte offset for `Short` indices. 
	 */
	val indexOffset: Int

	/**
	 * The first vertex component index.
	 * NB: For [Gl20.drawArrays] this can be divided by the number of components per vertex to get the `first` argument.
	 */
	val vertexComponentOffset: Int

	/**
	 * True if indices were pushed.
	 */
	val hasElements: Boolean
		get() = indexCount > 0

	val isEmpty: Boolean
		get() = indexCount == 0 && vertexComponentsCount == 0
}

class DrawElementsCall private constructor() : DrawElementsCallRo, Clearable {

	override var texture: TextureRo? = null
	override var blendMode = BlendMode.NORMAL
	override var premultipiedAlpha = false
	override var drawMode = TRIANGLES

	override var indexCount = 0
	override var vertexComponentsCount = 0
	override var indexOffset = 0
	override var vertexComponentOffset = 0

	override fun clear() {
		texture = null
		blendMode = BlendMode.NORMAL
		premultipiedAlpha = false
		drawMode = TRIANGLES
		indexCount = 0
		vertexComponentsCount = 0
		indexOffset = 0
		vertexComponentOffset = 0
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
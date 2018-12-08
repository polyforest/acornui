/*
 * Copyright 2015 Nicholas Bilyk
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

package com.acornui.js.gl

import com.acornui.core.graphic.BlendMode
import com.acornui.core.graphic.RgbData
import com.acornui.core.io.byteBuffer
import com.acornui.gl.core.*
import com.acornui.math.Matrix4
import org.w3c.dom.HTMLImageElement
import kotlin.browser.document

/**
 * @author nbilyk
 */
class WebGlTexture(
		gl: Gl20,
		glState: GlState
) : GlTextureBase(gl, glState) {

	val image = document.createElement("img") as HTMLImageElement

	override val width: Int
		get() {
			return image.naturalWidth
		}

	override val height: Int
		get() {
			return image.naturalHeight
		}

	private val _rgbData by lazy {
		// Creates a temporary frame buffer, draws the image to that, uses gl readPixels to get the data, then disposes.
		val batch = glState.batch
		val previousShader = glState.shader
		refInc()
		val framebuffer = Framebuffer(gl, glState, width, height, false, false)
		framebuffer.begin()
		glState.viewProjection = Matrix4.IDENTITY
		glState.setTexture(this)
		glState.blendMode(BlendMode.NORMAL, false)
		batch.putVertex(-1f, -1f, 0f, u = 0f, v = 0f)
		batch.putVertex(1f, -1f, 0f, u = 1f, v = 0f)
		batch.putVertex(1f, 1f, 0f, u = 1f, v = 1f)
		batch.putVertex(-1f, 1f, 0f, u = 0f, v = 1f)
		batch.putQuadIndices()
		batch.flush()
		val pixelData = byteBuffer(width * height * 4)
		gl.readPixels(0, 0, width, height, Gl20.RGBA, Gl20.UNSIGNED_BYTE, pixelData)
		framebuffer.end()
		glState.shader = previousShader
		framebuffer.dispose()
		val rgbData = RgbData(width, height, true)
		val bytes = rgbData.bytes
		var i = 0
		while (pixelData.hasRemaining) {
			bytes[i++] = pixelData.get()
		}
		refDec()
		rgbData
	}

	override val rgbData: RgbData
		get() = _rgbData
}
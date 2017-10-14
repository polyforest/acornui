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

package com.acornui.js.dom

import com.acornui.core.graphics.RgbData
import com.acornui.core.graphics.Texture
import com.acornui.gl.core.*
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.ArrayBufferView
import org.khronos.webgl.Uint8ClampedArray
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import kotlin.browser.document

/**
 * @author nbilyk
 */
class DomTexture: Texture {

	override var target: TextureTarget = TextureTarget.TEXTURE_2D

	/**
	 * Possible values:
	 * NEAREST, LINEAR
	 */
	override var filterMag: TextureMagFilter = TextureMagFilter.NEAREST

	/**
	 * Possible values:
	 * NEAREST, LINEAR, NEAREST_MIPMAP_NEAREST, LINEAR_MIPMAP_NEAREST, NEAREST_MIPMAP_LINEAR, LINEAR_MIPMAP_LINEAR
	 */
	override var filterMin: TextureMinFilter = TextureMinFilter.NEAREST

	/**
	 * Possible values:
	 * REPEAT, CLAMP_TO_EDGE, MIRRORED_REPEAT
	 */
	override var wrapS: TextureWrapMode = TextureWrapMode.CLAMP_TO_EDGE
	override var wrapT: TextureWrapMode = TextureWrapMode.CLAMP_TO_EDGE

	override var pixelFormat: TexturePixelFormat = TexturePixelFormat.RGBA
	override var pixelType: TexturePixelType = TexturePixelType.UNSIGNED_BYTE

	override var textureHandle: GlTextureRef? = null

	override var hasWhitePixel: Boolean = false

	val image: HTMLImageElement = document.createElement("img") as HTMLImageElement
	private var _src: String? = null

	override val width: Int
		get() = image.naturalWidth

	override val height: Int
		get() = image.naturalHeight

	val src: String
		get() = _src!!

	private fun setSrc(value: String?) {
		if (_src == value) return
		if (_src != null) {
			URL.revokeObjectURL(_src!!)
			_src = null
		}
		if (value != null) {
			_src = value
			image.src = value
		}
	}

	fun arrayBuffer(value: ArrayBuffer) {
		arrayBufferView(Uint8ClampedArray(value))
	}

	fun arrayBufferView(value: ArrayBufferView) {
		blob(Blob(arrayOf(value)))
	}

	fun blob(value: Blob) {
		setSrc(URL.createObjectURL(value))
	}

	override fun refInc() {
	}

	override val rgbData: RgbData
		get() {
			throw UnsupportedOperationException()
		}

	override fun refDec() {
	}
}
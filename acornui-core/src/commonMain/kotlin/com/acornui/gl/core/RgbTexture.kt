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

import com.acornui.di.Context
import com.acornui.graphic.RgbData
import com.acornui.io.byteBuffer

/**
 * A texture wrapping an [RgbData] instance.
 */
class RgbTexture(
		gl: Gl20,
		override val rgbData: RgbData,
		displayName: String? = null
) : GlTextureBase(gl, displayName) {

	init {
		pixelFormat = if (rgbData.hasAlpha) TexturePixelFormat.RGBA else TexturePixelFormat.RGB
	}

	override val widthPixels: Int
		get() = rgbData.width

	override val heightPixels: Int
		get() = rgbData.height
	
	override fun uploadTexture() {
		val buffer = byteBuffer(rgbData.bytes.size)
		for (i in 0..rgbData.bytes.lastIndex) {
			buffer.put(rgbData.bytes[i])
		}
		buffer.flip()
		gl.texImage2Db(target.value, 0, pixelFormat.value, rgbData.width, rgbData.height, 0, pixelFormat.value, pixelType.value, buffer)
	}
}


fun rgbTexture(gl: Gl20, rgbData: RgbData, displayName: String? = null, init: RgbTexture.() -> Unit = {}) =
		RgbTexture(gl, rgbData, displayName).apply(init)

fun Context.rgbTexture(rgbData: RgbData, displayName: String? = null, init: RgbTexture.() -> Unit = {}) = 
		RgbTexture(inject(CachedGl20), rgbData, displayName).apply(init)

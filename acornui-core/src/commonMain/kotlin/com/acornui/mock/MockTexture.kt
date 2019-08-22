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

package com.acornui.mock

import com.acornui.gl.core.*
import com.acornui.graphic.RgbData
import com.acornui.graphic.Texture

object MockTexture : Texture {

	override fun refInc() {
	}

	override fun refDec() {
	}

	override var target: TextureTarget = TextureTarget.TEXTURE_2D
	override var filterMag: TextureMagFilter = TextureMagFilter.LINEAR
	override var filterMin: TextureMinFilter = TextureMinFilter.LINEAR
	override var wrapS: TextureWrapMode = TextureWrapMode.CLAMP_TO_EDGE
	override var wrapT: TextureWrapMode = TextureWrapMode.CLAMP_TO_EDGE
	override var pixelFormat: TexturePixelFormat = TexturePixelFormat.ALPHA
	override var pixelType: TexturePixelType = TexturePixelType.UNSIGNED_BYTE
	override var textureHandle: GlTextureRef? = null
	override var hasWhitePixel: Boolean = false
	override val rgbData: RgbData = RgbData(1, 1, false)
	override val widthPixels: Int = 1
	override val heightPixels: Int = 1
}
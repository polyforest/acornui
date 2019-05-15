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

package com.acornui.core.graphic

import com.acornui.gl.core.*

interface Texture {

	var target: TextureTarget

	/**
	 * Possible values:
	 * NEAREST, LINEAR
	 */
	var filterMag: TextureMagFilter

	/**
	 * Possible values:
	 * NEAREST, LINEAR, NEAREST_MIPMAP_NEAREST, LINEAR_MIPMAP_NEAREST, NEAREST_MIPMAP_LINEAR, LINEAR_MIPMAP_LINEAR
	 */
	var filterMin: TextureMinFilter

	/**
	 * Possible values:
	 * REPEAT, CLAMP_TO_EDGE, MIRRORED_REPEAT
	 */
	var wrapS: TextureWrapMode
	var wrapT: TextureWrapMode

	var pixelFormat: TexturePixelFormat
	var pixelType: TexturePixelType

	var textureHandle: GlTextureRef?

	/**
	 * In a texture atlas, the 0,0 pixel may be set to white in order to allow vector drawing to be
	 * done in the same batch.
	 */
	var hasWhitePixel: Boolean

	/**
	 * Increments the number of places this Texture is used. If this Texture was previously not referenced,
	 * this texture will be initialized and uploaded to the GPU.
	 */
	fun refInc()

	/**
	 * Decrements the number of places this Texture is used. If the count reaches zero, the texture will be unloaded
	 * from the gpu.
	 */
	fun refDec()

	/**
	 * Returns an RgbData object representing the bitmap data for this texture.
	 * TODO: Not all Texture implementations currently support this feature.
	 */
	val rgbData: RgbData

	/**
	 * The natural width of this texture
	 */
	val width: Int

	/**
	 * The natural height of this texture
	 */
	val height: Int

}

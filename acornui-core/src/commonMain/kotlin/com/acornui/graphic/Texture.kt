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

package com.acornui.graphic

import com.acornui.gl.core.*

interface TextureRo {

	/**
	 * The total number of components using this texture.
	 * This is used to determine whether the texture should be created or deleted from the gpu.
	 */
	val refCount: Int

	val target: TextureTarget

	/**
	 * Possible values:
	 * NEAREST, LINEAR
	 */
	val filterMag: TextureMagFilter

	/**
	 * Possible values:
	 * NEAREST, LINEAR, NEAREST_MIPMAP_NEAREST, LINEAR_MIPMAP_NEAREST, NEAREST_MIPMAP_LINEAR, LINEAR_MIPMAP_LINEAR
	 */
	val filterMin: TextureMinFilter

	/**
	 * Possible values:
	 * REPEAT, CLAMP_TO_EDGE, MIRRORED_REPEAT
	 */
	val wrapS: TextureWrapMode
	val wrapT: TextureWrapMode

	val pixelFormat: TexturePixelFormat
	val pixelType: TexturePixelType

	val textureHandle: GlTextureRef?

	/**
	 * In a texture atlas, the 0,0 pixel may be set to white in order to allow vector drawing to be
	 * done in the same batch.
	 */
	val hasWhitePixel: Boolean

	/**
	 * Returns an RgbData object representing the bitmap data for this texture, or null if the texture implementation
	 * doesn't support rasterization.
	 */
	val rgbData: RgbData?

	/**
	 * The natural width of this texture, in pixels.
	 */
	val widthPixels: Int

	/**
	 * The natural height of this texture, in pixels.
	 */
	val heightPixels: Int

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

}

interface Texture : TextureRo {

	override var target: TextureTarget

	/**
	 * Possible values:
	 * NEAREST, LINEAR
	 */
	override var filterMag: TextureMagFilter

	/**
	 * Possible values:
	 * NEAREST, LINEAR, NEAREST_MIPMAP_NEAREST, LINEAR_MIPMAP_NEAREST, NEAREST_MIPMAP_LINEAR, LINEAR_MIPMAP_LINEAR
	 */
	override var filterMin: TextureMinFilter

	/**
	 * Possible values:
	 * REPEAT, CLAMP_TO_EDGE, MIRRORED_REPEAT
	 */
	override var wrapS: TextureWrapMode
	override var wrapT: TextureWrapMode

	override var pixelFormat: TexturePixelFormat
	override var pixelType: TexturePixelType

	override var textureHandle: GlTextureRef?

	/**
	 * In a texture atlas, the 0,0 pixel may be set to white in order to allow vector drawing to be
	 * done in the same batch.
	 */
	override var hasWhitePixel: Boolean

	/**
	 * Returns an RgbData object representing the bitmap data for this texture.
	 */
	override val rgbData: RgbData?

	override val widthPixels: Int

	override val heightPixels: Int

}

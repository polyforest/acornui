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

package com.acornui.gl.core

import com.acornui.core.graphics.RgbData
import com.acornui.core.graphics.Texture
import com.acornui.logging.Log
import com.acornui.math.MathUtils

/**
 * Textures are provided by the AssetManager. The Renderer implementation will cast the Texture to the back-end's
 * implementation.
 * @author nbilyk
 */
abstract class GlTextureBase(
		protected val gl: Gl20,
		protected val glState: GlState
) : Texture {

	/**
	 * The total number of components using this texture.
	 * This is used to determine whether the texture should be created or deleted from the gpu.
	 */
	private var refCount: Int = 0

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

	/**
	 * Increments the number of places this Texture is used. If this Texture was previously not referenced,
	 * this texture will be initialized and uploaded to the GPU.
	 */
	override fun refInc() {
		if (refCount++ == 0) {
			create()
		}
	}

	/**
	 * Creates the texture if it hasn't already been created. More often one should use [refInc] instead, in order to
	 * allow multiple references to the same Texture.
	 */
	open fun create(unit: Int = 0) {
		if (textureHandle != null) return
		textureHandle = gl.createTexture()
		glState.setTexture(this, unit)

		uploadTexture()
		gl.texParameteri(target.value, Gl20.TEXTURE_MAG_FILTER, filterMag.value)
		gl.texParameteri(target.value, Gl20.TEXTURE_MIN_FILTER, filterMin.value)
		gl.texParameteri(target.value, Gl20.TEXTURE_WRAP_S, wrapS.value)
		gl.texParameteri(target.value, Gl20.TEXTURE_WRAP_T, wrapT.value)

		if (filterMin.useMipMap) {
			if (!supportsNpot() && (!MathUtils.isPowerOfTwo(width) || !MathUtils.isPowerOfTwo(height))) {
				Log.warn("MipMaps cannot be generated for non power of two textures (${width}x$height)")
				gl.texParameteri(target.value, Gl20.TEXTURE_MIN_FILTER, TextureMinFilter.LINEAR.value)
			} else {
				gl.generateMipmap(target.value)
			}
		}
	}

	private var _supportsNpot: Boolean? = null

	protected fun supportsNpot(): Boolean {
		if (_supportsNpot == null) {
			val extensions = gl.getSupportedExtensions()
			_supportsNpot = extensions.contains("GL_ARB_texture_non_power_of_two")
		}
		return _supportsNpot!!
	}

	protected open fun uploadTexture() {
		gl.texImage2D(target.value, 0, pixelFormat.value, pixelFormat.value, pixelType.value, this)
	}

	/**
	 * Returns an RgbData object representing the bitmap data for this texture.
	 * Not all Texture implementations support this feature.
	 */
	override val rgbData: RgbData
		get() {
			throw UnsupportedOperationException("This Texture implementation not support rgbData")
		}

	/**
	 * Decrements the number of places this Texture is used. If the count reaches zero, the texture will be deleted.
	 */
	override fun refDec() {
		if (refCount == 0) throw Exception("Texture refInc/refDec is not paired correctly.")
		if (--refCount == 0) {
			delete()
		}
	}

	/**
	 * Deletes the texture if it was ever created. More often one should use [refDec] instead, in order to allow
	 * multiple references to the same Texture.
	 */
	open fun delete() {
		if (textureHandle == null) return
		glState.unsetTexture(this)
		gl.deleteTexture(textureHandle!!)
		textureHandle = null
	}

}


enum class TextureTarget(val value: Int) {
	TEXTURE_2D(Gl20.TEXTURE_2D),
	TEXTURE_CUBE_MAP(Gl20.TEXTURE_CUBE_MAP),
	TEXTURE_CUBE_MAP_POSITIVE_X(Gl20.TEXTURE_CUBE_MAP_POSITIVE_X),
	TEXTURE_CUBE_MAP_NEGATIVE_X(Gl20.TEXTURE_CUBE_MAP_NEGATIVE_X),
	TEXTURE_CUBE_MAP_POSITIVE_Y(Gl20.TEXTURE_CUBE_MAP_POSITIVE_Y),
	TEXTURE_CUBE_MAP_NEGATIVE_Y(Gl20.TEXTURE_CUBE_MAP_NEGATIVE_Y),
	TEXTURE_CUBE_MAP_POSITIVE_Z(Gl20.TEXTURE_CUBE_MAP_POSITIVE_Z),
	TEXTURE_CUBE_MAP_NEGATIVE_Z(Gl20.TEXTURE_CUBE_MAP_NEGATIVE_Z);

	companion object {
		val VALUES by lazy { values() }
	}
}

enum class TextureMagFilter(val value: Int) {
	NEAREST(Gl20.NEAREST),
	LINEAR(Gl20.LINEAR)
}

enum class TextureMinFilter(val value: Int, val useMipMap: Boolean = true) {
	NEAREST(Gl20.NEAREST, false),
	LINEAR(Gl20.LINEAR, false),
	NEAREST_MIPMAP_NEAREST(Gl20.NEAREST_MIPMAP_NEAREST),
	LINEAR_MIPMAP_NEAREST(Gl20.LINEAR_MIPMAP_NEAREST),
	NEAREST_MIPMAP_LINEAR(Gl20.NEAREST_MIPMAP_LINEAR),
	LINEAR_MIPMAP_LINEAR(Gl20.LINEAR_MIPMAP_LINEAR)
}

enum class TextureWrapMode(val value: Int) {
	REPEAT(Gl20.REPEAT),
	CLAMP_TO_EDGE(Gl20.CLAMP_TO_EDGE),
	MIRRORED_REPEAT(Gl20.MIRRORED_REPEAT)
}

enum class TexturePixelFormat(val value: Int, val bytesPerPixel: Int) {
	DEPTH_COMPONENT(Gl20.DEPTH_COMPONENT, 1),
	ALPHA(Gl20.ALPHA, 1),
	RGB(Gl20.RGB, 3),
	RGBA(Gl20.RGBA, 4),
	LUMINANCE(Gl20.LUMINANCE, 1),
	LUMINANCE_ALPHA(Gl20.LUMINANCE_ALPHA, 2)
}

enum class TexturePixelType(val value: Int) {
	UNSIGNED_BYTE(Gl20.UNSIGNED_BYTE),
	UNSIGNED_SHORT_5_6_5(Gl20.UNSIGNED_SHORT_5_6_5),
	UNSIGNED_SHORT_5_5_5_1(Gl20.UNSIGNED_SHORT_5_5_5_1),
	UNSIGNED_SHORT_4_4_4_4(Gl20.UNSIGNED_SHORT_4_4_4_4)
}

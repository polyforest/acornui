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


import com.acornui.core.graphic.Texture
import com.acornui.gl.core.*
import com.acornui.logging.Log
import com.acornui.math.MathUtils

/**
 * Wraps a standard OpenGL ES Cube map. Must be disposed when it is no longer used.
 * @author Xoppa
 * @author nbilyk
 */
class CubeMap(
		positiveX: Texture,
		negativeX: Texture,
		positiveY: Texture,
		negativeY: Texture,
		positiveZ: Texture,
		negativeZ: Texture,

		gl: Gl20,
		glState: GlState,
		private val writeMode: Boolean = false
) : GlTextureBase(gl, glState) {

	private val sides = arrayOf(positiveX, negativeX, positiveY, negativeY, positiveZ, negativeZ)

	private val _width: Int
	private val _height: Int
	private val _depth: Int

	private val firstSideOrdinal = TextureTarget.TEXTURE_CUBE_MAP_POSITIVE_X.ordinal

	init {
		target = TextureTarget.TEXTURE_CUBE_MAP
		filterMin = TextureMinFilter.LINEAR
		filterMag = TextureMagFilter.LINEAR
		wrapS = TextureWrapMode.CLAMP_TO_EDGE
		wrapT = TextureWrapMode.CLAMP_TO_EDGE

		var w = 0
		if (positiveZ.width > w) w = positiveZ.width
		if (negativeZ.width > w) w = negativeZ.width
		if (positiveY.width > w) w = positiveY.width
		if (negativeY.width > w) w = negativeY.width
		this._width = w

		var h = 0
		if (positiveZ.height > h) h = positiveZ.height
		if (negativeZ.height > h) h = negativeZ.height
		if (positiveX.height > h) h = positiveX.height
		if (negativeX.height > h) h = negativeX.height
		this._height = h

		var d = 0
		if (positiveX.width > d) d = positiveX.width
		if (negativeX.width > d) d = negativeX.width
		if (positiveY.height > d) d = positiveY.height
		if (negativeY.height > d) d = negativeY.height
		this._depth = d
	}

	override fun uploadTexture() {
		for (i in 0..sides.lastIndex) {
			val side = sides[i]
			side.target = TextureTarget.VALUES[i + firstSideOrdinal]

			side.textureHandle = gl.createTexture()
			if (writeMode) gl.texImage2Db(side.target.value, 0, pixelFormat.value, side.width, side.height, 0, pixelFormat.value, pixelType.value, null)
			else gl.texImage2D(side.target.value, 0, side.pixelFormat.value, side.pixelFormat.value, side.pixelType.value, side)
		}
		if (filterMin.useMipMap) {
			if (!supportsNpot() && (!MathUtils.isPowerOfTwo(width) || !MathUtils.isPowerOfTwo(height))) {
				Log.warn("MipMaps cannot be generated for non power of two textures (${width}x$height)")
				gl.texParameteri(target.value, Gl20.TEXTURE_MIN_FILTER, TextureMinFilter.LINEAR.value)
			} else {
				gl.generateMipmap(target.value)
			}
		}
	}

	override fun delete() {
		super.delete()
		for (i in 0..sides.lastIndex) {
			gl.deleteTexture(sides[i].textureHandle!!)
			sides[i].textureHandle = null
		}
	}

	/**
	 * @return The [Texture] for the specified side, can be null if the cube map is incomplete.
	 */
	fun getSide(target: TextureTarget): Texture {
		return sides[target.ordinal - firstSideOrdinal]
	}

	override val width: Int
		get() = _width

	override val height: Int
		get() = _height

	val depth: Int
		get() = _depth

}

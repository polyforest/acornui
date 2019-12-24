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

package com.acornui.component

import com.acornui.gl.core.CachedGl20
import com.acornui.gl.core.putQuadIndices
import com.acornui.gl.core.putVertex
import com.acornui.graphic.*
import com.acornui.math.IntRectangleRo
import com.acornui.math.Matrix4Ro
import com.acornui.math.RectangleRo
import com.acornui.math.Vector3
import com.acornui.recycle.Clearable
import kotlin.math.abs

/**
 * A Sprite represents a Quad region of a Texture. It can be drawn.
 *
 * @author nbilyk
 */
class Sprite(val gl: CachedGl20) : BasicRenderable, Clearable {

	/**
	 * [naturalWidth] uses uv coordinates multiplied by the texture size. If the texture uses dpi scaling, this
	 * scaling should be set on this sprite.
	 */
	var scaleX: Float = 1f

	/**
	 * [naturalHeight] uses uv coordinates multiplied by the texture size. If the texture uses dpi scaling, this
	 * scaling should be set on this sprite.
	 */
	var scaleY: Float = 1f

	fun setScaling(scaleX: Float, scaleY: Float) {
		this.scaleX = scaleX
		this.scaleY = scaleY
	}

	var texture: TextureRo? = null
		set(value) {
			if (field == value) return
			field = value
			updateUv()
		}

	var blendMode: BlendMode = BlendMode.NORMAL
	var premultipliedAlpha: Boolean = false

	var isRotated: Boolean = false
		private set

	/**
	 * Either represents uv values, or pixel coordinates, depending on isUv
	 * left, top, right, bottom
	 */
	private var region: FloatArray = floatArrayOf(0f, 0f, 1f, 1f)

	/**
	 * Returns the region component at the given index.
	 * @param index range 0-3
	 * @return returns either u, v, u2, v2 or x, y, x2, y2 depending on [isUv]
	 */
	fun getRegion(index: Int): Float {
		return region[index]
	}

	/**
	 * True if the region represents 0-1 uv coordinates.
	 * False if the region represents pixel coordinates.
	 */
	var isUv: Boolean = true
		private set

	private val normal = Vector3()

	fun setUv(u: Float, v: Float, u2: Float, v2: Float, isRotated: Boolean) {
		region[0] = u
		region[1] = v
		region[2] = u2
		region[3] = v2
		isUv = true
		this.isRotated = isRotated
		updateUv()
	}

	fun setRegion(bounds: RectangleRo, isRotated: Boolean) {
		setRegion(bounds.x, bounds.y, bounds.width, bounds.height, isRotated)
	}

	fun setRegion(bounds: IntRectangleRo, isRotated: Boolean) {
		setRegion(bounds.x.toFloat(), bounds.y.toFloat(), bounds.width.toFloat(), bounds.height.toFloat(), isRotated)
	}

	fun setRegion(x: Float, y: Float, width: Float, height: Float, isRotated: Boolean) {
		region[0] = x
		region[1] = y
		region[2] = width + x
		region[3] = height + y
		isUv = false
		this.isRotated = isRotated
		updateUv()
	}

	var u: Float = 0f
		private set

	var v: Float = 0f
		private set

	var u2: Float = 0f
		private set

	var v2: Float = 0f
		private set

	private fun updateUv() {
		val t = texture ?: return

		if (isUv) {
			u = region[0]
			v = region[1]
			u2 = region[2]
			v2 = region[3]
		} else {
			u = region[0] / t.widthPixels.toFloat()
			v = region[1] / t.heightPixels.toFloat()
			u2 = region[2] / t.widthPixels.toFloat()
			v2 = region[3] / t.heightPixels.toFloat()
		}
	}

	private val vertices = arrayOf(Vector3(), Vector3(), Vector3(), Vector3())
	private val tint = Color()

	override val naturalWidth: Float
		get() {
			val t = texture ?: return 0f
			return if (isRotated) {
				t.heightPixels.toFloat() * abs(v2 - v)
			} else {
				t.widthPixels.toFloat() * abs(u2 - u)
			} / scaleX
		}

	override val naturalHeight: Float
		get() {
			val t = texture ?: return 0f
			return if (isRotated) {
				t.widthPixels.toFloat() * abs(u2 - u)
			} else {
				t.heightPixels.toFloat() * abs(v2 - v)
			} / scaleY
		}

	override fun updateGlobalVertices(width: Float, height: Float, transform: Matrix4Ro, tint: ColorRo) {
		if (texture == null) return // Nothing to draw
		val vertices = vertices
		this.tint.set(tint)
		transform.rot(normal.set(Vector3.NEG_Z)).nor()

		transform.prj(vertices[0].set(0f, 0f, 0f))
		transform.prj(vertices[1].set(width, 0f, 0f))
		transform.prj(vertices[2].set(width, height, 0f))
		transform.prj(vertices[3].set(0f, height, 0f))
	}

	override fun render() {
		if (texture == null) return // Nothing to draw
		val tint = tint
		val normalGlobal = normal

		val batch = gl.batch
		batch.begin(texture = texture!!, blendMode = blendMode, premultipliedAlpha = premultipliedAlpha)

		if (isRotated) {
			// Top left
			batch.putVertex(vertices[0], normalGlobal, tint, u2, v)
			// Top right
			batch.putVertex(vertices[1], normalGlobal, tint, u2, v2)
			// Bottom right
			batch.putVertex(vertices[2], normalGlobal, tint, u, v2)
			// Bottom left
			batch.putVertex(vertices[3], normalGlobal, tint, u, v)
		} else {
			// Top left
			batch.putVertex(vertices[0], normalGlobal, tint, u, v)
			// Top right
			batch.putVertex(vertices[1], normalGlobal, tint, u2, v)
			// Bottom right
			batch.putVertex(vertices[2], normalGlobal, tint, u2, v2)
			// Bottom left
			batch.putVertex(vertices[3], normalGlobal, tint, u, v2)
		}
		batch.putQuadIndices()
	}

	/**
	 * Sets this sprite to match the properties of [other].
	 * This does not update the vertices.
	 * @param other The sprite to make this sprite match.
	 * @return Returns this sprite for chaining purposes.
	 */
	fun set(other: Sprite): Sprite {
		region[0] = other.region[0]
		region[1] = other.region[1]
		region[2] = other.region[2]
		region[3] = other.region[3]
		isUv = other.isUv
		isRotated = other.isRotated
		blendMode = other.blendMode
		premultipliedAlpha = other.premultipliedAlpha
		scaleX = other.scaleX
		scaleY = other.scaleY
		texture = other.texture
		updateUv()
		return this
	}

	override fun clear() {
		texture = null
		setUv(0f, 0f, 1f, 1f, false)
		setScaling(1f, 1f)
		blendMode = BlendMode.NORMAL
		premultipliedAlpha = false
	}
}

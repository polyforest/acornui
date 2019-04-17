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

package com.acornui.component

import com.acornui.core.graphic.BlendMode
import com.acornui.core.graphic.Texture
import com.acornui.gl.core.GlState
import com.acornui.gl.core.putCcwQuadIndices
import com.acornui.gl.core.putQuadIndices
import com.acornui.gl.core.putVertex
import com.acornui.graphic.ColorRo
import com.acornui.math.*
import com.acornui.recycle.Clearable
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.properties.Delegates

/**
 * A Sprite represents a Quad region of a Texture. It can be drawn.
 *
 * @author nbilyk
 */
class Sprite(val glState: GlState) : BasicDrawable, Clearable {

	/**
	 * If true, the normal and indices will be reversed.
	 */
	var useAsBackFace = false

	var texture: Texture? by Delegates.observable<Texture?>(null) {
		_, _, _ ->
		updateUv()
	}

	var blendMode: BlendMode = BlendMode.NORMAL
	var premultipliedAlpha: Boolean = false

	private var _isRotated: Boolean = false
	val isRotated: Boolean
		get() = _isRotated

	/**
	 * Either represents uv values, or pixel coordinates, depending on _isUv
	 * left, top, right, bottom
	 */
	private var region: FloatArray = floatArrayOf(0f, 0f, 1f, 1f)

	/**
	 * True if the region represents 0-1 uv coordinates.
	 * False if the region represents pixel coordinates.
	 */
	private var isUv: Boolean = true

	private val normalLocal = Vector3()
	private val normalWorld = Vector3()

	fun setUv(u: Float, v: Float, u2: Float, v2: Float, isRotated: Boolean) {
		region[0] = u
		region[1] = v
		region[2] = u2
		region[3] = v2
		isUv = true
		_isRotated = isRotated
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
		_isRotated = isRotated
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

	/**
	 * When the transform or the layout needs validation, update the 4 vertices of this texture.
	 */
	private val pointsLocal: Array<Vector3> = arrayOf(Vector3(), Vector3(), Vector3(), Vector3())

	override val naturalWidth: Float
		get() {
			val t = texture ?: return 0f
			return if (isRotated) {
				t.height.toFloat() * abs(v2 - v)
			} else {
				t.width.toFloat() * abs(u2 - u)
			}
		}

	override val naturalHeight: Float
		get() {
			val t = texture ?: return 0f
			return if (isRotated) {
				t.width.toFloat() * abs(u2 - u)
			} else {
				t.height.toFloat() * abs(v2 - v)
			}
		}

	private fun updateUv() {
		val t = texture ?: return

		if (isUv) {
			u = region[0]
			v = region[1]
			u2 = region[2]
			v2 = region[3]
		} else {
			u = region[0] / t.width.toFloat()
			v = region[1] / t.height.toFloat()
			u2 = region[2] / t.width.toFloat()
			v2 = region[3] / t.height.toFloat()
		}
	}

	private var width: Float = 0f
	private var height: Float = 0f

	override fun updateVertices(width: Float, height: Float, x: Float, y: Float, z: Float, rotation: Float, originX: Float, originY: Float) {
		this.width = width
		this.height = height
		// Transform vertex coordinates from local to global
		if (rotation == 0f) {
			val aX = x - originX
			val aY = y - originY
			val bX = x + width - originX
			val bY = y + height - originY
			pointsLocal[0].set(aX, aY, z)
			pointsLocal[1].set(bX, aY, z)
			pointsLocal[2].set(bX, bY, z)
			pointsLocal[3].set(aX, bY, z)
		} else {
			// (cos x - sin y, sin x + cos y)

			val cos = cos(rotation)
			val sin = sin(rotation)

			var x1: Float = -originX
			var y1: Float = -originY
			pointsLocal[0].set(cos * x1 - sin * y1 + x, sin * x1 + cos * y1 + y, z)
			x1 = -originX + width
			pointsLocal[1].set(cos * x1 - sin * y1 + x, sin * x1 + cos * y1 + y, z)
			y1 = -originY + height
			pointsLocal[2].set(cos * x1 - sin * y1 + x, sin * x1 + cos * y1 + y, z)
			x1 = -originX
			pointsLocal[3].set(cos * x1 - sin * y1 + x, sin * x1 + cos * y1 + y, z)
		}
		normalLocal.set(if (useAsBackFace) Vector3.Z else Vector3.NEG_Z)
	}

	private val tmpVec = Vector3()

	override fun render(clip: MinMaxRo, transform: Matrix4Ro, tint: ColorRo) {
		if (texture == null || tint.a <= 0f || width == 0f || height == 0f) return // Nothing to draw
		val tmpVec =  tmpVec
		transform.rot(normalWorld.set(normalLocal)).nor()

		val batch = glState.batch
		glState.setTexture(texture)
		glState.blendMode(blendMode, premultipliedAlpha)
		batch.begin()

		if (isRotated) {
			// Top left
			batch.putVertex(transform.prj(tmpVec.set(pointsLocal[0])), normalWorld, tint, u2, v)
			// Top right
			batch.putVertex(transform.prj(tmpVec.set(pointsLocal[1])), normalWorld, tint, u2, v2)
			// Bottom right
			batch.putVertex(transform.prj(tmpVec.set(pointsLocal[2])), normalWorld, tint, u, v2)
			// Bottom left
			batch.putVertex(transform.prj(tmpVec.set(pointsLocal[3])), normalWorld, tint, u, v)
		} else {
			// Top left
			batch.putVertex(transform.prj(tmpVec.set(pointsLocal[0])), normalWorld, tint, u, v)
			// Top right
			batch.putVertex(transform.prj(tmpVec.set(pointsLocal[1])), normalWorld, tint, u2, v)
			// Bottom right
			batch.putVertex(transform.prj(tmpVec.set(pointsLocal[2])), normalWorld, tint, u2, v2)
			// Bottom left
			batch.putVertex(transform.prj(tmpVec.set(pointsLocal[3])), normalWorld, tint, u, v2)
		}
		if (useAsBackFace) batch.putCcwQuadIndices()
		else batch.putQuadIndices()
	}

	override fun clear() {
		texture = null
		setUv(0f, 0f, 1f, 1f, false)
	}
}
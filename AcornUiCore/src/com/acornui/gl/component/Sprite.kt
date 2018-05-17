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

package com.acornui.gl.component

import com.acornui.core.graphics.BlendMode
import com.acornui.core.graphics.Texture
import com.acornui.gl.core.GlState
import com.acornui.gl.core.pushQuadIndices
import com.acornui.graphics.ColorRo
import com.acornui.math.*

/**
 * A Sprite represents a Quad region of a Texture. It can be drawn.
 *
 * @author nbilyk
 */
class Sprite {

	var texture: Texture? = null
	var blendMode: BlendMode = BlendMode.NORMAL
	var isRotated: Boolean = false
	var premultipliedAlpha: Boolean = false

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

	private val normal = Vector3()

	init {
	}

	fun setUv(u: Float, v: Float, u2: Float, v2: Float) {
		region[0] = u
		region[1] = v
		region[2] = u2
		region[3] = v2
		isUv = true
	}

	fun setRegion(bounds: Rectangle) {
		setRegion(bounds.x, bounds.y, bounds.width, bounds.height)
	}

	fun setRegion(bounds: IntRectangleRo) {
		setRegion(bounds.x.toFloat(), bounds.y.toFloat(), bounds.width.toFloat(), bounds.height.toFloat())
	}

	fun setRegion(x: Float, y: Float, width: Float, height: Float) {
		region[0] = x
		region[1] = y
		region[2] = width + x
		region[3] = height + y
		isUv = false
	}

	private var u: Float = 0f
	private var v: Float = 0f
	private var u2: Float = 0f
	private var v2: Float = 0f

	/**
	 * When the transform or the layout needs validation, update the 4 vertices of this texture.
	 */
	private val vertexPoints: Array<Vector3> = arrayOf(Vector3(), Vector3(), Vector3(), Vector3())

	val naturalWidth: Float
		get() {
			val t = texture ?: return 0f
			return if (isRotated) {
				t.height.toFloat() * MathUtils.abs(v2 - v)
			} else {
				t.width.toFloat() * MathUtils.abs(u2 - u)
			}
		}

	val naturalHeight: Float
		get() {
			val t = texture ?: return 0f
			return if (isRotated) {
				t.width.toFloat() * MathUtils.abs(u2 - u)
			} else {
				t.height.toFloat() * MathUtils.abs(v2 - v)
			}
		}

	fun updateUv() {
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

	/**
	 * Updates this Sprite's local vertices and then multiplies them with the world transformation matrix.
	 *
	 * @param worldTransform The transformation matrix to project the local coordinates to global coordinates.
	 * @param width The width of the sprite.
	 * @param height The height of the sprite.
	 * @param x translation
	 * @param y translation
	 * @param z translation
	 * @param rotation The rotation around the Z axis in radians.
	 * @param originX The x point of the rectangle that will be 0,0
	 * @param originY The y point of the rectangle that will be 0,0
	 */
	fun updateWorldVertices(worldTransform: Matrix4Ro, width: Float, height: Float, x: Float = 0f, y: Float = 0f, z: Float = 0f, rotation: Float = 0f, originX: Float = 0f, originY: Float = 0f) {
		updateVertices(width, height, x, y, z, rotation, originX, originY)
		worldTransform.prj(vertexPoints[0])
		worldTransform.prj(vertexPoints[1])
		worldTransform.prj(vertexPoints[2])
		worldTransform.prj(vertexPoints[3])
		worldTransform.rot(normal).nor()
	}

	/**
	 * Updates this Sprite's local vertices.
	 *
	 * @param width The width of the sprite.
	 * @param height The height of the sprite.
	 * @param x translation
	 * @param y translation
	 * @param z translation
	 * @param rotation The rotation around the Z axis in radians.
	 * @param originX The x point of the rectangle that will be 0,0
	 * @param originY The y point of the rectangle that will be 0,0
	 */
	fun updateVertices(width: Float, height: Float, x: Float = 0f, y: Float = 0f, z: Float = 0f, rotation: Float = 0f, originX: Float = 0f, originY: Float = 0f) {
		this.width = width
		this.height = height
		// Transform vertex coordinates from local to global
		val aX: Float
		val aY: Float
		val bX: Float
		val bY: Float
		if (rotation == 0f) {
			aX = x - originX
			aY = y - originY
			bX = x + width - originX
			bY = y + height - originY
		} else {
			val cos = MathUtils.cos(rotation)
			val sin = MathUtils.sin(rotation)
			aX = cos * -originX - sin * -originY + x
			aY = sin * -originX + cos * -originY + y
			bX = cos * (-originX + width) - sin * -originY + x
			bY = sin * (-originX + width) + cos * (-originY + height) + y
		}
		vertexPoints[0].set(aX, aY, z)
		vertexPoints[1].set(bX, aY, z)
		vertexPoints[2].set(bX, bY, z)
		vertexPoints[3].set(aX, bY, z)
		normal.set(Vector3.NEG_Z)
	}

	/**
	 * Draws this sprite.
	 * Remember to set the camera on the [GlState] object before drawing.
	 * If [updateVertices] was used (and therefore no world transformation), that world transform matrix must be
	 * supplied to [GlState.camera] first.
	 */
	fun draw(glState: GlState, colorTint: ColorRo) {
		if (texture == null || colorTint.a <= 0f || width == 0f || height == 0f) return // Nothing to draw
		val batch = glState.batch
		glState.setTexture(texture)
		batch.begin()
		glState.blendMode(blendMode, premultipliedAlpha)

		if (isRotated) {
			// Top left
			batch.putVertex(vertexPoints[0], normal, colorTint, u2, v)
			// Top right
			batch.putVertex(vertexPoints[1], normal, colorTint, u2, v2)
			// Bottom right
			batch.putVertex(vertexPoints[2], normal, colorTint, u, v2)
			// Bottom left
			batch.putVertex(vertexPoints[3], normal, colorTint, u, v)
		} else {
			// Top left
			batch.putVertex(vertexPoints[0], normal, colorTint, u, v)
			// Top right
			batch.putVertex(vertexPoints[1], normal, colorTint, u2, v)
			// Bottom right
			batch.putVertex(vertexPoints[2], normal, colorTint, u2, v2)
			// Bottom left
			batch.putVertex(vertexPoints[3], normal, colorTint, u, v2)
		}
		batch.pushQuadIndices()
	}
}
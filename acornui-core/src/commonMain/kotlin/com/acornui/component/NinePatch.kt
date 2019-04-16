/*
 * Copyright 2018 Nicholas Bilyk
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

/*
 * Copyright 2018 Nicholas Bilyk
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
import com.acornui.gl.core.putIndices
import com.acornui.gl.core.putIndicesReversed
import com.acornui.gl.core.putVertex
import com.acornui.graphic.ColorRo
import com.acornui.math.IntRectangleRo
import com.acornui.math.Matrix4Ro
import com.acornui.math.RectangleRo
import com.acornui.math.Vector3
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.properties.Delegates

class NinePatch(val glState: GlState) : BasicDrawable {

	private var _isRotated: Boolean = false

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

	private var _splitLeft = 0f
	private var _splitTop = 0f
	private var _splitRight = 0f
	private var _splitBottom = 0f

	/**
	 * If true, the normal and indices will be reversed.
	 */
	var useAsBackFace: Boolean = false

	var blendMode: BlendMode = BlendMode.NORMAL

	private var u: Float = 0f
	private var v: Float = 0f
	private var u2: Float = 0f
	private var v2: Float = 0f

	//    0   1   2   3
	//    4   5   6   7
	//    8   9   10  11
	//    12  13  14  15

	private val indices = intArrayOf(0, 1, 5, 5, 4, 0, 1, 2, 6, 6, 5, 1, 2, 3, 7, 7, 6, 2, 4, 5, 9, 9, 8, 4, 5, 6, 10, 10, 9, 5, 6, 7, 11, 11, 10, 6, 8, 9, 13, 13, 12, 8, 9, 10, 14, 14, 13, 9, 10, 11, 15, 15, 14, 10)

	private val vertexPoints = Array(16) { Vector3() }
	private val normal = Vector3()

	var texture: Texture? by Delegates.observable<Texture?>(null) {
		_, _, _ ->
		updateUv()
	}

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

	fun setRegion(bounds: RectangleRo, isRotated: Boolean) {
		setRegion(bounds.x, bounds.y, bounds.width, bounds.height, isRotated)
	}

	fun setRegion(region: IntRectangleRo, isRotated: Boolean) {
		setRegion(region.x.toFloat(), region.y.toFloat(), region.width.toFloat(), region.height.toFloat(), isRotated)
	}

	fun setRegion(x: Float, y: Float, width: Float, height: Float, isRotated: Boolean) {
		region[0] = x
		region[1] = y
		region[2] = width + x
		region[3] = height + y
		_isRotated = isRotated
		isUv = false
		updateUv()
	}

	fun setUv(u: Float, v: Float, u2: Float, v2: Float, isRotated: Boolean) {
		region[0] = u
		region[1] = v
		region[2] = u2
		region[3] = v2
		_isRotated = isRotated
		isUv = true
		updateUv()
	}

	val isRotated: Boolean
		get() = _isRotated

	val splitLeft: Float
		get() = _splitLeft
	val splitTop: Float
		get() = _splitTop
	val splitRight: Float
		get() = _splitRight
	val splitBottom: Float
		get() = _splitBottom

	fun split(splitLeft: Int, splitTop: Int, splitRight: Int, splitBottom: Int) {
		split(splitLeft.toFloat(), splitTop.toFloat(), splitRight.toFloat(), splitBottom.toFloat())
	}

	fun split(splitLeft: Float, splitTop: Float, splitRight: Float, splitBottom: Float) {
		this._splitLeft = splitLeft
		this._splitTop = splitTop
		this._splitRight = splitRight
		this._splitBottom = splitBottom
	}

	private var width: Float = 0f
	private var height: Float = 0f

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

	override fun updateWorldVertices(worldTransform: Matrix4Ro, width: Float, height: Float, x: Float, y: Float, z: Float, rotation: Float, originX: Float, originY: Float) {
		updateVertices(width, height, x, y, z, rotation, originX, originY)
		worldTransform.prj(vertexPoints[0])
		worldTransform.prj(vertexPoints[1])
		worldTransform.prj(vertexPoints[2])
		worldTransform.prj(vertexPoints[3])
		worldTransform.prj(vertexPoints[4])
		worldTransform.prj(vertexPoints[5])
		worldTransform.prj(vertexPoints[6])
		worldTransform.prj(vertexPoints[7])
		worldTransform.prj(vertexPoints[8])
		worldTransform.prj(vertexPoints[9])
		worldTransform.prj(vertexPoints[10])
		worldTransform.prj(vertexPoints[11])
		worldTransform.prj(vertexPoints[12])
		worldTransform.prj(vertexPoints[13])
		worldTransform.prj(vertexPoints[14])
		worldTransform.prj(vertexPoints[15])
		worldTransform.rot(normal).nor()
	}

	override fun updateVertices(width: Float, height: Float, x: Float, y: Float, z: Float, rotation: Float, originX: Float, originY: Float) {
		this.width = width
		this.height = height
		val minW = _splitLeft + _splitRight
		val minH = _splitTop + _splitBottom
		val scaleX = if (minW <= 0f || width > minW) 1f else width / minW
		val scaleY = if (minH <= 0f || height > minH) 1f else height / minH

		val x0 = -originX
		val x1 = scaleX * _splitLeft - originX
		val x2 = width - scaleX * _splitRight - originX
		val x3 = width - originX

		val y0 = -originY
		val y1 = scaleY * _splitTop - originY
		val y2 = height - scaleY * _splitBottom - originY
		val y3 = height - originY

		val cos = cos(rotation)
		val sin = sin(rotation)

		// Row 0
		vertexPoints[0].set(cos * x0 - sin * y0 + x, sin * x0 + cos * y0 + y, z)
		vertexPoints[1].set(cos * x1 - sin * y0 + x, sin * x1 + cos * y0 + y, z)
		vertexPoints[2].set(cos * x2 - sin * y0 + x, sin * x2 + cos * y0 + y, z)
		vertexPoints[3].set(cos * x3 - sin * y0 + x, sin * x3 + cos * y0 + y, z)

		// Row 1
		vertexPoints[4].set(cos * x0 - sin * y1 + x, sin * x0 + cos * y1 + y, z)
		vertexPoints[5].set(cos * x1 - sin * y1 + x, sin * x1 + cos * y1 + y, z)
		vertexPoints[6].set(cos * x2 - sin * y1 + x, sin * x2 + cos * y1 + y, z)
		vertexPoints[7].set(cos * x3 - sin * y1 + x, sin * x3 + cos * y1 + y, z)

		// Row 2
		vertexPoints[8].set(cos * x0 - sin * y2 + x, sin * x0 + cos * y2 + y, z)
		vertexPoints[9].set(cos * x1 - sin * y2 + x, sin * x1 + cos * y2 + y, z)
		vertexPoints[10].set(cos * x2 - sin * y2 + x, sin * x2 + cos * y2 + y, z)
		vertexPoints[11].set(cos * x3 - sin * y2 + x, sin * x3 + cos * y2 + y, z)

		// Row 3
		vertexPoints[12].set(cos * x0 - sin * y3 + x, sin * x0 + cos * y3 + y, z)
		vertexPoints[13].set(cos * x1 - sin * y3 + x, sin * x1 + cos * y3 + y, z)
		vertexPoints[14].set(cos * x2 - sin * y3 + x, sin * x2 + cos * y3 + y, z)
		vertexPoints[15].set(cos * x3 - sin * y3 + x, sin * x3 + cos * y3 + y, z)

		normal.set(if (useAsBackFace) Vector3.Z else Vector3.NEG_Z)
	}

	/**
	 * Draws this nine patch.
	 * Remember to set the camera on the [GlState] object before drawing.
	 * If [updateVertices] was used (and therefore no world transformation), that world transform matrix must be
	 * supplied to [GlState.setCamera] first.
	 */
	override fun render(colorTint: ColorRo) {
		val texture = texture
		if (texture == null || width <= 0f || height <= 0f) return
		glState.setTexture(texture)
		glState.blendMode(blendMode, premultipliedAlpha = false)

		val batch = glState.batch
		batch.begin()

		if (isRotated) {
			val splitLeftV = _splitLeft / texture.height
			val splitRightV = _splitRight / texture.height
			val splitTopU = _splitTop / texture.width
			val splitBottomU = _splitBottom / texture.width

			val colV0 = v
			val colV1 = v + splitLeftV
			val colV2 = v2 - splitRightV
			val colV3 = v2

			val rowU0 = u2
			val rowU1 = u2 - splitTopU
			val rowU2 = u + splitBottomU
			val rowU3 = u

			// u, v = u2, v
			// u2, v = u2, v2
			// u2, v2 = u, v2
			// u, v2 = u, v

			// Row 0
			batch.putVertex(vertexPoints[0], normal, colorTint, rowU0, colV0)
			batch.putVertex(vertexPoints[1], normal, colorTint, rowU0, colV1)
			batch.putVertex(vertexPoints[2], normal, colorTint, rowU0, colV2)
			batch.putVertex(vertexPoints[3], normal, colorTint, rowU0, colV3)

			// Row 1
			batch.putVertex(vertexPoints[4], normal, colorTint, rowU1, colV0)
			batch.putVertex(vertexPoints[5], normal, colorTint, rowU1, colV1)
			batch.putVertex(vertexPoints[6], normal, colorTint, rowU1, colV2)
			batch.putVertex(vertexPoints[7], normal, colorTint, rowU1, colV3)

			// Row 2
			batch.putVertex(vertexPoints[8], normal, colorTint, rowU2, colV0)
			batch.putVertex(vertexPoints[9], normal, colorTint, rowU2, colV1)
			batch.putVertex(vertexPoints[10], normal, colorTint, rowU2, colV2)
			batch.putVertex(vertexPoints[11], normal, colorTint, rowU2, colV3)

			// Row 3
			batch.putVertex(vertexPoints[12], normal, colorTint, rowU3, colV0)
			batch.putVertex(vertexPoints[13], normal, colorTint, rowU3, colV1)
			batch.putVertex(vertexPoints[14], normal, colorTint, rowU3, colV2)
			batch.putVertex(vertexPoints[15], normal, colorTint, rowU3, colV3)
		} else {
			val splitLeftU = _splitLeft / texture.width
			val splitRightU = _splitRight / texture.width
			val splitTopV = _splitTop / texture.height
			val splitBottomV = _splitBottom / texture.height

			val colU0 = u
			val colU1 = u + splitLeftU
			val colU2 = u2 - splitRightU
			val colU3 = u2

			val rowV0 = v
			val rowV1 = v + splitTopV
			val rowV2 = v2 - splitBottomV
			val rowV3 = v2

			// Row 0
			batch.putVertex(vertexPoints[0], normal, colorTint, colU0, rowV0)
			batch.putVertex(vertexPoints[1], normal, colorTint, colU1, rowV0)
			batch.putVertex(vertexPoints[2], normal, colorTint, colU2, rowV0)
			batch.putVertex(vertexPoints[3], normal, colorTint, colU3, rowV0)

			// Row 1
			batch.putVertex(vertexPoints[4], normal, colorTint, colU0, rowV1)
			batch.putVertex(vertexPoints[5], normal, colorTint, colU1, rowV1)
			batch.putVertex(vertexPoints[6], normal, colorTint, colU2, rowV1)
			batch.putVertex(vertexPoints[7], normal, colorTint, colU3, rowV1)

			// Row 2
			batch.putVertex(vertexPoints[8], normal, colorTint, colU0, rowV2)
			batch.putVertex(vertexPoints[9], normal, colorTint, colU1, rowV2)
			batch.putVertex(vertexPoints[10], normal, colorTint, colU2, rowV2)
			batch.putVertex(vertexPoints[11], normal, colorTint, colU3, rowV2)

			// Row 3
			batch.putVertex(vertexPoints[12], normal, colorTint, colU0, rowV3)
			batch.putVertex(vertexPoints[13], normal, colorTint, colU1, rowV3)
			batch.putVertex(vertexPoints[14], normal, colorTint, colU2, rowV3)
			batch.putVertex(vertexPoints[15], normal, colorTint, colU3, rowV3)
		}
		if (useAsBackFace) batch.putIndicesReversed(indices) else batch.putIndices(indices)
	}
}
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

import com.acornui.core.RenderableBase
import com.acornui.core.graphic.BlendMode
import com.acornui.core.graphic.Texture
import com.acornui.core.setCamera
import com.acornui.gl.core.GlState
import com.acornui.gl.core.putIndices
import com.acornui.gl.core.putVertex
import com.acornui.math.IntRectangleRo
import com.acornui.math.RectangleRo
import com.acornui.math.Vector3
import com.acornui.recycle.Clearable
import kotlin.math.abs
import kotlin.properties.Delegates

class NinePatch(val glState: GlState) : RenderableBase(), Clearable {

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

	var blendMode: BlendMode = BlendMode.NORMAL

	var premultipliedAlpha = false

	var splitLeft: Float = 0f
		private set

	var splitTop: Float = 0f
		private set

	var splitRight: Float = 0f
		private set

	var splitBottom: Float = 0f
		private set

	private var u: Float = 0f
	private var v: Float = 0f
	private var u2: Float = 0f
	private var v2: Float = 0f

	//    0   1   2   3
	//    4   5   6   7
	//    8   9   10  11
	//    12  13  14  15

	private val indices = intArrayOf(0, 1, 5, 5, 4, 0, 1, 2, 6, 6, 5, 1, 2, 3, 7, 7, 6, 2, 4, 5, 9, 9, 8, 4, 5, 6, 10, 10, 9, 5, 6, 7, 11, 11, 10, 6, 8, 9, 13, 13, 12, 8, 9, 10, 14, 14, 13, 9, 10, 11, 15, 15, 14, 10)

	private val localPoints = Array(16) { Vector3() }
	private val normal = Vector3.NEG_Z
	private val worldNormal = Vector3()
	private val tmpVec = Vector3()

	@Suppress("RemoveExplicitTypeArguments") // Kotlin compiler bug
	var texture: Texture? by Delegates.observable<Texture?>(null) {
		_, _, _ ->
		updateUv()
	}

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

	val naturalWidth: Float
		get() {
			val t = texture ?: return 0f
			return if (isRotated) {
				t.heightPixels.toFloat() * abs(v2 - v)
			} else {
				t.widthPixels.toFloat() * abs(u2 - u)
			} / scaleX
		}

	val naturalHeight: Float
		get() {
			val t = texture ?: return 0f
			return if (isRotated) {
				t.widthPixels.toFloat() * abs(u2 - u)
			} else {
				t.heightPixels.toFloat() * abs(v2 - v)
			} / scaleY
		}

	/**
	 * Sets the region for calculating uv coordinates.
	 */
	fun setRegion(bounds: RectangleRo, isRotated: Boolean) {
		setRegion(bounds.x, bounds.y, bounds.width, bounds.height, isRotated)
	}

	/**
	 * Sets the region for calculating uv coordinates.
	 */
	fun setRegion(region: IntRectangleRo, isRotated: Boolean) {
		setRegion(region.x.toFloat(), region.y.toFloat(), region.width.toFloat(), region.height.toFloat(), isRotated)
	}

	/**
	 * Sets the region for calculating uv coordinates.
	 */
	fun setRegion(x: Float, y: Float, width: Float, height: Float, isRotated: Boolean) {
		region[0] = x
		region[1] = y
		region[2] = width + x
		region[3] = height + y
		this.isRotated = isRotated
		isUv = false
		updateUv()
	}

	fun setUv(u: Float, v: Float, u2: Float, v2: Float, isRotated: Boolean) {
		region[0] = u
		region[1] = v
		region[2] = u2
		region[3] = v2
		this.isRotated = isRotated
		isUv = true
		updateUv()
	}

	fun split(splitLeft: Int, splitTop: Int, splitRight: Int, splitBottom: Int) {
		split(splitLeft.toFloat(), splitTop.toFloat(), splitRight.toFloat(), splitBottom.toFloat())
	}

	fun split(splitLeft: Float, splitTop: Float, splitRight: Float, splitBottom: Float) {
		this.splitLeft = splitLeft
		this.splitTop = splitTop
		this.splitRight = splitRight
		this.splitBottom = splitBottom
	}

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

	fun set(other: NinePatch) {
		texture = other.texture
		region[0] = other.getRegion(0)
		region[1] = other.getRegion(1)
		region[2] = other.getRegion(2)
		region[3] = other.getRegion(3)
		isRotated = other.isRotated
		isUv = other.isUv
		blendMode = other.blendMode
		premultipliedAlpha = other.premultipliedAlpha
		splitLeft = other.splitLeft
		splitTop = other.splitTop
		splitRight = other.splitRight
		splitBottom = other.splitBottom
	}

	fun set(other: Sprite) {
		texture = other.texture
		region[0] = other.getRegion(0)
		region[1] = other.getRegion(1)
		region[2] = other.getRegion(2)
		region[3] = other.getRegion(3)
		isRotated = other.isRotated
		isUv = other.isUv
		blendMode = other.blendMode
		premultipliedAlpha = other.premultipliedAlpha
		splitLeft = 0f
		splitTop = 0f
		splitRight = 0f
		splitBottom = 0f
	}

	override fun onSizeSet(oldW: Float?, oldH: Float?, newW: Float?, newH: Float?) {
		val width = newW ?: naturalWidth
		val height = newH ?: naturalHeight
		val minW = splitLeft + splitRight
		val minH = splitTop + splitBottom
		val scaleX = if (minW <= 0f || width > minW) 1f else width / minW
		val scaleY = if (minH <= 0f || height > minH) 1f else height / minH

		val x0 = 0f
		val x1 = scaleX * splitLeft
		val x2 = width - scaleX * splitRight
		val x3 = width

		val y0 = 0f
		val y1 = scaleY * splitTop
		val y2 = height - scaleY * splitBottom
		val y3 = height

		// Row 0
		localPoints[ 0].set(x0, y0, 0f)
		localPoints[ 1].set(x1, y0, 0f)
		localPoints[ 2].set(x2, y0, 0f)
		localPoints[ 3].set(x3, y0, 0f)

		// Row 1
		localPoints[ 4].set(x0, y1, 0f)
		localPoints[ 5].set(x1, y1, 0f)
		localPoints[ 6].set(x2, y1, 0f)
		localPoints[ 7].set(x3, y1, 0f)

		// Row 2
		localPoints[ 8].set(x0, y2, 0f)
		localPoints[ 9].set(x1, y2, 0f)
		localPoints[10].set(x2, y2, 0f)
		localPoints[11].set(x3, y2, 0f)

		// Row 3
		localPoints[12].set(x0, y3, 0f)
		localPoints[13].set(x1, y3, 0f)
		localPoints[14].set(x2, y3, 0f)
		localPoints[15].set(x3, y3, 0f)

		_bounds.set(width, height)
	}

	override fun render(renderContext: RenderContextRo) {
		val texture = texture
		val tint = renderContext.colorTint
		val transform = renderContext.modelTransform
		if (texture == null || width <= 0f || height <= 0f || tint.a <= 0f) return
		glState.setCamera(renderContext)

		transform.rot(worldNormal.set(normal)).nor()

		glState.setTexture(texture)
		glState.blendMode(blendMode, premultipliedAlpha)

		val batch = glState.batch
		batch.begin()

		if (isRotated) {
			val splitLeftV = splitLeft / texture.heightPixels
			val splitRightV = splitRight / texture.heightPixels
			val splitTopU = splitTop / texture.widthPixels
			val splitBottomU = splitBottom / texture.widthPixels

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
			batch.putVertex(transform.prj(tmpVec.set(localPoints[0])), worldNormal, tint, rowU0, colV0)
			batch.putVertex(transform.prj(tmpVec.set(localPoints[1])), worldNormal, tint, rowU0, colV1)
			batch.putVertex(transform.prj(tmpVec.set(localPoints[2])), worldNormal, tint, rowU0, colV2)
			batch.putVertex(transform.prj(tmpVec.set(localPoints[3])), worldNormal, tint, rowU0, colV3)

			// Row 1
			batch.putVertex(transform.prj(tmpVec.set(localPoints[4])), worldNormal, tint, rowU1, colV0)
			batch.putVertex(transform.prj(tmpVec.set(localPoints[5])), worldNormal, tint, rowU1, colV1)
			batch.putVertex(transform.prj(tmpVec.set(localPoints[6])), worldNormal, tint, rowU1, colV2)
			batch.putVertex(transform.prj(tmpVec.set(localPoints[7])), worldNormal, tint, rowU1, colV3)

			// Row 2
			batch.putVertex(transform.prj(tmpVec.set(localPoints[8])), worldNormal, tint, rowU2, colV0)
			batch.putVertex(transform.prj(tmpVec.set(localPoints[9])), worldNormal, tint, rowU2, colV1)
			batch.putVertex(transform.prj(tmpVec.set(localPoints[10])), worldNormal, tint, rowU2, colV2)
			batch.putVertex(transform.prj(tmpVec.set(localPoints[11])), worldNormal, tint, rowU2, colV3)

			// Row 3
			batch.putVertex(transform.prj(tmpVec.set(localPoints[12])), worldNormal, tint, rowU3, colV0)
			batch.putVertex(transform.prj(tmpVec.set(localPoints[13])), worldNormal, tint, rowU3, colV1)
			batch.putVertex(transform.prj(tmpVec.set(localPoints[14])), worldNormal, tint, rowU3, colV2)
			batch.putVertex(transform.prj(tmpVec.set(localPoints[15])), worldNormal, tint, rowU3, colV3)
		} else {
			val splitLeftU = splitLeft / texture.widthPixels
			val splitRightU = splitRight / texture.widthPixels
			val splitTopV = splitTop / texture.heightPixels
			val splitBottomV = splitBottom / texture.heightPixels

			val colU0 = u
			val colU1 = u + splitLeftU
			val colU2 = u2 - splitRightU
			val colU3 = u2

			val rowV0 = v
			val rowV1 = v + splitTopV
			val rowV2 = v2 - splitBottomV
			val rowV3 = v2

			// Row 0
			batch.putVertex(transform.prj(tmpVec.set(localPoints[0])), worldNormal, tint, colU0, rowV0)
			batch.putVertex(transform.prj(tmpVec.set(localPoints[1])), worldNormal, tint, colU1, rowV0)
			batch.putVertex(transform.prj(tmpVec.set(localPoints[2])), worldNormal, tint, colU2, rowV0)
			batch.putVertex(transform.prj(tmpVec.set(localPoints[3])), worldNormal, tint, colU3, rowV0)

			// Row 1
			batch.putVertex(transform.prj(tmpVec.set(localPoints[4])), worldNormal, tint, colU0, rowV1)
			batch.putVertex(transform.prj(tmpVec.set(localPoints[5])), worldNormal, tint, colU1, rowV1)
			batch.putVertex(transform.prj(tmpVec.set(localPoints[6])), worldNormal, tint, colU2, rowV1)
			batch.putVertex(transform.prj(tmpVec.set(localPoints[7])), worldNormal, tint, colU3, rowV1)

			// Row 2
			batch.putVertex(transform.prj(tmpVec.set(localPoints[8])), worldNormal, tint, colU0, rowV2)
			batch.putVertex(transform.prj(tmpVec.set(localPoints[9])), worldNormal, tint, colU1, rowV2)
			batch.putVertex(transform.prj(tmpVec.set(localPoints[10])), worldNormal, tint, colU2, rowV2)
			batch.putVertex(transform.prj(tmpVec.set(localPoints[11])), worldNormal, tint, colU3, rowV2)

			// Row 3
			batch.putVertex(transform.prj(tmpVec.set(localPoints[12])), worldNormal, tint, colU0, rowV3)
			batch.putVertex(transform.prj(tmpVec.set(localPoints[13])), worldNormal, tint, colU1, rowV3)
			batch.putVertex(transform.prj(tmpVec.set(localPoints[14])), worldNormal, tint, colU2, rowV3)
			batch.putVertex(transform.prj(tmpVec.set(localPoints[15])), worldNormal, tint, colU3, rowV3)
		}
		batch.putIndices(indices)
	}

	override fun clear() {
		texture = null
		setUv(0f, 0f, 1f, 1f, false)
		setScaling(1f, 1f)
		blendMode = BlendMode.NORMAL
		premultipliedAlpha = false
		splitLeft = 0f
		splitTop = 0f
		splitRight = 0f
		splitBottom = 0f
	}
}

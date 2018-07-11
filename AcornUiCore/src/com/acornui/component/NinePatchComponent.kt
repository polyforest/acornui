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

import com.acornui.async.then
import com.acornui.core.assets.AssetType
import com.acornui.core.assets.CachedGroup
import com.acornui.core.assets.cachedGroup
import com.acornui.core.assets.loadAndCache
import com.acornui.core.di.Owned
import com.acornui.core.di.inject
import com.acornui.core.graphics.BlendMode
import com.acornui.core.graphics.Texture
import com.acornui.gl.core.GlState
import com.acornui.gl.core.putIndices
import com.acornui.gl.core.putVertex
import com.acornui.math.Bounds
import com.acornui.math.IntRectangleRo
import com.acornui.math.MinMaxRo
import com.acornui.math.Vector3
import com.acornui.reflect.observable

/**
 * @author nbilyk
 */
class NinePatchComponent(owner: Owned) : ContainerImpl(owner) {

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

	private var _naturalWidth: Float = 0f
	private var _naturalHeight: Float = 0f

	private var _texture: Texture? = null

	var blendMode: BlendMode by observable(BlendMode.NORMAL) { window.requestRender() }

	private var u: Float = 0f
	private var v: Float = 0f
	private var u2: Float = 0f
	private var v2: Float = 0f

	//    0   1   2   3
	//    4   5   6   7
	//    8   9   10  11
	//    12  13  14  15

	private val glState = inject(GlState)
	private val indices = intArrayOf(0, 1, 5, 5, 4, 0, 1, 2, 6, 6, 5, 1, 2, 3, 7, 7, 6, 2, 4, 5, 9, 9, 8, 4, 5, 6, 10, 10, 9, 5, 6, 7, 11, 11, 10, 6, 8, 9, 13, 13, 12, 8, 9, 10, 14, 14, 13, 9, 10, 11, 15, 15, 14, 10)

	private val columns = FloatArray(4)
	private val rows = FloatArray(4)
	private val globalPositions = Array(16) { Vector3() }

	private var cached: CachedGroup? = null

	init {
		validation.addNode(1 shl 16, ValidationFlags.LAYOUT or ValidationFlags.CONCATENATED_TRANSFORM, this::updateWorldVertices)
	}

	override fun onActivated() {
		super.onActivated()
		_texture?.refInc()
	}

	override fun onDeactivated() {
		super.onDeactivated()
		_texture?.refDec()
	}

	private var _path: String? = null
	var path: String?
		get() = _path
		set(value) {
			if (_path == value) return
			cached?.dispose()
			cached = cachedGroup()
			if (value != null) {
				loadAndCache(value, AssetType.TEXTURE, cached!!).then {
					_setTexture(it)
				}
			} else {
				_setTexture(null)
			}
		}

	var texture: Texture?
		get() = _texture
		set(value) {
			if (_texture == value) return
			path = null
			_setTexture(value)
		}

	private fun _setTexture(value: Texture?) {
		if (_texture == value) return
		val oldTexture = _texture
		if (isActive) {
			oldTexture?.refDec()
		}
		_texture = value
		if (isActive) {
			_texture?.refInc()
		}
		invalidateLayout()
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
		invalidateLayout()
	}

	fun setUv(u: Float, v: Float, u2: Float, v2: Float, isRotated: Boolean) {
		region[0] = u
		region[1] = v
		region[2] = u2
		region[3] = v2
		_isRotated = isRotated
		isUv = true
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
		invalidateLayout()
	}

	val naturalWidth: Float
		get() {
			validateLayout()
			return _naturalWidth
		}

	val naturalHeight: Float
		get() {
			validateLayout()
			return _naturalHeight
		}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val texture = texture
		if (texture == null) {
			_naturalWidth = 0f
			_naturalHeight = 0f
			out.set(0f, 0f)
			return
		}

		if (isUv) {
			_naturalWidth = (region[2] - region[0]) * texture.width
			_naturalHeight = region[3] - region[1]
			u = region[0]
			v = region[1]
			u2 = region[2]
			v2 = region[3]
		} else {
			_naturalWidth = region[2] - region[0]
			_naturalHeight = region[3] - region[1]
			u = region[0] / texture.width.toFloat()
			v = region[1] / texture.height.toFloat()
			u2 = region[2] / texture.width.toFloat()
			v2 = region[3] / texture.height.toFloat()
		}

		val w = explicitWidth ?: _naturalWidth
		val h = explicitHeight ?: _naturalHeight
		out.set(w, h)

		val minW = _splitLeft + _splitRight
		val minH = _splitTop + _splitBottom
		val scaleX = if (minW <= 0f || w > minW) 1f else w / minW
		val scaleY = if (minH <= 0f || h > minH) 1f else h / minH

		columns[1] = scaleX * _splitLeft
		columns[2] = w - scaleX * _splitRight
		columns[3] = w

		rows[1] = scaleY * _splitTop
		rows[2] = h - scaleY * _splitBottom
		rows[3] = h
	}

	private fun updateWorldVertices() {
		for (i in 0..3) {
			val row = rows[i]
			for (j in 0..3) {
				concatenatedTransform.prj(globalPositions[i * 4 + j].set(columns[j], row, 0f))
			}
		}
	}

	override fun draw(viewport: MinMaxRo) {
		val texture = texture
		if (texture == null || width <= 0f || height <= 0f) return
		glState.camera(camera)
		glState.setTexture(texture)
		glState.blendMode(blendMode, premultipliedAlpha = false)

		val batch = glState.batch
		batch.begin()

		val c = concatenatedColorTint
		val normal = Vector3.NEG_Z


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
			batch.putVertex(globalPositions[0], normal, c, rowU0, colV0)
			batch.putVertex(globalPositions[1], normal, c, rowU0, colV1)
			batch.putVertex(globalPositions[2], normal, c, rowU0, colV2)
			batch.putVertex(globalPositions[3], normal, c, rowU0, colV3)

			// Row 1
			batch.putVertex(globalPositions[4], normal, c, rowU1, colV0)
			batch.putVertex(globalPositions[5], normal, c, rowU1, colV1)
			batch.putVertex(globalPositions[6], normal, c, rowU1, colV2)
			batch.putVertex(globalPositions[7], normal, c, rowU1, colV3)

			// Row 2
			batch.putVertex(globalPositions[8], normal, c, rowU2, colV0)
			batch.putVertex(globalPositions[9], normal, c, rowU2, colV1)
			batch.putVertex(globalPositions[10], normal, c, rowU2, colV2)
			batch.putVertex(globalPositions[11], normal, c, rowU2, colV3)

			// Row 3
			batch.putVertex(globalPositions[12], normal, c, rowU3, colV0)
			batch.putVertex(globalPositions[13], normal, c, rowU3, colV1)
			batch.putVertex(globalPositions[14], normal, c, rowU3, colV2)
			batch.putVertex(globalPositions[15], normal, c, rowU3, colV3)
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
			batch.putVertex(globalPositions[0], normal, c, colU0, rowV0)
			batch.putVertex(globalPositions[1], normal, c, colU1, rowV0)
			batch.putVertex(globalPositions[2], normal, c, colU2, rowV0)
			batch.putVertex(globalPositions[3], normal, c, colU3, rowV0)

			// Row 1
			batch.putVertex(globalPositions[4], normal, c, colU0, rowV1)
			batch.putVertex(globalPositions[5], normal, c, colU1, rowV1)
			batch.putVertex(globalPositions[6], normal, c, colU2, rowV1)
			batch.putVertex(globalPositions[7], normal, c, colU3, rowV1)

			// Row 2
			batch.putVertex(globalPositions[8], normal, c, colU0, rowV2)
			batch.putVertex(globalPositions[9], normal, c, colU1, rowV2)
			batch.putVertex(globalPositions[10], normal, c, colU2, rowV2)
			batch.putVertex(globalPositions[11], normal, c, colU3, rowV2)

			// Row 3
			batch.putVertex(globalPositions[12], normal, c, colU0, rowV3)
			batch.putVertex(globalPositions[13], normal, c, colU1, rowV3)
			batch.putVertex(globalPositions[14], normal, c, colU2, rowV3)
			batch.putVertex(globalPositions[15], normal, c, colU3, rowV3)
		}
		batch.putIndices(indices)
	}
}

fun Owned.ninePatch(init: ComponentInit<NinePatchComponent> = {}): NinePatchComponent {
	val ninePatch = NinePatchComponent(this)
	ninePatch.init()
	return ninePatch
}
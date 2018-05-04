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

import com.acornui.component.layout.SizeConstraints
import com.acornui.core.di.Owned
import com.acornui.core.graphics.BlendMode
import com.acornui.core.graphics.Texture
import com.acornui.math.Bounds
import com.acornui.math.IntRectangleRo
import com.acornui.math.Rectangle

/**
 * @author nbilyk
 */
class NinePatchComponent(owner: Owned) : ContainerImpl(owner) {

	private var _isRotated: Boolean = false
	private var _region: FloatArray = floatArrayOf(0f, 0f, 0f, 0f)

	private var _splitLeft: Float = 0f
	private var _splitTop: Float = 0f
	private var _splitRight: Float = 0f
	private var _splitBottom: Float = 0f

	private val leftC: TextureComponent = textureC()
	private val topLeftC: TextureComponent = textureC()
	private val topC: TextureComponent = textureC()
	private val topRightC: TextureComponent = textureC()
	private val rightC: TextureComponent = textureC()
	private val bottomRightC: TextureComponent = textureC()
	private val bottomC: TextureComponent = textureC()
	private val bottomLeftC: TextureComponent = textureC()
	private val midC: TextureComponent = textureC()

	private val regions = arrayOf(leftC, topLeftC, topC, topRightC, rightC, bottomRightC, bottomC, bottomLeftC, midC)

	private var _naturalWidth: Float = 0f
	private var _naturalHeight: Float = 0f

	private var _texture: Texture? = null
	private var _path: String? = null

	init {
		for (i in regions) {
			addChild(children.size, i)
		}
	}

	var blendMode: BlendMode
		get() = leftC.blendMode
		set(value) {
			for (i in 0..regions.lastIndex) {
				regions[i].blendMode = value
			}
		}

	var path: String?
		get() {
			return _path
		}
		set(value) {
			if (_path == value) return
			_texture = null
			_path = value
			for (i in regions) {
				i.path = value
			}
		}

	var texture: Texture?
		get() = _texture
		set(value) {
			if (_texture == value) return
			_path = null
			_texture = value
			for (i in regions) {
				i.texture = value
			}
		}

	fun setRegion(region: Rectangle, isRotated: Boolean) {
		setRegion(region.x, region.y, region.width, region.height, isRotated)
	}

	fun setRegion(region: IntRectangleRo, isRotated: Boolean) {
		setRegion(region.x.toFloat(), region.y.toFloat(), region.width.toFloat(), region.height.toFloat(), isRotated)
	}

	fun setRegion(x: Float, y: Float, width: Float, height: Float, isRotated: Boolean) {
		_region[0] = x
		_region[1] = y
		_region[2] = width + x
		_region[3] = height + y
		_isRotated = isRotated
		for (i in 0..regions.lastIndex) {
			regions[i].isRotated = isRotated
		}
		invalidate(ValidationFlags.PROPERTIES)
	}

	fun isRotated(): Boolean {
		return _isRotated
	}

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
		invalidate(ValidationFlags.PROPERTIES)
	}

	fun naturalWidth(): Float {
		validate(ValidationFlags.PROPERTIES)
		return _naturalWidth
	}

	fun naturalHeight(): Float {
		validate(ValidationFlags.PROPERTIES)
		return _naturalHeight
	}

	override fun updateProperties() {
		validateRegions()
	}

	/**
	 * Validates the slice regions. This method is dependent on the region and split properties.
	 */
	private fun validateRegions() {
		val x = _region[0]
		val y = _region[1]
		val w = _region[2] - x
		val h = _region[3] - y

		if (isRotated()) {
			val x2 = _region[2] - _splitTop
			val y2 = _region[3] - _splitRight
			val midW = w - _splitBottom - _splitTop
			val midH = h - _splitLeft - _splitRight

			leftC.setRegion(x + _splitBottom, y, midW, _splitLeft)
			topLeftC.setRegion(x2, y, _splitTop, _splitLeft)
			topC.setRegion(x2, y + _splitLeft, _splitTop, midH)
			topRightC.setRegion(x2, y2, _splitTop, _splitRight)
			rightC.setRegion(x + _splitBottom, y2, midW, _splitRight)
			bottomRightC.setRegion(x, y2, _splitBottom, _splitRight)
			bottomC.setRegion(x, y + _splitLeft, _splitBottom, midH)
			bottomLeftC.setRegion(x, y, _splitBottom, _splitLeft)
			midC.setRegion(x + _splitBottom, y + _splitLeft, midW, midH)

			_naturalWidth = h
			_naturalHeight = w
		} else {
			val x2 = _region[2] - _splitRight
			val y2 = _region[3] - _splitBottom
			val midW = w - _splitLeft - _splitRight
			val midH = h - _splitBottom - _splitTop

			leftC.setRegion(x, y + _splitTop, _splitLeft, midH)
			topLeftC.setRegion(x, y, _splitLeft, _splitTop)
			topC.setRegion(x + _splitLeft, y, midW, _splitTop)
			topRightC.setRegion(x2, y, _splitRight, _splitTop)
			rightC.setRegion(x2, y + _splitTop, _splitRight, midH)
			bottomRightC.setRegion(x2, y2, _splitRight, _splitBottom)
			bottomC.setRegion(x + _splitLeft, y2, midW, _splitBottom)
			bottomLeftC.setRegion(x, y2, _splitLeft, _splitBottom)
			midC.setRegion(x + _splitLeft, y + _splitTop, midW, midH)

			_naturalWidth = w
			_naturalHeight = h
		}
	}

	override fun updateSizeConstraints(out: SizeConstraints) {
		out.width.min = _splitLeft + _splitRight
		out.height.min = _splitTop + _splitBottom
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val scaledW = explicitWidth ?: naturalWidth()
		val scaledH = explicitHeight ?: naturalHeight()
		val scaledX2 = scaledW - _splitRight
		val scaledY2 = scaledH - _splitBottom
		val scaledMidW = scaledW - _splitLeft - _splitRight
		val scaledMidH = scaledH - _splitBottom - _splitTop

		leftC.setPosition(0f, _splitTop)
		leftC.setSize(null, scaledMidH)
		topLeftC.setPosition(0f, 0f)
		topC.setPosition(_splitLeft, 0f)
		topC.setSize(scaledMidW, null)
		topRightC.setPosition(scaledX2, 0f)
		rightC.setPosition(scaledX2, _splitTop)
		rightC.setSize(null, scaledMidH)
		bottomRightC.setPosition(scaledX2, scaledY2)
		bottomC.setPosition(_splitLeft, scaledY2)
		bottomC.setSize(scaledMidW, null)
		bottomLeftC.setPosition(0f, scaledY2)
		midC.setPosition(_splitLeft, _splitTop)
		midC.setSize(scaledMidW, scaledMidH)
		out.set(scaledW, scaledH)
	}
}

fun Owned.ninePatch(init: ComponentInit<NinePatchComponent> = {}): NinePatchComponent {
	val ninePatch = NinePatchComponent(this)
	ninePatch.init()
	return ninePatch
}
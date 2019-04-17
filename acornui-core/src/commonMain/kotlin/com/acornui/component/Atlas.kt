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

import com.acornui.recycle.Clearable
import com.acornui.core.graphic.*
import com.acornui.gl.core.GlState
import com.acornui.graphic.ColorRo
import com.acornui.math.Matrix4Ro
import com.acornui.math.MinMax
import com.acornui.math.MinMaxRo

class Atlas(private val glState: GlState) : BasicDrawable, Clearable {

	private var _region: AtlasRegionData? = null

	private var width = 0f
	private var height = 0f

	override fun drawRegion(out: MinMax): MinMax = out.set(0f, 0f, width, height)

	val region: AtlasRegionData?
		get() = _region

	private var texture: Texture? = null

	private val drawable: BasicDrawable?
		get() = _sprite ?: _ninePatch

	private var _sprite: Sprite? = null
	private var _ninePatch: NinePatch? = null

	private var _blendMode = BlendMode.NORMAL
	var blendMode: BlendMode
		get() = _blendMode
		set(value) {
			_blendMode = value
			_sprite?.blendMode = value
			_ninePatch?.blendMode = value
		}

	private var _useAsBackFace: Boolean = false

	/**
	 * If true, the normal and indices will be reversed.
	 */
	var useAsBackFace: Boolean
		get() = _useAsBackFace
		set(value) {
			_useAsBackFace = value
			_sprite?.useAsBackFace = value
			_ninePatch?.useAsBackFace = value
		}

	/**
	 * Sets the region and texture for what should be drawn.
	 */
	fun setRegionAndTexture(texture: Texture, region: AtlasRegionData) {
		if (region.splits == null) {
			_ninePatch = null
			if (_sprite == null) {
				_sprite = Sprite(glState)
				_sprite?.blendMode = _blendMode
				_sprite?.useAsBackFace = _useAsBackFace
			}
			val t = _sprite!!
			t.setRegion(region.bounds, region.isRotated)
		} else {
			_sprite = null
			if (_ninePatch == null) {
				_ninePatch = NinePatch(glState)
				_ninePatch?.blendMode = _blendMode
				_ninePatch?.useAsBackFace = _useAsBackFace
			}
			val t = _ninePatch!!
			val splits = region.splits
			t.split(
					maxOf(0f, splits[0] - region.padding[0]),
					maxOf(0f, splits[1] - region.padding[1]),
					maxOf(0f, splits[2] - region.padding[2]),
					maxOf(0f, splits[3] - region.padding[3])
			)
			t.setRegion(region.bounds, region.isRotated)
		}
		_sprite?.texture = texture
		_ninePatch?.texture = texture
		this._region = region
		this.texture = texture
	}

	override val naturalWidth: Float
		get() {
			val region = _region ?: return 0f
			val regionWidth = if (region.isRotated) region.bounds.height else region.bounds.width
			return (region.padding[0] + regionWidth + region.padding[2]).toFloat()
		}

	override val naturalHeight: Float
		get() {
			val region = _region ?: return 0f
			val regionHeight = if (region.isRotated) region.bounds.width else region.bounds.height
			return (region.padding[1] + regionHeight + region.padding[3]).toFloat()
		}

	private var totalPadLeft = 0f
	private var totalPadTop = 0f
	private var totalPadRight = 0f
	private var totalPadBottom = 0f

	override fun updateVertices(width: Float, height: Float, x: Float, y: Float, z: Float, rotation: Float, originX: Float, originY: Float) {
		this.width = width
		this.height = height
		val drawable = drawable ?: return
		updatePadding(width, height)

		drawable.updateVertices(
				width - totalPadLeft - totalPadRight,
				height - totalPadBottom - totalPadTop,
				x = totalPadLeft,
				y = totalPadTop,
				rotation = rotation,
				originX = originX,
				originY = originY
		)
	}

	private fun updatePadding(width: Float, height: Float) {
		val region = _region ?: return

		val paddingLeft = region.padding[0].toFloat()
		val paddingTop = region.padding[1].toFloat()
		val paddingRight = region.padding[2].toFloat()
		val paddingBottom = region.padding[3].toFloat()

		// Account for scaling with split regions if there are any.
		val splits = region.splits ?: EMPTY_SPLITS
		val unscaledPadLeft = minOf(paddingLeft, splits[0])
		val unscaledPadTop = minOf(paddingTop, splits[1])
		val unscaledPadRight = minOf(paddingRight, splits[2])
		val unscaledPadBottom = minOf(paddingBottom, splits[3])

		val scaledPadLeft = paddingLeft - unscaledPadLeft
		val scaledPadTop = paddingTop - unscaledPadTop
		val scaledPadRight = paddingRight - unscaledPadRight
		val scaledPadBottom = paddingBottom - unscaledPadBottom

		val uH = splits[0] + splits[2]
		val uV = splits[1] + splits[3]
		val sX: Float = (width - uH) / (naturalWidth - uH)
		val sY: Float = (height - uV) / (naturalHeight - uV)

		totalPadLeft = unscaledPadLeft + scaledPadLeft * sX
		totalPadTop = unscaledPadTop + scaledPadTop * sY
		totalPadRight = unscaledPadRight + scaledPadRight * sX
		totalPadBottom = unscaledPadBottom + scaledPadBottom * sY
	}

	override fun render(clip: MinMaxRo, transform: Matrix4Ro, tint: ColorRo) {
		drawable?.render(clip, transform, tint)
	}

	override fun clear() {
		texture = null
		_region = null
		_ninePatch = null
		_sprite = null
	}

	companion object {
		private val EMPTY_SPLITS = listOf(0f, 0f, 0f, 0f)
	}
}
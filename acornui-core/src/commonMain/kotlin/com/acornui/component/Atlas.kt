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

@file:Suppress("MemberVisibilityCanBePrivate")

package com.acornui.component

import com.acornui.gl.core.CachedGl20
import com.acornui.recycle.Clearable
import com.acornui.graphic.*
import com.acornui.math.Matrix4
import com.acornui.math.Matrix4Ro

class Atlas(val gl: CachedGl20) : BasicRenderable, Clearable {

	var region: AtlasRegion? = null
		private set

	val regionData: AtlasRegionData?
		get() = region?.data

	val texture: TextureRo?
		get() = region?.texture

	private val drawable: BasicRenderable?
		get() = sprite ?: ninePatch

	private var sprite: Sprite? = null
	private var ninePatch: NinePatch? = null

	var blendMode: BlendMode = BlendMode.NORMAL
		set(value) {
			field = value
			sprite?.blendMode = value
			ninePatch?.blendMode = value
		}

	var premultipliedAlpha: Boolean = false
		set(value) {
			field = value
			sprite?.premultipliedAlpha = value
			ninePatch?.premultipliedAlpha = value
		}

	/**
	 * Sets the region and texture for what should be drawn.
	 * NB: This component isn't responsible for incrementing or decrementing the texture reference counts.
	 */
	fun region(value: AtlasRegion?) {
		if (this.region == value) return
		this.region = value
		if (value == null) {
			ninePatch = null
			sprite = null
			return
		}
		val texture = value.texture
		val data = value.data
		val r = this
		if (data.splits == null) {
			ninePatch = null
			if (sprite == null) {
				sprite = Sprite(gl).apply {
					blendMode = r.blendMode
					premultipliedAlpha = r.premultipliedAlpha
					setScaling(r.scaleX, r.scaleY)
				}
			}
			val t = sprite!!
			t.region(data.bounds, data.isRotated)
		} else {
			sprite = null
			if (ninePatch == null) {
				ninePatch = NinePatch(gl).apply {
					blendMode = r.blendMode
					premultipliedAlpha = r.premultipliedAlpha
					setScaling(r.scaleX, r.scaleY)
				}
			}
			val t = ninePatch!!
			val splits = data.splits
			t.split(
					maxOf(0f, splits[0] - data.padding[0]),
					maxOf(0f, splits[1] - data.padding[1]),
					maxOf(0f, splits[2] - data.padding[2]),
					maxOf(0f, splits[3] - data.padding[3])
			)
			t.region(data.bounds, data.isRotated)
		}
		sprite?.texture = texture
		ninePatch?.texture = texture
	}

	/**
	 * [naturalWidth] uses uv coordinates multiplied by the texture size. If the texture uses dpi scaling, this
	 * scaling should be set on this sprite.
	 */
	var scaleX: Float = 1f
		set(value) {
			field = value
			sprite?.scaleX = value
			ninePatch?.scaleX = value
		}

	/**
	 * [naturalHeight] uses uv coordinates multiplied by the texture size. If the texture uses dpi scaling, this
	 * scaling should be set on this sprite.
	 */
	var scaleY: Float = 1f
		set(value) {
			field = value
			sprite?.scaleY = value
			ninePatch?.scaleY = value
		}

	fun setScaling(scaleX: Float, scaleY: Float) {
		this.scaleX = scaleX
		this.scaleY = scaleY
	}

	override val naturalWidth: Float
		get() {
			val region = regionData ?: return 0f
			val regionWidth = if (region.isRotated) region.bounds.height else region.bounds.width
			return (region.padding[0] + regionWidth + region.padding[2]).toFloat()
		}

	override val naturalHeight: Float
		get() {
			val region = regionData ?: return 0f
			val regionHeight = if (region.isRotated) region.bounds.width else region.bounds.height
			return (region.padding[1] + regionHeight + region.padding[3]).toFloat()
		}

	private var totalPadLeft = 0f
	private var totalPadTop = 0f
	private var totalPadRight = 0f
	private var totalPadBottom = 0f

	private val drawableTransform = Matrix4()

	override fun updateGlobalVertices(width: Float, height: Float, transform: Matrix4Ro, tint: ColorRo) {
		val drawable = drawable ?: return
		updatePadding(width, height)
		drawableTransform.set(transform).translate(totalPadLeft, totalPadTop, 0f)
		drawable.updateGlobalVertices(width - totalPadLeft - totalPadRight, height - totalPadBottom - totalPadTop, drawableTransform, tint)
	}

	private fun updatePadding(width: Float, height: Float) {
		val region = regionData ?: return

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

	override fun render() {
		drawable?.render()
	}

	override fun clear() {
		region = null
		ninePatch = null
		sprite = null
	}

	companion object {
		private val EMPTY_SPLITS = listOf(0f, 0f, 0f, 0f)
	}
}

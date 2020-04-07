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

package com.acornui.component.scroll

import com.acornui.component.ComponentInit
import com.acornui.component.UiComponent
import com.acornui.component.layout.HAlign
import com.acornui.component.style.StyleTag
import com.acornui.di.Context
import com.acornui.math.MathUtils.clamp
import com.acornui.math.MathUtils.lerp
import com.acornui.math.Vector2Ro
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.round

open class VScrollBar(
		owner: Context
) : ScrollBarBase(owner) {

	init {
		styleTags.add(VScrollBar)
	}

	override fun getModelValue(position: Vector2Ro): Float {
		val thumbH = thumb!!.height
		val minY = minTrack
		val maxY = maxTrack
		val denom = maxOf(0.001f, maxY - minY - thumbH)
		val pY = clamp((position.y - minY) / denom, 0f, 1f)
		return scrollModel.rawToDecorated(lerp(scrollModel.min, scrollModel.max, pY))
	}

	override fun updatePartsLayout(width: Float, height: Float, decrementButton: UiComponent?, incrementButton: UiComponent?, track: UiComponent) {
		// Size skin parts

		val sUBH: Float = decrementButton?.height ?: 0f
		val sDBH: Float = incrementButton?.height ?: 0f
		val trackLd = track.layoutDataCast
		track.setSize(trackLd?.getPreferredWidth(width), trackLd?.getPreferredHeight(height - sUBH - sDBH))

		// Position skin parts

		track.moveTo(trackLd.getX(width, track.width), sUBH)
		decrementButton?.moveTo(decrementButton.layoutDataCast.getX(width, decrementButton.width), 0f)
		incrementButton?.moveTo(incrementButton.layoutDataCast.getX(width, incrementButton.width), height - incrementButton.height)
	}

	override fun updateThumbLayout(width: Float, height: Float, thumb: UiComponent) {
		val scrollDiff = scrollModel.max - scrollModel.min

		val thumbAvailable = maxTrack - minTrack
		thumb.visible = thumbAvailable > maxOf(1f, thumb.minHeight)

		val thumbHeight = (thumbAvailable * thumbAvailable) / maxOf(1f, thumbAvailable + scrollDiff * modelToPoints)
		val thumbLd = thumb.layoutDataCast
		thumb.setSize(thumbLd?.getPreferredWidth(width), thumbLd?.getPreferredHeight(thumbHeight))

		val p = if (scrollDiff <= 0.000001f) 0f else (scrollModel.value - scrollModel.min) / scrollDiff

		val minY = minTrack
		val maxY = maxTrack
		val thumbH = thumb.height
		val h = round(maxY - minY + 0.000001f) - thumbH
		thumb.moveTo(thumb.layoutDataCast.getX(width, thumb.width), p * h + minY)
	}

	override val minTrack: Float
		get() = decrementButton?.bottom ?: 0f

	override val maxTrack: Float
		get() = incrementButton?.top ?: height

	companion object : StyleTag
}

private fun ScrollBarLayoutData?.getX(availableWidth: Float, partWidth: Float): Float {
	return when (this?.horizontalAlign ?: HAlign.CENTER) {
		HAlign.LEFT -> 0f
		HAlign.CENTER -> (availableWidth - partWidth) * 0.5f
		HAlign.RIGHT -> availableWidth - partWidth
	}
}

inline fun Context.vScrollBar(init: ComponentInit<VScrollBar> = {}): VScrollBar  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val v = VScrollBar(this)
	v.init()
	return v
}

open class VSlider(owner: Context) : VScrollBar(owner) {

	init {
		styleTags.add(VSlider)
		pageSize = 1f
	}

	companion object : StyleTag
}

fun Context.vSlider(init: ComponentInit<VSlider>): VSlider {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return VSlider(this).apply(init)
}
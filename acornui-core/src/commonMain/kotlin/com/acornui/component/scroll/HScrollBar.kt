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
import com.acornui.component.layout.VAlign
import com.acornui.component.style.StyleTag
import com.acornui.di.Context
import com.acornui.math.MathUtils.clamp
import com.acornui.math.MathUtils.lerp
import com.acornui.math.Vector2Ro
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.round

open class HScrollBar(
		owner: Context
) : ScrollBarBase(owner) {

	init {
		styleTags.add(HScrollBar)
	}

	override fun getModelValue(position: Vector2Ro): Float {
		val thumbW = thumb!!.width
		val minX = minTrack
		val maxX = maxTrack
		val denom = maxOf(0.001f, maxX - minX - thumbW)
		val pX = clamp((position.x - minX) / denom, 0f, 1f)
		return scrollModel.rawToDecorated(lerp(scrollModel.min, scrollModel.max, pX))
	}

	override fun updatePartsLayout(width: Float, height: Float, decrementButton: UiComponent?, incrementButton: UiComponent?, track: UiComponent) {
		// Size skin parts

		val sUBW: Float = decrementButton?.width ?: 0f
		val sDBW: Float = incrementButton?.width ?: 0f
		val trackLd = track.layoutDataCast
		track.setSize(trackLd?.getPreferredWidth(width - sUBW - sDBW), trackLd?.getPreferredHeight(height))

		// Position skin parts

		track.moveTo(sUBW, trackLd.getY(height, track.height))
		decrementButton?.moveTo(0f, decrementButton.layoutDataCast.getY(height, decrementButton.height))
		incrementButton?.moveTo(width - incrementButton.width, incrementButton.layoutDataCast.getY(height, incrementButton.height))
	}

	override fun updateThumbLayout(width: Float, height: Float, thumb: UiComponent) {
		val scrollDiff = scrollModel.max - scrollModel.min

		val thumbAvailable = maxTrack - minTrack
		thumb.visible = thumbAvailable > maxOf(1f, thumb.minWidth)

		val thumbWidth = (thumbAvailable * thumbAvailable) / maxOf(1f, thumbAvailable + scrollDiff * modelToPoints)
		val thumbLd = thumb.layoutDataCast
		thumb.setSize(thumbLd?.getPreferredWidth(thumbWidth), thumbLd?.getPreferredHeight(height))

		val p = if (scrollDiff <= 0.000001f) 0f else (scrollModel.value - scrollModel.min) / scrollDiff

		val minX = minTrack
		val maxX = maxTrack
		val thumbW = thumb.width
		val w = round(maxX - minX + 0.000001f) - thumbW
		thumb.moveTo(p * w + minX, thumb.layoutDataCast.getY(height, thumb.height))
	}

	override val minTrack: Float
		get() = decrementButton?.right ?: 0f

	override val maxTrack: Float
		get() = incrementButton?.left ?: width

	companion object : StyleTag
}

private fun ScrollBarLayoutData?.getY(availableHeight: Float, partHeight: Float): Float {
	return when (this?.verticalAlign ?: VAlign.MIDDLE) {
		VAlign.TOP -> 0f
		VAlign.MIDDLE -> (availableHeight - partHeight) * 0.5f
		VAlign.BASELINE, VAlign.BOTTOM -> availableHeight - partHeight
	}
}

inline fun Context.hScrollBar(init: ComponentInit<HScrollBar> = {}): HScrollBar  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val h = HScrollBar(this)
	h.init()
	return h
}

open class HSlider(owner: Context) : HScrollBar(owner) {

	init {
		styleTags.add(HSlider)
		pageSize = 1f
	}

	companion object : StyleTag
}

fun Context.hSlider(init: ComponentInit<HSlider>): HSlider {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return HSlider(this).apply(init)
}
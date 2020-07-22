///*
// * Copyright 2020 Poly Forest, LLC
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package com.acornui.component.scroll
//
//import com.acornui.component.ComponentInit
//import com.acornui.component.UiComponent
//import com.acornui.component.layout.VAlign
//import com.acornui.component.style.StyleTag
//import com.acornui.di.Context
//import com.acornui.math.clamp
//import com.acornui.math.lerp
//import com.acornui.math.Vector2Ro
//import kotlin.contracts.InvocationKind
//import kotlin.contracts.contract
//import kotlin.math.round
//
//open class HScrollbar(
//		owner: Context
//) : ScrollbarBase(owner) {
//
//	init {
//		addClass(HScrollbar)
//	}
//
//	override fun getModelValue(position: Vector2Ro): Double {
//		val thumbW = thumb!!.width
//		val minX = minTrack
//		val maxX = maxTrack
//		val denom = maxOf(0.001, maxX - minX - thumbW)
//		val pX = clamp((position.x - minX) / denom, 0.0, 1.0)
//		return scrollModel.rawToDecorated(lerp(scrollModel.min, scrollModel.max, pX))
//	}
//
//	override fun updatePartsLayout(width: Double, height: Double, decrementButton: UiComponent?, incrementButton: UiComponent?, track: UiComponent) {
//		// Size skin parts
//
//		val sUBW: Double = decrementButton?.width ?: 0.0
//		val sDBW: Double = incrementButton?.width ?: 0.0
//		val trackLd = track.layoutDataCast
//		track.size(trackLd?.getPreferredWidth(width - sUBW - sDBW), trackLd?.getPreferredHeight(height))
//
//		// Position skin parts
//
//		track.position(sUBW, trackLd.getY(height, track.height))
//		decrementButton?.position(0.0, decrementButton.layoutDataCast.getY(height, decrementButton.height))
//		incrementButton?.position(width - incrementButton.width, incrementButton.layoutDataCast.getY(height, incrementButton.height))
//	}
//
//	override fun updateThumbLayout(width: Double, height: Double, thumb: UiComponent) {
//		val scrollDiff = scrollModel.max - scrollModel.min
//
//		val thumbAvailable = maxTrack - minTrack
//		thumb.visible = thumbAvailable > maxOf(1.0, thumb.minWidth)
//
//		val thumbWidth = (thumbAvailable * thumbAvailable) / maxOf(1.0, thumbAvailable + scrollDiff * modelToPoints)
//		val thumbLd = thumb.layoutDataCast
//		thumb.size(thumbLd?.getPreferredWidth(thumbWidth), thumbLd?.getPreferredHeight(height))
//
//		val p = if (scrollDiff <= 0.000001) 0.0 else (scrollModel.value - scrollModel.min) / scrollDiff
//
//		val minX = minTrack
//		val maxX = maxTrack
//		val thumbW = thumb.width
//		val w = round(maxX - minX + 0.000001) - thumbW
//		thumb.position(p * w + minX, thumb.layoutDataCast.getY(height, thumb.height))
//	}
//
//	override val minTrack: Double
//		get() = decrementButton?.right ?: 0.0
//
//	override val maxTrack: Double
//		get() = incrementButton?.left ?: width
//
//	companion object : StyleTag
//}
//
//private fun ScrollbarLayoutData?.getY(availableHeight: Double, partHeight: Double): Double {
//	return when (this?.verticalAlign ?: VAlign.MIDDLE) {
//		VAlign.TOP -> 0.0
//		VAlign.MIDDLE -> (availableHeight - partHeight) * 0.5
//		VAlign.BASELINE, VAlign.BOTTOM -> availableHeight - partHeight
//	}
//}
//
//inline fun Context.hScrollbar(init: ComponentInit<HScrollbar> = {}): HScrollbar  {
//	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
//	val h = HScrollbar(this)
//	h.init()
//	return h
//}
//
//open class HSlider(owner: Context) : HScrollbar(owner) {
//
//	init {
//		addClass(HSlider)
//		pageSize = 1.0
//	}
//
//	companion object : StyleTag
//}
//
//fun Context.hSlider(init: ComponentInit<HSlider>): HSlider {
//	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
//	return HSlider(this).apply(init)
//}
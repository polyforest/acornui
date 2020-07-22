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
//import com.acornui.component.layout.HAlign
//import com.acornui.component.style.StyleTag
//import com.acornui.di.Context
//import com.acornui.math.lerp
//import com.acornui.math.Vector2Ro
//import kotlin.contracts.InvocationKind
//import kotlin.contracts.contract
//import kotlin.math.round
//
//open class VScrollbar(
//		owner: Context
//) : ScrollbarBase(owner) {
//
//	init {
//		addClass(VScrollbar)
//	}
//
//	override fun getModelValue(position: Vector2Ro): Double {
//		val thumbH = thumb!!.height
//		val minY = minTrack
//		val maxY = maxTrack
//		val denom = maxOf(0.001, maxY - minY - thumbH)
//		val pY = clamp((position.y - minY) / denom, 0.0, 1.0)
//		return scrollModel.rawToDecorated(lerp(scrollModel.min, scrollModel.max, pY))
//	}
//
//	override fun updatePartsLayout(width: Double, height: Double, decrementButton: UiComponent?, incrementButton: UiComponent?, track: UiComponent) {
//		// Size skin parts
//
//		val sUBH: Double = decrementButton?.height ?: 0.0
//		val sDBH: Double = incrementButton?.height ?: 0.0
//		val trackLd = track.layoutDataCast
//		track.size(trackLd?.getPreferredWidth(width), trackLd?.getPreferredHeight(height - sUBH - sDBH))
//
//		// Position skin parts
//
//		track.position(trackLd.getX(width, track.width), sUBH)
//		decrementButton?.position(decrementButton.layoutDataCast.getX(width, decrementButton.width), 0.0)
//		incrementButton?.position(incrementButton.layoutDataCast.getX(width, incrementButton.width), height - incrementButton.height)
//	}
//
//	override fun updateThumbLayout(width: Double, height: Double, thumb: UiComponent) {
//		val scrollDiff = scrollModel.max - scrollModel.min
//
//		val thumbAvailable = maxTrack - minTrack
//		thumb.visible = thumbAvailable > maxOf(1.0, thumb.minHeight)
//
//		val thumbHeight = (thumbAvailable * thumbAvailable) / maxOf(1.0, thumbAvailable + scrollDiff * modelToPoints)
//		val thumbLd = thumb.layoutDataCast
//		thumb.size(thumbLd?.getPreferredWidth(width), thumbLd?.getPreferredHeight(thumbHeight))
//
//		val p = if (scrollDiff <= 0.000001) 0.0 else (scrollModel.value - scrollModel.min) / scrollDiff
//
//		val minY = minTrack
//		val maxY = maxTrack
//		val thumbH = thumb.height
//		val h = round(maxY - minY + 0.000001) - thumbH
//		thumb.position(thumb.layoutDataCast.getX(width, thumb.width), p * h + minY)
//	}
//
//	override val minTrack: Double
//		get() = decrementButton?.bottom ?: 0.0
//
//	override val maxTrack: Double
//		get() = incrementButton?.top ?: height
//
//	companion object : StyleTag
//}
//
//private fun ScrollbarLayoutData?.getX(availableWidth: Double, partWidth: Double): Double {
//	return when (this?.horizontalAlign ?: HAlign.CENTER) {
//		HAlign.LEFT -> 0.0
//		HAlign.CENTER -> (availableWidth - partWidth) * 0.5
//		HAlign.RIGHT -> availableWidth - partWidth
//	}
//}
//
//inline fun Context.vScrollbar(init: ComponentInit<VScrollbar> = {}): VScrollbar  {
//	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
//	val v = VScrollbar(this)
//	v.init()
//	return v
//}
//
//open class VSlider(owner: Context) : VScrollbar(owner) {
//
//	init {
//		addClass(VSlider)
//		pageSize = 1.0
//	}
//
//	companion object : StyleTag
//}
//
//fun Context.vSlider(init: ComponentInit<VSlider>): VSlider {
//	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
//	return VSlider(this).apply(init)
//}
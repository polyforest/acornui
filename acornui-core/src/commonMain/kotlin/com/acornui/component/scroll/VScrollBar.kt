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
import com.acornui.component.InteractivityMode
import com.acornui.component.UiComponent
import com.acornui.component.layout.SizeConstraints
import com.acornui.component.layout.algorithm.BasicLayoutData
import com.acornui.component.style.StyleTag
import com.acornui.di.Owned
import com.acornui.math.Bounds
import com.acornui.math.Vector2
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.round

open class VScrollBar(
		owner: Owned
) : ScrollBarBase(owner) {

	init {
		styleTags.add(VScrollBar)
	}

	override fun getModelValue(position: Vector2): Float {
		val thumb = thumb!!
		val minY = minTrack()
		val maxY = maxTrack()
		val denom = maxOf(0.001f, maxY - minY - thumb.height)
		var pY = (position.y - minY) / denom
		if (pY > 0.99f) pY = 1f
		if (pY < 0.01f) pY = 0f
		return pY * (scrollModel.max - scrollModel.min) + scrollModel.min
	}

	override fun updateSizeConstraints(out: SizeConstraints) {
		val stepUpButton = decrementButton!!
		val stepDownButton = incrementButton!!
		out.height.min = stepUpButton.height + stepDownButton.height
		out.width.min = maxOf(stepUpButton.width, stepDownButton.width, track?.minWidth ?: 0f)
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val stepUpButton = decrementButton!!
		val stepDownButton = incrementButton!!
		val track = track!!
		val thumb = thumb!!
		val minH = minHeight ?: 0f
		val h = explicitHeight ?: maxOf(minH, style.defaultSize)
		val sUbh: Float = stepUpButton.height
		val sDbh: Float = stepDownButton.height
		val w = maxOf(stepUpButton.width, stepDownButton.width)
		val trackLd = track.layoutData as BasicLayoutData?
		if (trackLd == null) track.setSize(w, h - sUbh - sDbh)
		else track.setSize(trackLd.getPreferredWidth(w), trackLd.getPreferredHeight(h - sUbh - sDbh))
		track.moveTo(0f, sUbh)
		stepDownButton.moveTo(0f, h - stepDownButton.height)

		val scrollDiff = scrollModel.max - scrollModel.min
		val thumbAvailable = maxTrack() - minTrack()
		thumb.visible = thumbAvailable > maxOf(1f, (thumb.minHeight ?: 0f))
		track.visible = thumb.visible
		thumb.interactivityMode = if (style.pageMode && scrollDiff > 0f) InteractivityMode.ALL else InteractivityMode.NONE
		track.interactivityMode = if (scrollDiff > 0f) InteractivityMode.ALL else InteractivityMode.NONE
		if (thumb.visible) {
			val thumbHeight = (thumbAvailable * thumbAvailable) / maxOf(1f, thumbAvailable + scrollDiff * modelToPixels)
			val thumbLd = thumb.layoutData as BasicLayoutData?
			if (thumbHeight.isNaN())
				throw Exception("thumb height may not be NaN")
			if (thumbLd == null) thumb.setSize(w, thumbHeight)
			else thumb.setSize(thumbLd.getPreferredWidth(w), thumbLd.getPreferredHeight(thumbHeight))
			refreshThumbPosition()
		}
		out.set(maxOf(w, track.width, if (thumb.visible) thumb.width else 0f), h)
	}

	override fun refreshThumbPosition() {
		val thumb = thumb!!
		val scrollDiff = scrollModel.max - scrollModel.min
		val p = if (scrollDiff <= 0.000001f) 0f else (scrollModel.value - scrollModel.min) / scrollDiff

		val minY = minTrack()
		val maxY = maxTrack()
		val h = round(maxY - minY + 0.000001f) - thumb.height
		thumb.moveTo(0f, p * h + minY)
	}

	override fun minTrack(): Float {
		return decrementButton!!.bottom()
	}

	private fun UiComponent.bottom(): Float {
		return y + height
	}

	override fun maxTrack(): Float {
		return incrementButton!!.y
	}

	companion object : StyleTag
}

inline fun Owned.vScrollBar(init: ComponentInit<VScrollBar> = {}): VScrollBar  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val v = VScrollBar(this)
	v.init()
	return v
}

open class VSlider(owner: Owned) : VScrollBar(owner) {

	init {
		styleTags.add(VSlider)
		pageSize(1f)
	}

	companion object : StyleTag
}

fun Owned.vSlider(init: ComponentInit<VSlider>): VSlider {
	val h = VSlider(this)
	h.init()
	return h
}

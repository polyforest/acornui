/*
 * Copytop 2015 Nicholas Bilyk
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
import com.acornui.core.di.Owned
import com.acornui.math.Bounds
import com.acornui.math.Vector2
import kotlin.math.round

open class HScrollBar(
		owner: Owned
) : ScrollBarBase(owner) {

	init {
		styleTags.add(HScrollBar)
	}

	override fun getModelValue(position: Vector2): Float {
		val thumb = thumb!!
		val minX = minTrack()
		val maxX = maxTrack()
		val denom = maxOf(0.001f, maxX - minX - thumb.width)
		var pX = (position.x - minX) / denom
		if (pX > 0.99f) pX = 1f
		if (pX < 0.01f) pX = 0f
		return pX * (scrollModel.max - scrollModel.min) + scrollModel.min
	}

	override fun updateSizeConstraints(out: SizeConstraints) {
		val stepUpButton = decrementButton!!
		val stepDownButton = incrementButton!!
		out.width.min = stepUpButton.width + stepDownButton.width
		out.height.min = maxOf(stepUpButton.height, stepDownButton.height, track?.minHeight ?: 0f)
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val stepUpButton = decrementButton!!
		val stepDownButton = incrementButton!!
		val track = track!!
		val thumb = thumb!!
		val minW = minWidth ?: 0f
		val w = explicitWidth ?: maxOf(minW, style.defaultSize)
		val sUBW: Float = stepUpButton.width
		val sDBW: Float = stepDownButton.width
		val h: Float = maxOf(stepUpButton.height, stepDownButton.height)
		val trackLd = track.layoutData as BasicLayoutData?
		if (trackLd == null) track.setSize(w - sUBW - sDBW, h)
		else track.setSize(trackLd.getPreferredWidth(w - sUBW - sDBW), trackLd.getPreferredHeight(h))
		track.moveTo(sUBW, 0f)
		stepDownButton.moveTo(w - stepDownButton.width, 0f)

		val scrollDiff = scrollModel.max - scrollModel.min
		val thumbAvailable = maxTrack() - minTrack()
		thumb.visible = thumbAvailable > maxOf(1f, (thumb.minWidth ?: 0f))
		thumb.interactivityMode = if (style.pageMode && scrollDiff > 0f) InteractivityMode.ALL else InteractivityMode.NONE
		track.interactivityMode = if (scrollDiff > 0f) InteractivityMode.ALL else InteractivityMode.NONE
		if (thumb.visible) {
			val thumbWidth = (thumbAvailable * thumbAvailable) / maxOf(1f, thumbAvailable + scrollDiff * modelToPixels)
			val thumbLd = thumb.layoutData as BasicLayoutData?
			if (thumbLd == null) thumb.setSize(thumbWidth, h)
			else thumb.setSize(thumbLd.getPreferredWidth(thumbWidth), thumbLd.getPreferredHeight(h))
			refreshThumbPosition()
		}
		out.set(w, maxOf(h, track.height, if (thumb.visible) thumb.height else 0f))
	}

	override fun refreshThumbPosition() {
		val thumb = thumb!!
		val scrollDiff = scrollModel.max - scrollModel.min
		val p = if (scrollDiff <= 0.000001f) 0f else (scrollModel.value - scrollModel.min) / scrollDiff

		val minX = minTrack()
		val maxX = maxTrack()
		val w = round(maxX - minX + 0.000001f) - thumb.width
		thumb.moveTo(p * w + minX, 0f)
	}

	override fun minTrack(): Float {
		return decrementButton!!.right()
	}

	private fun UiComponent.right(): Float {
		return x + width
	}

	override fun maxTrack(): Float {
		return incrementButton!!.x
	}

	companion object : StyleTag
}

fun Owned.hScrollBar(init: ComponentInit<HScrollBar> = {}): HScrollBar {
	val h = HScrollBar(this)
	h.init()
	return h
}

open class HSlider(owner: Owned) : HScrollBar(owner) {

	init {
		styleTags.add(HSlider)
		pageSize(1f)
	}

	companion object : StyleTag
}

fun Owned.hSlider(init: ComponentInit<HSlider>): HSlider {
	val h = HSlider(this)
	h.init()
	return h
}
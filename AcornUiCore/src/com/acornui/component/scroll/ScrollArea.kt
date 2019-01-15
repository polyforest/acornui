/*
 * Copyright 2017 Nicholas Bilyk
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

import com.acornui.component.*
import com.acornui.component.layout.algorithm.LayoutDataProvider
import com.acornui.component.style.*
import com.acornui.core.di.Owned
import com.acornui.core.tween.Tween
import com.acornui.core.tween.createPropertyTween
import com.acornui.math.*

interface ScrollArea : LayoutDataProvider<StackLayoutData>, ElementContainer<UiComponent> {

	val style: ScrollAreaStyle

	val hScrollModel: ClampedScrollModel
	val vScrollModel: ClampedScrollModel

	var hScrollPolicy: ScrollPolicy
	var vScrollPolicy: ScrollPolicy

	/**
	 * The unclipped width of the contents.
	 */
	val contentsWidth: Float

	/**
	 * The unclipped height of the contents.
	 */
	val contentsHeight: Float

	/**
	 * The layout for the contents stack.
	 */
	val stackStyle: StackLayoutStyle

	override fun createLayoutData(): StackLayoutData = StackLayoutData()

	companion object : StyleTag {
		val VBAR_STYLE = styleTag()
		val HBAR_STYLE = styleTag()

		/**
		 * The validation flag used for scrolling.
		 */
		const val SCROLLING: Int = 1 shl 16
	}
}

private val tmpBounds = MinMax()

fun ScrollArea.scrollTo(target: UiComponentRo, pad: PadRo = Pad(10f)) {
	tmpBounds.set(0f, 0f, target.width, target.height)
	target.localToGlobal(tmpBounds)
	globalToLocal(tmpBounds)
	tmpBounds.xMin += hScrollModel.value
	tmpBounds.xMax += hScrollModel.value
	tmpBounds.yMin += vScrollModel.value
	tmpBounds.yMax += vScrollModel.value
	tmpBounds.inflate(pad)
	scrollTo(tmpBounds)
}

/**
 * Scrolls the minimum distance to show the given bounding rectangle.
 */
fun ScrollArea.scrollTo(bounds: RectangleRo) {
	validate(ValidationFlags.LAYOUT)
	if (bounds.x < hScrollModel.value)
		hScrollModel.value = bounds.x
	if (bounds.y < vScrollModel.value)
		vScrollModel.value = bounds.y
	val contentsSetW = contentsWidth - hScrollModel.max
	if (bounds.right > hScrollModel.value + contentsSetW)
		hScrollModel.value = bounds.right - contentsSetW
	val contentsSetH = contentsHeight - vScrollModel.max
	if (bounds.bottom > vScrollModel.value + contentsSetH)
		vScrollModel.value = bounds.bottom - contentsSetH
}

/**
 * Scrolls the minimum distance to show the given bounding MinMax.
 */
fun ScrollArea.scrollTo(bounds: MinMaxRo) {
	validate(ValidationFlags.LAYOUT)
	val contentsSetW = contentsWidth - hScrollModel.max
	val contentsSetH = contentsHeight - vScrollModel.max
	if (bounds.xMin >= hScrollModel.value || bounds.xMax <= hScrollModel.value + contentsSetW) {
		if (bounds.xMin < hScrollModel.value)
			hScrollModel.value = bounds.xMin
		if (bounds.xMax > hScrollModel.value + contentsSetW)
			hScrollModel.value = bounds.xMax - contentsSetW
	}
	if (bounds.yMin >= vScrollModel.value || bounds.yMax <= vScrollModel.value + contentsSetH) {
		if (bounds.yMin < vScrollModel.value)
			vScrollModel.value = bounds.yMin
		if (bounds.yMax > vScrollModel.value + contentsSetH)
			vScrollModel.value = bounds.yMax - contentsSetH
	}
}

fun ScrollArea.tweenScrollX(duration: Float, ease: Interpolation, toScrollX: Float, delay: Float = 0f): Tween {
	return createPropertyTween(this, "scrollX", duration, ease, { hScrollModel.value }, { hScrollModel.value = it }, toScrollX, delay)
}

fun ScrollArea.tweenScrollY(duration: Float, ease: Interpolation, toScrollY: Float, delay: Float = 0f): Tween {
	return createPropertyTween(this, "scrollY", duration, ease, { vScrollModel.value }, { vScrollModel.value = it }, toScrollY, delay)
}

enum class ScrollPolicy {
	OFF,
	ON,
	AUTO
}

fun ScrollPolicy.toCssString(): String {
	return when (this) {
		ScrollPolicy.OFF -> "hidden"
		ScrollPolicy.ON -> "scroll"
		ScrollPolicy.AUTO -> "auto"
	}
}

fun Owned.scrollArea(init: ComponentInit<ScrollAreaImpl> = {}): ScrollAreaImpl {
	val s = ScrollAreaImpl(this)
	s.init()
	return s
}

class ScrollAreaStyle : StyleBase() {

	override val type: StyleType<ScrollAreaStyle> = Companion

	var corner by prop(noSkin)

	var tossScrolling by prop(false)

	var borderRadius: CornersRo by prop(Corners())

	companion object : StyleType<ScrollAreaStyle>
}
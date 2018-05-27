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
import com.acornui.math.Corners
import com.acornui.math.CornersRo
import com.acornui.math.Interpolation
import com.acornui.math.RectangleRo

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
		val SCROLLING: Int = 1 shl 16
	}
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

fun Owned.scrollArea(init: ComponentInit<GlScrollArea> = {}): GlScrollArea {
	val s = GlScrollArea(this)
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

/**
 * A component with virtualization based on scrolling may implement this interface in order to virtualize the
 * visible area.
 */
interface ViewportComponent : UiComponent {

	/**
	 * Sets this component's visible area. The coordinates should be relative to this component.
	 */
	fun viewport(x: Float, y: Float, width: Float, height: Float)

	/**
	 * This entire component will be considered visible.
	 */
	fun clearViewport()
}
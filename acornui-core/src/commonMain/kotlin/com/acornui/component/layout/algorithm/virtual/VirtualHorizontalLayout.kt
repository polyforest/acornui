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

package com.acornui.component.layout.algorithm.virtual

import com.acornui.component.ComponentInit
import com.acornui.component.layout.DataScroller
import com.acornui.component.layout.LayoutElement
import com.acornui.component.layout.VAlign
import com.acornui.component.layout.algorithm.HorizontalLayoutData
import com.acornui.component.style.StyleBase
import com.acornui.component.style.StyleType
import com.acornui.di.Owned
import com.acornui.math.Bounds
import com.acornui.math.Pad
import com.acornui.math.PadRo


class VirtualHorizontalLayout : VirtualLayoutAlgorithm<VirtualHorizontalLayoutStyle, HorizontalLayoutData> {

	override val direction: VirtualLayoutDirection = VirtualLayoutDirection.HORIZONTAL

	override fun getOffset(width: Float, height: Float, element: LayoutElement, index: Int, lastIndex: Int, isReversed: Boolean, props: VirtualHorizontalLayoutStyle): Float {
		val padding = props.padding
		val gap = props.gap
		return if (isReversed) {
			(width - padding.right - (element.x + element.width)) / maxOf(0.0001f, element.width + gap)
		} else {
			(element.x - padding.bottom) / maxOf(0.0001f, element.width + gap)
		}
	}

	override fun updateLayoutEntry(explicitWidth: Float?, explicitHeight: Float?, element: LayoutElement, currentIndex: Int, startIndex: Float, lastIndex: Int, previousElement: LayoutElement?, isReversed: Boolean, props: VirtualHorizontalLayoutStyle) {
		val padding = props.padding
		val gap = props.gap
		val verticalAlign = props.verticalAlign
		val childAvailableWidth = padding.reduceWidth(explicitWidth)
		val childAvailableHeight = padding.reduceHeight(explicitHeight)

		// Size the element
		val layoutData = element.layoutDataCast
		val w = layoutData?.getPreferredWidth(childAvailableWidth)
		val h = layoutData?.getPreferredHeight(childAvailableHeight)
		element.setSize(w, h)

		// Position the element
		val x = if (previousElement == null) {
			val startX = (currentIndex - startIndex) * (element.width + gap)
			if (isReversed) {
				(childAvailableWidth ?: 0f) - padding.right + startX - element.width
			} else {
				padding.left + startX
			}
		} else {
			if (isReversed) {
				previousElement.x - gap - element.width
			} else {
				previousElement.x + previousElement.width + gap
			}
		}

		if (childAvailableHeight == null) {
			element.moveTo(x, padding.top)
		} else {
			when (layoutData?.verticalAlign ?: verticalAlign) {
				VAlign.TOP ->
					element.moveTo(x, padding.top)
				VAlign.MIDDLE ->
					element.moveTo(x, padding.top + (childAvailableHeight - element.height) * 0.5f)
				VAlign.BASELINE, VAlign.BOTTOM ->
					element.moveTo(x, padding.top + (childAvailableHeight - element.height))
			}
		}
	}

	override fun measure(explicitWidth: Float?, explicitHeight: Float?, elements: List<LayoutElement>, props: VirtualHorizontalLayoutStyle, out: Bounds) {
		val padding = props.padding
		super.measure(explicitWidth, explicitHeight, elements, props, out)
		out.add(padding.right, padding.bottom)
	}

	override fun shouldShowRenderer(explicitWidth: Float?, explicitHeight: Float?, element: LayoutElement, props: VirtualHorizontalLayoutStyle): Boolean {
		val buffer = props.buffer
		if (explicitWidth != null) {
			val right = element.x + element.width
			val bufferX = explicitWidth * buffer
			if (right < -bufferX ||
					element.x > explicitWidth + bufferX) {
				return false
			}
		}
		return true
	}

	override fun createLayoutData(): HorizontalLayoutData = HorizontalLayoutData()
}


open class VirtualHorizontalLayoutStyle : StyleBase() {

	override val type: StyleType<VirtualHorizontalLayoutStyle> = Companion

	var gap by prop(0f)

	/**
	 * If there was an explicit height, this represents the percent of that height out of bounds an element can be
	 * before being recycled.
	 */
	var buffer: Float by prop(0.15f)

	/**
	 * The Padding object with left, bottom, top, and right padding.
	 */
	var padding: PadRo by prop(Pad())

	/**
	 * The horizontal alignment of each element within the measured width.
	 */
	var verticalAlign by prop(VAlign.BOTTOM)

	companion object : StyleType<VirtualHorizontalLayoutStyle>

}

/**
 * Creates a virtualized data scroller with a horizontal layout.
 */
fun <E : Any> Owned.hDataScroller(
		init: ComponentInit<DataScroller<E, VirtualHorizontalLayoutStyle, HorizontalLayoutData>> = {}
): DataScroller<E, VirtualHorizontalLayoutStyle, HorizontalLayoutData> {
	val layoutAlgorithm = VirtualHorizontalLayout()
	val c = DataScroller<E, VirtualHorizontalLayoutStyle, HorizontalLayoutData>(this, layoutAlgorithm, VirtualHorizontalLayoutStyle())
	c.init()
	return c
}

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
import com.acornui.component.layout.HAlign
import com.acornui.component.layout.LayoutElement
import com.acornui.component.layout.LayoutElementRo
import com.acornui.component.layout.algorithm.VerticalLayoutData
import com.acornui.component.style.ObservableBase
import com.acornui.component.style.StyleType
import com.acornui.di.Context
import com.acornui.math.Bounds
import com.acornui.math.Pad
import com.acornui.math.PadRo
import kotlin.math.floor

class VirtualVerticalLayout : VirtualLayoutAlgorithm<VirtualVerticalLayoutStyle, VerticalLayoutData> {

	override val direction: VirtualLayoutDirection = VirtualLayoutDirection.VERTICAL

	override fun getOffset(width: Double, height: Double, element: LayoutElementRo, index: Int, lastIndex: Int, isReversed: Boolean, props: VirtualVerticalLayoutStyle): Double {
		val padding = props.padding
		val gap = props.gap
		val elementH = round(element.height)
		return if (isReversed) {
			(height - padding.bottom - element.bottom) / maxOf(0.0001, elementH + gap)
		} else {
			(element.y - padding.top) / maxOf(0.0001, elementH + gap)
		}
	}

	override fun updateLayoutEntry(explicitWidth: Double?, explicitHeight: Double?, element: LayoutElement, currentIndex: Int, startIndex: Double, lastIndex: Int, previousElement: LayoutElementRo?, isReversed: Boolean, props: VirtualVerticalLayoutStyle) {
		val padding = props.padding
		val gap = props.gap
		val horizontalAlign = props.horizontalAlign
		val childAvailableWidth = padding.reduceWidth(explicitWidth)
		val childAvailableHeight = padding.reduceHeight(explicitHeight)

		// Size the element
		val layoutData = element.layoutDataCast
		val w = layoutData?.getPreferredWidth(childAvailableWidth)
		val h = layoutData?.getPreferredHeight(childAvailableHeight)
		element.size(w, h)

		// Position the element
		val elementH = round(element.height)

		val y = if (previousElement == null) {
			val startY = (currentIndex - startIndex) * (elementH + gap)
			if (isReversed) {
				(childAvailableHeight ?: 0.0) - padding.bottom + startY - elementH
			} else {
				padding.top + startY
			}
		} else {
			if (isReversed) {
				previousElement.y - gap - elementH
			} else {
				previousElement.y + round(previousElement.height) + gap
			}
		}

		if (childAvailableWidth == null) {
			element.position(padding.left, y)
		} else {
			when (layoutData?.horizontalAlign ?: horizontalAlign) {
				HAlign.LEFT ->
					element.position(padding.left, y)
				HAlign.CENTER ->
					element.position(padding.left + floor(((childAvailableWidth - element.width) * 0.5)), y)
				HAlign.RIGHT ->
					element.position(padding.left + (childAvailableWidth - element.width), y)
			}
		}
	}

	@Suppress("NOTHING_TO_INLINE")
	private inline fun round(value: Double) : Double = com.acornui.math.offsetRound(value)

	override fun measure(explicitWidth: Double?, explicitHeight: Double?, elements: List<LayoutElementRo>, props: VirtualVerticalLayoutStyle, out: Bounds) {
		val padding = props.padding
		super.measure(explicitWidth, explicitHeight, elements, props, out)
		out.width += padding.right
		out.height += padding.bottom
	}

	override fun shouldShowRenderer(explicitWidth: Double?, explicitHeight: Double?, element: LayoutElementRo, props: VirtualVerticalLayoutStyle): Boolean {
		if (explicitHeight != null) {
			val bottom = element.y + element.height
			val bufferY = explicitHeight * props.buffer
			if (bottom < -bufferY ||
					element.y > explicitHeight + bufferY) {
				return false
			}
		}
		return true
	}

	override fun createLayoutData(): VerticalLayoutData = VerticalLayoutData()
}

open class VirtualVerticalLayoutStyle : ObservableBase() {

	override val type: StyleType<VirtualVerticalLayoutStyle> = Companion

	var gap by prop(0.0)

	/**
	 * If there was an explicit height, this represents the percent of that height out of bounds an element can be
	 * before being recycled.
	 */
	var buffer: Double by prop(0.15)

	/**
	 * The Padding object with left, bottom, top, and right padding.
	 */
	var padding by prop<PadRo>(Pad())

	/**
	 * The horizontal alignment of each element within the measured width.
	 */
	var horizontalAlign by prop(HAlign.LEFT)

	companion object : StyleType<VirtualVerticalLayoutStyle>

}

/**
 * Creates a virtualized data scroller with a vertical layout.
 */
fun <E : Any> Context.vDataScroller(
		init: ComponentInit<DataScroller<E, VirtualVerticalLayoutStyle, VerticalLayoutData>> = {}
): DataScroller<E, VirtualVerticalLayoutStyle, VerticalLayoutData> {
	val layoutAlgorithm = VirtualVerticalLayout()
	val c = DataScroller<E, VirtualVerticalLayoutStyle, VerticalLayoutData>(this, layoutAlgorithm, VirtualVerticalLayoutStyle())
	c.init()
	return c
}
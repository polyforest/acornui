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
import com.acornui.component.layout.ListItemRenderer
import com.acornui.component.layout.algorithm.VerticalLayoutData
import com.acornui.component.style.StyleBase
import com.acornui.component.style.StyleType
import com.acornui.core.di.Owned
import com.acornui.math.Bounds
import com.acornui.math.Pad
import com.acornui.math.PadRo
import kotlin.math.floor

class VirtualVerticalLayout : VirtualLayoutAlgorithm<VirtualVerticalLayoutStyle, VerticalLayoutData> {

	override val direction: VirtualLayoutDirection = VirtualLayoutDirection.VERTICAL

	override fun getOffset(width: Float, height: Float, element: LayoutElement, index: Int, lastIndex: Int, isReversed: Boolean, props: VirtualVerticalLayoutStyle): Float {
		val padding = props.padding
		val gap = props.gap
		return if (isReversed) {
			(height - padding.bottom - (element.y + element.height)) / maxOf(0.0001f, element.height + gap)
		} else {
			(element.y - padding.top) / maxOf(0.0001f, element.height + gap)
		}
	}

	override fun updateLayoutEntry(explicitWidth: Float?, explicitHeight: Float?, element: LayoutElement, currentIndex: Int, startIndex: Float, lastIndex: Int, previousElement: LayoutElement?, isReversed: Boolean, props: VirtualVerticalLayoutStyle) {
		val padding = props.padding
		val gap = props.gap
		val horizontalAlign = props.horizontalAlign
		val childAvailableWidth = padding.reduceWidth(explicitWidth)
		val childAvailableHeight = padding.reduceHeight(explicitHeight)

		// Size the element
		val layoutData = element.layoutDataCast
		val w = layoutData?.getPreferredWidth(childAvailableWidth)
		val h = layoutData?.getPreferredHeight(childAvailableHeight)
		element.setSize(w, h)

		// Position the element
		val y = if (previousElement == null) {
			val startY = (currentIndex - startIndex) * (element.height + gap)
			if (isReversed) {
				(childAvailableHeight ?: 0f) - padding.bottom + startY - element.height
			} else {
				padding.top + startY
			}
		} else {
			if (isReversed) {
				previousElement.y - gap - element.height
			} else {
				previousElement.y + previousElement.height + gap
			}
		}

		if (childAvailableWidth == null) {
			element.moveTo(padding.left, y)
		} else {
			when (layoutData?.horizontalAlign ?: horizontalAlign) {
				HAlign.LEFT ->
					element.moveTo(padding.left, y)
				HAlign.CENTER ->
					element.moveTo(padding.left + floor(((childAvailableWidth - element.width) * 0.5f)), y)
				HAlign.RIGHT ->
					element.moveTo(padding.left + (childAvailableWidth - element.width), y)
			}
		}
	}

	override fun measure(explicitWidth: Float?, explicitHeight: Float?, elements: List<LayoutElement>, props: VirtualVerticalLayoutStyle, out: Bounds) {
		val padding = props.padding
		super.measure(explicitWidth, explicitHeight, elements, props, out)
		out.add(padding.right, padding.bottom)
	}

	override fun shouldShowRenderer(explicitWidth: Float?, explicitHeight: Float?, element: LayoutElement, props: VirtualVerticalLayoutStyle): Boolean {
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

open class VirtualVerticalLayoutStyle : StyleBase() {

	override val type: StyleType<VirtualVerticalLayoutStyle> = Companion

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
	var horizontalAlign by prop(HAlign.LEFT)

	companion object : StyleType<VirtualVerticalLayoutStyle>

}

/**
 * Creates a virtualized data scroller with a vertical layout.
 */
fun <E : Any> Owned.vDataScroller(
		init: ComponentInit<DataScroller<E, VirtualVerticalLayoutStyle, VerticalLayoutData>> = {}
): DataScroller<E, VirtualVerticalLayoutStyle, VerticalLayoutData> {
	val layoutAlgorithm = VirtualVerticalLayout()
	val c = DataScroller<E, VirtualVerticalLayoutStyle, VerticalLayoutData>(this, layoutAlgorithm, VirtualVerticalLayoutStyle())
	c.init()
	return c
}

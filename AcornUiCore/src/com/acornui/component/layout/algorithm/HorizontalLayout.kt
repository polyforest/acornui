/*
 * Copyright 2015 Nicholas Bilyk
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

package com.acornui.component.layout.algorithm

import com.acornui.component.ComponentInit
import com.acornui.component.layout.*
import com.acornui.component.style.StyleBase
import com.acornui.component.style.StyleType
import com.acornui.core.di.Owned
import com.acornui.math.Bounds
import com.acornui.math.MathUtils
import com.acornui.math.Pad
import com.acornui.math.PadRo
import kotlin.math.floor

class HorizontalLayout : LayoutAlgorithm<HorizontalLayoutStyle, HorizontalLayoutData> {

	override fun calculateSizeConstraints(elements: List<LayoutElementRo>, props: HorizontalLayoutStyle, out: SizeConstraints) {
		val padding = props.padding
		val gap = props.gap

		var minHeight = 0f
		var minWidth = 0f
		for (i in 0..elements.lastIndex) {
			val element = elements[i]
			val sC = element.sizeConstraints
			val iMinHeight = sC.height.min
			if (iMinHeight != null) minHeight = maxOf(iMinHeight, minHeight)
			val iMinWidth = sC.width.min
			if (iMinWidth != null) minWidth += iMinWidth
		}
		minHeight += padding.bottom + padding.top
		minWidth += gap * elements.lastIndex + padding.left + padding.right
		out.height.min = minHeight
		out.width.min = minWidth
	}

	override fun layout(explicitWidth: Float?, explicitHeight: Float?, elements: List<LayoutElement>, props: HorizontalLayoutStyle, out: Bounds) {
		val padding = props.padding
		val gap = props.gap

		val childAvailableWidth: Float? = padding.reduceWidth(explicitWidth)
		val childAvailableHeight: Float? = padding.reduceHeight(explicitHeight)

		// Size inflexible elements first.
		var maxHeight = childAvailableHeight ?: 0f
		for (i in 0..elements.lastIndex) {
			val element = elements[i]
			val layoutData = element.layoutDataCast
			if (childAvailableWidth == null || layoutData?.widthPercent == null) {
				val w = layoutData?.getPreferredWidth(childAvailableWidth)
				val h = layoutData?.getPreferredHeight(childAvailableHeight)
				element.setSize(w, h)
				if (element.height > maxHeight) maxHeight = element.height
			}
		}

		// Size height flexible, but width inflexible second and measure the flexible/inflexible width.
		var inflexibleWidth = 0f
		var flexibleWidth = 0f
		for (i in 0..elements.lastIndex) {
			val element = elements[i]
			val layoutData = element.layoutDataCast
			if (childAvailableWidth == null || layoutData?.widthPercent == null) {
				if (layoutData?.heightPercent == null) {
					val w = layoutData?.getPreferredWidth(childAvailableWidth)
					val h = layoutData?.getPreferredHeight(maxHeight)
					element.setSize(w, h)
				}
				inflexibleWidth += element.width
				if (element.height > maxHeight) maxHeight = element.height
			} else {
				flexibleWidth += layoutData.widthPercent!! * childAvailableWidth
			}
			inflexibleWidth += gap
		}
		inflexibleWidth -= gap

		// Size flexible elements within the remaining space.
		if (childAvailableWidth != null) {
			val scale = if (flexibleWidth > 0) MathUtils.clamp((childAvailableWidth - inflexibleWidth) / flexibleWidth, 0f, 1f) else 1f
			for (i in 0..elements.lastIndex) {
				val element = elements[i]
				val layoutData = element.layoutDataCast
				if (layoutData?.widthPercent != null) {
					val h = layoutData.getPreferredHeight(childAvailableHeight)
					val w = scale * layoutData.widthPercent!! * childAvailableWidth
					element.setSize(w, h)
					if (element.height > maxHeight) maxHeight = element.height
				}
			}
		}

		// Position
		var x = padding.left
		if (childAvailableWidth != null && props.horizontalAlign != HAlign.LEFT) {
			val d = childAvailableWidth - (inflexibleWidth + flexibleWidth)
			if (d > 0f) {
				if (props.horizontalAlign == HAlign.RIGHT) {
					x += d
				} else if (props.horizontalAlign == HAlign.CENTER) {
					x += floor(d * 0.5f).toInt()
				}
			}
		}
		for (i in 0..elements.lastIndex) {
			val element = elements[i]
			val layoutData = element.layoutDataCast
			val y = when (layoutData?.verticalAlign ?: props.verticalAlign) {
				VAlign.TOP -> padding.top
				VAlign.MIDDLE -> (maxHeight - element.height) * 0.5f + padding.top
				VAlign.BOTTOM -> maxHeight - element.height + padding.top
				else -> throw Exception()
			}
			element.moveTo(x, y)
			x += element.width + gap
		}
		x += padding.right - gap
		out.set(x, maxHeight + padding.bottom + padding.top)
	}

	override fun createLayoutData() = HorizontalLayoutData()

}

open class HorizontalLayoutStyle : StyleBase() {

	override val type: StyleType<HorizontalLayoutStyle> = Companion

	/**
	 * The horizontal gap between elements.
	 */
	var gap by prop(5f)

	/**
	 * The Padding object with left, bottom, top, and right padding.
	 */
	var padding: PadRo by prop(Pad())

	/**
	 * The horizontal alignment of the entire row within the explicit width.
	 * If the explicit width is null, this will have no effect.
	 */
	var horizontalAlign by prop(HAlign.LEFT)

	/**
	 * The vertical alignment of each element within the measured height.
	 */
	var verticalAlign by prop(VAlign.BOTTOM)

	companion object : StyleType<HorizontalLayoutStyle>

}

class HorizontalLayoutData : BasicLayoutData() {

	/**
	 * If set, the vertical alignment for this item overrides the vertical layout's verticalAlign.
	 */
	var verticalAlign: HAlign? by bindable(null)
}

open class HorizontalLayoutContainer(owner: Owned) : LayoutContainerImpl<HorizontalLayoutStyle, HorizontalLayoutData>(owner, HorizontalLayout(), HorizontalLayoutStyle())

fun Owned.hGroup(init: ComponentInit<HorizontalLayoutContainer> = {}): HorizontalLayoutContainer {
	val horizontalGroup = HorizontalLayoutContainer(this)
	horizontalGroup.init()
	return horizontalGroup
}
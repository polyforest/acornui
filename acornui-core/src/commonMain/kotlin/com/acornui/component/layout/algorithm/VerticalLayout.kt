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

package com.acornui.component.layout.algorithm

import com.acornui.collection.sortTo
import com.acornui.component.ComponentInit
import com.acornui.component.layout.*
import com.acornui.component.style.StyleBase
import com.acornui.component.style.StyleType
import com.acornui.di.Owned
import com.acornui.math.Bounds
import com.acornui.math.MathUtils
import com.acornui.math.Pad
import com.acornui.math.PadRo
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.floor

class VerticalLayout : LayoutAlgorithm<VerticalLayoutStyle, VerticalLayoutData> {

	override val style = VerticalLayoutStyle()

	private val orderedElements = ArrayList<LayoutElement>()

	override fun calculateSizeConstraints(elements: List<LayoutElementRo>, out: SizeConstraints) {
		if (elements.isEmpty()) return
		val padding = style.padding
		val gap = style.gap

		var minWidth = 0f
		var minHeight = 0f
		for (i in 0..elements.lastIndex) {
			val element = elements[i]
			val sC = element.sizeConstraints
			val iMinWidth = sC.width.min
			if (iMinWidth != null) minWidth = maxOf(iMinWidth, minWidth)
			val iMinHeight = sC.height.min
			if (iMinHeight != null) minHeight += iMinHeight
		}
		minWidth += padding.left + padding.right
		minHeight += gap * elements.lastIndex + padding.top + padding.bottom
		out.width.min = minWidth
		out.height.min = minHeight
	}

	override fun layout(explicitWidth: Float?, explicitHeight: Float?, elements: List<LayoutElement>, out: Bounds) {
		if (elements.isEmpty()) return
		val padding = style.padding
		val gap = style.gap
		val allowRelativeSizing = style.allowRelativeSizing

		val childAvailableWidth = padding.reduceWidth(explicitWidth)
		val childAvailableHeight = padding.reduceHeight(explicitHeight)

		if (allowRelativeSizing) elements.sortTo(orderedElements, true, sizeOrderComparator)
		else orderedElements.addAll(elements)

		// Following the sizing precedence, size the children, maxing the maxWidth by the measured width if
		// allowRelativeSizing is true.
		// Size height inflexible elements first.
		var measuredW = childAvailableWidth
		var inflexibleHeight = 0f
		var flexibleHeight = 0f
		for (i in 0..orderedElements.lastIndex) {
			val element = orderedElements[i]
			val layoutData = element.layoutDataCast
			if (childAvailableHeight == null || layoutData?.heightPercent == null) {
				val w = layoutData?.getPreferredWidth(if (allowRelativeSizing) measuredW else childAvailableWidth)
				val h = layoutData?.getPreferredHeight(childAvailableHeight)
				element.setSize(w, h)
				inflexibleHeight += element.height
				if (measuredW == null || element.width > measuredW)
					measuredW = element.width
			} else {
				flexibleHeight += layoutData.heightPercent!! * childAvailableHeight
			}
			inflexibleHeight += gap
		}
		inflexibleHeight -= gap

		// Size flexible elements within the remaining space.
		if (childAvailableHeight != null) {
			val scale = if (flexibleHeight > 0) MathUtils.clamp((childAvailableHeight - inflexibleHeight) / flexibleHeight, 0f, 1f) else 1f
			for (i in 0..orderedElements.lastIndex) {
				val element = orderedElements[i]
				val layoutData = element.layoutDataCast
				if (layoutData?.heightPercent != null) {
					val w = layoutData.getPreferredWidth(if (allowRelativeSizing) measuredW else childAvailableWidth)
					val h = scale * layoutData.heightPercent!! * childAvailableHeight
					element.setSize(w, h)
					if (measuredW == null || element.width > measuredW)
						measuredW = element.width
				}
			}
		}
		if (measuredW == null)
			measuredW = 0f

		orderedElements.clear()

		// Position
		var y = padding.top
		if (childAvailableHeight != null && style.verticalAlign != VAlign.TOP) {
			val d = childAvailableHeight - (inflexibleHeight + flexibleHeight)
			if (d > 0f) {
				y += when (style.verticalAlign) {
					VAlign.TOP -> 0f
					VAlign.MIDDLE -> floor(d * 0.5f)
					VAlign.BASELINE, VAlign.BOTTOM -> d
				}
			}
		}
		var rightX = 0f
		for (i in 0..elements.lastIndex) {
			val element = elements[i]
			val layoutData = element.layoutDataCast
			val x = when (layoutData?.horizontalAlign ?: style.horizontalAlign) {
				HAlign.LEFT -> padding.left
				HAlign.CENTER -> (measuredW - element.width) * 0.5f + padding.left
				HAlign.RIGHT -> measuredW - element.width + padding.left
			}
			element.moveTo(x, y)
			y += element.height + gap
			if (element.right > rightX) {
				rightX = element.right
			}
		}
		y += padding.bottom - gap
		out.set(padding.right + rightX, y, elements.firstOrNull()?.baselineY ?: y)
	}

	override fun createLayoutData() = VerticalLayoutData()

	companion object {
		private val sizeOrderComparator = { o1: LayoutElement, o2: LayoutElement ->
			val layoutData1 = o1.layoutData as VerticalLayoutData?
			val layoutData2 = o2.layoutData as VerticalLayoutData?
			val r1 = -(layoutData1?.priority ?: 0f).compareTo(layoutData2?.priority ?: 0f)
			if (r1 == 0) {
				(layoutData1?.widthPercent == null).compareTo(layoutData2?.widthPercent == null)
			} else r1
		}
	}
}

class VerticalLayoutStyle : StyleBase() {

	override val type: StyleType<VerticalLayoutStyle> = Companion

	var gap by prop(5f)

	/**
	 * The Padding object with left, bottom, top, and right padding.
	 */
	var padding: PadRo by prop(Pad())

	/**
	 * The horizontal alignment of each element within the measured width.
	 */
	var horizontalAlign by prop(HAlign.LEFT)

	/**
	 * The vertical alignment of the entire column within the explicit height.
	 * If the explicit height is null, this will have no effect.
	 */
	var verticalAlign by prop(VAlign.TOP)

	/**
	 * If true, the actual size of an element can expand the bounds of the layout for the remaining elements, based
	 * on priority rules.
	 *
	 * Example:
	 *
	 * ```
	 * vGroup {
	 *    +rect() layout { widthPercent = 1f }   // width = if (allowRelativeSizing == true) 200f else 100f.
	 *    +rect() layout { width = 200f }
	 * } layout { width = 100f }
	 * ```
	 *
	 * @see VerticalLayoutData.priority
	 */
	var allowRelativeSizing by prop(true)

	companion object : StyleType<VerticalLayoutStyle>

}

class VerticalLayoutData : BasicLayoutData() {

	/**
	 * If set, the horizontal alignment for this item overrides the vertical layout's horizontalAlign.
	 */
	var horizontalAlign: HAlign? by bindable(null)

	/**
	 * The order of sizing precedence is as follows:
	 * - heightPercent null (inflexible height before flexible height)
	 * - priority value (higher values before lower values)
	 * - widthPercent null (inflexible width before flexible width)
	 */
	var priority: Float by bindable(0f)
}

open class VerticalLayoutContainer(owner: Owned) : ElementLayoutContainerImpl<VerticalLayoutStyle, VerticalLayoutData>(owner, VerticalLayout())

inline fun Owned.vGroup(init: ComponentInit<VerticalLayoutContainer> = {}): VerticalLayoutContainer  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val verticalGroup = VerticalLayoutContainer(this)
	verticalGroup.init()
	return verticalGroup
}

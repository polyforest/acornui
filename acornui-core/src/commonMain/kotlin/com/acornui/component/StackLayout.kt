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

package com.acornui.component

import com.acornui.collection.sortTo
import com.acornui.component.layout.*
import com.acornui.component.layout.algorithm.BasicLayoutData
import com.acornui.component.layout.algorithm.LayoutAlgorithm
import com.acornui.component.style.StyleBase
import com.acornui.component.style.StyleType
import com.acornui.di.Owned
import com.acornui.math.Bounds
import com.acornui.math.Pad
import com.acornui.math.PadRo
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.jvm.JvmName

/**
 * StackLayout places components one on top of another, allowing for percent-based sizes and alignment using
 * [StackLayoutData].
 */
class StackLayout : LayoutAlgorithm<StackLayoutStyle, StackLayoutData> {

	override val style = StackLayoutStyle()

	private val orderedElements = ArrayList<LayoutElement>()

	override fun layout(explicitWidth: Float?, explicitHeight: Float?, elements: List<LayoutElement>, out: Bounds) {
		if (elements.isEmpty()) return
		val padding = style.padding
		val allowRelativeSizing = style.allowRelativeSizing
		val childAvailableWidth = padding.reduceWidth(explicitWidth)
		val childAvailableHeight = padding.reduceHeight(explicitHeight)

		if (allowRelativeSizing) elements.sortTo(orderedElements, true, sizeOrderComparator)
		else orderedElements.addAll(elements)

		var measuredW = childAvailableWidth
		var measuredH = childAvailableHeight
		var measuredB: Float? = null
		for (i in 0..orderedElements.lastIndex) {
			val child = orderedElements[i]
			val layoutData = child.layoutDataCast
			if (allowRelativeSizing) child.setSize(layoutData?.getPreferredWidth(measuredW), layoutData?.getPreferredHeight(measuredH))
			else child.setSize(layoutData?.getPreferredWidth(childAvailableWidth), layoutData?.getPreferredHeight(childAvailableHeight))

			if (measuredW == null || child.width > measuredW)
				measuredW = child.width

			if (measuredH == null || child.height > measuredH)
				measuredH = child.height

			if (layoutData?.verticalAlign ?: style.verticalAlign == VAlign.BASELINE) {
				if (measuredB == null || child.baseline > measuredB)
					measuredB = child.baseline
			}
		}
		orderedElements.clear()

		for (i in 0..elements.lastIndex) {
			val element = elements[i]
			val layoutData = element.layoutDataCast
			val w = if (allowRelativeSizing) measuredW else explicitWidth
			val childX = padding.left + if (w == null) 0f else run {
				val remainingSpace = maxOf(0f, w - element.width)
				when (layoutData?.horizontalAlign ?: style.horizontalAlign) {
					HAlign.LEFT -> 0f
					HAlign.CENTER -> remainingSpace * 0.5f
					HAlign.RIGHT -> remainingSpace
				}
			}
			val h = if (allowRelativeSizing) measuredH else explicitHeight
			val childY = padding.top + if (h == null) 0f else run {
				val remainingSpace = maxOf(0f, h - element.height)
				when (layoutData?.verticalAlign ?: style.verticalAlign) {
					VAlign.TOP -> 0f
					VAlign.MIDDLE -> remainingSpace * 0.5f
					VAlign.BASELINE -> measuredB!! - element.baseline
					VAlign.BOTTOM -> remainingSpace
				}
			}
			element.moveTo(childX, childY)
		}
		out.set(padding.expandWidth(measuredW ?: 0f), padding.expandHeight(measuredH
				?: 0f), padding.top + if (measuredB == null) measuredH ?: 0f else measuredB)
	}

	override fun createLayoutData(): StackLayoutData = StackLayoutData()

	companion object {
		private val c = compareBy<StackLayoutData?>(
				{ -(it?.priority ?: 0f) },
				{ it?.widthPercent == null },
				{ it?.heightPercent == null }
		)

		private val sizeOrderComparator = { o1: LayoutElement, o2: LayoutElement ->
			val layoutData1 = o1.layoutData as StackLayoutData?
			val layoutData2 = o2.layoutData as StackLayoutData?
			c.compare(layoutData1, layoutData2)
		}
	}
}

open class StackLayoutData : BasicLayoutData() {

	/**
	 * If set, the vertical align of this element overrides the default of the scale layout algorithm.
	 */
	var verticalAlign: VAlign? by bindable(null)

	/**
	 * If set, the horizontal align of this element overrides the default of the scale layout algorithm.
	 */
	var horizontalAlign: HAlign? by bindable(null)

	/**
	 * The order of sizing precedence is as follows:
	 * - priority value (higher values before lower values)
	 * - widthPercent null (inflexible width before flexible width)
	 * - heightPercent null (inflexible height before flexible height)
	 */
	var priority: Float by bindable(0f)

	fun center() {
		verticalAlign = VAlign.MIDDLE
		horizontalAlign = HAlign.CENTER
	}
}

open class StackLayoutStyle : StyleBase() {

	override val type: StyleType<StackLayoutStyle> = Companion

	var padding: PadRo by prop(Pad())
	var verticalAlign by prop(VAlign.TOP)
	var horizontalAlign by prop(HAlign.LEFT)

	/**
	 * If true, the actual size of an element can expand the bounds of the layout for the remaining elements, based
	 * on priority rules.
	 *
	 * Example:
	 *
	 * ```
	 * stack {
	 *    +rect() layout { widthPercent = 1f }   // width = if (allowRelativeSizing == true) 200f else 100f.
	 *    +rect() layout { width = 200f }
	 * } layout { width = 100f }
	 * ```
	 *
	 * @see StackLayoutData.priority
	 */
	var allowRelativeSizing by prop(true)

	companion object : StyleType<StackLayoutStyle>
}

open class StackLayoutContainer<E : UiComponent>(owner: Owned) : ElementLayoutContainer<StackLayoutStyle, StackLayoutData, E>(owner, StackLayout())

@JvmName("stackT")
inline fun <E : UiComponent> Owned.stack(init: ComponentInit<StackLayoutContainer<E>> = {}): StackLayoutContainer<E> {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return StackLayoutContainer<E>(this).apply(init)
}

inline fun Owned.stack(init: ComponentInit<StackLayoutContainer<UiComponent>> = {}): StackLayoutContainer<UiComponent> {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return stack<UiComponent>(init)
}

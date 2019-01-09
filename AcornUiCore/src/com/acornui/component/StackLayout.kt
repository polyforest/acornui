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

package com.acornui.component

import com.acornui.component.layout.*
import com.acornui.component.layout.algorithm.BasicLayoutData
import com.acornui.component.layout.algorithm.LayoutAlgorithm
import com.acornui.component.style.StyleBase
import com.acornui.component.style.StyleType
import com.acornui.core.di.Owned
import com.acornui.math.Bounds
import com.acornui.math.Pad
import com.acornui.math.PadRo

/**
 * StackLayout places components one on top of another, allowing for percent-based sizes and alignment using
 * [StackLayoutData].
 */
class StackLayout : LayoutAlgorithm<StackLayoutStyle, StackLayoutData> {

	override val style = StackLayoutStyle()

	override fun calculateSizeConstraints(elements: List<LayoutElementRo>, out: SizeConstraints) {
		val padding = style.padding
		for (i in 0..elements.lastIndex) {
			out.bound(elements[i].sizeConstraints)
		}
		out.width.min = padding.expandWidth(out.width.min)
		out.height.min = padding.expandHeight(out.height.min)
	}

	override fun layout(explicitWidth: Float?, explicitHeight: Float?, elements: List<LayoutElement>, out: Bounds) {
		val padding = style.padding
		val childAvailableWidth = padding.reduceWidth(explicitWidth)
		val childAvailableHeight = padding.reduceHeight(explicitHeight)

		for (i in 0..elements.lastIndex) {
			val child = elements[i]
			val layoutData = child.layoutDataCast
			child.setSize(layoutData?.getPreferredWidth(childAvailableWidth), layoutData?.getPreferredHeight(childAvailableHeight))

			val childX = padding.left + if (explicitWidth == null) 0f else run {
				val remainingSpace = maxOf(0f, childAvailableWidth!! - child.width)
				when (layoutData?.horizontalAlign ?: style.horizontalAlign) {
					HAlign.LEFT -> 0f
					HAlign.CENTER -> remainingSpace * 0.5f
					HAlign.RIGHT -> remainingSpace
				}
			}
			val childY = padding.top + if (explicitHeight == null) 0f else run {
				val remainingSpace = maxOf(0f, childAvailableHeight!! - child.height)
				when (layoutData?.verticalAlign ?: style.verticalAlign) {
					VAlign.TOP -> 0f
					VAlign.MIDDLE -> remainingSpace * 0.5f
					VAlign.BOTTOM -> remainingSpace
				}
			}
			child.moveTo(childX, childY)
			out.ext(padding.expandWidth2(child.width), padding.expandHeight2(child.height))
		}
	}

	override fun createLayoutData(): StackLayoutData = StackLayoutData()
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

	companion object : StyleType<StackLayoutStyle>
}

open class StackLayoutContainer(owner: Owned) : LayoutElementContainerImpl<StackLayoutStyle, StackLayoutData>(owner, StackLayout())

fun Owned.stack(init: ComponentInit<StackLayoutContainer> = {}): StackLayoutContainer {
	val s = StackLayoutContainer(this)
	s.init()
	return s
}
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
import com.acornui.core.di.inject
import com.acornui.core.floor
import com.acornui.math.Bounds
import com.acornui.math.Pad
import com.acornui.math.PadRo

/**
 * StackLayout places components one on top of another, allowing for percent-based sizes and alignment using
 * [StackLayoutData].
 */
class StackLayout : LayoutAlgorithm<StackLayoutStyle, StackLayoutData> {

	override fun calculateSizeConstraints(elements: List<LayoutElementRo>, props: StackLayoutStyle, out: SizeConstraints) {
		val padding = props.padding
		for (i in 0..elements.lastIndex) {
			out.bound(elements[i].sizeConstraints)
		}
		out.width.min = padding.expandWidth(out.width.min)
		out.height.min = padding.expandHeight(out.height.min)
	}

	override fun layout(explicitWidth: Float?, explicitHeight: Float?, elements: List<LayoutElement>, props: StackLayoutStyle, out: Bounds) {
		val padding = props.padding
		val childAvailableWidth: Float? = padding.reduceWidth(explicitWidth)
		val childAvailableHeight: Float? = padding.reduceHeight(explicitHeight)

		for (i in 0..elements.lastIndex) {
			val child = elements[i]
			val layoutData = child.layoutData as StackLayoutData?
			child.setSize(layoutData?.getPreferredWidth(childAvailableWidth), layoutData?.getPreferredHeight(childAvailableHeight))
			child.moveTo(padding.left, padding.top)

			child.moveTo(padding.left, padding.top)

			if (explicitWidth != null) {
				val remainingSpace = childAvailableWidth!! - child.width
				if (remainingSpace > 0f) {
					when (layoutData?.horizontalAlign ?: props.horizontalAlign) {
						HAlign.LEFT -> {
						}
						HAlign.CENTER -> {
							val halfSpace = (remainingSpace * 0.5f).floor()
							child.x = halfSpace + padding.left
						}
						HAlign.RIGHT -> {
							child.x = remainingSpace + padding.left
						}
					}
				}
			}
			if (explicitHeight != null) {
				val remainingSpace = childAvailableHeight!! - child.height
				if (remainingSpace > 0f) {
					when (layoutData?.verticalAlign ?: props.verticalAlign) {
						VAlign.TOP -> {
						}
						VAlign.MIDDLE -> {
							val halfSpace = (remainingSpace * 0.5f).floor()
							child.y = halfSpace + padding.top
						}
						VAlign.BOTTOM -> {
							child.y = remainingSpace + padding.top
						}
					}
				}
			}

			out.ext(padding.expandWidth2(child.width), padding.expandHeight2(child.height))
		}
		println("Stack size $out")
		if (out.height == 656f) {
			println("...")
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

	override val type = Companion

	var padding: PadRo by prop(Pad())
	var verticalAlign by prop(VAlign.TOP)
	var horizontalAlign by prop(HAlign.LEFT)

	companion object : StyleType<StackLayoutStyle>
}

open class StackLayoutContainer(owner: Owned, native: NativeContainer = owner.inject(NativeContainer.FACTORY_KEY).invoke(owner)) : LayoutContainerImpl<StackLayoutStyle, StackLayoutData>(owner, StackLayout(), StackLayoutStyle(), native)

fun Owned.stack(init: ComponentInit<StackLayoutContainer> = {}): StackLayoutContainer {
	val s = StackLayoutContainer(this)
	s.init()
	return s
}
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

import com.acornui.component.layout.algorithm.LayoutDataProvider
import com.acornui.component.style.StyleBase
import com.acornui.component.style.StyleTag
import com.acornui.component.style.StyleType
import com.acornui.component.style.noSkin
import com.acornui.component.text.text
import com.acornui.di.Owned
import com.acornui.math.Bounds
import com.acornui.math.Pad
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

open class HeadingGroup(owner: Owned) : ElementContainerImpl<UiComponent>(owner), Labelable, LayoutDataProvider<StackLayoutData> {

	val style = bind(HeadingGroupStyle())

	private var background: UiComponent? = null
	private var heading: Labelable? = null
	private val contents = addChild(stack())

	init {
		styleTags.add(HeadingGroup)

		watch(style) {
			background?.dispose()
			background = it.background(this)
			addChild(0, background!!)

			heading?.dispose()
			val h = it.heading(this)
			h.label = _label
			heading = addChild(1, h)

			contents.style.padding = it.padding
		}
	}

	override fun createLayoutData(): StackLayoutData = StackLayoutData()

	private var _label = ""
	override var label: String
		get() = _label
		set(value) {
			_label = value
			heading?.label = value
		}

	override fun onElementAdded(oldIndex: Int, newIndex: Int, element: UiComponent) {
		contents.addElement(newIndex, element)
	}

	override fun onElementRemoved(index: Int, element: UiComponent) {
		contents.removeElement(element)
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val hP = style.headingPadding
		heading?.setSize(hP.reduceWidth(explicitWidth), null)
		heading?.setPosition(hP.left, hP.top)
		val headingW = hP.expandWidth2(heading?.width ?: 0f)
		val headingH = hP.expandHeight2(heading?.height ?: 0f)

		contents.setSize(explicitWidth, if (explicitHeight == null) null else explicitHeight - headingH)
		contents.setPosition(0f, headingH)
		out.set(maxOf(headingW, contents.width), headingH + contents.height)
		background?.setSize(out.width, out.height)
	}

	companion object : StyleTag

}

inline fun Owned.headingGroup(init: ComponentInit<HeadingGroup> = {}): HeadingGroup  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val f = HeadingGroup(this)
	f.init()
	return f
}

open class HeadingGroupStyle : StyleBase() {

	override val type: StyleType<HeadingGroupStyle> = HeadingGroupStyle

	/**
	 * The component to be placed as the background.
	 */
	var background by prop(noSkin)

	/**
	 * The labelable component to place at the top of the group.
	 */
	var heading by prop<Owned.() -> Labelable> { text() }

	/**
	 * The padding around the heading component.
	 */
	var headingPadding by prop(Pad(5f))

	/**
	 * The padding around the content area.
	 */
	var padding by prop(Pad(5f))

	companion object : StyleType<HeadingGroupStyle>
}

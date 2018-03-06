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

package com.acornui.component.layout

import com.acornui.collection.filterTo2
import com.acornui.component.*
import com.acornui.component.layout.algorithm.LayoutAlgorithm
import com.acornui.component.layout.algorithm.LayoutDataProvider
import com.acornui.component.style.Style
import com.acornui.core.di.Owned
import com.acornui.core.di.inject
import com.acornui.core.focus.Focusable
import com.acornui.math.Bounds

interface LayoutContainer<S, out T : LayoutData> : LayoutDataProvider<T>, ElementContainer<UiComponent> {

	val layoutAlgorithm: LayoutAlgorithm<S, T>

	val style: S

}

open class LayoutContainerImpl<S : Style, out U : LayoutData>(
		owner: Owned,
		override val layoutAlgorithm: LayoutAlgorithm<S, U>,
		style: S,
		native: NativeContainer = owner.inject(NativeContainer.FACTORY_KEY).invoke(owner)
) : ElementContainerImpl<UiComponent>(owner, native), LayoutContainer<S, U>, Focusable {

	override var focusEnabled: Boolean = false // Layout containers by default are not focusable.
	override var focusOrder: Float = 0f
	override var highlight: UiComponent? by createSlot()

	protected val elementsToLayout = ArrayList<LayoutElement>()

	final override val style: S = bind(style)
	final override fun createLayoutData(): U = layoutAlgorithm.createLayoutData()

	override fun updateSizeConstraints(out: SizeConstraints) {
		elementsToLayout.clear()
		elements.filterTo2(elementsToLayout, LayoutElement::shouldLayout)
		if (elementsToLayout.isNotEmpty())
			layoutAlgorithm.calculateSizeConstraints(elementsToLayout, style, out)
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		elementsToLayout.clear()
		elements.filterTo2(elementsToLayout, LayoutElement::shouldLayout)
		if (elementsToLayout.isNotEmpty()) {
			layoutAlgorithm.layout(explicitWidth, explicitHeight, elementsToLayout, style, out)
			if (explicitWidth != null && explicitWidth > out.width) out.width = explicitWidth
			if (explicitHeight != null && explicitHeight > out.height) out.height = explicitHeight
		}
		highlight?.setSize(out.width, out.height)
	}

}
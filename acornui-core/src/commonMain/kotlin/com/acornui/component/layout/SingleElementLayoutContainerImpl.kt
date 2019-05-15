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

package com.acornui.component.layout

import com.acornui.collection.SingleList
import com.acornui.collection.SingleListView
import com.acornui.component.*
import com.acornui.component.layout.algorithm.LayoutAlgorithm
import com.acornui.component.layout.algorithm.LayoutDataProvider
import com.acornui.component.style.Style
import com.acornui.core.di.Owned
import com.acornui.core.focus.Focusable
import com.acornui.math.Bounds

/**
 * A container that uses a [LayoutAlgorithm] to size and position its external [element].
 */
open class SingleElementLayoutContainerImpl<S : Style, out U : LayoutData>(
		owner: Owned,
		private val layoutAlgorithm: LayoutAlgorithm<S, U>
) : SingleElementContainerImpl<UiComponent>(owner), SingleElementContainer<UiComponent>, LayoutDataProvider<U>, Focusable {

	private val elements = SingleList<LayoutElement>()
	private val elementsToLayout = SingleListView(elements, LayoutElement::shouldLayout)

	val style: S = bind(layoutAlgorithm.style)
	final override fun createLayoutData(): U = layoutAlgorithm.createLayoutData()

	override fun onElementChanged(oldElement: UiComponent?, newElement: UiComponent?) {
		super.onElementChanged(oldElement, newElement)
		elements.clear()
		if (newElement != null)
			elements.add(newElement)
	}

	override fun updateSizeConstraints(out: SizeConstraints) {
		layoutAlgorithm.calculateSizeConstraints(elementsToLayout, out)
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		layoutAlgorithm.layout(explicitWidth, explicitHeight, elementsToLayout, out)
		if (explicitWidth != null && explicitWidth > out.width) out.width = explicitWidth
		if (explicitHeight != null && explicitHeight > out.height) out.height = explicitHeight
	}

}

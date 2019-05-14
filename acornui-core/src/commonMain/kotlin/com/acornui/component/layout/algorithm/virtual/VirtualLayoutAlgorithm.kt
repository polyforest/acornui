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

import com.acornui.component.layout.LayoutData
import com.acornui.component.layout.LayoutElement
import com.acornui.component.layout.LayoutElementRo
import com.acornui.component.layout.algorithm.LayoutDataProvider
import com.acornui.core.di.Owned
import com.acornui.math.Bounds

/**
 * A Virtualized layout algorithm is a type of layout that is only given a single layout element to size and position
 * at a time.
 */
interface VirtualLayoutAlgorithm<in S, out T : LayoutData> : LayoutDataProvider<T> {

	val direction: VirtualLayoutDirection

	/**
	 * Given a renderer, returns the position offset of that item.
	 *
	 * @param width The explicit width.
	 * @param height The explicit height.
	 * @param element The element for which the offset is calculated.
	 * @param index The index of the element.
	 * @param lastIndex The index of the last item.
	 * @param isReversed
	 * @param props The style parameters object.
	 * @return
	 */
	fun getOffset(width: Float, height: Float, element: LayoutElement, index: Int, lastIndex: Int, isReversed: Boolean, props: S): Float

	/**
	 * Sizes and positions the given layout element.
	 *
	 * @param explicitWidth The explicit width.
	 * @param explicitHeight The explicit height.
	 * @param element The layout element to size and position.
	 * @param currentIndex The index of the entry to lay out.
	 * @param startIndex The index of the first virtualized item.
	 * @param lastIndex The index of the last item.
	 * @param previousElement The layout element previously updated. May be null if this is the first item.
	 * @param isReversed If true, the layout is iterating in reverse order.
	 * @param props The style parameters object.
	 *
	 * @return out Returns x,y coordinates representing the measured width and height.
	 */
	fun updateLayoutEntry(explicitWidth: Float?, explicitHeight: Float?, element: LayoutElement, currentIndex: Int, startIndex: Float, lastIndex: Int, previousElement: LayoutElement?, isReversed: Boolean, props: S)

	/**
	 * Given a list of the elements laid out via [updateLayoutEntry] this calculates the measured dimensions considering
	 * whitespace such as padding.
	 */
	fun measure(explicitWidth: Float?, explicitHeight: Float?, elements: List<LayoutElement>, props: S, out: Bounds) {
		for (i in 0..elements.lastIndex) {
			val element = elements[i]
			val r = element.right
			if (r > out.width)
				out.width = r
			val b = element.bottom
			if (b > out.height)
				out.height = b
		}
	}

	/**
	 * Returns true if the layout element is in bounds.
	 */
	fun shouldShowRenderer(explicitWidth: Float?, explicitHeight: Float?, element: LayoutElement, props: S): Boolean

	/**
	 * A utility method to get the layout data automatically cast to the type it is expected to be.
	 */
	@Suppress("UNCHECKED_CAST")
	val LayoutElementRo.layoutDataCast: T?
		get() = this.layoutData as T?

}

interface ItemRendererOwner<out T : LayoutData> : Owned, LayoutDataProvider<T>

enum class VirtualLayoutDirection {
	VERTICAL,
	HORIZONTAL
}

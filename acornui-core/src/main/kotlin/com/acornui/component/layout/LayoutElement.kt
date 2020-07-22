/*
 * Copyright 2020 Poly Forest, LLC
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

import com.acornui.css.Length
import org.w3c.dom.css.CSSStyleDeclaration

/**
 * A LayoutElement is a Transformable component that can be used in layout algorithms.
 * It has features responsible for providing explicit dimensions, and returning measured dimensions.
 * @author nbilyk
 */
interface LayoutElement : Sizable, Positionable, Transformable {

	/**
	 * The left boundary x
	 */
	val left: Double
		get() = x

	/**
	 * The top boundary y
	 */
	val top: Double
		get() = y


	/**
	 * The right boundary x + width
	 */
	val right: Double
		get() = x + width

	/**
	 * The bottom boundary y + height
	 */
	val bottom: Double
		get() = y + height

}

interface Sizable {

	/**
	 * Returns the layout width of an element as a rounded double.
     *
	 * Typically, width is a measurement in pixels of the element's CSS width, including any borders, padding, and
	 * vertical scrollbars (if rendered). It does not include the width of pseudo-elements such as ::before or ::after.
	 *
	 * If the element is hidden (for example, by setting style.display on the element or one of its ancestors to
	 * "none"), then 0 is returned.
	 * @see org.w3c.dom.HTMLElement.offsetWidth
	 */
	val width: Double

	/**
	 * Returns the height of an element, including vertical padding and borders, as a rounded double.
	 *
	 * Typically, height is a measurement in pixels of the element's CSS height, including any borders, padding,
	 * and horizontal scrollbars (if rendered). It does not include the height of pseudo-elements such as ::before or
	 * ::after. For the document body object, the measurement includes total linear content height instead of the
	 * element's CSS height. Floated elements extending below other linear content are ignored.
	 *
	 * If the element is hidden (for example, by setting style.display on the element or one of its ancestors to
	 * "none"), then 0 is returned.
	 * @see org.w3c.dom.HTMLElement.offsetHeight
	 */
	val height: Double

	/**
	 * Sets the explicit dimensions of this component.
	 * @param width The explicit width. This will correspond to [CSSStyleDeclaration.width].
	 * @param height The explicit height. This will correspond to [CSSStyleDeclaration.height].
	 */
	fun size(width: Length?, height: Length?)

	/**
	 * Sets the explicit dimensions of this component in pixels.
	 * @see [com.acornui.css.px]
	 */
	fun size(width: Double?, height: Double?)

	/**
	 * Sets the explicit width for this component. (A null value represents using the measured width)
	 */
	fun width(value: Length?)

	/**
	 * Sets the explicit height for this component. (A null value represents using the measured height)
	 */
	fun height(value: Length?)

}
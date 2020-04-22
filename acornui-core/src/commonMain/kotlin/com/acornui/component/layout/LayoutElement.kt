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

import com.acornui.math.*
import com.acornui.math.MathUtils.clamp

/**
 * An element with a size, position, 3d transformation, and minimum dimensions.
 */
interface LayoutElementRo : BasicLayoutElementRo, TransformableRo {

	/**
	 * The minimum width this element can be.
	 */
	val minWidth: Float

	/**
	 * The minimum height this element can be.
	 */
	val minHeight: Float

	/**
	 * The maximum width this element can be.
	 */
	val maxWidth: Float

	/**
	 * The maximum height this element can be.
	 */
	val maxHeight: Float
}

fun LayoutElementRo.clampWidth(value: Float?): Float? {
	return clamp(value, minWidth, maxWidth)
}

fun LayoutElementRo.clampHeight(value: Float?): Float? {
	return clamp(value, minHeight, maxHeight)
}

/**
 * A LayoutElement is a Transformable component that can be used in layout algorithms.
 * It has features responsible for providing explicit dimensions, and returning measured dimensions.
 * @author nbilyk
 */
interface LayoutElement : LayoutElementRo, BasicLayoutElement, Transformable {

	override var minWidth: Float

	override var minHeight: Float

	override var maxWidth: Float

	override var maxHeight: Float
}

/**
 * An element with a size and position.
 */
interface BasicLayoutElementRo : SizableRo, PositionableRo {

	/**
	 * If this is false, this layout element will not be included in layout algorithms.
	 */
	val shouldLayout: Boolean

	/**
	 * The left boundary (x + bounds.left)
	 */
	val left: Float
		get() = x + bounds.left

	/**
	 * The top boundary (y + bounds.top)
	 */
	val top: Float
		get() = y + bounds.top


	/**
	 * The right boundary (x + bounds.right)
	 */
	val right: Float
		get() = x + bounds.right

	/**
	 * The bottom boundary (y + bounds.bottom)
	 */
	val bottom: Float
		get() = y + bounds.bottom

	/**
	 * The y value representing the baseline + y position.
	 */
	val baselineY: Float
		get() = y + bounds.baselineY

	/**
	 * The layout data to be used in layout algorithms.
	 * Most layout containers have a special layout method that statically types the type of
	 * layout data that a component should have.
	 */
	val layoutData: LayoutData?
}

interface BasicLayoutElement : BasicLayoutElementRo, Sizable, Positionable {

	/**
	 * The layout data to be used in layout algorithms.
	 * Most layout containers have a special layout method that statically types the type of
	 * layout data that a component should have.
	 */
	override var layoutData: LayoutData?
}

interface SizableRo {

	/**
	 * Returns the actual, untransformed width.
	 * If layout is invalid, this will invoke a layout validation.
	 * This is the same as `bounds.width`
	 */
	val width: Float
		get() = bounds.width

	/**
	 * Returns the actual, untransformed height.
	 * If layout is invalid, this will invoke a layout validation.
	 * This is the same as `bounds.height`
	 */
	val height: Float
		get() = bounds.height

	/**
	 * The y position representing the baseline of the first line of text.
	 */
	val baseline: Float
		get() = bounds.baseline

	/**
	 * The height below the baseline.
	 */
	val descender: Float
		get() = height - baseline

	/**
	 * The actual bounds of this component.
	 */
	val bounds: BoundsRo

	/**
	 * The explicit width, as set by `width(value)`
	 * Typically one would use [width] in order to retrieve the actual width.
	 */
	val explicitWidth: Float?

	/**
	 * The explicit height, as set by `height(value)`
	 * Typically one would use [height] in order to retrieve actual height.
	 */
	val explicitHeight: Float?

}

interface Sizable : SizableRo {

	/**
	 * Sets the explicit dimensions of this component.
	 *
	 * @param width The explicit width for the component. Use null to use the natural measured width.
	 * @param height The explicit height for the component. Use null to use the natural measured height.
	 */
	fun size(width: Float?, height: Float?)

	@Deprecated("use size", ReplaceWith("size(width, height)"))
	fun setSize(width: Float?, height: Float?) = size(width, height)

	/**
	 * Sets the explicit width for this layout element. (A null value represents using the measured width)
	 */
	fun width(value: Float?) = size(width = value, height = explicitHeight)


	/**
	 * Sets the explicit height for this layout element. (A null value represents using the measured height)
	 */
	fun height(value: Float?) = size(width = explicitWidth, height = value)

}

/**
 * @see LayoutElement.size
 */
fun LayoutElement.size(bounds: BoundsRo) = size(bounds.width, bounds.height)

@Deprecated("use size", ReplaceWith("size(bounds)"))
fun LayoutElement.setSize(bounds: BoundsRo) = size(bounds)

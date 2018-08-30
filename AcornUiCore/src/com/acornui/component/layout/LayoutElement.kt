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

import com.acornui.core.graphics.CameraRo
import com.acornui.math.*

interface LayoutElementRo : BasicLayoutElementRo, TransformableRo {

	/**
	 * Returns true if visible and the includeInLayout flag is true. If this is false, this layout element will not
	 * be included in layout algorithms.
	 */
	val shouldLayout: Boolean

	/**
	 * Given a canvas position, casts a ray in the direction of the camera, and returns true if that ray intersects
	 * with this component. This will always return false if this element is not active (on the stage)
	 * @param canvasX
	 * @param canvasY
	 */
	fun containsCanvasPoint(canvasX: Float, canvasY: Float): Boolean

	/**
	 * Returns true if this primitive intersects with the provided ray (in world coordinates)
	 * If there was an intersection, the intersection vector will be set to the intersection point.
	 *
	 * @param globalRay The ray (in world coordinates) to cast.
	 *
	 * @return Returns true if the ray intersects with the bounding box of this layout element.
	 */
	fun intersectsGlobalRay(globalRay: RayRo, intersection: Vector3): Boolean

	/**
	 * Returns the measured size constraints, bound by the explicit size constraints.
	 */
	val sizeConstraints: SizeConstraintsRo

	/**
	 * Returns the explicit size constraints.
	 */
	val explicitSizeConstraints: SizeConstraintsRo

	/**
	 * Returns the measured minimum width.
	 */
	val minWidth: Float?

	/**
	 * Returns the measured minimum height.
	 */
	val minHeight: Float?

	/**
	 * Returns the measured maximum width.
	 */
	val maxWidth: Float?

	/**
	 * Returns the maximum measured height.
	 */
	val maxHeight: Float?
}

private val tmpVec = Vector3()

/**
 * Returns true if this primitive intersects with the provided ray (in world coordinates)
 *
 * @return Returns true if the ray intersects with the bounding box of this layout element.
 */
fun LayoutElementRo.intersectsGlobalRay(globalRay: RayRo): Boolean = intersectsGlobalRay(globalRay, tmpVec)

fun LayoutElementRo.clampWidth(value: Float?): Float? {
	return sizeConstraints.width.clamp(value)
}

fun LayoutElementRo.clampHeight(value: Float?): Float? {
	return sizeConstraints.height.clamp(value)
}

private val a = Vector3()
private val b = Vector3()
private val c = Vector3()

/**
 * Returns true if any part of the element's bounding rectangle can be seen by this camera.
 */
fun CameraRo.intersects(element: LayoutElementRo): Boolean {
	return combined.intersects(element)
}

private fun Matrix4Ro.intersects(element: LayoutElementRo): Boolean {
	prj(element.localToGlobal(a.set(0f, 0f, 0f)))
	if (checkPoint(a)) return true
	prj(element.localToGlobal(b.set(element.width, 0f, 0f)))
	if (checkPoint(b)) return true
	prj(element.localToGlobal(c.set(element.width, element.height, 0f)))
	if (checkPoint(c)) return true
	if (checkTriangle(a, b, c)) return true
	prj(element.localToGlobal(b.set(0f, element.height, 0f)))
	if (checkPoint(b)) return true
	return checkTriangle(a, b, c)
}

private fun checkTriangle(a: Vector3Ro, b: Vector3Ro, c: Vector3Ro): Boolean {
	return checkSegment(a, b) || checkSegment(b, c) || checkSegment(a, c)
}

private fun checkSegment(a: Vector3Ro, b: Vector3Ro): Boolean {
	return (checkValue(a.x) || checkValue(b.x) || crosses(a.x, b.x)) &&
			(checkValue(a.y) || checkValue(b.y) || crosses(a.y, b.y)) &&
			(checkValue(a.z) || checkValue(b.z) || crosses(a.z, b.z))
}

private fun checkPoint(point: Vector3Ro): Boolean {
	return checkValue(point.x) && checkValue(point.y) && checkValue(point.z)
}

private fun crosses(a: Float, b: Float): Boolean {
	return (a <= -1f && b > -1f) || (a >= 1f && b < 1f)
}

private fun checkValue(x: Float): Boolean {
	return x > -1f && x < 1f
}

/**
 * A LayoutElement is a Transformable component that can be used in layout algorithms.
 * It has features responsible for providing explicit dimensions, and returning measured dimensions.
 * @author nbilyk
 */
interface LayoutElement : LayoutElementRo, BasicLayoutElement, Transformable {

	/**
	 * Sets the explicit minimum width.
	 */
	fun minWidth(value: Float?)

	/**
	 * Sets the explicit minimum height.
	 */
	fun minHeight(value: Float?)

	/**
	 * Sets the maximum measured width.
	 */
	fun maxWidth(value: Float?)

	/**
	 * Sets the maximum measured height.
	 */
	fun maxHeight(value: Float?)

}

interface BasicLayoutElementRo : SizableRo, PositionableRo {

	val right: Float

	val bottom: Float

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
	 * Returns the measured width.
	 * If layout is invalid, this will invoke a layout validation.
	 * This is the same as `bounds.width`
	 */
	val width: Float

	/**
	 * Returns the measured height.
	 * If layout is invalid, this will invoke a layout validation.
	 * This is the same as `bounds.height`
	 */
	val height: Float

	/**
	 * The measured bounds of this component.
	 */
	val bounds: BoundsRo

	/**
	 * The explicit width, as set by width(value)
	 * Typically one would use [width] in order to retrieve the explicit or measured width.
	 */
	val explicitWidth: Float?

	/**
	 * The explicit height, as set by height(value)
	 * Typically one would use [height] in order to retrieve the explicit or measured height.
	 */
	val explicitHeight: Float?
}

interface Sizable : SizableRo {

	/**
	 * Does the same thing as setting [width] and [height] individually, but may be more efficient depending on
	 * implementation.
	 * @param width The explicit width for the component. Use null to use the natural measured width.
	 * @param height The explicit height for the component. Use null to use the natural measured height.
	 */
	fun setSize(width: Float?, height: Float?)

	/**
	 * Sets the explicit width for this layout element. (A null value represents using the measured width)
	 */
	fun width(value: Float?)

	/**
	 * Sets the explicit height for this layout element. (A null value represents using the measured height)
	 */
	fun height(value: Float?)

}

fun LayoutElement.setSize(bounds: BoundsRo) = setSize(bounds.width, bounds.height)
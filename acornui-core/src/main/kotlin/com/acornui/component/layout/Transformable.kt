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

/**
 * The API for reading and modifying a component's 3d transformation.
 * @author nbilyk
 */
interface Transformable : Positionable {

	/**
	 * If set, will supply the component's transformation.
	 */
	var transform: Transform?

	/**
	 * This component's origin, upon which transformations are centered.
	 */
	var transformOrigin: TransformOrigin?
}

/**
 * Elements with explicitly set positions should have their
 */
interface Positionable {

	/**
	 * The calculated x position as a rounded double.
	 */
	val x: Double

	/**
	 * The calculated y position as a rounded double.
	 */
	val y: Double

	/**
	 * Sets the x position.
	 * @see org.w3c.dom.css.CSSStyleDeclaration.left
	 */
	fun x(value: Length?)

	/**
	 * Sets the x position, in pixels.
	 */
	fun x(value: Double?)

	/**
	 * Sets the y position.
	 * @see org.w3c.dom.css.CSSStyleDeclaration.top
	 */
	fun y(value: Length?)

	/**
	 * Sets the y position, in pixels.
	 */
	fun y(value: Double?)

	/**
	 * Sets the position.
	 */
	fun position(x: Length?, y: Length?)

	/**
	 * Sets the position as [Double.px] values.
	 */
	fun position(x: Double, y: Double)
}


inline class Transform(private val value: String) {
	override fun toString(): String = value

	/**
	 * Concatenates two transforms into one.
	 * E.g.
	 * scale(2.0) + rotate(45.deg)
	 */
	operator fun plus(other: Transform) : Transform {
		return Transform("$value $other")
	}
}

inline class TransformOrigin(private val value: String) {
	override fun toString(): String = value
}

fun String.toTransformOrigin(): TransformOrigin? =
	if (isEmpty()) null else TransformOrigin(this)

fun origin(x: Length, y: Length): TransformOrigin =
	TransformOrigin("$x $y")

fun origin(x: Length, y: Length, z: Length): TransformOrigin =
	TransformOrigin("$x $y $z")

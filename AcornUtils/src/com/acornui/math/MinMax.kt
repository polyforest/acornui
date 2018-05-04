/*
 * Copyright 2018 Nicholas Bilyk
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

package com.acornui.math

/**
 * A MinMax object represents a minimum and maximum cartesian point.
 */
interface MinMaxRo {

	val xMin: Float
	val xMax: Float
	val yMin: Float
	val yMax: Float
	val width: Float
	val height: Float
	fun isEmpty(): Boolean
	fun isNotEmpty(): Boolean

	fun intersects(other: MinMaxRo): Boolean {
		return (xMax > other.xMin && yMax > other.yMin && xMin < other.xMax && yMin < other.yMax)
	}

	fun contains(x: Float, y: Float): Boolean {
		return x > xMin && y > yMin && x < xMax && y < yMax
	}

	companion object {

		/**
		 * Infinitely negative size.
		 */
		val NEGATIVE_INFINITY: MinMaxRo = MinMax()

		/**
		 * Infinitely positive size.
		 */
		val POSITIVE_INFINITY: MinMaxRo = MinMax(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY)
	}
}

data class MinMax(
		override var xMin: Float = Float.POSITIVE_INFINITY,
		override var xMax: Float = Float.NEGATIVE_INFINITY,
		override var yMin: Float = Float.POSITIVE_INFINITY,
		override var yMax: Float = Float.NEGATIVE_INFINITY
) : MinMaxRo {

	fun inf() {
		xMin = Float.POSITIVE_INFINITY
		xMax = Float.NEGATIVE_INFINITY
		yMin = Float.POSITIVE_INFINITY
		yMax = Float.NEGATIVE_INFINITY
	}

	/**
	 * Expands this value to include the given point.
	 */
	fun ext(x: Float, y: Float) {
		if (x < xMin) xMin = x
		if (x > xMax) xMax = x
		if (y < yMin) yMin = y
		if (y > yMax) yMax = y
	}

	override fun isEmpty(): Boolean {
		return xMax <= xMin || yMax <= yMin
	}

	override fun isNotEmpty(): Boolean = !isEmpty()

	/**
	 * Scales this value by the given scalars.
	 */
	fun scl(x: Float, y: Float) {
		xMin *= x
		xMax *= x
		yMin *= y
		yMax *= y
	}

	/**
	 * Increases this value by the given deltas.
	 */
	fun inflate(left: Float, top: Float, right: Float, bottom: Float) {
		xMin -= left
		xMax += right
		yMin -= top
		yMax += bottom
	}

	override val width: Float
		get() = xMax - xMin

	override val height: Float
		get() = yMax - yMin

	fun set(other: MinMaxRo): MinMax {
		xMin = other.xMin
		xMax = other.xMax
		yMin = other.yMin
		yMax = other.yMax
		return this
	}

	fun set(xMin: Float, yMin: Float, xMax: Float, yMax: Float): MinMax {
		this.xMin = xMin
		this.yMin = yMin
		this.xMax = xMax
		this.yMax = yMax
		return this
	}

	/**
	 * Clamps a 2d vector to these bounds.
	 */
	fun clampPoint(value: Vector2) {
		if (value.x < xMin) value.x = xMin
		if (value.y < yMin) value.y = yMin
		if (value.x > xMax) value.x = xMax
		if (value.y > yMax) value.y = yMax
	}

}
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

	fun isEmpty(): Boolean {
		return xMax <= xMin || yMax <= yMin
	}

	fun isNotEmpty(): Boolean = !isEmpty()

	fun intersects(other: MinMaxRo): Boolean {
		return (xMax > other.xMin && yMax > other.yMin && xMin < other.xMax && yMin < other.yMax)
	}

	fun intersects(other: RectangleRo): Boolean {
		return (xMax > other.left && yMax > other.top && xMin < other.right && yMin < other.bottom)
	}

	fun contains(x: Float, y: Float): Boolean {
		return x > xMin && y > yMin && x < xMax && y < yMax
	}

	/**
	 * Clamps a 2d vector to these bounds.
	 */
	fun clampPoint(value: Vector2): Vector2 {
		if (value.x < xMin) value.x = xMin
		if (value.y < yMin) value.y = yMin
		if (value.x > xMax) value.x = xMax
		if (value.y > yMax) value.y = yMax
		return value
	}

	fun copy(xMin: Float = this.xMin,
			 xMax: Float = this.xMax,
			 yMin: Float = this.yMin,
			 yMax: Float = this.yMax): MinMax {
		return MinMax(xMin, yMin, xMax, yMax)
	}

	companion object {

		/**
		 * Infinitely negative size.
		 */
		val NEGATIVE_INFINITY: MinMaxRo = MinMax()

		/**
		 * Infinitely positive size.
		 */
		val POSITIVE_INFINITY: MinMaxRo = MinMax(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
	}
}

/**
 * A two dimensional minimum maximum range.
 */
class MinMax(
		override var xMin: Float = Float.POSITIVE_INFINITY,
		override var yMin: Float = Float.POSITIVE_INFINITY,
		override var xMax: Float = Float.NEGATIVE_INFINITY,
		override var yMax: Float = Float.NEGATIVE_INFINITY
) : MinMaxRo {

	fun inf(): MinMax {
		xMin = Float.POSITIVE_INFINITY
		yMin = Float.POSITIVE_INFINITY
		xMax = Float.NEGATIVE_INFINITY
		yMax = Float.NEGATIVE_INFINITY
		return this
	}

	/**
	 * Expands this value to include the given point.
	 */
	fun ext(x: Float, y: Float): MinMax {
		if (x < xMin) xMin = x
		if (y < yMin) yMin = y
		if (x > xMax) xMax = x
		if (y > yMax) yMax = y
		return this
	}

	/**
	 * Scales this value by the given scalars.
	 */
	fun scl(x: Float, y: Float): MinMax {
		xMin *= x
		yMin *= y
		xMax *= x
		yMax *= y
		return this
	}

	/**
	 * Increases this value by the given deltas.
	 */
	fun inflate(left: Float, top: Float, right: Float, bottom: Float): MinMax {
		xMin -= left
		yMin -= top
		xMax += right
		yMax += bottom
		return this
	}

	/**
	 * Increases this value by the given padding values.
	 */
	fun inflate(pad: PadRo): MinMax = inflate(pad.left, pad.top, pad.right, pad.bottom)

	override val width: Float
		get() = xMax - xMin

	override val height: Float
		get() = yMax - yMin

	fun set(other: MinMaxRo): MinMax {
		xMin = other.xMin
		yMin = other.yMin
		xMax = other.xMax
		yMax = other.yMax
		return this
	}

	/**
	 * Sets this region to match the bounds of the rectangle.
	 */
	fun set(rectangle: RectangleRo): MinMax {
		return set(rectangle.x, rectangle.y, rectangle.right, rectangle.bottom)
	}

	fun set(xMin: Float, yMin: Float, xMax: Float, yMax: Float): MinMax {
		this.xMin = xMin
		this.yMin = yMin
		this.xMax = xMax
		this.yMax = yMax
		return this
	}

	/**
	 * Sets this value to be the intersection of this and [other].
	 */
	fun intersection(other: MinMaxRo): MinMax {
		xMin = maxOf(xMin, other.xMin)
		yMin = maxOf(yMin, other.yMin)
		xMax = minOf(xMax, other.xMax)
		yMax = minOf(yMax, other.yMax)
		return this
	}

	/**
	 * Expands this value to include the given [MinMaxRo].
	 */
	fun ext(other: MinMaxRo): MinMax {
		ext(other.xMin, other.yMin)
		ext(other.xMax, other.yMax)
		return this
	}

	/**
	 * Translate this region by the given deltas.
	 */
	fun translate(xD: Float, yD: Float): MinMax {
		xMin += xD
		xMax += xD
		yMin += yD
		yMax += yD
		return this
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true

		other as MinMaxRo

		if (xMin != other.xMin) return false
		if (yMin != other.yMin) return false
		if (xMax != other.xMax) return false
		if (yMax != other.yMax) return false

		return true
	}

	override fun hashCode(): Int {
		var result = xMin.hashCode()
		result = 31 * result + yMin.hashCode()
		result = 31 * result + xMax.hashCode()
		result = 31 * result + yMax.hashCode()
		return result
	}

	override fun toString(): String {
		return "MinMax(xMin=$xMin, yMin=$yMin, xMax=$xMax, yMax=$yMax)"
	}

}

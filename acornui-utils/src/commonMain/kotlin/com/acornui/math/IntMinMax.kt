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

import com.acornui.recycle.Clearable

/**
 * A IntMinMax object represents a minimum and maximum cartesian point.
 */
interface IntMinMaxRo {

	val xMin: Int
	val xMax: Int
	val yMin: Int
	val yMax: Int
	val width: Int
	val height: Int

	fun isEmpty(): Boolean {
		return xMax <= xMin || yMax <= yMin
	}

	fun isNotEmpty(): Boolean = !isEmpty()

	fun intersects(other: IntMinMaxRo): Boolean {
		return (xMax > other.xMin && yMax > other.yMin && xMin < other.xMax && yMin < other.yMax)
	}

	fun intersects(other: RectangleRo): Boolean {
		return (xMax > other.left && yMax > other.top && xMin < other.right && yMin < other.bottom)
	}

	fun contains(x: Int, y: Int): Boolean {
		return x > xMin && y > yMin && x < xMax && y < yMax
	}

//	/**
//	 * Clamps a 2d vector to these bounds.
//	 */
//	fun clampPoint(value: Vector2): Vector2 {
//		if (value.x < xMin) value.x = xMin
//		if (value.y < yMin) value.y = yMin
//		if (value.x > xMax) value.x = xMax
//		if (value.y > yMax) value.y = yMax
//		return value
//	}

	fun copy(xMin: Int = this.xMin,
			 xMax: Int = this.xMax,
			 yMin: Int = this.yMin,
			 yMax: Int = this.yMax): IntMinMax {
		return IntMinMax(xMin, yMin, xMax, yMax)
	}

	companion object {

		/**
		 * Maximum negative size.
		 */
		val MIN: IntMinMaxRo = IntMinMax()

		/**
		 * Maximum positive size.
		 */
		val MAX: IntMinMaxRo = IntMinMax(Int.MIN_VALUE, Int.MIN_VALUE, Int.MAX_VALUE, Int.MAX_VALUE)
	}
}

/**
 * A two dimensional minimum maximum range.
 */
class IntMinMax(
		override var xMin: Int = Int.MAX_VALUE,
		override var yMin: Int = Int.MAX_VALUE,
		override var xMax: Int = Int.MIN_VALUE,
		override var yMax: Int = Int.MIN_VALUE
) : IntMinMaxRo, Clearable {

	override fun clear() {
		xMin = Int.MAX_VALUE
		yMin = Int.MAX_VALUE
		xMax = Int.MIN_VALUE
		yMax = Int.MIN_VALUE
	}

	/**
	 * Expands this value to include the given point.
	 */
	fun ext(x: Int, y: Int): IntMinMax {
		if (x < xMin) xMin = x
		if (y < yMin) yMin = y
		if (x > xMax) xMax = x
		if (y > yMax) yMax = y
		return this
	}

	/**
	 * Scales this value by the given scalars.
	 */
	fun scl(x: Int, y: Int): IntMinMax {
		xMin *= x
		yMin *= y
		xMax *= x
		yMax *= y
		return this
	}

	/**
	 * Increases this value by the given deltas.
	 */
	fun inflate(left: Int, top: Int, right: Int, bottom: Int): IntMinMax {
		xMin -= left
		yMin -= top
		xMax += right
		yMax += bottom
		return this
	}

	/**
	 * Increases this value by the given padding values.
	 */
	fun inflate(pad: IntPadRo): IntMinMax = inflate(pad.left, pad.top, pad.right, pad.bottom)

	override val width: Int
		get() = xMax - xMin

	override val height: Int
		get() = yMax - yMin

	fun set(other: IntMinMaxRo): IntMinMax {
		xMin = other.xMin
		yMin = other.yMin
		xMax = other.xMax
		yMax = other.yMax
		return this
	}

	/**
	 * Sets this region to match the bounds of the rectangle.
	 */
	fun set(rectangle: IntRectangleRo): IntMinMax {
		return set(rectangle.x, rectangle.y, rectangle.right, rectangle.bottom)
	}

	fun set(xMin: Int, yMin: Int, xMax: Int, yMax: Int): IntMinMax {
		this.xMin = xMin
		this.yMin = yMin
		this.xMax = xMax
		this.yMax = yMax
		return this
	}

	/**
	 * Sets this value to be the intersection of this and [other].
	 */
	fun intersection(other: IntMinMaxRo): IntMinMax {
		xMin = maxOf(xMin, other.xMin)
		yMin = maxOf(yMin, other.yMin)
		xMax = minOf(xMax, other.xMax)
		yMax = minOf(yMax, other.yMax)
		return this
	}

	/**
	 * Expands this value to include the given [IntMinMaxRo].
	 */
	fun ext(other: IntMinMaxRo): IntMinMax {
		ext(other.xMin, other.yMin)
		ext(other.xMax, other.yMax)
		return this
	}

	/**
	 * Translate this region by the given deltas.
	 */
	fun translate(xD: Int, yD: Int): IntMinMax {
		xMin += xD
		xMax += xD
		yMin += yD
		yMax += yD
		return this
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true

		other as IntMinMaxRo

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
		return "IntMinMax(xMin=$xMin, yMin=$yMin, xMax=$xMax, yMax=$yMax)"
	}

}

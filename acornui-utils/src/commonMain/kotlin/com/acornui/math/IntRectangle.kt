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

import com.acornui.ceilInt
import com.acornui.recycle.Clearable
import com.acornui.recycle.ClearableObjectPool
import kotlinx.serialization.*
import kotlinx.serialization.Serializer
import kotlinx.serialization.internal.ArrayListSerializer
import kotlinx.serialization.internal.IntSerializer
import kotlinx.serialization.internal.StringDescriptor
import kotlin.math.floor

/**
 * The read-only interface to [IntRectangle].
 */
@Serializable(with = IntRectangleSerializer::class)
interface IntRectangleRo {

	val x: Int
	val y: Int
	val width: Int
	val height: Int

	val left: Int
		get() = x

	val top: Int
		get() = y

	val right: Int
		get() = x + width

	val bottom: Int
		get() = y + height

	fun isEmpty(): Boolean = width <= 0 || height <= 0

	fun isNotEmpty(): Boolean = !isEmpty()

	/**
	 * @param x point x coordinate
	 * @param y point y coordinate
	 * @return whether the point is contained in the rectangle
	 */
	fun contains(x: Int, y: Int): Boolean {
		return x >= this.x && y >= this.y && x < this.right && y < this.bottom
	}

	/**
	 * @param r the other {@link Rectangle}
	 * @return whether this rectangle overlaps the other rectangle.
	 */
	fun intersects(r: IntRectangleRo): Boolean {
		return intersects(r.x, r.y, r.width, r.height)
	}

	/**
	 * Returns true if the provided region intersects with this rectangle.
	 * (Matching edges do not count as intersection)
	 */
	fun intersects(x: Int, y: Int, width: Int, height: Int): Boolean {
		return x + width > this.x && x < this.right && y + height > this.y && y < this.bottom
	}

	/**
	 * Returns true if the provided region intersects with this rectangle. Additionally sets the [out] Rectangle
	 * as the region of intersection (only if there was an intersection).
	 * (Matching edges do not count as intersection)
	 * @return Returns true if there was an area of intersection.
	 */
	fun intersects(x: Int, y: Int, width: Int, height: Int, out: IntRectangle): Boolean {
		val right = x + width
		val bottom = y + height
		return if (this.x < right && this.right > x && this.y < bottom && this.bottom > y) {
			val iLeft = maxOf(x, this.x)
			val iTop = maxOf(y, this.y)
			val iRight = minOf(right, this.right)
			val iBottom = minOf(bottom, this.bottom)
			out.set(x = iLeft, y = iTop, width = iRight - iLeft, height = iBottom - iTop)
			true
		} else {
			false
		}
	}

	fun intersects(r: IntRectangleRo, out: IntRectangle): Boolean = intersects(r.x, r.y, r.width, r.height, out)

	/**
	 * Returns true if the given rectangle is completely contained within this rectangle.
	 * @param other the other rectangle to check.
	 */
	fun contains(other: IntRectangleRo): Boolean {
		return other.x >= this.x && other.right <= this.right && other.y >= this.y && other.bottom <= this.bottom
	}

	/**
	 * Returns true if this rectangle's bounds can contain the given dimensions
	 * Note: x, y coordinates are not considered.
	 */
	fun canContain(width: Int, height: Int): Boolean {
		return this.width >= width && this.height >= height
	}

	val area: Int
		get() = this.width * this.height

	val perimeter: Int
		get() = 2 * (this.width + this.height)

	fun copy(x: Int = this.x, y: Int = this.y, width: Int = this.width, height: Int = this.height): IntRectangle {
		return IntRectangle(x, y, width, height)
	}

	fun reduce(padding: IntPadRo): IntRectangle = reduce(padding.left, padding.top, padding.right, padding.bottom)

	/**
	 * Clips the sides of this rectangle by the given amounts, returning a new rectangle.
	 */
	fun reduce(left: Int, top: Int, right: Int, bottom: Int): IntRectangle {
		return IntRectangle(x + left, y + left, width - left - right, height - top - bottom)
	}
}

/**
 * An x,y,width,height set of integers.
 */
@Serializable(with = IntRectangleSerializer::class)
class IntRectangle(
		override var x: Int = 0,
		override var y: Int = 0,
		override var width: Int = 0,
		override var height: Int = 0
) : IntRectangleRo, Clearable {

	/**
	 * @param x bottom-left x coordinate
	 * @param y bottom-left y coordinate
	 * @param width width
	 * @param height height
	 * @return this rectangle for chaining
	 */
	fun set(x: Int, y: Int, width: Int, height: Int): IntRectangle {
		this.x = x
		this.y = y
		this.width = width
		this.height = height

		return this
	}

	/**
	 * Sets this Rectangle to 0,0,0,0
	 */
	override fun clear() {
		x = 0
		y = 0
		width = 0
		height = 0
	}

	/**
	 * Sets the x and y-coordinates of the bottom left corner
	 * @param x The x-coordinate
	 * @param y The y-coordinate
	 * @return this rectangle for chaining
	 */
	fun setPosition(x: Int, y: Int): IntRectangle {
		this.x = x
		this.y = y
		return this
	}

	/**
	 * Sets the width and height of this rectangle
	 * @param width The width
	 * @param height The height
	 * @return this rectangle for chaining
	 */
	fun setSize(width: Int, height: Int): IntRectangle {
		this.width = width
		this.height = height
		return this
	}

	/**
	 * Sets the values of the given rectangle to this rectangle.
	 * @param rect the other rectangle
	 * @return this rectangle for chaining
	 */
	fun set(rect: IntRectangleRo): IntRectangle {
		this.x = rect.x
		this.y = rect.y
		this.width = rect.width
		this.height = rect.height
		return this
	}

	/**
	 * Returns true if this rectangle's bounds can contain the given dimensions
	 * Note: x, y coordinates are not considered.
	 */
	override fun canContain(width: Int, height: Int): Boolean {
		return this.width >= width && this.height >= height
	}

	fun inflate(left: Int, top: Int, right: Int, bottom: Int) {
		x -= left
		width += left + right
		y -= top
		height += top + bottom
	}

	/**
	 * Extends this rectangle to include the given coordinates.
	 */
	fun ext(x2: Int, y2: Int) {
		if (x2 > x + width) width = x2 - x
		if (x2 < x) x = x2
		if (y2 > y + height) height = y2 - y
		if (y2 < y) y = y2
	}

	/**
	 * Extends this rectangle by the other rectangle. The rectangle should not have negative width or negative height.
	 * @param rect the other rectangle
	 * @return this rectangle for chaining
	 */
	fun ext(rect: IntRectangleRo): IntRectangle {
		val minX = minOf(x, rect.x)
		val maxX = maxOf(x + width, rect.x + rect.width)
		x = minX
		width = maxX - minX

		val minY = minOf(y, rect.y)
		val maxY = maxOf(y + height, rect.y + rect.height)
		y = minY
		height = maxY - minY

		return this
	}

	fun scl(scalar: Int) {
		x *= scalar
		y *= scalar
		width *= scalar
		height *= scalar
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		other as IntRectangleRo
		if (x != other.x) return false
		if (y != other.y) return false
		if (width != other.width) return false
		if (height != other.height) return false
		return true
	}

	override fun hashCode(): Int {
		var result = x
		result = 31 * result + y
		result = 31 * result + width
		result = 31 * result + height
		return result
	}

	override fun toString(): String {
		return "IntRectangle(x=$x, y=$y, width=$width, height=$height)"
	}


	companion object {
		private val pool = ClearableObjectPool { IntRectangle() }

		fun obtain(): IntRectangle = pool.obtain()
		fun free(obj: IntRectangle) = pool.free(obj)

		val EMPTY: IntRectangleRo = IntRectangle()
	}
}

@Serializer(forClass = IntRectangle::class)
object IntRectangleSerializer : KSerializer<IntRectangle> {

	override val descriptor: SerialDescriptor =
			StringDescriptor.withName("IntRectangle")

	override fun serialize(encoder: Encoder, obj: IntRectangle) {
		encoder.encodeSerializableValue(ArrayListSerializer(IntSerializer), listOf(obj.x, obj.y, obj.width, obj.height))
	}

	override fun deserialize(decoder: Decoder): IntRectangle {
		val values = decoder.decodeSerializableValue(ArrayListSerializer(IntSerializer))
		return IntRectangle(
				x = values[0],
				y = values[1],
				width = values[2],
				height = values[3]
		)
	}
}

/**
 * Sets this rectangle to the given min max region, extending any fractions.
 * E.g. if `x` was `2.8f`, x will be `2` If `right` was `10.1`, x will be `11`
 */
fun IntRectangle.set(minMax: MinMaxRo): IntRectangle {
	val newX = minMax.xMin.toInt()
	val newY = minMax.yMin.toInt()
	val newR = ceilInt(minMax.xMax)
	val newB = ceilInt(minMax.yMax)
	return set(newX, newY, newR - newX, newB - newY)
}
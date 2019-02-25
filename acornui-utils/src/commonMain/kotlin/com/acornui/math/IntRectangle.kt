/*
 * Derived from LibGDX by Nicholas Bilyk
 * https://github.com/libgdx
 * Copyright 2011 See https://github.com/libgdx/libgdx/blob/master/AUTHORS
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
import com.acornui.recycle.ClearableObjectPool
import com.acornui.serialization.*

/**
 * The read-only interface to [IntRectangle].
 */
interface IntRectangleRo {

	val x: Int
	val y: Int
	val width: Int
	val height: Int
	val left: Int
	val top: Int
	val right: Int
	val bottom: Int
	val isEmpty: Boolean

	/**
	 * @param x point x coordinate
	 * @param y point y coordinate
	 * @return whether the point is contained in the rectangle
	 */
	fun intersects(x: Int, y: Int): Boolean

	/**
	 * @param rectangle the other {@link Rectangle}.
	 * @return whether the other rectangle is contained in this rectangle.
	 */
	fun contains(rectangle: IntRectangleRo): Boolean

	/**
	 * @param r the other {@link Rectangle}
	 * @return whether this rectangle overlaps the other rectangle.
	 */
	fun intersects(r: IntRectangleRo): Boolean

	fun intersects(xVal: Int, yVal: Int, widthVal: Int, heightVal: Int): Boolean
	/**
	 * Returns true if this rectangle's bounds can contain the given dimensions
	 * Note: x, y coordinates are not considered.
	 */
	fun canContain(width: Int, height: Int): Boolean

	val area: Int
	val perimeter: Int

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
class IntRectangle(
		override var x: Int = 0,
		override var y: Int = 0,
		override var width: Int = 0,
		override var height: Int = 0
) : IntRectangleRo, Clearable {

	override val left: Int
		get() = x

	override val top: Int
		get() = y

	override val right: Int
		get() {
			return x + width
		}

	override val bottom: Int
		get() {
			return y + height
		}

	override val isEmpty: Boolean
		get() {
			return width == 0 || height == 0
		}

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
	 * @param x point x coordinate
	 * @param y point y coordinate
	 * @return whether the point is contained in the rectangle
	 */
	override fun intersects(x: Int, y: Int): Boolean {
		return this.x <= x && this.x + this.width >= x && this.y <= y && this.y + this.height >= y
	}

	/**
	 * @param rectangle the other {@link Rectangle}.
	 * @return whether the other rectangle is contained in this rectangle.
	 */
	override fun contains(rectangle: IntRectangleRo): Boolean {
		val xmin = rectangle.x
		val xmax = xmin + rectangle.width

		val ymin = rectangle.y
		val ymax = ymin + rectangle.height

		return ((xmin > x && xmin < x + width) && (xmax > x && xmax < x + width)) && ((ymin > y && ymin < y + height) && (ymax > y && ymax < y + height))
	}

	/**
	 * @param r the other {@link Rectangle}
	 * @return whether this rectangle overlaps the other rectangle.
	 */
	override fun intersects(r: IntRectangleRo): Boolean {
		return intersects(r.x, r.y, r.width, r.height)
	}

	override fun intersects(xVal: Int, yVal: Int, widthVal: Int, heightVal: Int): Boolean {
		return x < xVal + widthVal && x + width > xVal && y < yVal + heightVal && y + height > yVal
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

	override val area: Int
		get() {
			return this.width * this.height
		}

	override val perimeter: Int
		get() {
			return 2 * (this.width + this.height)
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
	}

}

object IntRectangleSerializer : To<IntRectangleRo>, From<IntRectangle> {

	override fun IntRectangleRo.write(writer: Writer) {
		writer.int("x", x)
		writer.int("y", y)
		writer.int("width", width)
		writer.int("height", height)
	}

	override fun read(reader: Reader): IntRectangle {
		return IntRectangle(
				x = reader.int("x")!!,
				y = reader.int("y")!!,
				width = reader.int("width")!!,
				height = reader.int("height")!!
		)
	}
}
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

@file:Suppress("unused", "ConvertTwoComparisonsToRangeCheck")

package com.acornui.math

import com.acornui.recycle.Clearable
import com.acornui.recycle.ClearableObjectPool
import kotlinx.serialization.*
import kotlinx.serialization.internal.ArrayListSerializer
import kotlinx.serialization.internal.FloatSerializer
import kotlinx.serialization.internal.StringDescriptor

/**
 * A read-only interface to [Rectangle]
 */
@Serializable(with = RectangleSerializer::class)
interface RectangleRo {

	val x: Float

	val y: Float

	val width: Float

	val height: Float

	val left: Float
		get() = x

	val top: Float
		get() = y

	val right: Float
		get() = x + width

	val bottom: Float
		get() = y + height

	fun isEmpty(): Boolean {
		return width <= 0f || height <= 0f
	}

	fun isNotEmpty(): Boolean = !isEmpty()

	/**
	 * Returns true if the given point intersects with this rectangle.
	 * (Matching edges count as intersection)
	 * @param x point x coordinate
	 * @param y point y coordinate
	 * @return whether the point is contained in the rectangle
	 */
	fun contains(x: Float, y: Float): Boolean {
		return x >= this.x && y >= this.y && x < right && y < this.bottom
	}

	fun contains(point: Vector2Ro) = contains(point.x, point.y)

	/**
	 * Does an intersection test with a Ray (in the same coordinate space)
	 * (Matching edges do not count as intersection)
	 * @param r The ray to check against.
	 * @param out If provided, will be set to the intersection position if there was one.
	 * @return Returns true if the ray intersects this Rectangle.
	 */
	fun intersects(r: RayRo, out: Vector3? = null): Boolean {
		if (r.direction.z == 0f) return false
		val m = -r.origin.z * r.directionInv.z
		if (m < 0) return false // Intersection (if there is one) is behind the ray.
		val x2 = r.origin.x + m * r.direction.x
		val y2 = r.origin.y + m * r.direction.y

		val intersects = x2 >= x && x2 <= right && y2 >= y && y2 <= bottom
		if (out != null && intersects) {
			r.getEndPoint(m, out)
		}
		return intersects
	}

	/**
	 * Returns true if the given rectangle is completely contained within this rectangle.
	 * @param other the other rectangle to check.
	 */
	fun contains(other: RectangleRo): Boolean {
		return other.x >= this.x && other.right <= this.right && other.y >= this.y && other.bottom <= this.bottom
	}

	/**
	 * Returns true if the provided region intersects with this rectangle.
	 * (Matching edges do not count as intersection)
	 */
	fun intersects(x: Float, y: Float, width: Float, height: Float): Boolean {
		return this.x < x + width && this.right > x && this.y < y + height && this.bottom > y
	}

	fun intersects(r: RectangleRo): Boolean = intersects(r.x, r.y, r.width, r.height)

	/**
	 * Returns true if the provided region intersects with this rectangle. Additionally sets the [out] Rectangle
	 * as the region of intersection (only if there was an intersection).
	 * (Matching edges do not count as intersection)
	 * @return Returns true if there was an area of intersection.
	 */
	fun intersects(x: Float, y: Float, width: Float, height: Float, out: Rectangle): Boolean {
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

	fun intersects(r: RectangleRo, out: Rectangle): Boolean = intersects(r.x, r.y, r.width, r.height, out)

	/**
	 * Calculates the aspect ratio ( width / height ) of this rectangle
	 * @return the aspect ratio of this rectangle. Returns 0 if height is 0 to avoid NaN
	 */
	fun getAspectRatio(): Float {
		return if (height == 0f) 0f else width / height
	}

	/**
	 * Calculates the center of the rectangle. Results are located in the given Vector2
	 * @param out the Vector2 to use
	 * @return the given out with results stored inside
	 */
	fun getCenter(out: Vector2): Vector2 {
		out.x = x + width * 0.5f
		out.y = y + height * 0.5f
		return out
	}

	/**
	 * Returns true if this rectangle's bounds can contain the given dimensions
	 * Note: x, y coordinates are not considered.
	 */
	fun canContain(width: Float, height: Float): Boolean {
		return this.width >= width && this.height >= height
	}

	val area: Float
		get() = width * height

	val perimeter: Float
		get() = 2 * (this.width + this.height)

	fun reduce(padding: PadRo): Rectangle = reduce(padding.left, padding.top, padding.right, padding.bottom)

	/**
	 * Clips the sides of this rectangle by the given amounts, returning a new rectangle.
	 */
	fun reduce(left: Float, top: Float, right: Float, bottom: Float): Rectangle {
		return Rectangle(x + left, y + left, width - left - right, height - top - bottom)
	}

	/**
	 * Clamps a 2d vector to these bounds.
	 */
	fun clampPoint(value: Vector2): Vector2 {
		if (value.x < x) value.x = x
		if (value.y < y) value.y = y
		if (value.x > right) value.x = right
		if (value.y > bottom) value.y = bottom
		return value
	}

	companion object {
		val EMPTY = Rectangle()
	}
}

fun RectangleRo.copy(x: Float = this.x, y: Float = this.y, width: Float = this.width, height: Float = this.height): Rectangle {
	return Rectangle(x, y, width, height)
}

@Serializable(with = RectangleSerializer::class)
class Rectangle(
		override var x: Float = 0f,
		override var y: Float = 0f,
		override var width: Float = 0f,
		override var height: Float = 0f
) : Clearable, RectangleRo {

	override var left: Float
		get() = x
		set(value) {
			x = value
		}

	override var top: Float
		get() = y
		set(value) {
			y = value
		}

	/**
	 * The x + width value.
	 * If set, this will change the width to width = right - x
	 */
	override var right: Float
		get() = x + width
		set(value) {
			width = value - x
		}

	/**
	 * The y + height value.
	 * If set, this will change the height to height = bottom - y
	 */
	override var bottom: Float
		get() = y + height
		set(value) {
			height = value - y
		}

	/**
	 * @param x bottom-left x coordinate
	 * @param y bottom-left y coordinate
	 * @param width width
	 * @param height height
	 * @return this rectangle for chaining
	 */
	fun set(x: Float, y: Float, width: Float, height: Float): Rectangle {
		this.x = x
		this.y = y
		this.width = width
		this.height = height

		return this
	}

	fun set(x: Int, y: Int, width: Int, height: Int): Rectangle {
		return set(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat())
	}

	/**
	 * Sets this rectangle to match the given [minMax] region.
	 */
	fun set(minMax: MinMaxRo): Rectangle {
		return set(minMax.xMin, minMax.yMin, minMax.width, minMax.height)
	}

	/**
	 * Sets this Rectangle to 0,0,0,0
	 */
	override fun clear() {
		x = 0f
		y = 0f
		width = 0f
		height = 0f
	}

	/**
	 * Sets the x and y-coordinates of the bottom left corner
	 * @param x The x-coordinate
	 * @param y The y-coordinate
	 * @return this rectangle for chaining
	 */
	fun setPosition(x: Float, y: Float): Rectangle {
		this.x = x
		this.y = y

		return this
	}

	fun setPosition(position: Vector2Ro): Rectangle = setPosition(position.x, position.y)

	/**
	 * Sets the width and height of this rectangle
	 * @param width The width
	 * @param height The height
	 * @return this rectangle for chaining
	 */
	fun setSize(width: Float, height: Float): Rectangle {
		this.width = width
		this.height = height

		return this
	}

	/**
	 * Sets this rectangle to the intersection of this rectangle and [other].
	 */
	fun intersection(other: RectangleRo): Rectangle {
		x = maxOf(x, other.x)
		y = maxOf(y, other.y)
		right = minOf(right, other.right)
		bottom = minOf(bottom, other.bottom)
		return this
	}

	/**
	 * Sets the values of the given rectangle to this rectangle.
	 * @param rect the other rectangle
	 * @return this rectangle for chaining
	 */
	fun set(rect: RectangleRo): Rectangle {
		this.x = rect.x
		this.y = rect.y
		this.width = rect.width
		this.height = rect.height

		return this
	}

	/**
	 * Moves this rectangle so that its center point is located at a given position
	 * @param x the position's x
	 * @param y the position's y
	 * @return this for chaining
	 */
	fun setCenter(x: Float, y: Float): Rectangle {
		setPosition(x - width * 0.5f, y - height * 0.5f)
		return this
	}

	/**
	 * Moves this rectangle so that its center point is located at a given position
	 * @param position the position
	 * @return this for chaining
	 */
	fun setCenter(position: Vector2Ro): Rectangle {
		setPosition(position.x - width * 0.5f, position.y - height * 0.5f)
		return this
	}

	/**
	 * Fits this rectangle around another rectangle while maintaining aspect ratio. This scales and centers the
	 * rectangle to the other rectangle (e.g. Having a camera translate and scale to show a given area)
	 * @param rect the other rectangle to fit this rectangle around
	 * @return this rectangle for chaining
	 */
	fun fitOutside(rect: RectangleRo): Rectangle {
		val ratio = getAspectRatio()

		if (ratio > rect.getAspectRatio()) {
			// Wider than tall
			setSize(rect.height * ratio, rect.height)
		} else {
			// Taller than wide
			setSize(rect.width, rect.width / ratio)
		}

		setPosition((rect.x + rect.width * 0.5f) - width * 0.5f, (rect.y + rect.height * 0.5f) - height * 0.5f)
		return this
	}

	/**
	 * Fits this rectangle into another rectangle while maintaining aspect ratio. This scales and centers the rectangle
	 * to the other rectangle (e.g. Scaling a texture within a arbitrary cell without squeezing)
	 * @param rect the other rectangle to fit this rectangle inside
	 * @return this rectangle for chaining
	 */
	fun fitInside(rect: RectangleRo): Rectangle {
		val ratio = getAspectRatio()

		if (ratio < rect.getAspectRatio()) {
			// Taller than wide
			setSize(rect.height * ratio, rect.height)
		} else {
			// Wider than tall
			setSize(rect.width, rect.width / ratio)
		}

		setPosition((rect.x + rect.width / 2) - width / 2, (rect.y + rect.height / 2) - height / 2)
		return this
	}

	fun inflate(left: Float, top: Float, right: Float, bottom: Float) {
		x -= left
		width += left + right
		y -= top
		height += top + bottom
	}

	fun inflate(pad: PadRo) = inflate(pad.left, pad.top, pad.right, pad.bottom)

	/**
	 * Extends this rectangle to include the given coordinates.
	 */
	fun ext(x2: Float, y2: Float) {
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
	fun ext(rect: RectangleRo): Rectangle {
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

	fun scl(scalar: Float) {
		x *= scalar
		y *= scalar
		width *= scalar
		height *= scalar
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		other as RectangleRo

		if (x != other.x) return false
		if (y != other.y) return false
		if (width != other.width) return false
		if (height != other.height) return false

		return true
	}

	override fun hashCode(): Int {
		var result = x.hashCode()
		result = 31 * result + y.hashCode()
		result = 31 * result + width.hashCode()
		result = 31 * result + height.hashCode()
		return result
	}

	override fun toString(): String {
		return "Rectangle(x=$x, y=$y, width=$width, height=$height)"
	}

	companion object {
		private val pool = ClearableObjectPool { Rectangle() }
		fun obtain(): Rectangle = pool.obtain()
		fun free(obj: Rectangle) = pool.free(obj)
	}
}

@Serializer(forClass = Rectangle::class)
object RectangleSerializer : KSerializer<Rectangle> {

	override val descriptor: SerialDescriptor =
			StringDescriptor.withName("Rectangle")

	override fun serialize(encoder: Encoder, obj: Rectangle) {
		encoder.encodeSerializableValue(ArrayListSerializer(FloatSerializer), listOf(obj.x, obj.y, obj.width, obj.height))
	}

	override fun deserialize(decoder: Decoder): Rectangle {
		val values = decoder.decodeSerializableValue(ArrayListSerializer(FloatSerializer))
		return Rectangle(
				x = values[0],
				y = values[1],
				width = values[2],
				height = values[3]
		)
	}
}
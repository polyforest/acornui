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

import kotlinx.serialization.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.listSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = RectangleSerializer::class)
data class Rectangle(
	val x: Double = 0.0,
	val y: Double = 0.0,
	val width: Double = 0.0,
	val height: Double = 0.0
) {

	val left: Double
		get() = x

	val top: Double
		get() = y

	/**
	 * The x + width value.
	 * If set, this will change the width to width = right - x
	 */
	val right: Double
		get() = x + width

	/**
	 * The y + height value.
	 * If set, this will change the height to height = bottom - y
	 */
	val bottom: Double
		get() = y + height

	/**
	 * Expands all boundaries [left], [top], [right], and [bottom] by the given amount.
	 */
	fun inflate(all: Double) = inflate(all, all, all, all)

	/**
	 * Expands all boundaries [left], [top], [right], and [bottom] by the given values.
	 */
	fun inflate(left: Double, top: Double, right: Double, bottom: Double): Rectangle {
		return Rectangle(
			x = x - left,
			y = y - top,
			width = width + left + right,
			height = height + top + bottom
		)
	}

	/**
	 * Reduces all boundaries [left], [top], [right], and [bottom] by the given value.
	 */
	fun reduce(all: Double) = inflate(-all, -all, -all, -all)

	/**
	 * Reduces all boundaries [left], [top], [right], and [bottom] by the given values.
	 */
	fun reduce(left: Double, top: Double, right: Double, bottom: Double) = inflate(-left, -top, -right, -bottom)

	/**
	 * Extends this rectangle to include the given coordinates.
	 */
	fun ext(x2: Double, y2: Double): Rectangle =
		Rectangle(
			x = if (x2 < x) x2 else x,
			y = if (y2 < y) y2 else y,
			width = if (x2 > x + width) x2 - x else width,
			height = if (y2 > y + height) y2 - y else height
		)

	/**
	 * Extends this rectangle by the other rectangle. The rectangle should not have negative width or negative height.
	 * @param rect the other rectangle
	 */
	fun ext(rect: Rectangle): Rectangle {
		val minX = minOf(x, rect.x)
		val maxX = maxOf(right, rect.right)


		val minY = minOf(y, rect.y)
		val maxY = maxOf(bottom, rect.bottom)

		return Rectangle(
			x = minX,
			y = minY,
			width = maxX - minX,
			height = maxY - minY
		)
	}

	fun isEmpty(): Boolean = width <= 0.0 || height <= 0.0

	fun isNotEmpty(): Boolean = !isEmpty()

	/**
	 * Returns true if the given point intersects with this rectangle.
	 * (Matching edges count as intersection)
	 * @param x point x coordinate
	 * @param y point y coordinate
	 * @return whether the point is contained in the rectangle
	 */
	fun contains(x: Double, y: Double): Boolean =
		x >= this.x && y >= this.y && x < right && y < this.bottom

	fun contains(point: Vector2): Boolean =
		contains(point.x, point.y)

	/**
	 * Returns true if the given rectangle is completely contained within this rectangle.
	 * @param other the other rectangle to check.
	 */
	fun contains(other: Rectangle): Boolean {
		return other.x >= this.x && other.right <= this.right && other.y >= this.y && other.bottom <= this.bottom
	}

	/**
	 * Calculates the intersection of this rectangle and the rectangle for the given values.
	 * If there is no intersection, [EMPTY] is returned.
	 */
	fun intersection(x: Double, y: Double, width: Double, height: Double): Rectangle {
		val right = x + width
		val bottom = y + height
		return if (this.x < right && this.right > x && this.y < bottom && this.bottom > y) {
			val iLeft = maxOf(x, this.x)
			val iTop = maxOf(y, this.y)
			val iRight = minOf(right, this.right)
			val iBottom = minOf(bottom, this.bottom)
			Rectangle(x = iLeft, y = iTop, width = iRight - iLeft, height = iBottom - iTop)
		} else EMPTY
	}

	fun intersects(x: Double, y: Double, width: Double, height: Double): Boolean = intersection(x, y, width, height).isNotEmpty()

	fun intersection(r: Rectangle): Rectangle =
		intersection(r.x, r.y, r.width, r.height)

	fun intersects(r: Rectangle): Boolean = intersection(r).isNotEmpty()

	/**
	 * Calculates the aspect ratio ( width / height ) of this rectangle
	 * @return the aspect ratio of this rectangle. Returns 0 if height is 0 to avoid NaN
	 */
	val aspectRatio: Double
		get() = if (height == 0.0) 0.0 else width / height

	/**
	 * Returns the center point.
	 */
	val center: Vector2
		get() = vec2(x + width * 0.5, y + height * 0.5)

	/**
	 * Returns true if this rectangle's bounds can contain the given dimensions
	 * Note: x, y coordinates are not considered.
	 */
	fun canContain(width: Double, height: Double): Boolean {
		return this.width >= width && this.height >= height
	}

	val area: Double
		get() = width * height

	val perimeter: Double
		get() = 2 * (this.width + this.height)

	/**
	 * Clamps a 2d vector to these bounds.
	 */
	fun clampPoint(value: Vector2): Vector2 {
		return if (contains(value)) value
		else vec2(clamp(value.x, left, right), clamp(value.y, top, bottom))
	}

	companion object {
		val EMPTY = Rectangle()
	}
}

@Serializer(forClass = Rectangle::class)
object RectangleSerializer : KSerializer<Rectangle> {

	override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Rectangle") {
		listSerialDescriptor<Double>()
	}

	override fun serialize(encoder: Encoder, value: Rectangle) {
		encoder.encodeSerializableValue(ListSerializer(Double.serializer()), listOf(value.x, value.y, value.width, value.height))
	}

	override fun deserialize(decoder: Decoder): Rectangle {
		val values = decoder.decodeSerializableValue(ListSerializer(Double.serializer()))
		return Rectangle(
			x = values[0],
			y = values[1],
			width = values[2],
			height = values[3]
		)
	}
}
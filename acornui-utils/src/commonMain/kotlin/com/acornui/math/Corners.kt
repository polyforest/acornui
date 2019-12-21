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
import kotlinx.serialization.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.internal.ListLikeDescriptor

@Serializable(with = CornersSerializer2::class)
interface CornersRo {

	val topLeft: Vector2Ro
	val topRight: Vector2Ro
	val bottomRight: Vector2Ro
	val bottomLeft: Vector2Ro

	fun isEmpty(): Boolean {
		return topLeft.isZero() && topRight.isZero() && bottomRight.isZero() && bottomLeft.isZero()
	}

	fun copy(topLeft: Vector2Ro = this.topLeft, topRight: Vector2Ro = this.topRight, bottomRight: Vector2Ro = this.bottomRight, bottomLeft: Vector2Ro = this.bottomLeft): Corners {
		return Corners(topLeft.copy(), topRight.copy(), bottomRight.copy(), bottomLeft.copy())
	}
}

private object CornersSerializer2 : KSerializer<CornersRo> {
	override val descriptor: SerialDescriptor = ListDescriptor("com.acornui.math.Corners", FloatDescriptor)

	override fun deserialize(decoder: Decoder): Corners {
		val arr = decoder.decodeSerializableValue(FloatSerializer.list)
		return when (arr.size) {
			1 -> Corners(arr[0])
			4 -> Corners(arr[0], arr[1], arr[2], arr[3])
			8 -> Corners(arr[0], arr[1], arr[2], arr[3], arr[4], arr[5], arr[6], arr[7])
			else -> error("Expected 1, 4, or 8 elements")
		}
	}

	override fun serialize(encoder: Encoder, obj: CornersRo) {
		val list = listOf(
				obj.topLeft.x,
				obj.topLeft.y,
				obj.topRight.x,
				obj.topRight.y,
				obj.bottomRight.x,
				obj.bottomRight.y,
				obj.bottomLeft.x,
				obj.bottomLeft.y
		)
		var allEqual = true
		var pairsEqual = true
		val first = list[0]
		for (i in 0 until 8 step 2) {
			val a = list[i]
			val b = list[i + 1]
			if (a != first) allEqual = false
			if (a != b) {
				allEqual = false
				pairsEqual = false
				break
			}
		}
		when {
			allEqual -> encoder.encodeSerializableValue(FloatSerializer.list, listOf(first))
			pairsEqual -> encoder.encodeSerializableValue(FloatSerializer.list, listOf(list[0], list[2], list[4], list[6]))
			else -> encoder.encodeSerializableValue(FloatSerializer.list, list)
		}
	}
}

private class ListDescriptor(override val name: String, val elementDesc: SerialDescriptor) : SerialDescriptor {
	override val kind: SerialKind get() = StructureKind.LIST
	override val elementsCount: Int = 1
	override fun getElementName(index: Int): String = index.toString()
	override fun getElementIndex(name: String): Int =
			name.toIntOrNull() ?: throw IllegalArgumentException("$name is not a valid list index")

	override fun getElementDescriptor(index: Int): SerialDescriptor = elementDesc

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is ListLikeDescriptor) return false

		if (elementDesc == other.elementDesc && name == other.name) return true

		return false
	}

	override fun hashCode(): Int {
		return elementDesc.hashCode() * 31 + name.hashCode()
	}
}


/**
 * A representation of corner radii.
 *
 * @author nbilyk
 */
@Serializable(with = CornersSerializer2::class)
class Corners() : CornersRo, Clearable {

	override val topLeft = Vector2()
	override val topRight = Vector2()
	override val bottomRight = Vector2()
	override val bottomLeft = Vector2()

	constructor(topLeft: Vector2Ro = Vector2(),
				topRight: Vector2Ro = Vector2(),
				bottomRight: Vector2Ro = Vector2(),
				bottomLeft: Vector2Ro = Vector2()) : this() {
		set(topLeft, topRight, bottomRight, bottomLeft)
	}

	constructor(all: Float) : this() {
		set(all)
	}

	constructor(topLeft: Float, topRight: Float, bottomRight: Float, bottomLeft: Float) : this() {
		set(topLeft, topRight, bottomRight, bottomLeft)
	}

	constructor(topLeftX: Float, topLeftY: Float, topRightX: Float, topRightY: Float, bottomRightX: Float, bottomRightY: Float, bottomLeftX: Float, bottomLeftY: Float) : this() {
		set(topLeftX, topLeftY, topRightX, topRightY, bottomRightX, bottomRightY, bottomLeftX, bottomLeftY)
	}

	fun set(all: Float): Corners {
		val allClamped = maxOf(0f, all)
		topLeft.set(allClamped, allClamped)
		topRight.set(allClamped, allClamped)
		bottomRight.set(allClamped, allClamped)
		bottomLeft.set(allClamped, allClamped)
		return this
	}

	fun set(other: CornersRo): Corners {
		return set(other.topLeft.x, other.topLeft.y, other.topRight.x, other.topRight.y, other.bottomRight.x, other.bottomRight.y, other.bottomLeft.x, other.bottomRight.y)
	}

	fun set(topLeft: Float = 0f, topRight: Float = 0f, bottomRight: Float = 0f, bottomLeft: Float = 0f): Corners {
		return set(topLeft, topLeft, topRight, topRight, bottomRight, bottomRight, bottomLeft, bottomLeft)
	}

	fun set(topLeft: Vector2Ro, topRight: Vector2Ro, bottomRight: Vector2Ro, bottomLeft: Vector2Ro): Corners {
		return set(topLeft.x, topLeft.y, topRight.x, topRight.y, bottomRight.x, bottomRight.y, bottomLeft.x, bottomRight.y)
	}

	fun set(topLeftX: Float, topLeftY: Float, topRightX: Float, topRightY: Float, bottomRightX: Float, bottomRightY: Float, bottomLeftX: Float, bottomLeftY: Float): Corners {
		this.topLeft.set(maxOf(0f, topLeftX), maxOf(0f, topLeftY))
		this.topRight.set(maxOf(0f, topRightX), maxOf(0f, topRightY))
		this.bottomRight.set(maxOf(0f, bottomRightX), maxOf(0f, bottomRightY))
		this.bottomLeft.set(maxOf(0f, bottomLeftX), maxOf(0f, bottomLeftY))
		return this
	}

	/**
	 * Decreases the corner radius by the given padding.
	 */
	fun deflate(pad: PadRo): Corners = inflate(-pad.left, -pad.top, -pad.right, -pad.bottom)

	fun deflate(left: Float, top: Float, right: Float, bottom: Float): Corners = inflate(-left, -top, -right, -bottom)
	fun deflate(all: Float): Corners = inflate(-all, -all, -all, -all)

	/**
	 * Increases the corner radius by the given padding.
	 */
	fun inflate(pad: PadRo): Corners = inflate(pad.left, pad.top, pad.right, pad.bottom)

	/**
	 * Increases all dimensions of the corner radius by the given amount.
	 */
	fun inflate(all: Float): Corners = inflate(all, all, all, all)

	fun inflate(left: Float, top: Float, right: Float, bottom: Float): Corners {
		topLeft.x += left
		topLeft.y += top
		topRight.x += right
		topRight.y += top
		bottomRight.x += right
		bottomRight.y += bottom
		bottomLeft.x += left
		bottomLeft.y += bottom
		return this
	}

	override fun clear() {
		set(0f)
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is CornersRo) return false
		if (topLeft != other.topLeft) return false
		if (topRight != other.topRight) return false
		if (bottomRight != other.bottomRight) return false
		if (bottomLeft != other.bottomLeft) return false

		return true
	}

	override fun hashCode(): Int {
		var result = topLeft.hashCode()
		result = 31 * result + topRight.hashCode()
		result = 31 * result + bottomRight.hashCode()
		result = 31 * result + bottomLeft.hashCode()
		return result
	}

	override fun toString(): String {
		return "Corners(topLeft=$topLeft, topRight=$topRight, bottomRight=$bottomRight, bottomLeft=$bottomLeft)"
	}
}
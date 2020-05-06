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
import kotlinx.serialization.builtins.list
import kotlinx.serialization.builtins.serializer
import kotlin.math.ceil

/**
 * A read-only interface to [Pad]
 */
@Serializable(with = PadSerializer::class)
interface PadRo {
	val top: Float
	val right: Float
	val bottom: Float
	val left: Float

	fun isEmpty(): Boolean = top == 0f && right == 0f && bottom == 0f && left == 0f
	fun isNotEmpty(): Boolean = !isEmpty()

	fun reduceWidth(width: Float?): Float? {
		if (width == null) return null
		return width - left - right
	}

	fun reduceHeight(height: Float?): Float? {
		if (height == null) return null
		return height - top - bottom
	}

	fun reduceWidth(width: Float): Float {
		return width - left - right
	}

	fun reduceHeight(height: Float): Float {
		return height - top - bottom
	}

	fun expandWidth(width: Float?): Float? {
		if (width == null) return null
		return width + left + right
	}

	fun expandHeight(height: Float?): Float? {
		if (height == null) return null
		return height + top + bottom
	}

	fun expandWidth(width: Float): Float {
		return width + left + right
	}

	fun expandHeight(height: Float): Float {
		return height + top + bottom
	}

	operator fun plus(value: PadRo): Pad {
		return copy().add(value)
	}

	operator fun minus(value: PadRo): Pad {
		return copy().sub(value)
	}

	fun toCssString(): String {
		return "${top}px ${right}px ${bottom}px ${left}px"
	}

	fun copy(top: Float = this.top, right: Float = this.right, bottom: Float = this.bottom, left: Float = this.left): Pad {
		return Pad(left, top, right, bottom)
	}
}

/**
 * A representation of margins or padding.
 *
 * @author nbilyk
 */
@Serializable(with = PadSerializer::class)
class Pad(
		override var left: Float,
		override var top: Float,
		override var right: Float,
		override var bottom: Float
) : PadRo, Clearable {

	constructor() : this(0f, 0f, 0f, 0f)

	constructor(all: Float) : this(all, all, all, all)

	/**
	 * Sets all values to the given float.
	 */
	fun set(all: Float): Pad {
		top = all
		bottom = all
		right = all
		left = all
		return this
	}

	/**
	 * Sets values to match [other].
	 */
	fun set(other: PadRo): Pad {
		top = other.top
		bottom = other.bottom
		right = other.right
		left = other.left
		return this
	}

	/**
	 * Sets values to the given values.
	 */
	fun set(left: Float = 0f, top: Float = 0f, right: Float = 0f, bottom: Float = 0f): Pad {
		this.left = left
		this.top = top
		this.right = right
		this.bottom = bottom
		return this
	}

	/**
	 * Adjusts all values [left], [top], [right], and [bottom] by the given amount.
	 */
	fun add(all: Float) = add(all, all, all, all)

	/**
	 * Adjusts all values by [left], [top], [right], and [bottom]
	 */
	fun add(left: Float, top: Float, right: Float, bottom: Float): Pad {
		this.left += left
		this.right += right
		this.top += top
		this.bottom += bottom
		return this
	}

	/**
	 * Adjusts all values [left], [top], [right], and [bottom] by the [pad] values.
	 */
	fun add(pad: PadRo) = add(pad.left, pad.top, pad.right, pad.bottom)

	/**
	 * Subtracts all values by the given amount.
	 */
	fun sub(all: Float) = add(-all, -all, -all, -all)

	/**
	 * Subtracts all values by [left], [top], [right], and [bottom]
	 */
	fun sub(left: Float, top: Float, right: Float, bottom: Float) = add(-left, -top, -right, -bottom)

	/**
	 * Subtracts all values by the [pad] values.
	 */
	fun sub(pad: PadRo) = add(-pad.left, -pad.top, -pad.right, -pad.bottom)

	operator fun minusAssign(pad: PadRo) {
		sub(pad)
	}

	operator fun plusAssign(pad: PadRo) {
		add(pad)
	}

	/**
	 * Scales all padding values by the given scalar.
	 */
	fun scl(scalar: Float) {
		left *= scalar
		top *= scalar
		right *= scalar
		bottom *= scalar
	}

	/**
	 * Ceils each value of this padding object. E.g. `top = ceil(top)`
	 */
	fun ceil(): Pad {
		left = ceil(left)
		top = ceil(top)
		right = ceil(right)
		bottom = ceil(bottom)
		return this
	}

	override fun clear() {
		left = 0f
		top = 0f
		right = 0f
		bottom = 0f
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is PadRo) return false

		if (left != other.left) return false
		if (top != other.top) return false
		if (right != other.right) return false
		if (bottom != other.bottom) return false

		return true
	}

	override fun hashCode(): Int {
		var result = left.hashCode()
		result = 31 * result + top.hashCode()
		result = 31 * result + right.hashCode()
		result = 31 * result + bottom.hashCode()
		return result
	}

	override fun toString(): String {
		return "Pad(left=$left, top=$top, right=$right, bottom=$bottom)"
	}


	companion object {
		val EMPTY_PAD: PadRo = Pad()
	}
}

@Serializer(forClass = Pad::class)
object PadSerializer : KSerializer<Pad> {

	override val descriptor: SerialDescriptor = SerialDescriptor("Pad") {
		listDescriptor<Float>()
	}

	override fun serialize(encoder: Encoder, value: Pad) {
		encoder.encodeSerializableValue(Float.serializer().list, listOf(value.left, value.top, value.right, value.bottom))
	}

	override fun deserialize(decoder: Decoder): Pad {
		val values = decoder.decodeSerializableValue(Float.serializer().list)
		return Pad(
				left = values[0],
				top = values[1],
				right = values[2],
				bottom = values[3]
		)
	}
}

operator fun Matrix4Ro.times(p: PadRo): Pad {
	val tL = rot(vec3(-p.left, -p.top, 0f))
	val tR = rot(vec3(p.right, -p.top, 0f))
	val bR = rot(vec3(p.right, p.bottom, 0f))
	val bL = rot(vec3(-p.left, p.bottom, 0f))
	return Pad(
			left = -minOf4(tL.x, tR.x, bR.x, bL.x),
			top = -minOf4(tL.y, tR.y, bR.y, bL.y),
			right = maxOf4(tL.x, tR.x, bR.x, bL.x),
			bottom = maxOf4(tL.y, tR.y, bR.y, bL.y)
	)
}
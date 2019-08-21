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
import kotlinx.serialization.internal.ArrayListSerializer
import kotlinx.serialization.internal.IntSerializer
import kotlinx.serialization.internal.StringDescriptor

/**
 * A read-only interface to [IntPad]
 */
@Serializable(with = IntPadSerializer::class)
interface IntPadRo {
	val top: Int
	val right: Int
	val bottom: Int
	val left: Int

	fun isEmpty(): Boolean = top == 0 && right == 0 && bottom == 0 && left == 0
	fun isNotEmpty(): Boolean = !isEmpty()

	fun reduceWidth(width: Int?): Int? {
		if (width == null) return null
		return width - left - right
	}

	fun reduceHeight(height: Int?): Int? {
		if (height == null) return null
		return height - top - bottom
	}

	// TODO: we might be able to overload the nullable ints now.

	fun reduceWidth2(width: Int): Int {
		return width - left - right
	}

	fun reduceHeight2(height: Int): Int {
		return height - top - bottom
	}

	fun expandWidth(width: Int?): Int? {
		if (width == null) return null
		return width + left + right
	}

	fun expandHeight(height: Int?): Int? {
		if (height == null) return null
		return height + top + bottom
	}

	fun expandWidth2(width: Int): Int {
		return width + left + right
	}

	fun expandHeight2(height: Int): Int {
		return height + top + bottom
	}

	fun toCssString(): String {
		return "${top}px ${right}px ${bottom}px ${left}px"
	}

	fun copy(top: Int = this.top, right: Int = this.right, bottom: Int = this.bottom, left: Int = this.left): IntPad {
		return IntPad(top, right, bottom, left)
	}
}

/**
 * A representation of margins or padding with integer values.
 *
 * @author nbilyk
 */
@Serializable(with = IntPadSerializer::class)
class IntPad(
		override var top: Int,
		override var right: Int,
		override var bottom: Int,
		override var left: Int) : IntPadRo, Clearable {

	constructor() : this(0, 0, 0, 0)

	constructor(all: Int) : this(all, all, all, all)

	constructor(all: Array<Int>) : this(all[0], all[1], all[2], all[3])

	fun set(all: Int): IntPad {
		top = all
		bottom = all
		right = all
		left = all
		return this
	}

	fun set(other: IntPadRo): IntPad {
		top = other.top
		bottom = other.bottom
		right = other.right
		left = other.left
		return this
	}

	fun set(left: Int = 0, top: Int = 0, right: Int = 0, bottom: Int = 0): IntPad {
		this.top = top
		this.right = right
		this.bottom = bottom
		this.left = left
		return this
	}

	override fun clear() {
		top = 0
		right = 0
		bottom = 0
		left = 0
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is IntPadRo) return false

		if (top != other.top) return false
		if (right != other.right) return false
		if (bottom != other.bottom) return false
		if (left != other.left) return false

		return true
	}

	override fun hashCode(): Int {
		var result = top.hashCode()
		result = 31 * result + right.hashCode()
		result = 31 * result + bottom.hashCode()
		result = 31 * result + left.hashCode()
		return result
	}

	companion object {
		val EMPTY_PAD: IntPadRo = IntPad()
	}
}

@Serializer(forClass = IntPad::class)
object IntPadSerializer : KSerializer<IntPad> {

	override val descriptor: SerialDescriptor =
			StringDescriptor.withName("IntPad")

	override fun serialize(encoder: Encoder, obj: IntPad) {
		encoder.encodeSerializableValue(ArrayListSerializer(IntSerializer), listOf(obj.top, obj.right, obj.bottom, obj.left))
	}

	override fun deserialize(decoder: Decoder): IntPad {
		val values = decoder.decodeSerializableValue(ArrayListSerializer(IntSerializer))
		return IntPad(
				top = values[0],
				right = values[1],
				bottom = values[2],
				left = values[3]
		)
	}
}
/*
 * Copyright 2018 Poly Forest
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

package com.acornui.collection

/**
 * A wrapper to a FloatArray that implements List<Float>
 */
class FloatList(private val target: FloatArray) : ListBase<Float>() {

	constructor(size: Int) : this(FloatArray(size))

	override val size: Int
		get() = target.size

	override fun get(index: Int): Float = target[index]

	operator fun set(index: Int, element: Float) {
		target[index] = element
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		@Suppress("UNCHECKED_CAST")
		other as List<Float>
		for (i in 0..target.lastIndex) {
			if (target[i] != other[i]) return false
		}
		return true
	}

	override fun hashCode(): Int {
		return target.hashCode()
	}

	override fun toString(): String {
		return "FloatList(${target.contentToString()})"
	}
}

fun arrayCopy(src: FloatArray,
			  srcPos: Int,
			  dest: FloatList,
			  destPos: Int = 0,
			  length: Int = src.size) {
	if (destPos > srcPos) {
		var destIndex = length + destPos - 1
		for (i in srcPos + length - 1 downTo srcPos) {
			dest[destIndex--] = src[i]
		}
	} else {
		var destIndex = destPos
		for (i in srcPos..srcPos + length - 1) {
			dest[destIndex++] = src[i]
		}
	}
}

fun arrayCopy(src: List<Float>,
			  srcPos: Int,
			  dest: FloatList,
			  destPos: Int = 0,
			  length: Int = src.size) {
	if (destPos > srcPos) {
		var destIndex = length + destPos - 1
		for (i in srcPos + length - 1 downTo srcPos) {
			dest[destIndex--] = src[i]
		}
	} else {
		var destIndex = destPos
		for (i in srcPos..srcPos + length - 1) {
			dest[destIndex++] = src[i]
		}
	}
}
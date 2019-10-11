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

package com.acornui.collection

/**
 * Returns first index of *element*, or -1 if the collection does not contain element
 */
fun <E> Array<out E>.indexOf(element: E, fromIndex: Int): Int {
	for (i in fromIndex..lastIndex) {
		if (element == this[i]) {
			return i
		}
	}
	return -1
}


/**
 * @author nbilyk
 */
@Deprecated("Use kotlin copyInto methods", ReplaceWith("src.copyInto(dest, destPos, srcPos, length + destPos - 1"))
fun <E> arrayCopy(src: Array<out E>,
						 srcPos: Int,
						 dest: Array<E>,
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

@Deprecated("Use kotlin copyInto methods", ReplaceWith("src.copyInto(dest, destPos, srcPos, length + destPos - 1"))
fun arrayCopy(src: FloatArray,
					 srcPos: Int,
					 dest: FloatArray,
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

@Deprecated("Use kotlin copyInto methods", ReplaceWith("src.copyInto(dest, destPos, srcPos, length + destPos - 1"))
fun arrayCopy(src: IntArray,
					 srcPos: Int,
					 dest: IntArray,
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

fun <E> MutableList<E>.pop(): E {
	return removeAt(size - 1)
}

fun <E> MutableList<E>.popOrNull(): E? {
	if (isEmpty()) return null
	return removeAt(size - 1)
}

fun <E> MutableList<E>.poll(): E {
	return removeAt(0)
}

fun <E> MutableList<E>.shift(): E {
	return removeAt(0)
}

fun <E> MutableList<E>.shiftOrNull(): E? {
	if (isEmpty()) return null
	return removeAt(0)
}

fun <E> MutableList<E>.unshift(element: E) {
	return add(0, element)
}


/**
 * Shifts this list so that [delta] becomes the new zero.
 *
 * E.g. if this list is   [0, 1, 2, 3, 4, 5] shift(3) will change the list to be [3, 4, 5, 0, 1, 2].
 *
 */
fun <E> MutableList<E>.shiftAll(delta: Int) {
	if (delta == 0) return
	var delta2 = delta
	if (delta2 < 0)
		delta2 += size
	val copy = copy()
	arrayCopy(copy, delta2, this, 0, size - delta2)
	arrayCopy(copy, 0, this, size - delta2, delta2)
}

@Suppress("BASE_WITH_NULLABLE_UPPER_BOUND") fun <T> List<T>.peek(): T? {
	return if (isEmpty()) null
	else this[lastIndex]
}

fun <E> Array<E>.equalsArray(other: Array<E>): Boolean {
	if (this === other) return true
	if (other.size != size) return false
	for (i in 0..lastIndex) {
		if (this[i] != other[i]) return false
	}
	return true
}

fun FloatArray.equalsArray(other: FloatArray): Boolean {
	if (this === other) return true
	if (other.size != size) return false
	for (i in 0..lastIndex) {
		if (this[i] != other[i]) return false
	}
	return true
}

fun <E> Iterable<E>.hashCodeIterable(): Int {
	var hashCode = 1
	for (e in this) {
		hashCode = 31 * hashCode + (e?.hashCode() ?: 0)
	}
	return hashCode
}

fun <E> Array<E>.hashCodeIterable(): Int {
	var hashCode = 1
	for (e in this) {
		hashCode = 31 * hashCode + (e?.hashCode() ?: 0)
	}
	return hashCode
}

inline fun <E : Any> Array<E?>.iterateNotNulls(inner: (Int, E) -> Boolean) {
	var i = 0
	val n = size
	while (i < n) {
		val value = this[i]
		if (value != null) {
			val ret = inner(i, value)
			if (!ret) break
		}
		i++
	}
}

private fun <E : Any> Array<E?>.nextNotNull(start: Int): Int {
	var index = start
	val n = size
	while (index < n && this[index] == null) { index++ }
	return index
}

fun FloatArray.fill2(element: Float, fromIndex: Int = 0, toIndex: Int = size): Unit {
	for (i in fromIndex .. toIndex - 1) {
		this[i] = element
	}
}

fun FloatArray.scl(scalar: Float) {
	for (i in 0..lastIndex) {
		this[i] *= scalar
	}
}

/**
 * Returns the index of the largest value in this list.
 */
fun IntArray.indexOfMax(): Int {
	if (isEmpty()) return -1
	var max = this[0]
	var maxIndex = 0
	for (i in 1..lastIndex) {
		val e = this[i]
		if (max < e) {
			max = e
			maxIndex = i
		}
	}
	return maxIndex
}

/**
 * Returns the index of the largest value in this list.
 */
fun IntArray.indexOfMin(): Int {
	if (isEmpty()) return -1
	var min = this[0]
	var minIndex = 0
	for (i in 1..lastIndex) {
		val e = this[i]
		if (min > e) {
			min = e
			minIndex = i
		}
	}
	return minIndex
}

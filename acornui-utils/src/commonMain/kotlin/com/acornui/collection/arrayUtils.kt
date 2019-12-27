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
 * Returns first index of [element] starting at [fromIndex], or -1 if the array does not contain element after
 * [fromIndex].
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
@Deprecated("Use kotlin copyInto methods", ReplaceWith("src.copyInto(dest, destPos, srcPos, length + srcPos"))
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

@Deprecated("Use kotlin copyInto methods", ReplaceWith("src.copyInto(dest, destPos, srcPos, length + srcPos)"))
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

@Deprecated("Use kotlin copyInto methods", ReplaceWith("src.copyInto(dest, destPos, srcPos, length + srcPos)"))
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
	return removeAt(lastIndex)
}

fun <E> MutableList<E>.popOrNull(): E? {
	if (isEmpty()) return null
	return removeAt(lastIndex)
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
 */
fun <E> MutableList<E>.shiftAll(delta: Int) {
	if (delta == 0) return
	var offset = -delta
	if (offset < 0)
		offset += size
	check(offset >= 0) { "delta cannot be less than -size" }
	val copy = copy()
	copy.copyInto(destination = this, destinationOffset = 0, startIndex = size - offset, endIndex = size)
	copy.copyInto(destination = this, destinationOffset = offset, startIndex = 0, endIndex = size - offset)
}

@Suppress("BASE_WITH_NULLABLE_UPPER_BOUND") fun <T> List<T>.peek(): T? {
	return if (isEmpty()) null
	else this[lastIndex]
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
@Deprecated("May be removed", level = DeprecationLevel.ERROR)
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
 * Returns the index of the smallest value in this list.
 */
@Deprecated("May be removed", level = DeprecationLevel.ERROR)
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

/**
 * Creates a new array of the necessary capacity, inserting the new elements at the given position.
 */
fun FloatArray.add(index: Int, elements: FloatArray): FloatArray {
	val newArr = FloatArray(size + elements.size)
	copyInto(newArr, 0, 0, index)
	elements.copyInto(newArr, index)
	copyInto(newArr, index + elements.size, index)
	return newArr
}

/**
 * Creates a new array of the necessary capacity, removing [count] elements from the given position.
 */
fun FloatArray.remove(index: Int, count: Int): FloatArray {
	val newArr = FloatArray(size - count)
	copyInto(newArr, 0, 0, index)
	copyInto(newArr, index, index + count)
	return newArr
}

/**
 * Searches this list or a sub-range for the insertion position of the given element, assuming this list is sorted
 * in ascending order by the elements spaced [stride] apart.
 *
 * @param element The element for which to calculate the insertion index.
 * @param stride The number of elements per set of numbers. For example if we have a float array of numbers such as:
 * `[time0, r0, g0, b0, a0, time1, r1, g1, b1, a1, ..., timen, rn, gn, bn, an]` the stride would be 5 because there are
 * 5 elements in each set.
 * @param offset The offset within the set to use for comparison. Note: [fromIndex], [toIndex], and the return values
 * are before the offset is added.
 * Example: `[a0, b0, c0, a1, b1, c1, a2, b2, c2]`, assuming the `b` values are sorted in ascending order.
 * `getInsertionIndex(element = b1, stride = 3, offset = 1, fromIndex = 0, toIndex = size)` will return 6, as that index
 * represents the beginning of the first set where the `b` value (offset = 1) is greater than element.
 *
 * @param fromIndex The start of the range to search. This should be the beginning of a set, before [offset].
 * @param toIndex The end of the range to search (exclusive). This should be the beginning of a set, before [offset].
 * @return Returns the index of the beginning of the set where [element] is greater than the compared elements.
 */
fun FloatArray.getInsertionIndex(element: Float, stride: Int = 1, offset: Int = 0, fromIndex: Int = 0, toIndex: Int = size): Int {
	var indexA = fromIndex / stride
	var indexB = toIndex / stride
	while (indexA < indexB) {
		val midIndex = (indexA + indexB) ushr 1
		if (element >= this[midIndex * stride + offset]) {
			indexA = midIndex + 1
		} else {
			indexB = midIndex
		}
	}
	return indexA * stride
}
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

import com.acornui.recycle.Clearable

/**
 * A read-only view of a IntArray.
 */
interface IntArrayListRo : List<Int> {

	/**
	 * Returns the unboxed IntArray.
	 * @suppress
	 */
	fun asNative(): IntArray
}

inline class IntArrayList(val inner: IntArray) : IntArrayListRo {

	override val size: Int
		get() = inner.size

	override fun contains(element: Int): Boolean {
		return inner.contains(element)
	}

	override fun containsAll(elements: Collection<Int>): Boolean {
		for (element in elements) {
			if (!contains(element)) return false
		}
		return true
	}

	override fun get(index: Int): Int {
		return inner[index]
	}

	operator fun set(index: Int, element: Int) {
		inner[index] = element
	}

	override fun indexOf(element: Int): Int {
		return inner.indexOf(element)
	}

	override fun isEmpty(): Boolean {
		return inner.isEmpty()
	}

	override fun iterator(): Iterator<Int> {
		return inner.iterator()
	}

	override fun lastIndexOf(element: Int): Int {
		return inner.lastIndexOf(element)
	}

	override fun listIterator(): IntArrayIterator {
		return IntArrayIterator(inner)
	}

	override fun listIterator(index: Int): IntArrayIterator {
		val iterator = IntArrayIterator(inner)
		iterator.cursor = index
		return iterator
	}

	override fun subList(fromIndex: Int, toIndex: Int): List<Int> {
		return SubList(this, fromIndex, toIndex)
	}

	override fun asNative(): IntArray = inner
}

/**
 * An iterator object for IntArray.
 * Use this wrapper when using an Array<T> where an Iterable<T> is needed.
 */
open class IntArrayIterator(
		val array: IntArray
) : Clearable, ListIterator<Int>, Iterable<Int> {

	var cursor: Int = 0     // index of next element to return
	var lastRet: Int = -1   // index of last element returned; -1 if no such

	override fun hasNext(): Boolean {
		return cursor != array.size
	}

	override fun next(): Int {
		val i = cursor
		if (i >= array.size)
			throw Exception("Iterator does not have next.")
		cursor = i + 1
		lastRet = i
		return array[i]
	}

	override fun nextIndex(): Int {
		return cursor
	}

	override fun hasPrevious(): Boolean {
		return cursor != 0
	}

	override fun previous(): Int {
		val i = cursor - 1
		if (i < 0)
			throw Exception("Iterator does not have previous.")
		cursor = i
		lastRet = i
		return array[i]
	}

	override fun previousIndex(): Int {
		return cursor - 1
	}

	/**
	 * A IntArrayIterator can have elements be set, but it cannot implement [MutableListIterator] because the array's
	 * size cannot change.
	 */
	fun set(element: Int) {
		if (lastRet < 0)
			throw Exception("Cannot set before iteration.")
		array[lastRet] = element
	}

	override fun clear() {
		cursor = 0
		lastRet = -1
	}

	override fun iterator(): Iterator<Int> {
		return this
	}
}
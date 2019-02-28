/*
 * Copyright 2017 Nicholas Bilyk
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

package com.acornui.recycle

import com.acornui.collection.*
import com.acornui.core.Disposable

/**
 * A layer between item reuse and an object pool that first seeks an item from the same index.
 * It is to reduce the amount of changes a pooled item may need to make.
 *
 * To use this class, make a series of [obtain] calls to grab recycled elements, then call [flip] to send unused
 * elements back to the object pool, and make the obtained elements available for the next series.
 *
 * A set may look like this:
 *
 * ```
 * obtain(3)
 * obtain(4)
 * obtain(5)
 * obtain(2)
 * obtain(1)
 * flip() // 1 through 5 is available in the next set for fast access.
 * ```
 *
 * A set pulling in reverse order should look like this:
 * ```
 * obtain(5)
 * obtain(4)
 * obtain(3)
 * obtain(6)
 * obtain(7)
 * flip() // 3 through 7 is available in the next set for fast access.
 * ```
 *
 * Indices may have gaps, but never insertions.  That is, for `obtain(index)`, `index` must be either the highest, or the
 * lowest index obtained in the current set.
 *
 * @param pool The indexed cache will call [Pool.obtain] when a new element is needed, and [Pool.free] when an element
 * is returned.
 */
open class IndexedPool<E>(

		/**
		 * @suppress
		 */
		internal val pool: Pool<E>
) : ListBase<E>(), Clearable {

	/**
	 * Creates an [IndexedPool] with a basic [ObjectPool] implementation.
	 * @param create The factory method for creating new elements.
	 */
	constructor(create: () -> E) : this(ObjectPool(create))

	/**
	 * The size of this list. This will be updated after a [flip]
	 */
	override val size: Int
		get() = current.size

	/**
	 * Returns the size of the obtained list.
	 */
	val obtainedSize: Int
		get() = obtained.size

	override fun get(index: Int): E = current[index]

	private var current = ArrayList<E>()
	private var currentIndices = ArrayList<Int>()

	private var obtained = ArrayList<E>()
	private var obtainedIndices = ArrayList<Int>()

	/**
	 * Obtains an element.
	 * This algorithm attempts to obtain an element with the same index that was cached in the last obtain/flip set.
	 */
	fun obtain(index: Int): E {
		if (obtainedIndices.isEmpty()) {
			val shiftIndex = currentIndices.sortedInsertionIndex(index, matchForwards = false)
			currentIndices.shiftAll(shiftIndex)
			current.shiftAll(shiftIndex)
		}
		val isForward = if (obtainedIndices.isEmpty()) {
			if (currentIndices.isEmpty()) true
			else {
				val mid = currentIndices.size shr 1
				index < currentIndices[mid]
			}
		} else index >= obtainedIndices.first()

		val element: E
		element = if (current.isEmpty()) {
			pool.obtain()
		} else {
			if (isForward) if (currentIndices.first() == index) {
				currentIndices.shift()
				current.shift()
			} else {
				currentIndices.pop()
				current.pop()
			}
			else if (currentIndices.last() == index) {
				currentIndices.pop()
				current.pop()
			} else {
				currentIndices.shift()
				current.shift()
			}
		}
		if (isForward) {
			obtained.add(element)
			obtainedIndices.add(index)
		} else {
			obtained.unshift(element)
			obtainedIndices.unshift(index)
		}
		return element
	}

	/**
	 * Returns the element obtained via [obtain] in this set for the given index.
	 * @throws IndexOutOfBoundsException If the index is out of range, or the index was not obtained via [obtain].
	 */
	fun getObtainedByIndex(index: Int): E = getByIndex(index, obtained, obtainedIndices)

	/**
	 * Returns the current element in this set for the given index.
	 * This will only be valid after a [flip].
	 * @throws IndexOutOfBoundsException If the index is out of range, or the index was not found.
	 */
	fun getByIndex(index: Int): E = getByIndex(index, current, currentIndices)

	private fun getByIndex(index: Int, elements: List<E>, indices: List<Int>): E {
		if (elements.isEmpty()) throw IndexOutOfBoundsException()

		val offset = indices.first()
		return if (indices[index - offset] == index) {
			elements[index - offset]
		} else {
			val i = indices.sortedInsertionIndex(index)
			if (indices[i] != index) throw IndexOutOfBoundsException("")
			elements[i]
		}
	}

	/**
	 * Iterates over each unused item still in the cache.
	 */
	fun forEachUnused(callback: (index: Int, renderer: E) -> Unit): IndexedPool<E> {
		if (current.isEmpty()) return this
		for (i in 0..current.lastIndex) {
			callback(currentIndices[i], current[i])
		}
		return this
	}

	/**
	 * Sets the items returned via [obtain] to be used as the cached items for the next set.
	 */
	fun flip() {
		current.forEach2(pool::free)
		val tmp = current
		val tmpIndices = currentIndices
		current = obtained
		currentIndices = obtainedIndices
		obtained = tmp
		obtained.clear()
		obtainedIndices = tmpIndices
		obtainedIndices.clear()
	}

	/**
	 * Clears this list, sending all known elements back to the [pool].
	 */
	override fun clear() {
		current.freeTo(pool)
		obtained.freeTo(pool)
		currentIndices.clear()
		obtainedIndices.clear()
	}
}

/**
 * Clears the index cache back into the pool, then disposes all elements and clears the pool.
 */
fun <E : Disposable> IndexedPool<E>.disposeAndClear() {
	clear()
	pool.disposeAndClear()
}
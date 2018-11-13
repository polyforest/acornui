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

package com.acornui.core.cache

import com.acornui.collection.*
import com.acornui.component.ElementContainer
import com.acornui.component.UiComponent
import com.acornui.core.Disposable

/**
 * A layer between item reuse and an object pool that first seeks an item from the same index.
 * It is to reduce the amount of changes a pooled item may need to make.
 *
 * To use this class, make a series of [obtain] calls to grab recycled elements, call [forEachUnused] to iterate over
 * the elements that weren't obtained, then call [flip] to send unused elements back to the object pool, and make the
 * obtained elements available for the next series.
 *
 * A set may look like this:
 *
 * ```
 * obtain(3)
 * obtain(4)
 * obtain(5)
 * obtain(2)
 * obtain(1)
 * forEachUnused { ... } // Deactivate unused items
 * flip()
 * ```
 *
 * A set pulling in reverse order should look like this:
 * ```
 * obtain(5)
 * obtain(4)
 * obtain(3)
 * obtain(6)
 * obtain(7)
 * forEachUnused { ... } // Deactivate unused items
 * flip()
 * ```
 */
class IndexedCache<E>(val pool: Pool<E>) : ListBase<E>() {

	constructor(factory: () -> E) : this(ObjectPool(factory))

	/**
	 * The current items are indexed in this list starting at index 0. This offset represents the lowest index used in
	 * the last [obtain] set.
	 */
	val offset: Int
		get() = currentIndices.firstOrNull() ?: 0

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
		val isForward = obtainedIndices.isEmpty() || index >= obtainedIndices.first()

		val element: E
		element = if (current.isEmpty()) {
			pool.obtain()
		} else {
			if (isForward) current.shift()
			else current.pop()
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
	 * Returns the cached element at the given index, if it exists.
	 */
	fun getCached(index: Int): E {
		return current[index - offset]!!
	}

	/**
	 * Iterates over each unused item still in the cache.
	 */
	fun forEachUnused(callback: (index: Int, renderer: E) -> Unit) {
		if (current.isEmpty()) return
		for (i in 0..current.lastIndex) {
			callback(currentIndices[i], current[i])
		}
	}

	/**
	 * Clears the current cache values. Note that this does not clear the pool, nor does it move the current cache
	 * values into the pool.
	 * @see flip
	 */
	fun clear() {
		current.clear()
		currentIndices.clear()
	}

	/**
	 * Sets the items returned via [obtain] to be used as the cached items for the next set.
	 */
	fun flip() {
		pool.freeAll(current)
		val tmp = current
		val tmpIndices = currentIndices
		current = obtained
		currentIndices = obtainedIndices
		obtained = tmp
		obtained.clear()
		obtainedIndices = tmpIndices
		obtainedIndices.clear()
	}
}

/**
 * Disposes each instance in the cache and pool.
 */
fun <E : Disposable> IndexedCache<E>.disposeAndClear() {
	forEachUnused {
		_, it ->
		it.dispose()
	}
	pool.disposeAndClear()
	clear()
}

/**
 * Removes all unused cache instances from the given container before flipping.
 */
fun <E : UiComponent> IndexedCache<E>.removeAndFlip(parent: ElementContainer<UiComponent>) {
	forEachUnused {
		_, it ->
		parent.removeElement(it)
	}
	flip()
}

/**
 * Sets visible to false on all unused cache instances before flipping.
 */
fun <E : UiComponent> IndexedCache<E>.hideAndFlip() {
	forEachUnused {
		_, it ->
		it.visible = false
	}
	flip()
}
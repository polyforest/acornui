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
import com.acornui.math.MathUtils

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
 *
 *
 */
class IndexedCache<E>(val pool: Pool<E>) : ListBase<E>() {

	constructor(factory: ()->E) : this(ObjectPool(factory))

	private var usedStartIndex = 0

	private var _startIndex = 0
	val startIndex: Int
		get() = _startIndex

	override val size: Int
		get() = cache.size

	override fun get(index: Int): E = cache[index]!!

	private var cache = CyclicList<E?>()
	private var used = CyclicList<E?>()

	private var _unusedSize = 0
	val unusedSize: Int
		get() = _unusedSize

	/**
	 * Obtain will provide a pooled instance in this order:
	 * 1) An item from the previous set with a matching index.
	 * 2) If
	 * 3) If no items remain from the previous set, the provided [Pool] will be used.
	 *
	 * @param index Obtain must pull from the tail or the head. That is, [index] must be sequential from the first index
	 * obtained, where the head is the lowest index, and the tail is the highest index.
	 * Note that the indices are arbitrary and only used for matching purposes.
	 *
	 * Legal: 6, 7, 8, 5, 4, 3, 9, 10, 11
	 * Illegal: 6, 7, 8, 10
	 * Illegal: 6, 7, 8, 4
	 */
	fun obtain(index: Int): E {
		val element: E
		element = if (_unusedSize == 0) {
			pool.obtain()
		} else {
			val index2 = MathUtils.mod(index - _startIndex, size)
			_unusedSize--
			val e = cache[index2]!!
			cache[index2] = null
			e
		}

		if (used.isEmpty())
			usedStartIndex = index

		if (index >= usedStartIndex) {
			if (index != usedStartIndex + used.size) throw IllegalStateException("IndexedCache.obtain must be requested sequentially.")
			used.add(element)
		} else {
			if (index != usedStartIndex - 1) throw IllegalStateException("IndexedCache.obtain must be requested sequentially.")
			usedStartIndex = index
			used.unshift(element)
		}
		return element
	}

	/**
	 * Returns the cached element at the given index, if it exists.
	 */
	fun getCached(index: Int): E {
		return cache[index - _startIndex]!!
	}

	/**
	 * Iterates over each unused item still in the cache.
	 */
	fun forEachUnused(callback: (renderer: E) -> Unit) {
		if (unusedSize == 0) return
		for (i in 0..cache.lastIndex) {
			val e = cache[i]
			if (e != null)
				callback(e)
		}
	}

	/**
	 * Clears the current cache values. Note that this does not clear the pool, nor does it move the current cache
	 * values into the pool.
	 * @see flip
	 */
	fun clear() {
		cache.clear()
		_startIndex = 0
		usedStartIndex = 0
		_unusedSize = 0
	}

	/**
	 * Sets the items returned via [obtain] to be used as the cached items for the next set.
	 */
	fun flip() {
		forEachUnused(pool::free)
		val tmp = cache
		cache = used
		_startIndex = usedStartIndex
		usedStartIndex = 0
		_unusedSize = cache.size
		used = tmp
		used.clear()
	}
}

/**
 * Disposes each instance in the cache and pool.
 */
fun <E : Disposable> IndexedCache<E>.disposeAndClear() {
	forEachUnused { it.dispose() }
	pool.disposeAndClear()
	clear()
}

/**
 * Removes all unused cache instances from the given container before flipping.
 */
fun <E : UiComponent> IndexedCache<E>.removeAndFlip(parent: ElementContainer<UiComponent>) {
	forEachUnused {
		parent.removeElement(it)
	}
	flip()
}

/**
 * Sets visible to false on all unused cache instances before flipping.
 */
fun <E : UiComponent> IndexedCache<E>.hideAndFlip() {
	forEachUnused {
		it.visible = false
	}
	flip()
}
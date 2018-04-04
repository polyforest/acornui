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

import com.acornui._assert
import com.acornui.collection.*
import com.acornui.component.ElementContainer
import com.acornui.component.UiComponent
import com.acornui.core.Disposable

/**
 * A layer between item reuse and an object pool that first seeks an item from the same index.
 * It is to reduce the amount of changes a pooled item may need to make.
 *
 * To use this class, make a series of [obtain] calls to grab recycled elements, call [forEach] to iterate over the
 * elements that weren't obtained, then call [flip] to send unused elements back to the object pool, and make the
 * obtained elements available for the next series.
 *
 * A set may look like this:
 *
 * ```
 * obtain(3)
 * obtain(4)
 * obtain(5)
 * obtain(2, highestFirst = false)
 * obtain(1, highestFirst = false)
 * forEach { ... } // Deactivate unused items
 * flip()
 * ```
 *
 * A set pulling in reverse order should look like this:
 * ```
 * obtain(5, highestFirst = false)
 * obtain(4, highestFirst = false)
 * obtain(3, highestFirst = false)
 * obtain(6, highestFirst = true)
 * obtain(7, highestFirst = true)
 * forEach { ... } // Deactivate unused items
 * flip()
 * ```
 */
class IndexedCache<E>(val pool: Pool<E>) {

	constructor(factory: ()->E) : this(ObjectPool(factory))

	private var _startIndex = 0
	val startIndex: Int
		get() = _startIndex

	private var _size = 0
	val size: Int
		get() = _size


	private var cache = ArrayList<E?>()
	private var used = ArrayList<E?>()
	private var usedStartIndex = Int.MAX_VALUE

	/**
	 * Obtain will provide a pooled instance in this order:
	 * 1) An item from the previous set with a matching index.
	 * 2) If highestFirst, the highest available index from the previous set, otherwise, the lowest.
	 * 3) If no items remain from the previous set, the provided [Pool] will be used.
	 *
	 * @param index Obtain must pull from the tail or the head. That is, [index] must be sequential from the first index
	 * obtained, where the head is the lowest index, and the tail is the highest index.
	 * Note that the indices are arbitrary and only used for matching purposes.
	 *
	 * Legal: 6, 7, 8, 5, 4, 3, 9, 10, 11
	 * Illegal: 6, 7, 8, 10
	 * Illegal: 6, 7, 8, 4
	 *
	 * @param highestFirst If true, and an item with a matching index cannot be found, the item with the highest index
	 * will be returned before the item with the lowest.
	 */
	fun obtain(index: Int, highestFirst: Boolean = true): E {
		val element: E
		if (_size == 0) {
			element = pool.obtain()
		} else {
			val e = cache.getOrNull(index - _startIndex)
			if (e != null) {
				element = e
				cache[index - _startIndex] = null
				_size--
			} else {
				val index2 = if (highestFirst) cache.indexOfLast2 { it != null } else cache.indexOfFirst2 { it != null }
				element = cache[index2]!!
				cache[index2] = null
				_size--
			}
		}
		if (index < usedStartIndex) {
			_assert(used.isEmpty() || index == usedStartIndex - 1)
			usedStartIndex = index
			used.add(0, element)
		} else {
			_assert(index == usedStartIndex + used.size)
			used.add(element)
		}
		return element
	}

	/**
	 * Returns the cached element at the given index, if it exists.
	 */
	fun getCached(index: Int): E? {
		return cache.getOrNull(index - _startIndex)
	}

	/**
	 * Iterates over each unused item still in the cache.
	 */
	fun forEach(callback: (renderer: E) -> Unit) {
		if (_size == 0) return
		for (i in 0..cache.lastIndex) {
			val element = cache[i]
			if (element != null)
				callback(element)
		}
	}

	/**
	 * Clears the current cache values. Note that this does not clear the pool, nor does it move the current cache
	 * values into the pool.
	 */
	fun clear() {
		cache.clear()
		_startIndex = usedStartIndex
		usedStartIndex = Int.MAX_VALUE
		_size = cache.size
	}

	/**
	 * Sets the items returned via [obtain] to be used as the cached items for the next set.
	 */
	fun flip() {
		forEach(pool::free)
		val tmp = cache
		cache = used
		_startIndex = usedStartIndex
		usedStartIndex = Int.MAX_VALUE
		_size = cache.size
		used = tmp
		used.clear()
	}
}

/**
 * Disposes each instance in the cache and pool.
 */
fun <E : Disposable> IndexedCache<E>.disposeAndClear() {
	forEach { it.dispose() }
	pool.disposeAndClear()
	clear()
}

/**
 * Removes all unused cache instances from the given container before flipping.
 */
fun <E : UiComponent> IndexedCache<E>.removeAndFlip(parent: ElementContainer<UiComponent>) {
	forEach {
		parent.removeElement(it)
	}
	flip()
}

/**
 * Sets visible to false on all unused cache instances before flipping.
 */
fun <E : UiComponent> IndexedCache<E>.hideAndFlip() {
	forEach {
		it.visible = false
	}
	flip()
}
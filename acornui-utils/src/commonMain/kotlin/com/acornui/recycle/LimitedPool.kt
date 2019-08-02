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

package com.acornui.recycle

import com.acornui.Disposable


/**
 * A Pool implementation that doesn't allow more than [size] instances to be created. Once
 * the capacity has been exceeded, active objects will be cleared and recycled.
 * @author nbilyk
 */
class LimitedPoolImpl<T : Clearable>(
		val size: Int,
		arrayFactory: (size: Int) -> Array<T?>,
		private val create: () -> T
) : Pool<T> {

	private var totalFreeObjects: Int = 0
	private val freeObjects: Array<T?> = arrayFactory(size)
	private var mostRecent: Int = -1
	private val activeObjects: Array<T?> = arrayFactory(size)

	/**
	 * Takes an object from the free objects if there is one. If there are no free objects, then if the active objects
	 * are not at capacity yet, a new object is constructed from the factory. If we are at capacity, this will take
	 * the oldest instance, reset it, and use that.
	 */
	override fun obtain(): T {
		mostRecent++
		mostRecent %= size
		val obj = if (totalFreeObjects > 0) {
			freeObjects[--totalFreeObjects]!!
		} else {
			val leastRecent = activeObjects[mostRecent]
			if (leastRecent == null) {
				create()
			} else {
				leastRecent.clear()
				leastRecent
			}
		}
		activeObjects[mostRecent] = obj
		return obj
	}

	/**
	 * Returns an object back to the pool.
	 */
	override fun free(obj: T) {
		obj.clear()
		freeObjects[totalFreeObjects++] = obj
	}

	override fun forEach(callback: (T) -> Unit) {
		for (i in 0..totalFreeObjects - 1) {
			callback(freeObjects[i]!!)
		}
	}

	override fun clear() {
		for (i in 0..totalFreeObjects - 1) {
			freeObjects[i] = null
		}
		for (i in 0..size - 1) {
			val obj = activeObjects[i] ?: continue
			(obj as? Disposable)?.dispose()
			activeObjects[i] = null
		}
		mostRecent = -1
		totalFreeObjects = 0
	}
}

inline fun <reified T : Clearable> limitedPool(size: Int, noinline create: () -> T): LimitedPoolImpl<T> {
	return LimitedPoolImpl(size, { arrayOfNulls(it) }, create)
}

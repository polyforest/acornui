/*
 * Copyright 2014 Nicholas Bilyk
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

import com.acornui.core.Disposable


/**
 * An interface for objects that can be reset to their original state.
 */
interface Clearable {

	/**
	 * Clears this instance back to its original state.
	 */
	fun clear()
}

interface Pool<T> {

	fun obtain(): T
	fun free(obj: T)

	fun freeAll(list: List<T>) {
		for (i in 0..list.lastIndex) {
			free(list[i])
		}
	}

	/**
	 * Iterates over each item in the pool, invoking the provided callback.
	 */
	fun forEach(callback: (T)->Unit)

	/**
	 * Clears the free objects from this Pool.
	 * If the objects are disposable, consider using [disposeAndClear]
	 */
	fun clear()
}

fun <T> MutableList<T>.freeTo(pool: Pool<T>) {
	pool.freeAll(this)
	clear()
}

@Deprecated("Use disposeAndClear")
fun <T : Disposable> Pool<T>.clear(dispose: Boolean) {
	forEach {
		it.dispose()
	}
	clear()
}

/**
 * Disposes each object in this pool, then clears them from this list.
 */
fun <T : Disposable> Pool<T>.disposeAndClear() {
	forEach {
		it.dispose()
	}
	clear()
}

open class ObjectPool<T>(initialCapacity: Int, private val create: () -> T) : Pool<T> {

	constructor(create: () -> T) : this(8, create)

	private val freeObjects = ArrayList<T>(initialCapacity)

	/**
	 * Takes an object from the pool if there is one, or constructs a new object from the factory provided
	 * to this Pool's constructor.
	 * */
	override fun obtain(): T {
		return if (freeObjects.isEmpty()) {
			create()
		} else {
			freeObjects.pop()
		}
	}

	/**
	 * Returns an object back to the pool.
	 */
	override fun free(obj: T) {
		freeObjects.add(obj)
	}

	override fun forEach(callback: (T)->Unit) {
		for (i in 0..freeObjects.lastIndex) {
			callback(freeObjects[i])
		}
	}

	override fun clear() {
		freeObjects.clear()
	}
}



/**
 * An ObjectPool implementation that resets the objects as they go back into the pool.
 * @author nbilyk
 */
open class ClearableObjectPool<T : Clearable>(initialCapacity: Int, create: () -> T) : ObjectPool<T>(initialCapacity, create) {

	constructor(create: () -> T) : this(8, create)

	override fun free(obj: T) {
		obj.clear()
		super.free(obj)
	}
}

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
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
import com.acornui.collection.forEach2
import com.acornui.collection.pop
import kotlin.jvm.Synchronized


interface Pool<T> {

	fun obtain(): T
	fun free(obj: T)

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

/**
 * Frees all items in the given list back to the pool.
 */
fun <T> Pool<T>.freeAll(list: List<T>) = list.forEach2(action = ::free)

/**
 * Frees all items in this list back to the given pool, then clears this list.
 */
fun <T> MutableList<T>.freeTo(pool: Pool<T>) {
	pool.freeAll(this)
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

/**
 * @param initialCapacity The initial array capacity for this pool. The array size will expand automatically, but
 * if this pool will have an extraordinarily high number of elements, setting this to that estimation may reduce the
 * number of resizes the backing array requires.
 * @param capacity The pool will never contain more than this many free objects.
 * @param create The factory method for producing new elements when this pool is empty.
 */
open class ObjectPool<T>(initialCapacity: Int, private val capacity: Int, private val create: () -> T) : Pool<T>, Disposable {

	constructor(create: () -> T) : this(8, 20000, create)

	private val freeObjects = ArrayList<T>(initialCapacity)

	/**
	 * Takes an object from the pool if there is one, or constructs a new object from the factory provided
	 * to this Pool's constructor.
	 * */
	@Synchronized
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
	@Synchronized
	override fun free(obj: T) {
		if (freeObjects.size >= capacity) return
		freeObjects.add(obj)
	}

	@Synchronized
	override fun forEach(callback: (T)->Unit) {
		for (i in 0..freeObjects.lastIndex) {
			callback(freeObjects[i])
		}
	}

	override fun clear() {
		freeObjects.clear()
	}

	override fun dispose() = clear()
}
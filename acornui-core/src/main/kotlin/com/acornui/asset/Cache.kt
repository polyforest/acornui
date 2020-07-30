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

@file:Suppress("MemberVisibilityCanBePrivate")

package com.acornui.asset

import com.acornui.Disposable
import com.acornui.di.Context
import com.acornui.di.ContextImpl
import com.acornui.di.dependencyFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.browser.window
import kotlin.jvm.Synchronized
import kotlin.time.Duration
import kotlin.time.seconds

/**
 * Cache is a map with reference counting. When the reference counter on a cached element reaches zero, if it remains
 * at zero for a certain number of frames, it will be removed and disposed.
 */
interface Cache {

	/**
	 * Returns true if the key is found.
	 */
	fun containsKey(key: Any): Boolean

	/**
	 * Retrieves the cache value for the given key.
	 * When cache items are retrieved, [refInc] should be used to indicate that it's in use.
	 * @return Returns the set value, or null if the key is not in this cache.
	 */
	operator fun <T : Any> get(key: Any): T?

	operator fun set(key: Any, value: Any)

	/**
	 * Retrieves the cache value for the given key if it exists. Otherwise, constructs a new value via the [factory]
	 * and adds the new value to the cache.
	 * @param key The key to use for the cache index.
	 * @return Returns the cached value.
	 */
	fun <T : Any> getOrPut(key: Any, factory: () -> T): T

	/**
	 * Decrements the use count for the cache value with the given key.
	 * If the use count reaches zero, and remains at zero for a certain number of frames, the cache value will be
	 * removed and disposed if the value implements [Disposable].
	 *
	 * @throws IllegalArgumentException if key is not in this cache.
	 */
	fun refDec(key: Any)

	/**
	 * Increments the reference count for the cache value. This should be paired with [refDec]
	 * @param key The cache key used in the lookup. If the key is not found, an exception is thrown.
	 *
	 * @see containsKey
	 * @throws IllegalArgumentException if key is not in this cache.
	 */
	fun refInc(key: Any)

	companion object : Context.Key<Cache> {

		override val factory = dependencyFactory {
			CacheImpl(it)
		}
	}
}

val Context.cache: Cache
	get() = inject(Cache)

class CacheImpl(

		owner: Context,

		/**
		 * The time before an unreferenced cache item is removed and destroyed.
		 */
		val disposalTime: Duration = 4.seconds
) : ContextImpl(owner), Cache, Disposable {

	private val map = mutableMapOf<Any, CacheValue>()

	@Synchronized
	override fun containsKey(key: Any): Boolean {
		checkDisposed()
		return map.containsKey(key)
	}

	@Synchronized
	override fun <T : Any> get(key: Any): T? {
		checkDisposed()
		@Suppress("UNCHECKED_CAST")
		return map[key]?.value as T?
	}

	@Synchronized
	override fun set(key: Any, value: Any) {
		checkDisposed()
		require(!containsKey(key)) { "Cache already contains key $key" }

		val cacheValue = CacheValue(key, value)
		map[key] = cacheValue

		// Start in the death pool, it will be removed on refInc.
		cacheValue.doom()
	}

	/**
	 * Retrieves the cache value for the given key if it exists. Otherwise, constructs a new value via the [factory]
	 * and adds the new value to the cache.
	 * @param key The key to use for the cache index.
	 * @return Returns the cached value.
	 */
	override fun <T : Any> getOrPut(key: Any, factory: () -> T): T {
		@Suppress("UNCHECKED_CAST")
		if (containsKey(key)) return get(key)!!
		val value = factory()
		set(key, value)
		return value
	}

	@Synchronized
	override fun refDec(key: Any) {
		if (isDisposed) return
		if (map.containsKey(key)) {
			val cacheValue = map[key]!!
			check(cacheValue.refCount > 0) { "refInc / refDec pairs are unbalanced." }
			--cacheValue.refCount
		}
	}

	@Synchronized
	override fun refInc(key: Any) {
		checkDisposed()
		val cacheValue = map[key] ?: throw IllegalArgumentException("The key $key is not in the cache.")
		cacheValue.refCount++
	}

	@Synchronized
	override fun dispose() {
		super.dispose()
		map.values.forEach(CacheValue::dispose)
	}

	private inner class CacheValue(
		val key: Any,
		val value: Any
	) : Disposable {

		var refCount: Int = 0
			set(value) {
				if (field <= 0 && value > 0)
					revive()
				if (field > 0 && value <= 0)
					doom()
				field = value
			}

		private var disposalId: Int = -1

		override fun dispose() {
			if (disposalId != -1)
				window.clearInterval(disposalId)
			(value as? Job)?.cancel()
			(value as? Disposable)?.dispose()
			map.remove(key)
		}

		fun doom() {
			disposalId = window.setInterval(::dispose, disposalTime.inMilliseconds.toInt())
		}

		fun revive() {
			window.clearInterval(disposalId)
			disposalId = -1
		}
	}

}

/**
 * CacheSet is a set of keys that are reference incremented on the [Cache] when added, and
 * reference decremented when this group is disposed.
 */
class CacheSet(
		owner: Context
) : ContextImpl(owner), Set<Any> {

	private val cache by Cache

	private val keys = mutableSetOf<Any>()

	override val size: Int
		get() = keys.size

	override fun contains(element: Any): Boolean = keys.contains(element)

	override fun iterator(): MutableIterator<Any> = keys.iterator()

	override fun containsAll(elements: Collection<Any>): Boolean = keys.containsAll(elements)

	override fun isEmpty(): Boolean = keys.isEmpty()

	/**
	 * Adds a key to be tracked.
	 * The key will be reference incremented on the [Cache].
	 * @return Returns `true` if the key has been added, `false` if the key is already contained in the set.
	 */
	private fun addKey(key: Any): Boolean {
		checkDisposed()
		if (keys.contains(key)) return false
		cache.refInc(key)
		keys.add(key)
		return true
	}

	/**
	 * Retrieves the cache value for the given key if it exists. Otherwise, constructs a new value via the [factory]
	 * and adds the new value to the cache.
	 * @param key The key to use for the cache index. If this key is not currently in this set, it will be added
	 * and [Cache.refInc] will be called. When this set is disposed, [Cache.refDec] will be called for the added key.
	 *
	 * @return Returns the cached value.
	 */
	fun <T : Any> getOrPut(key: Any, factory: () -> T): T {
		checkDisposed()
		val r = cache.getOrPut(key, factory)
		addKey(key)
		return r
	}

	@Deprecated("Use getOrPutAsync", ReplaceWith("getOrPutAsync(key, factory)"))
	fun <T> cacheAsync(key: Any, factory: suspend CoroutineScope.() -> T): Deferred<T> = getOrPutAsync(key, factory)

	/**
	 * Invokes [getOrPut] with an [async] coroutine using the scope in which this set was created.
	 */
	fun <T> getOrPutAsync(key: Any, factory: suspend CoroutineScope.() -> T): Deferred<T> = getOrPut(key) {
		async { factory() }
	}

	override fun dispose() {
		super.dispose()
		for (key in keys) {
			cache.refDec(key)
		}
		keys.clear()
	}
}

/**
 * Constructs a new [CacheSet] object which will increment or decrement a list of keys on the [Cache].
 */
fun Context.cacheSet(): CacheSet {
	return CacheSet(this)
}

@Deprecated("Use cacheSet", ReplaceWith("this.cacheSet()"))
fun Context.cachedGroup(): CacheSet {
	return CacheSet(this)
}
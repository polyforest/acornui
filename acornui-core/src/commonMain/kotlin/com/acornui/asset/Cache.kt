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

package com.acornui.asset

import com.acornui.Disposable
import com.acornui.collection.MutableListIteratorImpl
import com.acornui.collection.stringMapOf
import com.acornui.di.Context
import com.acornui.di.ContextImpl
import com.acornui.di.DKey
import com.acornui.recycle.Clearable
import com.acornui.time.tick
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.jvm.Synchronized

/**
 * Cache is a map with reference counting. When the reference counter on a cached element reaches zero, if it remains
 * at zero for a certain number of frames, it will be removed and disposed.
 */
interface Cache : Clearable {

	fun containsKey(key: String): Boolean

	/**
	 * Retrieves the cache value for the given key.
	 * When cache items are retrieved, [refInc] should be used to indicate that it's in use.
	 */
	operator fun <T : Any> get(key: String): T?

	operator fun <T : Any> set(key: String, value: T)

	/**
	 * Retrieves the cache value for the given key if it exists. Otherwise, constructs a new value via the [factory]
	 * and adds the new value to the cache.
	 * @param key The key to use for the cache index.
	 * @return Returns the cached value.
	 */
	fun <T : Any> getOrPut(key: String, factory: () -> T): T {
		if (containsKey(key)) return get(key)!!
		val value = factory()
		set(key, value)
		return value
	}

	/**
	 * Decrements the use count for the cache value with the given key.
	 * If the use count reaches zero, and remains at zero for a certain number of frames, the cache value will be
	 * removed and disposed if the value implements [Disposable].
	 */
	fun refDec(key: String)

	/**
	 * Increments the reference count for the cache value. This should be paired with [refDec]
	 * @param key The cache key used in the lookup. If the key is not found, an exception is thrown.
	 *
	 * @see containsKey
	 */
	fun refInc(key: String)

	companion object : DKey<Cache> {

		override fun factory(context: Context): Cache? = CacheImpl()
	}
}

/**
 * A key-value store that keeps track of references. When references reach zero, and stay at zero for a certain number
 * of frames, the cached asset will be disposed.
 */
class CacheImpl(

		/**
		 * The number of frames before an unreferenced cache item is removed and destroyed.
		 */
		private val gcFrames: Int = 500
) : Cache {

	private val cache = stringMapOf<CacheValue>()

	private val deathPool = ArrayList<String>()
	private val deathPoolIterator = MutableListIteratorImpl(deathPool)

	private val checkInterval = maxOf(1, gcFrames / 5)
	private var timerPending = checkInterval

	init {
		tick {
			if (--timerPending <= 0) {
				timerPending = checkInterval
				deathPoolIterator.clear()
				while (deathPoolIterator.hasNext()) {
					val cacheKey = deathPoolIterator.next()
					val cacheValue = cache[cacheKey]!!
					cacheValue.deathTimer -= checkInterval
					if (cacheValue.deathTimer < 0) {
						cache.remove(cacheKey)
						(cacheValue.value as? Job)?.cancel()
						(cacheValue.value as? Disposable)?.dispose()
						deathPoolIterator.remove()
					}
				}
			}
		}
	}

	@Synchronized
	override fun containsKey(key: String): Boolean {
		return cache.containsKey(key)
	}

	@Synchronized
	override fun <T : Any> get(key: String): T? {
		@Suppress("UNCHECKED_CAST")
		return cache[key]?.value as T?
	}

	@Synchronized
	override fun <T : Any> set(key: String, value: T) {
		(cache[key]?.value as? Disposable)?.dispose()
		val cacheValue = CacheValue(value, gcFrames)
		cache[key] = cacheValue
		// Start in the death pool, it will be removed on refInc.
		deathPool.add(key)
	}

	@Synchronized
	override fun refDec(key: String) {
		if (cache.containsKey(key)) {
			val cacheValue = cache[key]!!
			if (cacheValue.refCount <= 0)
				throw Exception("refInc / refDec pairs are unbalanced.")
			if (--cacheValue.refCount <= 0) {
				deathPool.add(key)
			}
		}
	}

	@Synchronized
	override fun refInc(key: String) {
		val cacheValue = cache[key] ?: throw Exception("The key $key is not in the cache.")
		if (cacheValue.refCount == 0) {
			// Revive from the death pool.
			cacheValue.deathTimer = gcFrames
			val success = deathPool.remove(key)
			if (!success)
				throw Exception("Could not find the key in the death pool.")
		}
		cacheValue.refCount++
	}

	@Synchronized
	override fun clear() {
		for (cacheValue in cache.values) {
			(cacheValue.value as? Disposable)?.dispose()
		}
		cache.clear()
	}
}

private class CacheValue(
		var value: Any,
		var deathTimer: Int
) {
	var refCount: Int = 0
}

/**
 * A caching group tracks a list of keys to refDec when the group is disposed.
 */
class CachedGroup(

		owner: Context

) : ContextImpl(owner) {

	private val cache: Cache = inject(Cache)

	private val keys = ArrayList<String>()

	/**
	 * Adds a key to be tracked.
	 */
	fun add(key: String) {
		checkDisposed()
		if (keys.contains(key)) return
		cache.refInc(key)
		keys.add(key)
	}

	/**
	 * Gets if exists or creates a deferred value for this group's [CachedGroup.cache].
	 * Using the [GlobalScope] and provided [context], creates a [Deferred] object as the cache value for
	 * the given [key].
	 */
	fun <T : Any> cacheAsync(key: String, context: CoroutineContext = Dispatchers.Default, factory: suspend () -> T): Deferred<T> {
		checkDisposed()
		val r = cache.getOrPut(key) {
			async(context) {
				factory()
			}
		}
		add(key)
		return r
	}

	fun <T : Any> cache(key: String, factory: () -> T): T {
		checkDisposed()
		val r = cache.getOrPut(key, factory)
		add(key)
		return r
	}

	override fun dispose() {
		super.dispose()
		for (i in 0..keys.lastIndex) {
			cache.refDec(keys[i])
		}
		keys.clear()
	}

}

/**
 * Constructs a new [CachedGroup] object which will increment or decrement a list of keys on the [Cache].
 */
fun Context.cachedGroup(): CachedGroup {
	return CachedGroup(this)
}
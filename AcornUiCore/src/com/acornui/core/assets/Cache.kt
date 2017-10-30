package com.acornui.core.assets

import com.acornui.collection.MutableListIteratorImpl
import com.acornui.core.Disposable
import com.acornui.core.di.DKey
import com.acornui.core.di.Injector
import com.acornui.core.di.Scoped
import com.acornui.core.di.inject
import com.acornui.core.time.TimeDriver
import com.acornui.core.time.enterFrame


interface Cache : Disposable {

	fun containsKey(key: CacheKey<*>): Boolean

	/**
	 * Retrieves the cache value for the given key.
	 * When cache items are retrieved, [refInc] should be used to indicate that it's in use.
	 */
	operator fun <T : Any> get(key: CacheKey<T>): T?

	operator fun <T : Any> set(key: CacheKey<T>, value: T)

	/**
	 * Retrieves the cache value for the given key if it exists. Otherwise, constructs a new value via the [factory]
	 * and adds the new value to the cache.
	 * @param key The key to use for the cache index.
	 * @return Returns the cached value.
	 */
	fun <T : Any> getOr(key: CacheKey<T>, factory: () -> T): T {
		if (containsKey(key)) return get(key)!!
		val value = factory()
		set(key, value)
		return value
	}

	/**
	 * Decrements the use count for the cache value with the given key.
	 * If the use count reaches zero, the cache value will be removed and if it's [Disposable], disposed.
	 */
	fun refDec(key: CacheKey<*>)

	/**
	 * Increments the reference count for the cache value. This should be paired with [refDec]
	 * @param key The cache key used in the lookup. If the key is not found, an exception is thrown.
	 *
	 * @see containsKey
	 */
	fun refInc(key: CacheKey<*>)

	companion object : DKey<Cache> {
		override fun factory(injector: Injector): Cache? {
			return CacheImpl(injector.inject(TimeDriver))
		}
	}
}

/**
 * A key-value store that keeps track of references. When references reach zero, the cached asset will be disposed
 * in the future.
 */
class CacheImpl(
		timeDriver: TimeDriver,

		/**
		 * The number of frames before an unreferenced cache item is removed and destroyed.
		 */
		private val gcFrames: Int = 500) : Cache {

	private val cache = HashMap<CacheKey<*>, CacheValue>()

	private val deathPool = ArrayList<CacheKey<*>>()
	private val deathPoolIterator = MutableListIteratorImpl(deathPool)

	private val checkInterval = maxOf(1, gcFrames / 5)
	private var timerPending = checkInterval

	init {
		enterFrame(timeDriver) {
			if (--timerPending <= 0) {
				timerPending = checkInterval
				deathPoolIterator.clear()
				while (deathPoolIterator.hasNext()) {
					val cacheKey = deathPoolIterator.next()
					val cacheValue = cache[cacheKey]!!
					cacheValue.deathTimer -= checkInterval
					if (cacheValue.deathTimer < 0) {
						cache.remove(cacheKey)
						(cacheValue.value as? Disposable)?.dispose()
						deathPoolIterator.remove()
					}
				}
			}
		}
	}

	override fun containsKey(key: CacheKey<*>): Boolean {
		return cache.containsKey(key)
	}

	override fun <T : Any> get(key: CacheKey<T>): T? {
		@Suppress("UNCHECKED_CAST")
		return cache[key]?.value as T?
	}

	override fun <T : Any> set(key: CacheKey<T>, value: T) {
		(cache[key]?.value as? Disposable)?.dispose()
		val cacheValue = CacheValue(value, gcFrames)
		cache[key] = cacheValue
		// Start in the death pool, it will be removed on refInc.
		deathPool.add(key)
	}

	override fun refDec(key: CacheKey<*>) {
		if (cache.containsKey(key)) {
			val cacheValue = cache[key]!!
			if (cacheValue.refCount <= 0)
				throw Exception("refInc / refDec pairs are unbalanced.")
			if (--cacheValue.refCount <= 0) {
				deathPool.add(key)
			}
		}
	}

	override fun refInc(key: CacheKey<*>) {
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

	override fun dispose() {
		for (cacheValue in cache.values) {
			(cacheValue.value as? Disposable)?.dispose()
		}
		cache.clear()
	}

}

@Suppress("unused")
interface CacheKey<T>

private class CacheValue(
		var value: Any,
		var deathTimer: Int
) {
	var refCount: Int = 0
}

interface CachedGroup : Disposable {

	/**
	 * The cache instance this group uses.
	 */
	val cache: Cache

	/**
	 * Adds a key to be tracked.
	 */
	fun add(key: CacheKey<*>)
}

/**
 * A caching group tracks a list of keys to refDec when the group is disposed.
 */
class CachedGroupImpl(
		override val cache: Cache
) : CachedGroup {

	private var isDisposed = false

	private val keys = ArrayList<CacheKey<*>>()

	override fun add(key: CacheKey<*>) {
		if (isDisposed) {
			// This group has been disposed, immediately refDec the key.
			cache.refInc(key)
			cache.refDec(key)
		} else {
			cache.refInc(key)
			keys.add(key)
		}
	}

	override fun dispose() {
		if (isDisposed) throw Exception("Already disposed")
		isDisposed = true
		for (i in 0..keys.lastIndex) {
			cache.refDec(keys[i])
		}
		keys.clear()
	}
}

/**
 * Constructs a new [CachedGroup] object with the current scope's [Cache] manager.
 */
fun Scoped.cachedGroup(): CachedGroup {
	return CachedGroupImpl(inject(Cache))
}
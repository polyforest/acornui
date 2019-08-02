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

import com.acornui.action.Decorator
import com.acornui.action.noopDecorator
import com.acornui.async.*
import com.acornui.Disposable
import com.acornui.di.Scoped
import com.acornui.di.inject

/**
 * Loads the given file and caches the result.
 * Subsequent calls to [loadAndCache] with the same [path] and [type] will return the same [Deferred] instance.
 */
fun <T> Scoped.loadAndCache(path: String, type: AssetType<T>, group: CachedGroup): Deferred<T> {
	return loadAndCache(inject(AssetManager), path, type, noopDecorator(), group)
}

/**
 * Loads the given file, applies a decorator to the results, and caches the final value.
 * Subsequent calls to [loadAndCache] with the same [path], [type], and [decorator] will return the same [Deferred] instance.
 */
fun <T, R> Scoped.loadAndCache(path: String, type: AssetType<T>, decorator: Decorator<T, R>, group: CachedGroup): Deferred<R> {
	return loadAndCache(inject(AssetManager), path, type, decorator, group)
}

/**
 * Loads the given file, applies a decorator to the results, and caches the final value.
 * Subsequent calls to [loadAndCache] with the same [path], [type], and [decorator] will return the same [Deferred] instance.
 */
fun <T, R> loadAndCache(assetManager: AssetManager, path: String, type: AssetType<T>, decorator: Decorator<T, R>, group: CachedGroup): Deferred<R> {
	val key = AssetDecoratorCacheKey(path, type, decorator)
	val value = group.cache.getOr(key) { DecoratedCacheValue(assetManager.load(path, type), decorator) }
	group.add(key)
	return value.task
}

@Suppress("EqualsOrHashCode")
private class AssetDecoratorCacheKey<T, R>(
		val path: String,
		val type: AssetType<T>,
		val decorator: Decorator<T, R>
) : CacheKey<DecoratedCacheValue<T, R>> {

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		return _hashCode == other?.hashCode()
	}

	private val _hashCode: Int = run {
		var result = path.hashCode()
		result = 31 * result + type.hashCode()
		31 * result + decorator.hashCode()
	}

	override fun hashCode(): Int {
		return _hashCode
	}
}

private class DecoratedCacheValue<T, out R>(
		private val target: CancelableDeferred<T>,
		private val decorator: Decorator<T, R>
) : Disposable {

	val task: Deferred<R> = async {
		decorator.decorate(target.await())
	}

	override fun dispose() {
		target.cancel()
		launch {
			(target.awaitOrNull() as? Disposable)?.dispose()
			(task.awaitOrNull() as? Disposable)?.dispose()
		}
	}
}

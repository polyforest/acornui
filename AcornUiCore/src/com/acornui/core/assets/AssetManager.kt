/*
 * Copyright 2015 Nicholas Bilyk
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

package com.acornui.core.assets

import com.acornui.action.Progress
import com.acornui.async.awaitAll
import com.acornui.async.delay
import com.acornui.async.launch
import com.acornui.collection.sumByFloat2
import com.acornui.core.Disposable
import com.acornui.core.di.DKey
import com.acornui.core.di.Scoped
import com.acornui.core.di.inject
import com.acornui.signal.Signal

/**
 * @author nbilyk
 */
interface AssetManager : Disposable, Progress {

	/**
	 * Sets the factory for asset loaders associated with the specified asset type.
	 */
	fun <T> setLoaderFactory(type: AssetType<T>, factory: LoaderFactory<T>)

	/**
	 * Dispatched when the current loaders list has changed.
	 */
	val currentLoadersChanged: Signal<() -> Unit>

	val currentLoaders: List<AssetLoaderRo<*>>

	/**
	 * Loads an asset.
	 * To cancel the loading, call `cancel` on the return value.
	 * To await the result, create a coroutine, and call 'await'.
	 *
	 * ```
	 * launch {
	 *    val a = load("foo.txt", AssetTypes.TEXT)
	 *    val b = load("bar.png", AssetTypes.TEXTURE)
	 *    val str = a.await()
	 *    val texture = b.await()
	 * }
	 *
	 * ```
	 *
	 * @param path The location of the asset.
	 * @param type The type of asset the path represents. For common asset types, see [AssetTypes]
	 * @see AssetTypes
	 */
	fun <T> load(path: String, type: AssetType<T>): AssetLoader<T>

	override val secondsLoaded: Float
		get() {
			return currentLoaders.sumByFloat2 { it.secondsLoaded }
		}

	override val secondsTotal: Float
		get() {
			return currentLoaders.sumByFloat2 { it.secondsTotal }
		}

	companion object : DKey<AssetManager>

}

val AssetManager.secondsRemaining: Float
	get() = secondsTotal - secondsLoaded


fun AssetManager.onLoadersEmpty(callback: () -> Unit) {
	launch {
		currentLoaders.awaitAll()
		callback()
	}
}

/**
 * Uses the scope's asset manager to load the given resource.
 */
fun <T> Scoped.load(path: String, type: AssetType<T>): AssetLoaderRo<T> {
	return inject(AssetManager).load(path, type)
}

/**
 * Waits until there are no currently active loaders.
 */
suspend fun AssetManager.awaitAll() {
	while (currentLoaders.isNotEmpty()) {
		currentLoaders.awaitAll()
		delay(1L)
	}
}
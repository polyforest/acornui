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

import com.acornui.async.awaitOrNull
import com.acornui.async.launch
import com.acornui.browser.appendParam
import com.acornui.core.io.file.Files
import com.acornui.signal.Signal
import com.acornui.signal.Signal0


/**
 * @author nbilyk
 */
class AssetManagerImpl(

		/**
		 * All relative files will be prepended with this string.
		 */
		private val rootPath: String = "",

		private val files: Files,

		private val loaderFactories: Map<AssetType<*>, LoaderFactory<*>>,

		/**
		 * If true, a version number will be appended to file requests for relative files.
		 */
		private val appendVersion: Boolean = false
) : AssetManager {

	private val _currentLoadersChanged = Signal0()
	override val currentLoadersChanged: Signal<() -> Unit>
		get() = _currentLoadersChanged

	private val _currentLoaders = ArrayList<AssetLoader<*>>()
	override val currentLoaders: List<AssetLoaderRo<*>>
		get() = _currentLoaders

	@Suppress("UNCHECKED_CAST")
	override fun <T> load(path: String, type: AssetType<T>): AssetLoader<T> {
		// Check if we can determine the estimated size by the manifest
		val file = files.getFile(path)
		val finalPath = if (file == null) path else rootPath + if (appendVersion) path.appendParam("version", file.modified.toString()) else path
		val estimatedBytesTotal = file?.size?.toInt() ?: 0

		val factory = loaderFactories[type] ?: return FailedLoader(type, path)
		val loader = factory(finalPath, estimatedBytesTotal) as AssetLoader<T>
		_currentLoaders.add(loader)
		_currentLoadersChanged.dispatch()
		launch {
			// Remove the loader when it's finished.
			loader.awaitOrNull()
			_currentLoaders.remove(loader)
			_currentLoadersChanged.dispatch()
		}
		return loader
	}

	private fun abortAll() {
		for (i in 0.._currentLoaders.lastIndex) {
			_currentLoaders[i].cancel()
		}
		_currentLoaders.clear()
		_currentLoadersChanged.dispatch()
	}

	override fun dispose() {
		abortAll()
		_currentLoadersChanged.dispose()
	}
}

private class FailedLoader<T>(
		override val type: AssetType<T>,
		override val path: String
) : AssetLoader<T> {

	override val estimatedBytesTotal: Int = 0

	override val secondsLoaded: Float = 0f
	override val secondsTotal: Float = 0f

	override fun cancel() {}

	suspend override fun await(): T {
		throw AssetLoadingException(path, type)
	}
}
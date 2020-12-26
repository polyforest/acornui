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

import com.acornui.async.progressReporter
import com.acornui.audio.*
import com.acornui.config
import com.acornui.di.Context
import com.acornui.di.contextKey
import com.acornui.di.dependencyFactory
import com.acornui.io.*
import org.khronos.webgl.ArrayBuffer
import kotlin.time.seconds

object Loaders {
	val textLoader = contextKey<Loader<String>> {
		TextLoader(it.defaultRequestSettings)
	}

	val binaryLoader = contextKey<Loader<ArrayBuffer>> {
		BinaryLoader(it.defaultRequestSettings)
	}

	val musicLoader = contextKey<Loader<Music>> {
		val audioManager = it.inject(AudioManager)
		object : Loader<Music> {
			override val requestSettings: RequestSettings =
				it.defaultRequestSettings.copy(initialTimeEstimate = 0.seconds) // Audio element is immediately returned.

			override suspend fun load(requestData: UrlRequestData, settings: RequestSettings): Music {
				return JsAudioElementMusic(audioManager, audio(requestData.toUrlStr(settings.rootPath)))
			}
		}
	}

	val soundLoader = contextKey<Loader<SoundFactory>> {
		val audioContextSupported = audioContextSupported
		val audioManager = it.inject(AudioManager)
		object : Loader<SoundFactory> {
			override val requestSettings: RequestSettings =
				it.defaultRequestSettings.copy(initialTimeEstimate = Bandwidth.downBpsInv.seconds * 100_000)

			override suspend fun load(requestData: UrlRequestData, settings: RequestSettings): SoundFactory {
				return if (audioContextSupported) {
					loadAudioSound(audioManager, requestData, settings)
				} else {
					loadAudioElement(audioManager, requestData, settings)
				}
			}
		}
	}
}

val defaultRequestSettingsKey = object : Context.Key<RequestSettings> {
	override val factory = dependencyFactory {
		RequestSettings(it.config.rootPath, it.progressReporter)
	}
}

val Context.defaultRequestSettings: RequestSettings
	get() = inject(defaultRequestSettingsKey)


suspend fun <T> Loader<T>.load(url: String, settings: RequestSettings): T =
		load(url.toUrlRequestData(), settings)

/**
 * Requests a [String] resource.
 */
suspend fun Context.loadText(
		requestData: UrlRequestData,
		settings: RequestSettings = inject(Loaders.textLoader).requestSettings
): String = inject(Loaders.textLoader).load(requestData, settings)

/**
 * Requests a [String] resource.
 */
suspend fun Context.loadText(
		path: String,
		settings: RequestSettings = inject(Loaders.textLoader).requestSettings
): String = loadText(path.toUrlRequestData(), settings)

/**
 * Loads and caches a [String] resource.
 */
suspend fun Context.loadAndCacheText(
		requestData: UrlRequestData,
		settings: RequestSettings = inject(Loaders.textLoader).requestSettings,
		cacheSet: CacheSet = cacheSet()
): String {
	return cacheSet.getOrPutAsync(requestData) {
		loadText(requestData, settings)
	}.await()
}

/**
 * Requests a [ArrayBuffer] resource.
 */
suspend fun Context.loadBinary(
		requestData: UrlRequestData,
		settings: RequestSettings = inject(Loaders.binaryLoader).requestSettings
): ArrayBuffer {
	return inject(Loaders.binaryLoader).load(requestData, settings)
}

/**
 * Requests a [ArrayBuffer] resource.
 */
suspend fun Context.loadBinary(
		path: String,
		settings: RequestSettings = inject(Loaders.binaryLoader).requestSettings
): ArrayBuffer = loadBinary(path.toUrlRequestData(), settings)

/**
 * Loads and caches an [ArrayBuffer] resource.
 */
suspend fun Context.loadAndCacheBinary(
		requestData: UrlRequestData,
		settings: RequestSettings = inject(Loaders.binaryLoader).requestSettings,
		cacheSet: CacheSet = cacheSet()
): ArrayBuffer {
	return cacheSet.getOrPutAsync(requestData) {
		loadBinary(requestData, settings)
	}.await()
}

/**
 * Requests a [Music] resource.
 */
suspend fun Context.loadMusic(
		requestData: UrlRequestData,
		settings: RequestSettings = inject(Loaders.musicLoader).requestSettings
): Music = inject(Loaders.musicLoader).load(requestData, settings)

/**
 * Requests a [Music] resource.
 */
suspend fun Context.loadMusic(
		path: String,
		settings: RequestSettings = inject(Loaders.musicLoader).requestSettings
): Music = loadMusic(path.toUrlRequestData(), settings)

/**
 * Requests a [SoundFactory] resource.
 */
suspend fun Context.loadSound(
		requestData: UrlRequestData,
		settings: RequestSettings = inject(Loaders.soundLoader).requestSettings
): SoundFactory = inject(Loaders.soundLoader).load(requestData, settings)

/**
 * Requests a [SoundFactory] resource.
 */
suspend fun Context.loadSound(
		path: String,
		settings: RequestSettings = inject(Loaders.soundLoader).requestSettings
): SoundFactory = loadSound(path.toUrlRequestData(), settings)
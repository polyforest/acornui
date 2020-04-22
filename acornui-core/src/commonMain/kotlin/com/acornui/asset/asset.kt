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

import com.acornui.audio.Music
import com.acornui.audio.SoundFactory
import com.acornui.di.Context
import com.acornui.di.contextKey
import com.acornui.graphic.RgbData
import com.acornui.graphic.Texture
import com.acornui.io.*

object Loaders {
	val textLoader = contextKey<Loader<String>>()
	val binaryLoader = contextKey<Loader<ReadByteBuffer>>()
	val textureLoader = contextKey<Loader<Texture>>()
	val rgbDataLoader = contextKey<Loader<RgbData>>()
	val musicLoader = contextKey<Loader<Music>>()
	val soundLoader = contextKey<Loader<SoundFactory>>()
}

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
 * Requests a [ReadByteBuffer] resource.
 */
suspend fun Context.loadBinary(
		requestData: UrlRequestData,
		settings: RequestSettings = inject(Loaders.binaryLoader).requestSettings
): ReadByteBuffer {
	return inject(Loaders.binaryLoader).load(requestData, settings)
}

/**
 * Requests a [ReadByteBuffer] resource.
 */
suspend fun Context.loadBinary(
		path: String,
		settings: RequestSettings = inject(Loaders.binaryLoader).requestSettings
): ReadByteBuffer = loadBinary(path.toUrlRequestData(), settings)

/**
 * Loads and caches a [ReadByteBuffer] resource.
 */
suspend fun Context.loadAndCacheBinary(
		requestData: UrlRequestData,
		settings: RequestSettings = inject(Loaders.binaryLoader).requestSettings,
		cacheSet: CacheSet = cacheSet()
): ReadByteBuffer {
	return cacheSet.getOrPutAsync(requestData) {
		loadBinary(requestData, settings)
	}.await()
}

/**
 * Requests a [Texture] resource.
 */
suspend fun Context.loadTexture(
		requestData: UrlRequestData,
		settings: RequestSettings = inject(Loaders.textureLoader).requestSettings
): Texture {
	return inject(Loaders.textureLoader).load(requestData, settings)
}

/**
 * Loads and caches a [Texture] resource.
 */
suspend fun Context.loadAndCacheTexture(
		requestData: UrlRequestData,
		settings: RequestSettings = inject(Loaders.textureLoader).requestSettings,
		cacheSet: CacheSet = cacheSet()
): Texture {
	return cacheSet.getOrPutAsync(requestData) {
		loadTexture(requestData, settings)
	}.await()
}

/**
 * Requests a [Texture] resource.
 */
suspend fun Context.loadTexture(
		path: String,
		settings: RequestSettings = inject(Loaders.textureLoader).requestSettings
): Texture = loadTexture(path.toUrlRequestData(), settings)

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
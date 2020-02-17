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
import com.acornui.di.dKey
import com.acornui.graphic.RgbData
import com.acornui.graphic.Texture
import com.acornui.io.*
import kotlin.time.Duration

object Loaders {
	val textLoader = dKey<Loader<String>>()
	val binaryLoader = dKey<Loader<ReadByteBuffer>>()
	val textureLoader = dKey<Loader<Texture>>()
	val rgbDataLoader = dKey<Loader<RgbData>>()
	val musicLoader = dKey<Loader<Music>>()
	val soundLoader = dKey<Loader<SoundFactory>>()
}

suspend fun <T> Loader<T>.load(url: String,
							   progressReporter: ProgressReporter = GlobalProgressReporter,
							   initialTimeEstimate: Duration = defaultInitialTimeEstimate,
							   connectTimeout: Duration = defaultConnectTimeout
): T = load(url.toUrlRequestData(), progressReporter, initialTimeEstimate, connectTimeout)

/**
 * Requests a [String] resource.
 */
suspend fun Context.loadText(
		requestData: UrlRequestData,
		progressReporter: ProgressReporter = GlobalProgressReporter,
		initialTimeEstimate: Duration = inject(Loaders.textureLoader).defaultInitialTimeEstimate,
		connectTimeout: Duration = inject(Loaders.textureLoader).defaultConnectTimeout
): String {
	return inject(Loaders.textLoader).load(requestData, progressReporter, initialTimeEstimate, connectTimeout)
}

/**
 * Requests a [String] resource.
 */
suspend fun Context.loadText(
		path: String,
		progressReporter: ProgressReporter = GlobalProgressReporter,
		initialTimeEstimate: Duration = inject(Loaders.textLoader).defaultInitialTimeEstimate,
		connectTimeout: Duration = inject(Loaders.textLoader).defaultConnectTimeout
): String {
	return inject(Loaders.textLoader).load(path, progressReporter, initialTimeEstimate, connectTimeout)
}

/**
 * Requests a [ReadByteBuffer] resource.
 */
suspend fun Context.loadBinary(
		requestData: UrlRequestData,
		progressReporter: ProgressReporter = GlobalProgressReporter,
		initialTimeEstimate: Duration = inject(Loaders.binaryLoader).defaultInitialTimeEstimate,
		connectTimeout: Duration = inject(Loaders.binaryLoader).defaultConnectTimeout
): ReadByteBuffer {
	return inject(Loaders.binaryLoader).load(requestData, progressReporter, initialTimeEstimate, connectTimeout)
}

/**
 * Requests a [ReadByteBuffer] resource.
 */
suspend fun Context.loadBinary(
		path: String,
		progressReporter: ProgressReporter = GlobalProgressReporter,
		initialTimeEstimate: Duration = inject(Loaders.binaryLoader).defaultInitialTimeEstimate,
		connectTimeout: Duration = inject(Loaders.binaryLoader).defaultConnectTimeout
): ReadByteBuffer {
	return inject(Loaders.binaryLoader).load(path, progressReporter, initialTimeEstimate, connectTimeout)
}

/**
 * Requests a [Texture] resource.
 */
suspend fun Context.loadTexture(
		requestData: UrlRequestData,
		progressReporter: ProgressReporter = GlobalProgressReporter,
		initialTimeEstimate: Duration = inject(Loaders.textureLoader).defaultInitialTimeEstimate,
		connectTimeout: Duration = inject(Loaders.textureLoader).defaultConnectTimeout
): Texture {
	return inject(Loaders.textureLoader).load(requestData, progressReporter, initialTimeEstimate, connectTimeout)
}

/**
 * Requests a [Texture] resource.
 */
suspend fun Context.loadTexture(
		path: String,
		progressReporter: ProgressReporter = GlobalProgressReporter,
		initialTimeEstimate: Duration = inject(Loaders.textureLoader).defaultInitialTimeEstimate
): Texture = inject(Loaders.textureLoader).load(path, progressReporter, initialTimeEstimate)

/**
 * Requests a [Music] resource.
 */
suspend fun Context.loadMusic(
		requestData: UrlRequestData,
		progressReporter: ProgressReporter = GlobalProgressReporter,
		initialTimeEstimate: Duration = inject(Loaders.musicLoader).defaultInitialTimeEstimate,
		connectTimeout: Duration = inject(Loaders.musicLoader).defaultConnectTimeout
): Music {
	return inject(Loaders.musicLoader).load(requestData, progressReporter, initialTimeEstimate, connectTimeout)
}

/**
 * Requests a [Music] resource.
 */
suspend fun Context.loadMusic(
		path: String,
		progressReporter: ProgressReporter = GlobalProgressReporter,
		initialTimeEstimate: Duration = inject(Loaders.musicLoader).defaultInitialTimeEstimate,
		connectTimeout: Duration = inject(Loaders.musicLoader).defaultConnectTimeout
): Music {
	return inject(Loaders.musicLoader).load(path, progressReporter, initialTimeEstimate, connectTimeout)
}

/**
 * Requests a [SoundFactory] resource.
 */
suspend fun Context.loadSound(
		requestData: UrlRequestData,
		progressReporter: ProgressReporter = GlobalProgressReporter,
		initialTimeEstimate: Duration = inject(Loaders.soundLoader).defaultInitialTimeEstimate,
		connectTimeout: Duration = inject(Loaders.soundLoader).defaultConnectTimeout
): SoundFactory {
	return inject(Loaders.soundLoader).load(requestData, progressReporter, initialTimeEstimate, connectTimeout)
}

/**
 * Requests a [SoundFactory] resource.
 */
suspend fun Context.loadSound(
		path: String,
		progressReporter: ProgressReporter = GlobalProgressReporter,
		initialTimeEstimate: Duration = inject(Loaders.soundLoader).defaultInitialTimeEstimate,
		connectTimeout: Duration = inject(Loaders.soundLoader).defaultConnectTimeout
): SoundFactory {
	return inject(Loaders.soundLoader).load(path, progressReporter, initialTimeEstimate, connectTimeout)
}
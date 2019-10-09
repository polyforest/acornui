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
import com.acornui.di.*
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
				 initialTimeEstimate: Duration = defaultInitialTimeEstimate
): T = load(url.toUrlRequestData(), progressReporter, initialTimeEstimate)

/**
 * Requests a [String] resource.
 */
suspend fun Scoped.loadText(
		requestData: UrlRequestData,
		progressReporter: ProgressReporter = GlobalProgressReporter,
		initialTimeEstimate: Duration = inject(Loaders.textureLoader).defaultInitialTimeEstimate
): String {
	return inject(Loaders.textLoader).load(requestData, progressReporter, initialTimeEstimate)
}

/**
 * Requests a [String] resource.
 */
suspend fun Scoped.loadText(
		path: String,
		progressReporter: ProgressReporter = GlobalProgressReporter,
		initialTimeEstimate: Duration = inject(Loaders.textLoader).defaultInitialTimeEstimate
): String {
	return inject(Loaders.textLoader).load(path, progressReporter, initialTimeEstimate)
}

/**
 * Requests a [ReadByteBuffer] resource.
 */
suspend fun Scoped.loadBinary(
		requestData: UrlRequestData,
		progressReporter: ProgressReporter = GlobalProgressReporter,
		initialTimeEstimate: Duration = inject(Loaders.binaryLoader).defaultInitialTimeEstimate
): ReadByteBuffer {
	return inject(Loaders.binaryLoader).load(requestData, progressReporter, initialTimeEstimate)
}

/**
 * Requests a [ReadByteBuffer] resource.
 */
suspend fun Scoped.loadBinary(
		path: String,
		progressReporter: ProgressReporter = GlobalProgressReporter,
		initialTimeEstimate: Duration = inject(Loaders.binaryLoader).defaultInitialTimeEstimate
): ReadByteBuffer {
	return inject(Loaders.binaryLoader).load(path, progressReporter, initialTimeEstimate)
}

/**
 * Requests a [Texture] resource.
 */
suspend fun Scoped.loadTexture(
		requestData: UrlRequestData,
		progressReporter: ProgressReporter = GlobalProgressReporter,
		initialTimeEstimate: Duration = inject(Loaders.textureLoader).defaultInitialTimeEstimate
): Texture {
	return inject(Loaders.textureLoader).load(requestData, progressReporter, initialTimeEstimate)
}

/**
 * Requests a [Texture] resource.
 */
suspend fun Scoped.loadTexture(
		path: String,
		progressReporter: ProgressReporter = GlobalProgressReporter,
		initialTimeEstimate: Duration = inject(Loaders.textureLoader).defaultInitialTimeEstimate
): Texture = inject(Loaders.textureLoader).load(path, progressReporter, initialTimeEstimate)

/**
 * Requests a [Music] resource.
 */
suspend fun Scoped.loadMusic(
		requestData: UrlRequestData,
		progressReporter: ProgressReporter = GlobalProgressReporter,
		initialTimeEstimate: Duration = inject(Loaders.musicLoader).defaultInitialTimeEstimate
): Music {
	return inject(Loaders.musicLoader).load(requestData, progressReporter, initialTimeEstimate)
}

/**
 * Requests a [Music] resource.
 */
suspend fun Scoped.loadMusic(
		path: String,
		progressReporter: ProgressReporter = GlobalProgressReporter,
		initialTimeEstimate: Duration = inject(Loaders.musicLoader).defaultInitialTimeEstimate
): Music {
	return inject(Loaders.musicLoader).load(path, progressReporter, initialTimeEstimate)
}

/**
 * Requests a [SoundFactory] resource.
 */
suspend fun Scoped.loadSound(
		requestData: UrlRequestData,
		progressReporter: ProgressReporter = GlobalProgressReporter,
		initialTimeEstimate: Duration = inject(Loaders.soundLoader).defaultInitialTimeEstimate
): SoundFactory {
	return inject(Loaders.soundLoader).load(requestData, progressReporter, initialTimeEstimate)
}

/**
 * Requests a [SoundFactory] resource.
 */
suspend fun Scoped.loadSound(
		path: String,
		progressReporter: ProgressReporter = GlobalProgressReporter,
		initialTimeEstimate: Duration = inject(Loaders.soundLoader).defaultInitialTimeEstimate
): SoundFactory {
	return inject(Loaders.soundLoader).load(path, progressReporter, initialTimeEstimate)
}
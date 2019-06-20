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

package com.acornui.js.audio

import com.acornui.async.Deferred
import com.acornui.async.async
import com.acornui.core.asset.AssetLoader
import com.acornui.core.asset.AssetType
import com.acornui.core.audio.AudioManager
import com.acornui.core.audio.SoundFactory
import com.acornui.request.JsArrayBufferRequest
import com.acornui.request.UrlRequestData

/**
 * An asset loader for js AudioContext sounds.
 * Does not work in IE.
 *
 * @author nbilyk
 */
class JsWebAudioSoundLoader(
		override val path: String = "",
		private val audioManager: AudioManager
) : AssetLoader<SoundFactory> {

	override val type: AssetType<SoundFactory> = AssetType.SOUND


	private val fileLoader = JsArrayBufferRequest(requestData = UrlRequestData(url = path))

	override val secondsLoaded: Float
		get() = fileLoader.secondsLoaded

	override val secondsTotal: Float
		get() = fileLoader.secondsTotal

	private var work: Deferred<JsWebAudioSoundFactory>

	init {
		if (!audioContextSupported) {
			throw Exception("Audio not supported in this browser.")
		}
		work = async {
			val context = JsAudioContext.instance
			val decodedData = context.decodeAudioData(fileLoader.await())
			JsWebAudioSoundFactory(audioManager, context, decodedData.await())
		}
	}

	override val status: Deferred.Status
		get() = work.status
	override val result: SoundFactory
		get() = work.result
	override val error: Throwable
		get() = work.error

	override suspend fun await(): SoundFactory = work.await()

	override fun cancel() = fileLoader.cancel()
}


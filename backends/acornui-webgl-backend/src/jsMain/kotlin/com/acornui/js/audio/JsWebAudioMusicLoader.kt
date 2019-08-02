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
import com.acornui.asset.AssetLoader
import com.acornui.asset.AssetType
import com.acornui.audio.AudioManager
import com.acornui.audio.Music

/**
 * An asset loader for js AudioContext sounds.
 * Does not work in IE.
 *
 * @author nbilyk
 */
class JsWebAudioMusicLoader(
		override val path: String,
		private val audioManager: AudioManager
) : AssetLoader<Music> {

	override val type: AssetType<Music> = AssetType.MUSIC


	override val secondsLoaded: Float
		get() = 0f

	override val secondsTotal: Float
		get() = 0f

	val element = Audio(path)

	init {
		element.load()
	}

	override val status: Deferred.Status
		get() = Deferred.Status.SUCCESSFUL

	override val result: Music by lazy {
		if (!audioContextSupported) throw Exception("Audio not supported in this browser.")
		JsWebAudioMusic(audioManager, JsAudioContext.instance, element)
	}

	override val error: Throwable
		get() {
			throw Exception("status is not FAILED")
		}

	override suspend fun await(): Music = result

	override fun cancel() {
	}
}


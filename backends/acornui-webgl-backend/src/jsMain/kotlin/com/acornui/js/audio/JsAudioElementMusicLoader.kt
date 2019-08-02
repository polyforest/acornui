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
import org.w3c.dom.HTMLAudioElement
import kotlin.browser.document

/**
 * An asset loader for js AudioContext sounds.
 * @author nbilyk
 */
class JsAudioElementMusicLoader(
		override val path: String,
		audioManager: AudioManager
) : AssetLoader<Music> {

	override val type: AssetType<Music> = AssetType.MUSIC

	override val secondsLoaded: Float
		get() = 0f

	override val secondsTotal: Float
		get() = 0f

	private val music = JsAudioElementMusic(audioManager, Audio(path))

	override val status: Deferred.Status = Deferred.Status.SUCCESSFUL
	override val result: Music = music
	override val error: Throwable
		get() {
			throw Exception("status is not FAILED")
		}

	override suspend fun await(): Music = music

	override fun cancel() {
	}
}

fun Audio(source: String): HTMLAudioElement {
	val audio = document.createElement("AUDIO") as HTMLAudioElement
	audio.src = source
	return audio
}

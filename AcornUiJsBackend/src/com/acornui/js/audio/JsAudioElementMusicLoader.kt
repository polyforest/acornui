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

package com.acornui.js.audio

import com.acornui.core.assets.AssetLoader
import com.acornui.core.assets.AssetType
import com.acornui.core.audio.AudioManager
import com.acornui.core.audio.Music
import org.w3c.dom.HTMLAudioElement
import kotlin.browser.document

/**
 * An asset loader for js AudioContext sounds.
 * @author nbilyk
 */
class JsAudioElementMusicLoader(
		override val path: String,
		override val estimatedBytesTotal: Int,
		audioManager: AudioManager
) : AssetLoader<Music> {

	override val type: AssetType<Music> = AssetType.MUSIC

	override val secondsLoaded: Float
		get() = 0f

	override val secondsTotal: Float
		get() = 0f

	private val music = JsAudioElementMusic(audioManager, Audio(path))

	suspend override fun await(): Music = music

	override fun cancel() {
	}
}

fun Audio(source: String): HTMLAudioElement {
	val audio = document.createElement("AUDIO") as HTMLAudioElement
	audio.src = source
	return audio
}
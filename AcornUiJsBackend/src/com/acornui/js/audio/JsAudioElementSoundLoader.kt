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

import com.acornui.async.Promise
import com.acornui.core.assets.AssetLoader
import com.acornui.core.assets.AssetType
import com.acornui.core.assets.AssetTypes
import com.acornui.core.audio.AudioManager
import com.acornui.core.audio.SoundFactory
import org.w3c.dom.HTMLAudioElement
import org.w3c.dom.events.Event

/**
 * An asset loader for js Audio element sounds.
 * Works in IE.
 * @author nbilyk
 */
class JsAudioElementSoundLoader(
		override val path: String,
		override val estimatedBytesTotal: Int,
		private val audioManager: AudioManager
) : Promise<SoundFactory>(), AssetLoader<SoundFactory> {

	override val type: AssetType<SoundFactory> = AssetTypes.SOUND

	override val secondsLoaded: Float
		get() = 0f

	override val secondsTotal: Float
		get() = 0f


	private val element = Audio(path)

	init {
		element.addEventListener("loadeddata", {
			event: Event ->
			val e = event.currentTarget as HTMLAudioElement
			// Load just enough of the asset to get its duration.
			if (e.readyState >= 1) {
				// METADATA
				// Untested: http://stackoverflow.com/questions/3258587/how-to-properly-unload-destroy-a-video-element
				val duration = e.duration
				val asset = JsAudioElementSoundFactory(audioManager, path, duration.toFloat())
				success(asset)
				cancel() // Unload the element now that we have the duration.
			}
		})
	}

	override fun cancel() {
		element.pause()
		element.src = ""
		element.load()
	}
}
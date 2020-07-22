/*
 * Copyright 2020 Poly Forest, LLC
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

package com.acornui.audio

import com.acornui.io.RequestSettings
import com.acornui.io.UrlRequestData
import com.acornui.io.toUrlStr
import kotlinx.coroutines.CompletableDeferred
import org.w3c.dom.Audio
import org.w3c.dom.HTMLAudioElement
import org.w3c.dom.events.Event
import kotlin.time.seconds

fun Audio.unload() {
	pause()
	src = ""
	load()
}

/**
 * An asset loader for js Audio element sounds.
 * Works in IE.
 */
suspend fun loadAudioElement(audioManager: AudioManager, requestData: UrlRequestData, settings: RequestSettings): JsAudioElementSoundFactory {
	val path = requestData.toUrlStr(settings.rootPath)
	val c = CompletableDeferred<JsAudioElementSoundFactory>()
	val element = Audio(path)

	element.addEventListener("loadeddata", {
		event: Event ->
		val e = event.currentTarget.unsafeCast<HTMLAudioElement>()
		// Load just enough of the asset to get its duration.
		if (e.readyState >= 1) {
			// METADATA
			// Untested: http://stackoverflow.com/questions/3258587/how-to-properly-unload-destroy-a-video-element
			val duration = e.duration
			val asset =
                JsAudioElementSoundFactory(audioManager, path, duration.seconds)
			c.complete(asset)
			element.unload() // Unload the element now that we have the duration.
		}
	})
	return c.await()
}
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

import com.acornui.audio.AudioManager
import com.acornui.audio.Music
import com.acornui.io.UrlRequestData
import org.w3c.dom.HTMLAudioElement
import kotlin.browser.document

fun Audio(source: String): HTMLAudioElement {
	val audio = document.createElement("AUDIO") as HTMLAudioElement
	audio.src = source
	return audio
}

fun loadMusic(audioManager: AudioManager, urlRequestData: UrlRequestData): Music {
	return JsAudioElementMusic(audioManager, Audio(urlRequestData.toUrlStr()))
}
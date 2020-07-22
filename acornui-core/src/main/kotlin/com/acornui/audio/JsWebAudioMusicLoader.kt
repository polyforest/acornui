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

package com.acornui.audio

import com.acornui.audio.AudioManager
import com.acornui.audio.Music
import com.acornui.io.UrlRequestData

/**
 * An asset loader for js AudioContext sounds.
 * Does not work in IE.
 */
//fun loadAudioMusic(audioManager: AudioManager, requestData: UrlRequestData): Music {
//	if (!audioContextSupported) throw Exception("Audio not supported in this browser.")
//	val path = requestData.urlStr
//	val element = Audio(path)
//	element.load()
//	return JsWebAudioMusic(audioManager, JsAudioContext.instance, element)
//}
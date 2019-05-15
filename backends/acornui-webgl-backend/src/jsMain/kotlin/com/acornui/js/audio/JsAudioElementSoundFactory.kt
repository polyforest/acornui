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

import com.acornui.core.audio.AudioManager
import com.acornui.core.audio.Sound
import com.acornui.core.audio.SoundFactory

class JsAudioElementSoundFactory(
		private val audioManager: AudioManager,
		private val path: String,
		override val duration: Float
) : SoundFactory {

	override var defaultPriority: Float = 0f

	init {
		audioManager.registerSoundSource(this)
	}

	override fun createInstance(priority: Float): Sound? {
		if (!audioManager.canPlaySound(priority))
			return null
		return JsAudioElementSound(audioManager, path, priority)
	}

	override fun dispose() {
		audioManager.unregisterSoundSource(this)
	}
}


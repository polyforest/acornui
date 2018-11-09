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


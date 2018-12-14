package com.acornui.js.audio

import com.acornui.core.audio.AudioManager
import com.acornui.core.audio.Sound
import com.acornui.core.audio.SoundFactory
import org.khronos.webgl.ArrayBuffer

/**
 * A sound source for audio buffer source nodes using an AudioContext.
 * Does not work in IE.
 */
class JsWebAudioSoundFactory(
		private val audioManager: AudioManager,
		private val context: AudioContext,
		private val decodedData: ArrayBuffer
) : SoundFactory {

	override var defaultPriority: Float = 0f

	override val duration: Float
		get() = decodedData.duration.toFloat()

	init {
		audioManager.registerSoundSource(this)
	}

	override fun createInstance(priority: Float): Sound? {
		if (!audioManager.canPlaySound(priority))
			return null
		return JsWebAudioSound(audioManager, context, decodedData, priority)
	}

	override fun dispose() {
		audioManager.unregisterSoundSource(this)
	}

}

private val ArrayBuffer.duration: Double
	get() = asDynamic().duration
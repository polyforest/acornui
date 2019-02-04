package com.acornui.js.audio

import com.acornui.core.audio.Music
import com.acornui.core.audio.MusicReadyState
import com.acornui.core.audio.AudioManager
import com.acornui.math.MathUtils
import com.acornui.signal.Signal
import com.acornui.signal.Signal0
import org.w3c.dom.HTMLMediaElement

class JsWebAudioMusic(
		private val audioManager: AudioManager,
		context: AudioContext,
		private val element: HTMLMediaElement
) : Music {

	override val duration: Float
		get() = 0f

	override val readyStateChanged: Signal<() -> Unit> = Signal0()

	override val readyState: MusicReadyState
		get() = MusicReadyState.READY

	override fun pause() {
	}

	override var onCompleted: (() -> Unit)? = null

	private var gain: GainNode

	private val mediaElementNode: MediaElementAudioSourceNode

	private var _isPlaying: Boolean = false
	override val isPlaying: Boolean
		get() = _isPlaying

	init {
		// create a sound source
		mediaElementNode = context.createMediaElementSource(element)
		mediaElementNode.addEventListener("ended", {
			complete()
		})

		// Volume
		gain = context.createGain()
		gain.gain.value = audioManager.soundVolume

		// Wire them together.
		mediaElementNode.connect(gain)
		gain.connect(context.destination)

		audioManager.registerMusic(this)
	}

	private fun complete() {
		_isPlaying = false
		onCompleted?.invoke()
		onCompleted = null
		audioManager.unregisterMusic(this)
	}

	override var loop: Boolean
		get() = element.loop
		set(value) {
			element.loop = value
		}

	private var _volume: Float = 1f

	override var volume: Float
		get() = _volume
		set(value) {
			_volume = value
			gain.gain.value = MathUtils.clamp(value * audioManager.musicVolume, 0f, 1f)
		}


	override fun play() {
		element.play()
	}

	override fun stop() {
		element.pause()
		element.currentTime = 0.0
	}


	override var currentTime: Float
		get() = element.currentTime.toFloat()
		set(value) {
			element.currentTime = value.toDouble()
		}

	override fun update() {
	}

	override fun dispose() {
		stop()
	}

}
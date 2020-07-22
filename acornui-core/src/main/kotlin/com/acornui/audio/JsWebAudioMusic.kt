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

import com.acornui.signal.unmanagedSignal
import org.w3c.dom.HTMLMediaElement
import kotlin.time.Duration
import kotlin.time.seconds

class JsWebAudioMusic(
    private val audioManager: AudioManager,
    context: AudioContext,
    private val element: HTMLMediaElement
) : Music {

	override val duration: Duration
		get() = Duration.ZERO

	override val readyStateChanged = unmanagedSignal<Unit>()

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

	private var _volume: Double = 1.0

	override var volume: Double
		get() = _volume
		set(value) {
			_volume = value
			gain.gain.value = com.acornui.math.clamp(value * audioManager.musicVolume, 0.0, 1.0)
		}


	override fun play() {
		element.play()
	}

	override fun stop() {
		element.pause()
		element.currentTime = 0.0
	}


	override var currentTime: Duration
		get() = element.currentTime.seconds
		set(value) {
			element.currentTime = value.inSeconds
		}

	override fun dispose() {
		stop()
		readyStateChanged.dispose()
	}

}

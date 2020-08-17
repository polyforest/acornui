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

import org.w3c.dom.HTMLAudioElement
import org.w3c.dom.events.Event
import kotlin.time.Duration
import kotlin.time.seconds

class JsAudioElementSound(
		private val audioManager: AudioManager,
		private val src: String,
		override val priority: Double
) : Sound {

	override var onCompleted: (() -> Unit)? = null

	private var _isPlaying: Boolean = false
	override val isPlaying: Boolean
		get() = _isPlaying


	private var element: HTMLAudioElement

	private val elementEndedHandler = {
		event: Event ->
		if (!loop)
			complete()
		Unit
	}

	init {
		element = audio(src)
		element.addEventListener("ended", elementEndedHandler)
		audioManager.registerSound(this)
	}

	private fun complete() {
		_isPlaying = false
		onCompleted?.invoke()
		onCompleted = null
		audioManager.unregisterSound(this)
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
			element.volume = com.acornui.math.clamp(value * audioManager.soundVolume, 0.0, 1.0).toDouble()
		}

	override fun setPosition(x: Double, y: Double, z: Double) {
	}

	override fun start() {
		if (_isPlaying) return
		_isPlaying = true
		element.play() // TODO: Delay and start time
	}

	override fun stop() {
		if (!_isPlaying) return
		_isPlaying = false
		element.pause() // TODO: Delay and start time
		element.currentTime = 0.0
	}

	override val currentTime: Duration
		get() = element.currentTime.seconds

	override fun dispose() {
		_isPlaying = false
		// Untested: http://stackoverflow.com/questions/3258587/how-to-properly-unload-destroy-a-video-element
		element.pause()
		element.src = ""
		element.load()
	}
}

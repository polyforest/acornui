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

import com.acornui.time.nowS
import org.khronos.webgl.ArrayBuffer
import kotlin.time.Duration
import kotlin.time.seconds

class JsWebAudioSound(
    private val audioManager: AudioManager,
    private val context: AudioContext,
    decodedData: ArrayBuffer,
    override val priority: Double) : Sound {

	override var onCompleted: (() -> Unit)? = null

	private var gain: GainNode
	private val panner: PannerNode

	private val audioBufferSourceNode: AudioBufferSourceNode

	private var _isPlaying: Boolean = false
	override val isPlaying: Boolean
		get() = _isPlaying

	private var _startTime: Double = 0.0
	private var _stopTime: Double = 0.0

	init {
		// create a sound source
		audioBufferSourceNode = context.createBufferSource()
		audioBufferSourceNode.addEventListener("ended", {
			complete()
		})

		// Add the buffered data to our object
		audioBufferSourceNode.buffer = decodedData

		// Panning
		panner = context.createPanner()
		panner.panningModel = PanningModel.EQUAL_POWER.value

		// Volume
		gain = context.createGain()
		gain.gain.value = audioManager.soundVolume

		// Wire them together.
		audioBufferSourceNode.connect(panner)
		panner.connect(gain)
		panner.setPosition(0.0, 0.0, 1.0)
		gain.connect(context.destination)

		audioManager.registerSound(this)
	}

	private fun complete() {
		_stopTime = nowS()
		_isPlaying = false
		onCompleted?.invoke()
		onCompleted = null
		audioManager.unregisterSound(this)
	}

	override var loop: Boolean
		get() = audioBufferSourceNode.loop
		set(value) {
			audioBufferSourceNode.loop = value
		}

	private var _volume: Double = 1.0

	override var volume: Double
		get() = _volume
		set(value) {
			_volume = value
			gain.gain.value = com.acornui.math.clamp(value * audioManager.soundVolume, 0.0, 1.0)
		}

	override fun setPosition(x: Double, y: Double, z: Double) {
		panner.setPosition(x, y, z)
	}

	override fun start() {
		audioBufferSourceNode.start(context.currentTime)
		_startTime = nowS()
	}

	override fun stop() {
		audioBufferSourceNode.stop(0.0)
	}

	override val currentTime: Duration
		get() {
			return if (!_isPlaying)
				(_stopTime - _startTime).seconds
			else
				(nowS() - _startTime).seconds
		}

	override fun dispose() {
		stop()
	}

}

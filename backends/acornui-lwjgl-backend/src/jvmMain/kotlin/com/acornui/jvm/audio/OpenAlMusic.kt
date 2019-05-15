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

package com.acornui.jvm.audio

import com.acornui.core.audio.Music
import com.acornui.core.audio.MusicReadyState
import com.acornui.signal.Signal
import com.acornui.signal.Signal0
import org.lwjgl.BufferUtils
import org.lwjgl.openal.AL10
import org.lwjgl.openal.AL11
import java.nio.IntBuffer

class OpenAlMusic(
		val audioManager: OpenAlAudioManager,
		private val streamReader: MusicStreamReader
) : Music {

	private val channels: Int = streamReader.channels
	private val sampleRate: Int = streamReader.sampleRate
	private val bufferOverhead = streamReader.bufferOverhead

	override var onCompleted: (() -> Unit)? = null

	override val duration: Float
		get() = streamReader.duration

	override val readyStateChanged: Signal<() -> Unit> = Signal0()

	override val readyState: MusicReadyState = MusicReadyState.NOTHING

	private var format: Int = 0
	private var rate: Int = 0
	private var secondsPerBuffer: Float = 0f
	private var renderedSeconds: Float = 0f

	private var buffers: IntBuffer

	private var sourceId = -1

	init {
		this.format = if (channels > 1) AL10.AL_FORMAT_STEREO16 else AL10.AL_FORMAT_MONO16
		this.rate = sampleRate
		secondsPerBuffer = (bufferSize - bufferOverhead).toFloat() / (bytesPerSample * channels * sampleRate)

		sourceId = AL10.alGenSources()
		if (AL10.alGetError() != AL10.AL_NO_ERROR) {
			throw Exception("Could not create source for music.")
		}
		AL10.alSourcei(sourceId, AL10.AL_LOOPING, AL10.AL_FALSE)
		AL10.alSourcef(sourceId, AL10.AL_GAIN, audioManager.musicVolume)
		AL10.alSource3f(sourceId, AL10.AL_POSITION, 0f, 0f, 1f)

		buffers = BufferUtils.createIntBuffer(bufferCount)
		AL10.alGenBuffers(buffers)
		if (AL10.alGetError() != AL10.AL_NO_ERROR)
			throw Exception("Unable to allocate audio buffers.")

		var filled = false // Check if there's anything to actually play.
		for (i in 0..bufferCount - 1) {
			val bufferId = buffers.get(i)
			if (!fill(bufferId)) break
			filled = true
			AL10.alSourceQueueBuffers(sourceId, bufferId)
		}
		if (!filled)
			onCompleted?.invoke()

		if (AL10.alGetError() != AL10.AL_NO_ERROR) {
			throw Exception("Buffers could not be queued.")
		}

		audioManager.registerMusic(this)
	}

	private var _isPlaying: Boolean = false
	override val isPlaying: Boolean
		get() = _isPlaying

	override var loop: Boolean = false

	private var _volume: Float = 1f
	override var volume: Float
		get() = _volume
		set(value) {
			_volume = value
			AL10.alSourcef(sourceId, AL10.AL_GAIN, _volume * audioManager.musicVolume)
		}

	override var currentTime: Float
		get() {
			return renderedSeconds + AL10.alGetSourcef(sourceId, AL11.AL_SEC_OFFSET)
		}
		set(value) {
			val wasPlaying = _isPlaying
			_isPlaying = false
			AL10.alSourceStop(sourceId)
			AL10.alSourceUnqueueBuffers(sourceId, buffers)
			renderedSeconds += secondsPerBuffer * bufferCount
			if (value <= renderedSeconds) {
				streamReader.reset()
				renderedSeconds = 0f
			}
			while (renderedSeconds < value - secondsPerBuffer) {
				if (streamReader.read(tempBytes) <= 0) break
				renderedSeconds += secondsPerBuffer
			}
			var filled = false
			for (i in 0..bufferCount - 1) {
				val bufferId = buffers.get(i)
				if (!fill(bufferId)) break
				filled = true
				AL10.alSourceQueueBuffers(sourceId, bufferId)
			}
			if (!filled) {
				stop()
				onCompleted?.invoke()
			}
			AL10.alSourcef(sourceId, AL11.AL_SEC_OFFSET, value - renderedSeconds)
			if (wasPlaying) {
				AL10.alSourcePlay(sourceId)
				_isPlaying = true
			}
		}

	override fun play() {
		if (!_isPlaying) {
			_isPlaying = true
			update()
			AL10.alSourcePlay(sourceId)
		}
	}

	override fun stop() {
		if (!_isPlaying) return
		streamReader.reset()
		renderedSeconds = 0f
		_isPlaying = false
		AL10.alSourceStop(sourceId)
	}

	override fun pause() {
		AL10.alSourcePause(sourceId)
		_isPlaying = false
	}

	private fun fill(bufferId: Int): Boolean {
		tempBuffer.clear()
		var length = streamReader.read(tempBytes)
		if (length <= 0) {
			if (loop) {
				streamReader.loop()
				renderedSeconds = 0f
				length = streamReader.read(tempBytes)
				if (length <= 0) return false
			} else
				return false
		}
		tempBuffer.put(tempBytes, 0, length).flip()
		AL10.alBufferData(bufferId, format, tempBuffer, rate)
		return true
	}

	override fun update() {
		var end = false
		var buffers = AL10.alGetSourcei(sourceId, AL10.AL_BUFFERS_PROCESSED)
		while (buffers-- > 0) {
			val bufferId = AL10.alSourceUnqueueBuffers(sourceId)
			if (bufferId == AL10.AL_INVALID_VALUE) break
			renderedSeconds += secondsPerBuffer
			if (end)
				continue
			if (fill(bufferId))
				AL10.alSourceQueueBuffers(sourceId, bufferId)
			else
				end = true
		}
		if (end && AL10.alGetSourcei(sourceId, AL10.AL_BUFFERS_QUEUED) == 0) {
			stop()
			onCompleted?.invoke()
		}

		// A buffer underflow will cause the source to stop.
		if (_isPlaying && AL10.alGetSourcei(sourceId, AL10.AL_SOURCE_STATE) != AL10.AL_PLAYING) AL10.alSourcePlay(sourceId)
	}


	override fun dispose() {
		stop()
		AL10.alSourcei(sourceId, AL10.AL_BUFFER, 0)
		AL10.alDeleteSources(sourceId)
		AL10.alDeleteBuffers(buffers)
		onCompleted = null
		sourceId = -1
		audioManager.unregisterMusic(this)
	}

	companion object {
		private val bufferSize = 4096 * 10
		private val bufferCount = 3
		private val bytesPerSample = 2
		private val tempBytes = ByteArray(bufferSize)
		private val tempBuffer = BufferUtils.createByteBuffer(bufferSize)
	}
}

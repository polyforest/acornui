package com.acornui.jvm.audio

import com.acornui.core.audio.Sound
import com.acornui.core.audio.SoundFactory
import com.acornui.math.MathUtils
import com.acornui.math.Vector3
import org.lwjgl.openal.AL10
import org.lwjgl.openal.AL11
import java.nio.ByteBuffer
import java.nio.ByteOrder

class OpenAlSoundFactory(
		private val audioManager: OpenAlAudioManager,
		pcm: ByteArray,
		channels: Int,
		sampleRate: Int
) : SoundFactory {

	override var defaultPriority: Float = 0f

	override val duration: Float

	private val bufferId: Int

	init {
		// Initialize the buffer for sounds created from this source.
		val bytes = pcm.size - pcm.size % if (channels > 1) 4 else 2
		val samples = bytes / (2 * channels)
		duration = samples / sampleRate.toFloat()

		val buffer = ByteBuffer.allocateDirect(bytes)
		buffer.order(ByteOrder.nativeOrder())
		buffer.put(pcm, 0, bytes)
		buffer.flip()

		bufferId = AL10.alGenBuffers()
		AL10.alBufferData(bufferId, if (channels > 1) AL10.AL_FORMAT_STEREO16 else AL10.AL_FORMAT_MONO16, buffer.asShortBuffer(), sampleRate)

		audioManager.registerSoundSource(this)
	}


	override fun createInstance(priority: Float): Sound? {
		if (!audioManager.canPlaySound(priority))
			return null
		return OpenAlSound(audioManager, bufferId, priority)
	}

	override fun dispose() {
		if (bufferId == -1) return
		AL10.alDeleteBuffers(bufferId)
		audioManager.unregisterSoundSource(this)
	}
}

class OpenAlSound(
		private val audioManager: OpenAlAudioManager,
		private val bufferId: Int,
		override val priority: Float) : Sound {

	private var sourceId: Int = -1

	override val currentTime: Float
		get() {
			if (sourceId == -1) return 0f
			return AL10.alGetSourcef(sourceId, AL11.AL_SEC_OFFSET)
		}

	override val isPlaying: Boolean
		get() {
			if (sourceId != -1) {
				val state = AL10.alGetSourcei(sourceId, AL10.AL_SOURCE_STATE)
				return (state == AL10.AL_PLAYING || state == AL10.AL_PAUSED)
			}
			return false
		}

	override var onCompleted: (() -> Unit)? = null

	private fun complete() {
		if (sourceId == -1) return
		AL10.alSourcei(sourceId, AL10.AL_BUFFER, 0)
		onCompleted?.invoke()
		onCompleted = null
		audioManager.freeSourceId(sourceId)
		sourceId = -1
		audioManager.unregisterSound(this)
	}

	private var _loop: Boolean = false

	override var loop: Boolean
		get() = _loop
		set(value) {
			if (_loop == value) return
			_loop = value
			if (sourceId != -1)
				AL10.alSourcei(sourceId, AL10.AL_LOOPING, if (value) AL10.AL_TRUE else AL10.AL_FALSE)
		}

	private var _volume: Float = 1f

	override var volume: Float
		get() = _volume
		set(value) {
			_volume = value
			if (sourceId != -1) {
				AL10.alSourcef(sourceId, AL10.AL_GAIN, MathUtils.clamp(volume * audioManager.soundVolume, 0f, 1f))
			}
		}

	init {
		audioManager.registerSound(this)
	}

	private val _position = Vector3(0f, 0f, 1f)

	override fun setPosition(x: Float, y: Float, z: Float) {
		if (_position.x == x && _position.y == y && _position.z == z) return
		_position.set(x, y, z)
		if (sourceId != -1)
			AL10.alSource3f(sourceId, AL10.AL_POSITION, _position.x, _position.y, _position.z)
	}

	override fun start() {
		sourceId = audioManager.obtainSourceId()

		AL10.alSourcei(sourceId, AL10.AL_BUFFER, bufferId)
		AL10.alSourcef(sourceId, AL10.AL_GAIN, MathUtils.clamp(volume * audioManager.soundVolume, 0f, 1f))
		AL10.alSourcef(sourceId, AL10.AL_PITCH, 1f)
		AL10.alSource3f(sourceId, AL10.AL_POSITION, _position.x, _position.y, _position.z)
		AL10.alSourcei(sourceId, AL10.AL_LOOPING, if (_loop) AL10.AL_TRUE else AL10.AL_FALSE)
		AL10.alSourcePlay(sourceId)
	}

	override fun stop() {
		if (sourceId == -1) return
		AL10.alSourceStop(sourceId)
		complete()
	}

	override fun update() {
		if (sourceId != -1 && !isPlaying) {
			complete()
		}
	}

	override fun dispose() {
		if (isPlaying) {
			stop()
		}
	}
}
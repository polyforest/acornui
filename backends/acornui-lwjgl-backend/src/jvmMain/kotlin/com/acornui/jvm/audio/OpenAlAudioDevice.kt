package com.acornui.jvm.audio

import com.acornui.core.audio.AudioDevice
import com.acornui.math.MathUtils
import java.nio.ByteBuffer
import java.nio.IntBuffer

import org.lwjgl.BufferUtils
import org.lwjgl.openal.AL11

import org.lwjgl.openal.AL10.*

/**
 * @author Nathan Sweet
 */
class OpenAlAudioDevice(
		private val audio: OpenAlAudioManager,
		val rate: Int,
		isMono: Boolean,
		private val bufferSize: Int,
		private val bufferCount: Int
) : AudioDevice {

	private val channels: Int
	private var buffers: IntBuffer? = null
	private var sourceId = -1
	private val format: Int
	private var isPlaying: Boolean = false
	private var volume = 1f

	var position: Float = 0f
		get() {
			if (sourceId == -1) return 0f
			return position + alGetSourcef(sourceId, AL11.AL_SEC_OFFSET)
		}

	private val secondsPerBuffer: Float
	private var bytes: ByteArray? = null
	private val tempBuffer: ByteBuffer

	init {
		channels = if (isMono) 1 else 2
		this.format = if (channels > 1) AL_FORMAT_STEREO16 else AL_FORMAT_MONO16
		secondsPerBuffer = bufferSize.toFloat() / bytesPerSample.toFloat() / channels.toFloat() / rate.toFloat()
		tempBuffer = BufferUtils.createByteBuffer(bufferSize)
	}

	override fun writeSamples(samples: ShortArray, offset: Int, numSamples: Int) {
		if (bytes == null || bytes!!.size < numSamples * 2)
			bytes = ByteArray(numSamples * 2)
		val bytes = bytes!!
		val end = minOf(offset + numSamples, samples.size)
		var i = offset
		var ii = 0
		while (i < end) {
			val sample = samples[i].toInt()
			bytes[ii++] = (sample and 0xFF).toByte()
			bytes[ii++] = (sample shr 8 and 0xFF).toByte()
			i++
		}
		writeSamples(bytes, 0, numSamples * 2)
	}

	override fun writeSamples(samples: FloatArray, offset: Int, numSamples: Int) {
		if (bytes == null || bytes!!.size < numSamples * 2)
			bytes = ByteArray(numSamples * 2)
		val bytes = bytes!!
		val end = minOf(offset + numSamples, samples.size)
		var i = offset
		var ii = 0
		while (i < end) {
			var floatSample = samples[i]
			floatSample = MathUtils.clamp(floatSample, -1f, 1f)
			val intSample = (floatSample * 32767).toInt()
			bytes[ii++] = (intSample and 0xFF).toByte()
			bytes[ii++] = (intSample shr 8 and 0xFF).toByte()
			i++
		}
		writeSamples(bytes, 0, numSamples * 2)
	}

	fun writeSamples(data: ByteArray, offset: Int, length: Int) {
		var offset = offset
		var length = length
		if (length < 0) throw IllegalArgumentException("length cannot be < 0.")

		if (sourceId == -1) {
			sourceId = audio.obtainSourceId()
			if (sourceId == -1) return
			if (buffers == null) {
				buffers = BufferUtils.createIntBuffer(bufferCount)
				alGenBuffers(buffers!!)
				if (alGetError() != AL_NO_ERROR)
					throw Exception("Unabe to allocate audio buffers.")
			}
			alSourcei(sourceId, AL_LOOPING, AL_FALSE)
			alSourcef(sourceId, AL_GAIN, volume)
			// Fill initial buffers.
			var queuedBuffers = 0
			for (i in 0..bufferCount - 1) {
				val bufferID = buffers!!.get(i)
				val written = minOf(bufferSize, length)
				tempBuffer.clear()
				tempBuffer.put(data, offset, written).flip()
				alBufferData(bufferID, format, tempBuffer, rate)
				alSourceQueueBuffers(sourceId, bufferID)
				length -= written
				offset += written
				queuedBuffers++
			}
			// Queue rest of buffers, empty.
			tempBuffer.clear().flip()
			for (i in queuedBuffers..bufferCount - 1) {
				val bufferID = buffers!!.get(i)
				alBufferData(bufferID, format, tempBuffer, rate)
				alSourceQueueBuffers(sourceId, bufferID)
			}
			alSourcePlay(sourceId)
			isPlaying = true
		}

		while (length > 0) {
			val written = fillBuffer(data, offset, length)
			length -= written
			offset += written
		}
	}

	/** Blocks until some of the data could be buffered.  */
	private fun fillBuffer(data: ByteArray, offset: Int, length: Int): Int {
		val written = minOf(bufferSize, length)

		outer@ while (true) {
			var buffers = alGetSourcei(sourceId, AL_BUFFERS_PROCESSED)
			while (buffers-- > 0) {
				val bufferID = alSourceUnqueueBuffers(sourceId)
				if (bufferID == AL_INVALID_VALUE) break
				position += secondsPerBuffer

				tempBuffer.clear()
				tempBuffer.put(data, offset, written).flip()
				alBufferData(bufferID, format, tempBuffer, rate)

				alSourceQueueBuffers(sourceId, bufferID)
				break@outer
			}
			// Wait for buffer to be free.
			try {
				Thread.sleep((1000 * secondsPerBuffer / bufferCount).toLong())
			} catch (ignored: InterruptedException) {
			}

		}

		// A buffer underflow will cause the source to stop.
		if (!isPlaying || alGetSourcei(sourceId, AL_SOURCE_STATE) != AL_PLAYING) {
			alSourcePlay(sourceId)
			isPlaying = true
		}

		return written
	}

	fun stop() {
		if (sourceId == -1) return
		audio.freeSourceId(sourceId)
		sourceId = -1
		position = 0f
		isPlaying = false
	}

	fun isPlaying(): Boolean {
		if (sourceId == -1) return false
		return isPlaying
	}

	override fun setVolume(volume: Float) {
		this.volume = volume
		if (sourceId != -1) alSourcef(sourceId, AL_GAIN, volume)
	}

	fun getChannels(): Int {
		return if (format == AL_FORMAT_STEREO16) 2 else 1
	}

	override fun dispose() {
		if (buffers == null) return
		if (sourceId != -1) {
			audio.freeSourceId(sourceId)
			sourceId = -1
		}
		alDeleteBuffers(buffers!!)
		buffers = null
	}

	override val isMono: Boolean
		get() = channels == 1

	override val latency: Int
		get() = (secondsPerBuffer * bufferCount.toFloat() * 1000f).toInt()

	companion object {
		private val bytesPerSample = 2
	}
}
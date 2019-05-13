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

import com.acornui.jvm.io.closeQuietly
import java.io.*


class WavMusicStreamReader(private val inputStreamFactory: ()->InputStream) : MusicStreamReader {

	private var input: WavInputStream? = null

	override val channels: Int
	override val sampleRate: Int
	override val duration: Float = -1f

	init {
		val input = WavInputStream(inputStreamFactory())
		channels = input.channels
		sampleRate = input.sampleRate
		this.input = input
	}

	override fun reset() {
		input?.closeQuietly()
		input = null
	}

	override fun read(buffer: ByteArray): Int {
		if (input == null) {
			input = WavInputStream(inputStreamFactory())
		}
		return input!!.read(buffer)
	}
}

object WavDecoder : SoundDecoder {
	override fun decodeSound(inputStream: InputStream): PcmSoundData {
		val input = WavInputStream(inputStream)
		val pcm = copyStreamToByteArray(input, input.dataRemaining)
		input.closeQuietly()
		return PcmSoundData(pcm, input.channels, input.sampleRate)
	}

	/**
	 * Copy the data from an [InputStream] to a byte array. The stream is not closed.
	 * @param estimatedSize Used to allocate the output byte[] to possibly avoid an array copy.
	 */
	fun copyStreamToByteArray(input: InputStream, estimatedSize: Int): ByteArray {
		val baos = OptimizedByteArrayOutputStream(maxOf(0, estimatedSize))
		copyStream(input, baos)
		return baos.toByteArray()
	}

	/**
	 * Allocates a {@value #DEFAULT_BUFFER_SIZE} byte[] for use as a temporary buffer and calls [copyStream].
	 */
	fun copyStream(input: InputStream, output: OutputStream) {
		copyStream(input, output, ByteArray(DEFAULT_BUFFER_SIZE))
	}



	/**
	 * Copy the data from an [InputStream] to an [OutputStream], using the specified byte[] as a temporary buffer. The
	 * stream is not closed.
	 */
	fun copyStream(input: InputStream, output: OutputStream, buffer: ByteArray) {
		var bytesRead: Int
		while (true) {
			bytesRead = input.read(buffer)
			if (bytesRead == -1) break
			output.write(buffer, 0, bytesRead)
		}
	}


	/**
	 * A ByteArrayOutputStream which avoids copying of the byte array if possible.
	 */
	private class OptimizedByteArrayOutputStream(initialSize: Int) : ByteArrayOutputStream(initialSize) {
		@Synchronized override fun toByteArray(): ByteArray {
			if (count == buf.size) return buf
			return super.toByteArray()
		}
		val buffer: ByteArray
			get() = buf
	}

}

/**
 * @author Nathan Sweet
 */
private class WavInputStream internal constructor(inputStream: InputStream) : FilterInputStream(inputStream) {
	internal var channels: Int = 0
	internal var sampleRate: Int = 0
	internal var dataRemaining: Int = 0

	init {
		try {
			if (readChar() != 'R' || readChar() != 'I' || readChar() != 'F' || readChar() != 'F')
				throw Exception("RIFF header not found")

			skipFully(4)

			if (readChar() != 'W' || readChar() != 'A' || readChar() != 'V' || readChar() != 'E')
				throw Exception("Invalid wave file header")

			val fmtChunkLength = seekToChunk('f', 'm', 't', ' ')

			val type = (read() and 0xff) or (read() and 0xff shl 8)
			if (type != 1) throw Exception("WAV files must be PCM: " + type)

			channels = read() and 0xff or (read() and 0xff shl 8)
			if (channels != 1 && channels != 2)
				throw Exception("WAV files must have 1 or 2 channels: " + channels)

			sampleRate = (read() and 0xff) or ((read() and 0xff) shl 8) or ((read() and 0xff) shl 16) or ((read() and 0xff) shl 24)

			skipFully(6)

			val bitsPerSample = (read() and 0xff) or ((read() and 0xff) shl 8)
			if (bitsPerSample != 16) throw Exception("WAV files must have 16 bits per sample: " + bitsPerSample)

			skipFully(fmtChunkLength - 16)

			dataRemaining = seekToChunk('d', 'a', 't', 'a')
		} catch (ex: Throwable) {
			closeQuietly()
			throw Exception("Error reading WAV file", ex)
		}

	}

	private fun readChar(): Char {
		return read().toChar()
	}

	private fun seekToChunk(c1: Char, c2: Char, c3: Char, c4: Char): Int {
		while (true) {
			var found = readChar() == c1
			found = found and (readChar() == c2)
			found = found and (readChar() == c3)
			found = found and (readChar() == c4)
//			val chunkLength = read() and 0xff or (read() and 0xff) shl 8 or (read() and 0xff) shl 16 or (read() and 0xff) shl 24
			val chunkLength = (read() and 0xff) or ((read() and 0xff) shl 8) or ((read() and 0xff) shl 16) or ((read() and 0xff) shl 24)
			if (chunkLength == -1)
				throw IOException("Chunk not found: " + c1 + c2 + c3 + c4)
			if (found)
				return chunkLength
			skipFully(chunkLength)
		}
	}

	@Throws(IOException::class)
	private fun skipFully(count: Int) {
		var count = count
		while (count > 0) {
			val skipped = `in`.skip(count.toLong())
			if (skipped <= 0) throw EOFException("Unable to skip.")
			count -= skipped.toInt()
		}
	}

	@Throws(IOException::class)
	override fun read(buffer: ByteArray): Int {
		if (dataRemaining == 0) return -1
		val length = minOf(super.read(buffer), dataRemaining)
		if (length == -1) return -1
		dataRemaining -= length
		return length
	}
}

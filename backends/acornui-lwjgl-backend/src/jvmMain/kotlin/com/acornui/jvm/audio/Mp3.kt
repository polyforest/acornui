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

import javazoom.jl.decoder.*
import java.io.ByteArrayOutputStream
import java.io.InputStream

class Mp3MusicStreamReader(
		val inputStreamFactory: () -> InputStream
) : MusicStreamReader {

	override val channels: Int
	override val sampleRate: Int
	override val bufferOverhead: Int = 4096
	override val duration: Float

	private var bitstream: Bitstream? = null
	private var outputBuffer: OutputBuffer? = null
	private var decoder: MP3Decoder? = null

	init {
		val fis = inputStreamFactory()
		bitstream = Bitstream(fis)
		decoder = MP3Decoder()
		try {
			val streamSize = fis.available()
			val header = bitstream!!.readFrame() ?: throw Exception("Empty MP3")
			channels = if (header.mode() == Header.SINGLE_CHANNEL) 1 else 2
			outputBuffer = OutputBuffer(channels, false)
			decoder!!.setOutputBuffer(outputBuffer)
			sampleRate = header.sampleRate
			duration = header.total_ms(streamSize) / 1000f
		} catch (e: BitstreamException) {
			throw Exception("error while preloading mp3", e)
		}
	}

	override fun reset() {
		if (bitstream == null) return
		try {
			bitstream!!.close()
		} catch (ignored: BitstreamException) {
		}
		bitstream = null
	}

	override fun read(buffer: ByteArray): Int {
		try {
			var setup = bitstream == null
			if (setup) {
				bitstream = Bitstream(inputStreamFactory())
				decoder = MP3Decoder()
			}

			var totalLength = 0
			val minRequiredLength = buffer.size - OutputBuffer.BUFFERSIZE * 2
			while (totalLength <= minRequiredLength) {
				val header = bitstream!!.readFrame() ?: break
				if (setup) {
					val channels = if (header.mode() == Header.SINGLE_CHANNEL) 1 else 2
					outputBuffer = OutputBuffer(channels, false)
					decoder!!.setOutputBuffer(outputBuffer)
					setup = false
				}
				try {
					decoder!!.decodeFrame(header, bitstream)
				} catch (ignored: Throwable) {
					// JLayer's decoder throws ArrayIndexOutOfBoundsException sometimes!?
				}
				bitstream!!.closeFrame()

				val length = outputBuffer!!.reset()
				System.arraycopy(outputBuffer!!.buffer, 0, buffer, totalLength, length)
				totalLength += length
			}
			return totalLength
		} catch (ex: Throwable) {
			reset()
			throw Exception("Error reading audio data.", ex)
		}
	}
}



object Mp3SoundDecoder : SoundDecoder {
	override fun decodeSound(inputStream: InputStream): PcmSoundData {
		val output = ByteArrayOutputStream(4096)
		val bitstream = Bitstream(inputStream)
		val decoder = MP3Decoder()

		var outputBuffer: OutputBuffer? = null
		var sampleRate = -1
		var channels = -1
		while (true) {
			val header = bitstream.readFrame() ?: break
			if (outputBuffer == null) {
				channels = if (header.mode() == Header.SINGLE_CHANNEL) 1 else 2
				outputBuffer = OutputBuffer(channels, false)
				decoder.setOutputBuffer(outputBuffer)
				sampleRate = header.sampleRate
			}
			try {
				decoder.decodeFrame(header, bitstream)
			} catch (ignored: Throwable) {
				// JLayer's decoder throws ArrayIndexOutOfBoundsException sometimes!?
			}
			bitstream.closeFrame()
			output.write(outputBuffer.buffer, 0, outputBuffer.reset())
		}
		bitstream.close()
		return PcmSoundData(output.toByteArray(), channels, sampleRate)
	}
}

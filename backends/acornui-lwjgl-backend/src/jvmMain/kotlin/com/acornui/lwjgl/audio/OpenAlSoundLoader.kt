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

package com.acornui.lwjgl.audio

import com.acornui.collection.stringMapOf
import com.acornui.io.ProgressReporter
import com.acornui.io.UrlRequestData
import com.acornui.io.load
import java.io.InputStream
import kotlin.time.Duration

suspend fun loadSound(audioManager: OpenAlAudioManager, requestData: UrlRequestData, progressReporter: ProgressReporter, initialTimeEstimate: Duration, connectTimeout: Duration) {
	load(requestData, progressReporter, initialTimeEstimate, connectTimeout) { inputStream ->
		val data = SoundDecoders.decode(requestData.url.extension(), inputStream)
		OpenAlSoundFactory(audioManager, data.pcm, data.channels, data.sampleRate)
	}
}

fun registerDefaultSoundDecoders() {
	SoundDecoders.addDecoder("ogg", OggSoundDecoder)
	SoundDecoders.addDecoder("mp3", Mp3SoundDecoder)
	SoundDecoders.addDecoder("wav", WavDecoder)
}

private fun String.extension(): String {
	return substringAfterLast('.').toLowerCase()
}

object SoundDecoders {

	private val decoders = stringMapOf<SoundDecoder>()

	/**
	 * Adds a decoder for the given file type.
	 * The decoder should accept an input stream and return a byte array.
	 */
	fun addDecoder(extension: String, decoder: SoundDecoder) {
		decoders[extension.toLowerCase()] = decoder
	}

	fun hasDecoder(extension: String): Boolean = decoders.containsKey(extension)

	fun decode(extension: String, fis: InputStream): PcmSoundData {
		val decoder = decoders[extension] ?: throw Exception("Decoder not found for extension: $extension")
		return decoder.decodeSound(fis)
	}

}

class PcmSoundData(
		val pcm: ByteArray,
		val channels: Int,
		val sampleRate: Int
)

interface SoundDecoder {
	fun decodeSound(inputStream: InputStream): PcmSoundData
}

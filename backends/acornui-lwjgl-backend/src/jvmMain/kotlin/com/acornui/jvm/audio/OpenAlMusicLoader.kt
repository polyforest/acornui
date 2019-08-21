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

import com.acornui.audio.Music
import com.acornui.collection.stringMapOf
import com.acornui.io.UrlRequestData
import com.acornui.io.toUrl
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStream

fun loadOpenAlMusic(audioManager: OpenAlAudioManager, requestData: UrlRequestData): Music {
	val extension = requestData.url.extension()
	if (!MusicDecoders.hasDecoder(extension)) throw Exception("No decoder found for music extension: $extension")

	val url = requestData.url.toUrl()
	val inputStreamFactory: () -> InputStream
	@Suppress("LiftReturnOrAssignment")
	if (url != null) {
		inputStreamFactory = { url.openStream() }
	} else {
		val file = File(requestData.url)
		if (!file.exists()) throw FileNotFoundException(requestData.url)
		inputStreamFactory = { FileInputStream(file) }
	}
	val streamReader = MusicDecoders.createReader(extension, inputStreamFactory)
	return OpenAlMusic(audioManager, streamReader)
}

private fun String.extension(): String {
	return substringAfterLast('.').toLowerCase()
}

fun registerDefaultMusicDecoders() {
	MusicDecoders.setReaderFactory("ogg") { OggMusicStreamReader(it) }
	MusicDecoders.setReaderFactory("mp3") { Mp3MusicStreamReader(it) }
	MusicDecoders.setReaderFactory("wav") { WavMusicStreamReader(it) }
}

interface MusicStreamReader {

	val channels: Int
	val sampleRate: Int
	val bufferOverhead: Int
		get() = 0

	/**
	 * The duration of the music, in seconds.
	 * This returns -1 if the duration is unknown.
	 */
	val duration: Float

	/**
	 * Resets the stream to the beginning.
	 */
	fun reset()

	/**
	 * By default, does just the same as reset(). Used to add special behaviour in Ogg music.
	 */
	fun loop() {
		reset()
	}

	/**
	 * Fills as much of the buffer as possible and returns the number of bytes filled. Returns <= 0 to indicate the end
	 * of the stream.
	 */
	fun read(buffer: ByteArray): Int
}

/**
 * Registered music decoders.
 */
object MusicDecoders {

	private val readerFactories = stringMapOf<(() -> InputStream) -> MusicStreamReader>()

	/**
	 * Adds a decoder for the given file type.
	 * The decoder should accept an input stream and return a byte array.
	 */
	fun setReaderFactory(extension: String, readerFactory: (() -> InputStream) -> MusicStreamReader) {
		readerFactories[extension.toLowerCase()] = readerFactory
	}

	fun hasDecoder(extension: String): Boolean = readerFactories.containsKey(extension)

	fun createReader(extension: String, inputStreamFactory: () -> InputStream): MusicStreamReader {
		val readerFactory = readerFactories[extension] ?: throw Exception("Decoder not found for extension: $extension")
		return readerFactory(inputStreamFactory)
	}

}


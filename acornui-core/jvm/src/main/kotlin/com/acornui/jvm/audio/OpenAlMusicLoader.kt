package com.acornui.jvm.audio

import com.acornui.async.Promise
import com.acornui.async.launch
import com.acornui.collection.stringMapOf
import com.acornui.core.asset.AssetLoader
import com.acornui.core.asset.AssetType
import com.acornui.core.audio.Music
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStream
import java.net.URL


class OpenAlMusicLoader(
		override val path: String,
		audioManager: OpenAlAudioManager
) : Promise<Music>(), AssetLoader<Music> {

	override val type: AssetType<Music> = AssetType.MUSIC

	override val secondsLoaded: Float = 0f
	override val secondsTotal: Float = 0f

	init {
		launch {
			val extension = path.extension()
			if (!MusicDecoders.hasDecoder(extension)) throw Exception("No decoder found for music extension: $extension")

			val inputStreamFactory: () -> InputStream
			@Suppress("LiftReturnOrAssignment")
			if (path.startsWith("http:", ignoreCase = true) || path.startsWith("https:", ignoreCase = true)) {
				inputStreamFactory = { URL(path).openStream() }
			} else {
				val file = File(path)
				if (!file.exists()) throw FileNotFoundException(path)
				inputStreamFactory = { FileInputStream(file) }
			}
			val streamReader = MusicDecoders.createReader(extension, inputStreamFactory)
			success(OpenAlMusic(audioManager, streamReader))
		}
	}

	override fun cancel() {
	}

	companion object {

		fun registerDefaultDecoders() {
			MusicDecoders.setReaderFactory("ogg") { OggMusicStreamReader(it) }
			MusicDecoders.setReaderFactory("mp3") { Mp3MusicStreamReader(it) }
			MusicDecoders.setReaderFactory("wav") { WavMusicStreamReader(it) }
		}

		private fun String.extension(): String {
			return substringAfterLast('.').toLowerCase()
		}
	}
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


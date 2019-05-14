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

import com.acornui.io.closeQuietly
import com.acornui.logging.Log
import com.jcraft.jogg.Packet
import com.jcraft.jogg.Page
import com.jcraft.jogg.StreamState
import com.jcraft.jogg.SyncState
import com.jcraft.jorbis.Block
import com.jcraft.jorbis.Comment
import com.jcraft.jorbis.DspState
import com.jcraft.jorbis.Info
import org.lwjgl.BufferUtils
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class OggMusicStreamReader(
		val inputStreamFactory: () -> InputStream
) : MusicStreamReader {

	override val channels: Int
	override val sampleRate: Int
	override val duration: Float = -1f

	private var input: OggInputStream? = null
	private var previousInput: OggInputStream? = null

	init {
		input = OggInputStream(inputStreamFactory())
		channels = input!!.channels
		sampleRate = input!!.sampleRate
	}

	override fun read(buffer: ByteArray): Int {
		if (input == null) {
			input = OggInputStream(inputStreamFactory(), previousInput)
			previousInput = null // release this reference
		}
		return input!!.read(buffer)
	}

	override fun reset() {
		input?.closeQuietly()
		previousInput = null
		input = null
	}

	override fun loop() {
		input?.closeQuietly()
		previousInput = input
		input = null
	}
}


object OggSoundDecoder : SoundDecoder {
	override fun decodeSound(inputStream: InputStream): PcmSoundData {
		val input = OggInputStream(inputStream)
		val output = ByteArrayOutputStream(4096)

		val buffer = ByteArray(2048)
		while (!input.atEnd()) {
			val length = input.read(buffer)
			if (length == -1) break
			output.write(buffer, 0, length)
		}
		input.closeQuietly()
		return PcmSoundData(output.toByteArray(), input.channels, input.sampleRate)
	}
}

/**
 * An input stream to read Ogg Vorbis.
 * @author kevin
 */
class OggInputStream

/**
 * Create a new stream to decode OGG data, reusing buffers from another stream.
 * It's not a good idea to use the old stream instance afterwards.
 * @param input The input stream from which to read the OGG file
 * *
 * @param previousStream The stream instance to reuse buffers from, may be null
 */
internal constructor(
		/** The stream we're reading the OGG file from  */
		private val input: InputStream, previousStream: OggInputStream? = null) : InputStream() {

	/** The conversion buffer size  */
	private var convsize = BUFFER_SIZE * 4
	/** The buffer used to read OGG file  */
	private var convbuffer: ByteArray
	/** The audio information from the OGG header  */
	private val oggInfo = Info() // struct that stores all the static vorbis bitstream settings
	/** True if we're at the end of the available data  */
	private var endOfStream: Boolean = false

	/** The Vorbis SyncState used to decode the OGG  */
	private val syncState = SyncState() // sync and verify incoming physical bitstream
	/** The Vorbis Stream State used to decode the OGG  */
	private val streamState = StreamState() // take physical pages, weld into a logical stream of packets
	/** The current OGG page  */
	private val page = Page() // one Ogg bitstream page. Vorbis packets are inside
	/** The current packet page  */
	private val packet = Packet() // one raw packet of data for decode

	/** The comment read from the OGG file  */
	private val comment = Comment() // struct that stores all the bitstream user comments
	/** The Vorbis DSP stat eused to decode the OGG  */
	private val dspState = DspState() // central working state for the packet->PCM decoder
	/** The OGG block we're currently working with to convert PCM  */
	private val vorbisBlock = Block(dspState) // local working space for packet->PCM decode

	/** Temporary scratch buffer  */
	internal var buffer: ByteArray? = null
	/** The number of bytes read  */
	internal var bytes = 0
	/** The true if we should be reading big endian  */
	internal var bigEndian = ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN
	/** True if we're reached the end of the current bit stream  */
	internal var endOfBitStream = true
	/** True if we're initialise the OGG info block  */
	internal var inited = false

	/** The index into the byte array we currently read from  */
	private var readIndex: Int = 0
	/** The byte array store used to hold the data read from the ogg  */
	private var pcmBuffer: ByteBuffer? = null
	/** The total number of bytes  */
	/** Get the number of bytes on the stream

	 * @return The number of the bytes on the stream
	 */
	var length: Int = 0
		private set

	init {
		if (previousStream == null) {
			convbuffer = ByteArray(convsize)
			pcmBuffer = BufferUtils.createByteBuffer(4096 * 500)
		} else {
			convbuffer = previousStream.convbuffer
			pcmBuffer = previousStream.pcmBuffer
		}
		length = input.available()

		init()
	}

	val channels: Int
		get() = oggInfo.channels

	val sampleRate: Int
		get() = oggInfo.rate

	/** Initialise the streams and thread involved in the streaming of OGG data  */
	private fun init() {
		initVorbis()
		readPCM()
	}

	/** @see java.io.InputStream.available
	 */
	override fun available(): Int {
		return if (endOfStream) 0 else 1
	}

	/** Initialise the vorbis decoding  */
	private fun initVorbis() {
		syncState.init()
	}

	/** Get a page and packet from that page

	 * @return True if there was a page available
	 */
	// grab some data at the head of the stream. We want the first page
	// (which is guaranteed to be small and only contain the Vorbis
	// stream initial header) We need the first page to get the stream
	// serialno.
	// submit a 4k block to libvorbis' Ogg layer
	// Get the first page.
	// have we simply run out of data? If so, we're done.
	// error case. Must not be Vorbis data
	// Get the serial number and set up the rest of decode.
	// serialno first; use it to set up a logical stream
	// extract the initial header from the first page and verify that the
	// Ogg bitstream is in fact Vorbis data
	// I handle the initial header first instead of just having the code
	// read all three Vorbis headers at once because reading the initial
	// header is an easy way to identify a Vorbis bitstream and it's
	// useful to see that functionality seperated out.
	// error; stream version mismatch perhaps
	// no page? must not be vorbis
	// error case; not a vorbis header
	// At this point, we're sure we're Vorbis. We've set up the logical
	// (Ogg) bitstream decoder. Get the comment and codebook headers and
	// set up the Vorbis decoder
	// The next two packets in order are the comment and codebook headers.
	// They're likely large and may span multiple pages. Thus we reead
	// and submit data until we get our two pacakets, watching that no
	// pages are missing. If a page is missing, error out; losing a
	// header page is the only place where missing data is fatal. */
	// Need more data
	// Don't complain about missing or corrupt data yet. We'll
	// catch it at the packet output phase
	// we can ignore any errors here
	// as they'll also become apparent
	// at packetout
	// Uh oh; data at some point was corrupted or missing!
	// We can't tolerate that in a header. Die.
	// no harm in not checking before adding more
	// OK, got and parsed all three headers. Initialize the Vorbis
	// packet->PCM decoder.
	// central decode state
	// local state for most of the decode
	// so multiple block decodes can
	// proceed in parallel. We could init
	// multiple vorbis_block structures
	// for vd here
	private val pageAndPacket: Boolean
		get() {
			var index = syncState.buffer(BUFFER_SIZE)
			if (index == -1) return false

			buffer = syncState.data
			if (buffer == null) {
				endOfStream = true
				return false
			}

			bytes = input.read(buffer!!, index, BUFFER_SIZE)

			syncState.wrote(bytes)
			if (syncState.pageout(page) != 1) {
				if (bytes < BUFFER_SIZE) return false
				throw Exception("Input does not appear to be an Ogg bitstream.")
			}
			streamState.init(page.serialno())

			oggInfo.init()
			comment.init()
			if (streamState.pagein(page) < 0) {
				throw Exception("Error reading first page of Ogg bitstream.")
			}

			if (streamState.packetout(packet) != 1) {
				throw Exception("Error reading initial header packet.")
			}

			if (oggInfo.synthesis_headerin(comment, packet) < 0) {
				throw Exception("Ogg bitstream does not contain Vorbis audio data.")
			}

			var i = 0
			while (i < 2) {
				while (i < 2) {
					var result = syncState.pageout(page)
					if (result == 0) break

					if (result == 1) {
						streamState.pagein(page)
						while (i < 2) {
							result = streamState.packetout(packet)
							if (result == 0) break
							if (result == -1) {
								throw Exception("Corrupt secondary header.")
							}

							oggInfo.synthesis_headerin(comment, packet)
							i++
						}
					}
				}
				index = syncState.buffer(BUFFER_SIZE)
				if (index == -1) return false
				buffer = syncState.data
				try {
					bytes = input.read(buffer!!, index, BUFFER_SIZE)
				} catch (e: Throwable) {
					throw Exception("Failed to read Vorbis.", e)
				}

				if (bytes == 0 && i < 2) {
					throw Exception("End of file before finding all Vorbis headers.")
				}
				syncState.wrote(bytes)
			}

			convsize = BUFFER_SIZE / oggInfo.channels
			dspState.synthesis_init(oggInfo)
			vorbisBlock.init(dspState)

			return true
		}

	/** Decode the OGG file as shown in the jogg/jorbis examples  */
	private fun readPCM() {
		var wrote = false

		while (true) { // we repeat if the bitstream is chained
			if (endOfBitStream) {
				if (!pageAndPacket) {
					break
				}
				endOfBitStream = false
			}

			if (!inited) {
				inited = true
				return
			}

			val _pcm = arrayOfNulls<Array<FloatArray>>(1)
			val _index = IntArray(oggInfo.channels)
			// The rest is just a straight decode loop until end of stream
			while (!endOfBitStream) {
				while (!endOfBitStream) {
					var result = syncState.pageout(page)

					if (result == 0) {
						break // need more data
					}

					if (result == -1) { // missing or corrupt data at this page position
						// throw new Exception("Corrupt or missing data in bitstream.");
						Log.warn("Error reading OGG: Corrupt or missing data in bitstream.")
					} else {
						streamState.pagein(page) // can safely ignore errors at
						// this point
						while (true) {
							result = streamState.packetout(packet)

							if (result == 0) break // need more data
							if (result == -1) { // missing or corrupt data at this page position
								// no reason to complain; already complained above
							} else {
								// we have a packet. Decode it
								if (vorbisBlock.synthesis(packet) === 0) { // test for success!
									dspState.synthesis_blockin(vorbisBlock)
								}

								// **pcm is a multichannel float vector. In stereo, for
								// example, pcm[0] is left, and pcm[1] is right. samples is
								// the size of each channel. Convert the float values
								// (-1.<=range<=1.) to whatever PCM format and write it out

								while (true) {
									val samples = dspState.synthesis_pcmout(_pcm, _index)
									if (samples <= 0) break
									val pcm = _pcm[0]!!
									// boolean clipflag = false;
									val bout = if (samples < convsize) samples else convsize

									// convert floats to 16 bit signed ints (host order) and
									// interleave
									for (i in 0..oggInfo.channels - 1) {
										var ptr = i * 2
										// int ptr=i;
										val mono = _index[i]
										for (j in 0..bout - 1) {
											var value = (pcm[i][mono + j] * 32767.0).toInt()
											// might as well guard against clipping
											if (value > 32767) {
												value = 32767
											}
											if (value < -32768) {
												value = -32768
											}
											if (value < 0) value = value or 0x8000

											if (bigEndian) {
												convbuffer[ptr] = value.ushr(8).toByte()
												convbuffer[ptr + 1] = value.toByte()
											} else {
												convbuffer[ptr] = value.toByte()
												convbuffer[ptr + 1] = value.ushr(8).toByte()
											}
											ptr += 2 * oggInfo.channels
										}
									}

									val bytesToWrite = 2 * oggInfo.channels * bout
									if (bytesToWrite > pcmBuffer!!.remaining()) {
										throw Exception("Ogg block too big to be buffered: " + bytesToWrite + " :: " + pcmBuffer!!.remaining())
									} else {
										pcmBuffer!!.put(convbuffer, 0, bytesToWrite)
									}

									wrote = true
									dspState.synthesis_read(bout) // tell libvorbis how
									// many samples we
									// actually consumed
								}
							}
						}
						if (page.eos() !== 0) {
							endOfBitStream = true
						}

						if (!endOfBitStream && wrote) {
							return
						}
					}
				}

				if (!endOfBitStream) {
					bytes = 0
					val index = syncState.buffer(BUFFER_SIZE)
					if (index >= 0) {
						buffer = syncState.data
						try {
							bytes = input.read(buffer!!, index, BUFFER_SIZE)
						} catch (e: Throwable) {
							throw Exception("Error during Vorbis decoding.", e)
						}

					} else {
						bytes = 0
					}
					syncState.wrote(bytes)
					if (bytes == 0) {
						endOfBitStream = true
					}
				}
			}

			// clean up this logical bitstream; before exit we see if we're
			// followed by another [chained]
			streamState.clear()

			// ogg_page and ogg_packet structs always point to storage in
			// libvorbis. They're never freed or manipulated directly

			vorbisBlock.clear()
			dspState.clear()
			oggInfo.clear() // must be called last
		}

		// OK, clean up the framer
		syncState.clear()
		endOfStream = true
	}

	override fun read(): Int {
		if (readIndex >= pcmBuffer!!.position()) {
			pcmBuffer!!.clear()
			readPCM()
			readIndex = 0
		}
		if (readIndex >= pcmBuffer!!.position()) {
			return -1
		}

		var value = pcmBuffer!!.get(readIndex).toInt()
		if (value < 0) {
			value += 256
		}
		readIndex++

		return value
	}

	fun atEnd(): Boolean {
		return endOfStream && readIndex >= pcmBuffer!!.position()
	}

	override fun read(b: ByteArray, off: Int, len: Int): Int {
		for (i in 0..len - 1) {
			val value = read()
			if (value >= 0) {
				b[i] = value.toByte()
			} else {
				if (i == 0) {
					return -1
				} else {
					return i
				}
			}
		}

		return len
	}

	override fun read(b: ByteArray): Int {
		return read(b, 0, b.size)
	}

	override fun close() {
		try {
			input.close()
		} catch (ignore: Throwable) {
		}
	}

	companion object {
		private val BUFFER_SIZE = 512
	}
}

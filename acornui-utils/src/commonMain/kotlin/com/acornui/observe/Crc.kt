/*
 * Copyright 2015 Nicholas Bilyk
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

package com.acornui.observe

class Crc32 {

	/**
	 * The crc checksum.
	 */
	private var crc = 0

	/**
	 * The fast CRC table. Computed once when the CRC32 class is loaded.
	 */
	private val crcTable = makeCrcTable()

	/**
	 * Make the table for a fast CRC.
	 */
	private fun makeCrcTable(): IntArray {
		val table = IntArray(256)
		for (n in 0..255) {
			var c = n
			var k = 8
			while (--k >= 0) {
				if ((c and 1) != 0)
					c = -306674912 xor (c.ushr(1))
				else
					c = c.ushr(1)
			}
			table[n] = c
		}
		return table
	}

	/**
	 * Returns the CRC32 data checksum computed so far.
	 */
	fun getValue(): Long {
		return crc.toLong() and 0xffffffffL
	}

	/**
	 * Resets the CRC32 data checksum as if no update was ever called.
	 */
	fun reset() {
		crc = 0
	}

	/**
	 * Updates the checksum with given long.
	 */
	fun update(longVal: Long) {
		update((longVal shr 56).toByte())
		update((longVal shr 48).toByte())
		update((longVal shr 40).toByte())
		update((longVal shr 32).toByte())

		update((longVal shr 24).toByte())
		update((longVal shr 16).toByte())
		update((longVal shr 8).toByte())
		update(longVal.toByte())
	}

	/**
	 * Updates the checksum with given integer.
	 */
	fun update(intVal: Int) {
		update((intVal shr 24).toByte())
		update((intVal shr 16).toByte())
		update((intVal shr 8).toByte())
		update(intVal.toByte())
	}

	/**
	 * Updates the checksum with the given byte.
	 */
	fun update(byteVal: Byte) {
		var c = crc.inv()
		c = crcTable[(c xor byteVal.toInt()) and 255] xor (c.ushr(8))
		crc = c.inv()
	}

	/**
	 * Adds the byte array to the data checksum.
	 * @param buf the buffer which contains the data
	 *
	 * @param off the offset in the buffer where the data starts
	 *
	 * @param len the length of the data
	 */
	fun update(buf: ByteArray, off: Int = 0, len: Int = buf.size) {
		var i = off
		var n = len
		var c = crc.inv()
		while (--n >= 0) {
			c = crcTable[(c xor buf[i++].toInt()) and 255] xor (c.ushr(8))
		}
		crc = c.inv()
	}

	companion object {
		val CRC = Crc32()
	}
}
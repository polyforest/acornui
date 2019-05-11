package com.acornui.serialization

interface Base64 {

	/**
	 * Encodes the byte array to a base 64 String.
	 */
	fun encodeToString(src: ByteArray): String

	/**
	 * Decodes the base 64 string into a byte array.
	 * Note that the string is expected to be an 8 bit ascii string.
	 * An exception will be thrown if the string is out of the 8 bit range.
	 */
	fun decodeFromString(str: String): ByteArray
}

fun Base64.encodeToUtf8String(str: String): String {
	return encodeToString(ByteArray(str.length) {
		str[it].toByte()
	})
}

expect val base64: Base64
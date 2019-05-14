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

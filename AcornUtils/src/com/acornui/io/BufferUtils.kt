/*
 * Copyright 2018 Nicholas Bilyk
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

package com.acornui.io

import com.acornui.graphics.ColorRo
import com.acornui.math.Vector2Ro
import com.acornui.math.Vector3Ro


// Common buffer read/write utility

fun WriteBuffer<Float>.putVector3(value: Vector3Ro) {
	put(value.x)
	put(value.y)
	put(value.z)
}

fun WriteBuffer<Float>.putVector2(value: Vector2Ro) {
	put(value.x)
	put(value.y)
}

fun ColorRo.writeUnpacked(buffer: WriteBuffer<Float>) {
	buffer.put(r)
	buffer.put(g)
	buffer.put(b)
	buffer.put(a)
}

/**
 * Converts the bytes from the current position until the limit into a ByteArray.
 */
fun ReadBuffer<Byte>.toByteArray(): ByteArray {
	val bytes = ByteArray(limit - position)
	var i = 0
	while (hasRemaining)
		bytes[i++] = get()
	return bytes
}

/**
 * Converts the shorts from the current position until the limit into a ShortArray.
 */
fun ReadBuffer<Short>.toShortArray(): ShortArray {
	val bytes = ShortArray(limit - position)
	var i = 0
	while (hasRemaining)
		bytes[i++] = get()
	return bytes
}

/**
 * Converts the ints from the current position until the limit into a IntArray.
 */
fun ReadBuffer<Int>.toIntArray(): IntArray {
	val bytes = IntArray(limit - position)
	var i = 0
	while (hasRemaining)
		bytes[i++] = get()
	return bytes
}

/**
 * Converts the floats from the current position until the limit into a FloatArray.
 */
fun ReadBuffer<Float>.toFloatArray(): FloatArray {
	val floats = FloatArray(limit - position)
	var i = 0
	while (hasRemaining)
		floats[i++] = get()
	return floats
}

/**
 * Converts the doubles from the current position until the limit into a DoubleArray.
 */
fun ReadBuffer<Double>.toDoubleArray(): DoubleArray {
	val doubles = DoubleArray(limit - position)
	var i = 0
	while (hasRemaining)
		doubles[i++] = get()
	return doubles
}

/**
 * Reads a UTF-8 string from the byte buffer.
 */
fun ReadByteBuffer.getString8(): String {
	val builder = StringBuilder()
	val stop: Char = 0.toChar()
	while (true) {
		val char = getChar8()
		if (char == stop) break
		else builder.append(char)
	}
	return builder.toString()
}

/**
 * Reads a UTF-16 string from the byte buffer.
 */
fun ReadByteBuffer.getString16(): String {
	val builder = StringBuilder()
	val stop: Char = 0.toChar()
	while (true) {
		val char = getChar16()
		if (char == stop) break
		else builder.append(char)
	}
	return builder.toString()
}

/**
 * Writes a UTF-8 string to the byte buffer.
 */
fun WriteByteBuffer.putString8(value: String) {
	for (i in 0..value.lastIndex) {
		putChar8(value[i])
	}
	putChar8(0.toChar())
}

/**
 * Writes a UTF-16 string to the byte buffer.
 */
fun WriteByteBuffer.putString16(value: String) {
	for (i in 0..value.lastIndex) {
		putChar16(value[i])
	}
	putChar16(0.toChar())
}
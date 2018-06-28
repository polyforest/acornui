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
import com.acornui.math.Vector2
import com.acornui.math.Vector3


// Common buffer read/write utility

fun Vector3.write(buffer: WriteBuffer<Float>) {
	buffer.put(x)
	buffer.put(y)
	buffer.put(z)
}

fun Vector2.write(buffer: WriteBuffer<Float>) {
	buffer.put(x)
	buffer.put(y)
}

fun ColorRo.writeUnpacked(buffer: WriteBuffer<Float>) {
	buffer.put(r)
	buffer.put(g)
	buffer.put(b)
	buffer.put(a)
}

fun ReadBuffer<Float>.toFloatArray(): FloatArray {
	mark()
	val floats = FloatArray(limit - position)
	var i = 0
	while (hasRemaining)
		floats[i++] = get()
	reset()
	return floats
}

fun ReadBuffer<Byte>.toByteArray(): ByteArray {
	mark()
	val bytes = ByteArray(limit - position)
	var i = 0
	while (hasRemaining)
		bytes[i++] = get()
	reset()
	return bytes
}

fun ReadByteBuffer.getString(): String {
	val builder = StringBuilder()
	val stop: Char = 0.toChar()
	while (true) {
		val char = getChar()
		if (char == stop) break
		else builder.append(char)
	}
	return builder.toString()
}

fun WriteByteBuffer.putString(value: String) {
	for (i in 0..value.lastIndex) {
		putChar(value[i])
	}
	putChar(0.toChar())
}
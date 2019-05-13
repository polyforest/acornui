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

package com.acornui.io

import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream
import java.nio.*
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.nio.ShortBuffer
import java.nio.DoubleBuffer
import java.nio.charset.Charset

/**
 * @author nbilyk
 */
object JvmBufferUtil {

	fun wrap(value: ByteArray): ByteBuffer {
		val result = ByteBuffer.allocateDirect(value.size).order(ByteOrder.nativeOrder())
		result.put(value).flip()
		return result
	}

	fun wrap(value: ShortArray): ShortBuffer {
		val result = ByteBuffer.allocateDirect(value.size shl 1).order(ByteOrder.nativeOrder()).asShortBuffer()
		result.put(value).flip()
		return result
	}

	fun wrap(value: IntArray): IntBuffer {
		val result = ByteBuffer.allocateDirect(value.size shl 2).order(ByteOrder.nativeOrder()).asIntBuffer()
		result.put(value).flip()
		return result
	}

	fun wrap(value: FloatArray): FloatBuffer {
		val result = ByteBuffer.allocateDirect(value.size shl 2).order(ByteOrder.nativeOrder()).asFloatBuffer()
		result.put(value).flip()
		return result
	}

	fun wrap(value: DoubleArray): DoubleBuffer {
		val result = ByteBuffer.allocateDirect(value.size shl 3).order(ByteOrder.nativeOrder()).asDoubleBuffer()
		result.put(value).flip()
		return result
	}
}

fun InputStream.readTextAndClose(charset: Charset = Charsets.UTF_8): String {
	return bufferedReader(charset).use { it.readText() }
}

fun OutputStream.writeTextAndClose(string: String, charset: Charset = Charsets.UTF_8) {
	bufferedWriter(charset).use { it.write(string); it.flush() }
}

fun Closeable.closeQuietly() {
	try {
		close()
	} catch (ignore: Throwable) {
	}
}

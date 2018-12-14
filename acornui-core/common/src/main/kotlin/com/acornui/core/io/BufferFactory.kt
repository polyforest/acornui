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

package com.acornui.core.io

import com.acornui.io.NativeReadWriteBuffer
import com.acornui.io.NativeReadWriteByteBuffer

// TODO: When we migrate to new build system, switch this factory with expects/actual

/**
 * @author nbilyk
 */
interface BufferFactory {
	fun byteBuffer(capacity: Int): NativeReadWriteByteBuffer
	fun shortBuffer(capacity: Int): NativeReadWriteBuffer<Short>
	fun intBuffer(capacity: Int): NativeReadWriteBuffer<Int>
	fun floatBuffer(capacity: Int): NativeReadWriteBuffer<Float>
	fun doubleBuffer(capacity: Int): NativeReadWriteBuffer<Double>
}

expect val bufferFactory: BufferFactory

fun byteBuffer(capacity: Int): NativeReadWriteByteBuffer {
	return bufferFactory.byteBuffer(capacity)
}

fun shortBuffer(capacity: Int): NativeReadWriteBuffer<Short> {
	return bufferFactory.shortBuffer(capacity)
}

fun intBuffer(capacity: Int): NativeReadWriteBuffer<Int> {
	return bufferFactory.intBuffer(capacity)
}
fun floatBuffer(capacity: Int): NativeReadWriteBuffer<Float> {
	return bufferFactory.floatBuffer(capacity)
}

fun doubleBuffer(capacity: Int): NativeReadWriteBuffer<Double> {
	return bufferFactory.doubleBuffer(capacity)
}

fun resizableByteBuffer(initialCapacity: Int = 16): ResizableByteBuffer {
	return ResizableByteBuffer(initialCapacity)
}

fun resizableShortBuffer(initialCapacity: Int = 16): ResizableBuffer<Short, NativeReadWriteBuffer<Short>> {
	return ResizableBuffer(initialCapacity, factory = { bufferFactory.shortBuffer(it) })
}

fun resizableIntBuffer(initialCapacity: Int = 16): ResizableBuffer<Int, NativeReadWriteBuffer<Int>> {
	return ResizableBuffer(initialCapacity, factory = { bufferFactory.intBuffer(it) })
}

fun resizableFloatBuffer(initialCapacity: Int = 16): ResizableBuffer<Float, NativeReadWriteBuffer<Float>> {
	return ResizableBuffer(initialCapacity, factory = { bufferFactory.floatBuffer(it) })
}

fun resizableDoubleBuffer(initialCapacity: Int = 16): ResizableBuffer<Double, NativeReadWriteBuffer<Double>> {
	return ResizableBuffer(initialCapacity, factory = { bufferFactory.doubleBuffer(it) })
}
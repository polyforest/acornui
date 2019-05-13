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

/**
 * @author nbilyk
 */
expect object BufferFactory {
	fun byteBuffer(capacity: Int): NativeReadWriteByteBuffer
	fun shortBuffer(capacity: Int): NativeReadWriteBuffer<Short>
	fun intBuffer(capacity: Int): NativeReadWriteBuffer<Int>
	fun floatBuffer(capacity: Int): NativeReadWriteBuffer<Float>
	fun doubleBuffer(capacity: Int): NativeReadWriteBuffer<Double>
}

fun byteBuffer(capacity: Int): NativeReadWriteByteBuffer {
	return BufferFactory.byteBuffer(capacity)
}

fun shortBuffer(capacity: Int): NativeReadWriteBuffer<Short> {
	return BufferFactory.shortBuffer(capacity)
}

fun intBuffer(capacity: Int): NativeReadWriteBuffer<Int> {
	return BufferFactory.intBuffer(capacity)
}
fun floatBuffer(capacity: Int): NativeReadWriteBuffer<Float> {
	return BufferFactory.floatBuffer(capacity)
}

fun doubleBuffer(capacity: Int): NativeReadWriteBuffer<Double> {
	return BufferFactory.doubleBuffer(capacity)
}

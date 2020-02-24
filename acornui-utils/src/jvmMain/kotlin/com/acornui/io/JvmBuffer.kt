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

package com.acornui.io

/**
 * Wraps a JVM buffer with the abstract Acorn Buffer interface.
 * This is so we can use both JVM and JS buffers via the same abstraction.
 */
abstract class JvmBuffer<T>(private val _buffer: java.nio.Buffer) : NativeReadWriteBuffer<T> {

	override fun clear(): Buffer {
		_buffer.clear()
		return this
	}

	override fun flip(): Buffer {
		_buffer.flip()
		return this
	}

	override val capacity: Int
		get() = _buffer.capacity()

	override val limit: Int
		get() = _buffer.limit()

	override fun limit(newLimit: Int): Buffer {
		_buffer.limit(newLimit)
		return this
	}

	override fun mark(): Buffer {
		_buffer.mark()
		return this
	}

	override var position: Int
		get() = _buffer.position()
		set(value) {
			_buffer.position(value)
		}

	override fun reset(): Buffer {
		_buffer.reset()
		return this
	}

	override fun rewind(): Buffer {
		_buffer.rewind()
		return this
	}

	override val native: Any
		get() = _buffer
}

class JvmByteBuffer(private val buffer: java.nio.ByteBuffer) : JvmBuffer<Byte>(buffer), NativeReadWriteByteBuffer {

	override val dataSize: Int = 1

	override fun get(): Byte = buffer.get()

	override fun put(value: Byte) {
		buffer.put(value)
	}

	override val native: java.nio.ByteBuffer
		get() = buffer

	override fun getShort(): Short = buffer.short

	override fun getInt(): Int = buffer.int

	override fun getFloat(): Float = buffer.float

	override fun getDouble(): Double = buffer.double

	override fun getLong(): Long = buffer.long

	override fun putShort(value: Short) {
		buffer.putShort(value)
	}

	override fun putInt(value: Int) {
		buffer.putInt(value)
	}

	override fun putFloat(value: Float) {
		buffer.putFloat(value)
	}

	override fun putDouble(value: Double) {
		buffer.putDouble(value)
	}

	override fun putLong(value: Long) {
		buffer.putLong(value)
	}
}

class JvmShortBuffer(private val buffer: java.nio.ShortBuffer) : JvmBuffer<Short>(buffer) {

	override val dataSize: Int = 2

	override fun get(): Short {
		return buffer.get()
	}

	override fun put(value: Short) {
		buffer.put(value)
	}

	override val native: java.nio.ShortBuffer
		get() = buffer

}

class JvmIntBuffer(private val buffer: java.nio.IntBuffer) : JvmBuffer<Int>(buffer) {

	override val dataSize: Int = 4

	override fun get(): Int {
		return buffer.get()
	}

	override fun put(value: Int) {
		buffer.put(value)
	}

	override val native: java.nio.IntBuffer
		get() = buffer
}

class JvmFloatBuffer(private val buffer: java.nio.FloatBuffer) : JvmBuffer<Float>(buffer) {

	override val dataSize: Int = 4

	override fun get(): Float {
		return buffer.get()
	}

	override fun put(value: Float) {
		buffer.put(value)
	}

	override val native: java.nio.FloatBuffer
		get() = buffer
}

class JvmDoubleBuffer(private val buffer: java.nio.DoubleBuffer) : JvmBuffer<Double>(buffer) {

	override val dataSize: Int = 8

	override fun get(): Double = buffer.get()

	override fun put(value: Double) {
		buffer.put(value)
	}

	override val native: java.nio.DoubleBuffer
		get() = buffer
}

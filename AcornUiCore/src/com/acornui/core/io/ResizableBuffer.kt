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

package com.acornui.core.io

import com.acornui.io.*
import com.acornui.math.ceil

open class ResizableBuffer<T, S : ReadWriteNativeBuffer<T>>(initialCapacity: Int = 16, private val factory: (newCapacity: Int) -> S) : ReadWriteNativeBuffer<T>, BufferBase(Int.MAX_VALUE) {

	private var _wrapped: S = factory(initialCapacity)
	protected val wrapped: S
		get() = _wrapped

	override val dataSize: Int
		get() = _wrapped.dataSize

	override var _position: Int
		get() = _wrapped.position
		set(value) {
			_wrapped.position = value
		}

	override fun get(): T = _wrapped.get()

	override fun put(value: T) {
		if (position >= _wrapped.capacity) {
			resize((_wrapped.capacity * 1.75f).ceil())
		}
		_wrapped.put(value)
	}

	override fun limit(newLimit: Int): Buffer {
		if (newLimit > _wrapped.capacity) {
			resize((newLimit * 1.75f).ceil())
		}
		_wrapped.limit(minOf(_wrapped.capacity, newLimit))
		return super.limit(newLimit)
	}

	private fun resize(newCapacity: Int) {
		val newWrapped = factory(newCapacity)
		newWrapped.put(_wrapped)
		newWrapped.position = _wrapped.position
		newWrapped.limit(minOf(newCapacity, limit))
		_wrapped = newWrapped
	}

	override val native: Any
		get() = _wrapped.native
}

class ResizableByteBuffer(initialCapacity: Int = 16) : ResizableBuffer<Byte, ReadWriteNativeByteBuffer>(initialCapacity, { it -> BufferFactory.instance.byteBuffer(it) }), ReadWriteNativeByteBuffer {

	override fun getShort(): Short = wrapped.getShort()

	override fun getInt(): Int  = wrapped.getInt()

	override fun getFloat(): Float = wrapped.getFloat()

	override fun getDouble(): Double = wrapped.getDouble()

	override fun getLong(): Long = wrapped.getLong()

	override fun putShort(value: Short) {
		wrapped.putShort(value)
	}

	override fun putInt(value: Int) {
		wrapped.putInt(value)
	}

	override fun putFloat(value: Float) {
		wrapped.putFloat(value)
	}

	override fun putDouble(value: Double) {
		wrapped.putDouble(value)
	}

	override fun putLong(value: Long) {
		wrapped.putLong(value)
	}
}
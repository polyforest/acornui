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

open class ResizableBuffer<T, S : NativeReadWriteBuffer<T>>(initialCapacity: Int = 16, private val factory: (newCapacity: Int) -> S) : NativeReadWriteBuffer<T> {

	private var _limit = Int.MAX_VALUE
	private var _mark = BufferBase.UNSET_MARK

	private var _wrapped: S = factory(initialCapacity)
	protected val wrapped: S
		get() = _wrapped

	override val dataSize: Int
		get() = _wrapped.dataSize

	override val native: Any
		get() = _wrapped.native

	override fun flip(): Buffer {
		_limit = position
		_wrapped.flip()
		return this
	}

	override val hasRemaining: Boolean
		get() = position < _limit

	override val capacity: Int = Int.MAX_VALUE

	override val limit: Int
		get() = _limit

	override fun mark(): Buffer {
		_mark = position
		return this
	}

	override var position: Int
		get() = _wrapped.position
		set(value) {
			_wrapped.position = value
		}

	override fun reset(): Buffer {
		if (_mark == BufferBase.UNSET_MARK) {
			throw InvalidMarkException("Mark not set")
		}
		position = _mark
		return this
	}

	override fun rewind(): Buffer {
		_mark = BufferBase.UNSET_MARK
		position = 0
		return this
	}

	override fun get(): T = _wrapped.get()

	override fun put(value: T) {
		if (position >= _wrapped.capacity) {
			resize((_wrapped.capacity * 1.75f).ceil(), _limit)
		}
		_wrapped.put(value)
	}

	override fun clear(): Buffer {
		_wrapped.clear()
		_mark = BufferBase.UNSET_MARK
		_limit = Int.MAX_VALUE
		_wrapped.limit(_wrapped.capacity)
		return this
	}

	override fun limit(newLimit: Int): Buffer {
		if (newLimit > _wrapped.capacity) {
			resize((newLimit * 1.75f).ceil(), newLimit)
		}
		_limit = newLimit
		_wrapped.limit(newLimit)
		return this
	}

	private fun resize(newCapacity: Int, newLimit: Int) {
		val newWrapped = factory(newCapacity)
		val p = _wrapped.position
		_wrapped.limit(_wrapped.capacity)
		_wrapped.rewind()
		newWrapped.put(_wrapped)
		newWrapped.limit(minOf(newCapacity, newLimit))
		newWrapped.position = p
		_limit = newLimit
		_wrapped = newWrapped
	}
}

class ResizableByteBuffer(initialCapacity: Int = 16) : ResizableBuffer<Byte, NativeReadWriteByteBuffer>(initialCapacity, { it -> byteBuffer(it) }), NativeReadWriteByteBuffer {

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
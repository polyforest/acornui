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

abstract class BufferBase(
		/**
		 * The capacity of this buffer, which never changes.
		 */
		final override val capacity: Int
) : Buffer {

	/**
	 * `limit - 1` is the last element that can be read or written.
	 * Limit must be no less than zero and no greater than `capacity`.
	 */
	protected open var _limit: Int = capacity

	/**
	 * Mark is where position will be set when `reset()` is called.
	 * Mark is not set by default. Mark is always no less than zero and no
	 * greater than `position`.
	 */
	protected open var _mark = UNSET_MARK

	/**
	 * The current position of this buffer. Position is always no less than zero
	 * and no greater than `limit`.
	 */
	protected open var _position = 0

	init {
		if (capacity < 0) {
			throw IllegalArgumentException("capacity < 0: $capacity")
		}
	}

	fun clear(): Buffer {
		_position = 0
		_mark = UNSET_MARK
		_limit = capacity
		return this
	}

	override fun flip(): Buffer {
		_limit = _position
		_position = 0
		_mark = UNSET_MARK
		return this
	}

	override val hasRemaining: Boolean
		get() = _position < _limit

	override val limit: Int
		get() = _limit

	open fun limit(newLimit: Int): Buffer {
		if (newLimit < 0 || newLimit > capacity) {
			throw IllegalArgumentException("Bad limit (capacity $capacity): $newLimit")
		}
		_limit = newLimit
		if (_position > newLimit) {
			_position = newLimit
		}
		if (_mark > newLimit) {
			_mark = UNSET_MARK
		}
		return this
	}

	override fun mark(): Buffer {
		_mark = _position
		return this
	}

	override var position: Int
		get() {
			return _position
		}
		set(value) {
			if (value < 0 || value > _limit) {
				throw IllegalArgumentException("Bad position (limit $_limit): $value")
			}
			_position = value
			if (_mark != UNSET_MARK && _mark > _position) {
				_mark = UNSET_MARK
			}
		}

	override fun reset(): Buffer {
		if (_mark == UNSET_MARK) {
			throw InvalidMarkException("Mark not set")
		}
		_position = _mark
		return this
	}

	override fun rewind(): Buffer {
		_position = 0
		_mark = UNSET_MARK
		return this
	}


	/**
	 * Checks the current position against the limit, throwing a [BufferUnderflowException] if it is not smaller than
	 * the limit, and then increments the position.
	 * @param inc The position delta. Should be positive.
	 *
	 * @return The current position value, before it is incremented
	 */
	protected fun nextPosition(inc: Int = 1): Int {
		if (limit - position < inc)
			throw BufferUnderflowException()
		val p = position
		position = p + inc
		return p
	}

	override fun toString(): String {
		return "BufferBase(capacity=$capacity, limit=$_limit, position=$_position)"
	}

	companion object {

		/**
		 * `UNSET_MARK` means the mark has not been set.
		 */
		const val UNSET_MARK = -1
	}
}

class InvalidMarkException(message: String) : Throwable(message)
class BufferUnderflowException : Throwable()
class BufferOverflowException : Throwable()

/**
 * An interface common to [ReadBuffer] and [WriteBuffer]
 * These methods manipulate the [position] and [mark] cursors.
 */
interface Buffer {

	/**
	 * Flips this buffer.
	 *
	 * The limit is set to the current position, then the position is set to
	 * zero, and the mark is cleared.
	 *
	 * The content of this buffer is not changed.
	 *
	 * @return this buffer.
	 */
	fun flip(): Buffer

	/**
	 * Indicates if there are elements remaining in this buffer, that is if
	 * {@code position < limit}.
	 *
	 * @return {@code true} if there are elements remaining in this buffer,
	 *         {@code false} otherwise.
	 */
	val hasRemaining: Boolean

	/**
	 * The capacity of this buffer. This typically will never change.
	 */
	val capacity: Int

	/**
	 * Returns the limit of this buffer.
	 *
	 * @return the limit of this buffer.
	 */
	val limit: Int

	/**
	 * Marks the current position, so that the position may return to this point
	 * later by calling `reset()`.
	 *
	 * @return this buffer.
	 */
	fun mark(): Buffer

	/**
	 * The position of this buffer.
	 * If the mark is set and it is greater than the new position, then it is
	 * cleared.
	 * Must be not negative and not greater than limit.
	 * @return the value of this buffer's current position.
	 */
	var position: Int

	/**
	 * Returns the number of remaining elements in this buffer, that is
	 * {@code limit - position}.
	 *
	 * @return the number of remaining elements in this buffer.
	 */
	val remaining: Int
		get() {
			return limit - position
		}

	/**
	 * Resets the position of this buffer to the `mark`.
	 *
	 * @return this buffer.
	 * @exception InvalidMarkException if the mark is not set.
	 */
	fun reset(): Buffer

	/**
	 * Rewinds this buffer.
	 *
	 * The position is set to zero, and the mark is cleared. The content of this
	 * buffer is not changed.
	 *
	 * @return this buffer.
	 */
	fun rewind(): Buffer
}

interface ReadBuffer<out T> : Buffer {

	/**
	 * The number of bytes each element represents.
	 */
	val dataSize: Int

	/**
	 * Retrieves the element at the current `position()`
	 * The position marker is incremented by the data size.
	 */
	fun get(): T

}

interface ReadByteBuffer : ReadBuffer<Byte> {

	fun getChar8(): Char = get().toChar()
	fun getChar16(): Char = getShort().toChar()
	fun getShort(): Short
	fun getInt(): Int
	fun getFloat(): Float
	fun getDouble(): Double
	fun getLong(): Long
}

interface WriteBuffer<in T> : Buffer {

	/**
	 * Puts the provided element at the current `position()`.
	 * The position marker is incremented.
	 */
	fun put(value: T)

	/**
	 * Clears this buffer.
	 *
	 * While the content of this buffer is not changed, the following internal
	 * changes take place: the current position is reset back to the start of
	 * the buffer, the value of the buffer limit is made equal to the capacity
	 * and mark is cleared.
	 *
	 * @return this buffer.
	 */
	fun clear(): Buffer

	/**
	 * Sets the limit of this buffer.
	 *
	 * If the current position in the buffer is in excess of
	 * [newLimit] then, on returning from this call, it will have
	 * been adjusted to be equivalent to `newLimit`. If the mark
	 * is set and is greater than the new limit, then it is cleared.
	 *
	 * @param newLimit the new limit, must not be negative and not greater than [capacity].
	 * @return this buffer.
	 * @exception IllegalArgumentException if `newLimit` is invalid.
	 */
	fun limit(newLimit: Int): Buffer
}

fun <T> WriteBuffer<T>.put(value: ReadBuffer<T>) {
	while (value.hasRemaining) {
		put(value.get())
	}
}

fun <T> WriteBuffer<T>.put(value: Iterable<T>) {
	for (i in value) {
		put(i)
	}
}

fun <T> WriteBuffer<T>.put(value: Iterator<T>) {
	while (value.hasNext()) {
		put(value.next())
	}
}

fun WriteBuffer<Float>.put(value: FloatArray) {
	for (i in 0..value.lastIndex) {
		put(value[i])
	}
}

fun WriteBuffer<Short>.put(value: ShortArray) {
	for (i in 0..value.lastIndex) {
		put(value[i])
	}
}

/**
 * Fills the buffer with the given value.
 */
fun <T> WriteBuffer<T>.fill(value: T) {
	for (i in position..limit - 1) {
		put(value)
	}
}

interface WriteByteBuffer : WriteBuffer<Byte> {

	fun putChar8(value: Char) = put(value.toByte())
	fun putChar16(value: Char) = putShort(value.toShort())
	fun putShort(value: Short)
	fun putInt(value: Int)
	fun putFloat(value: Float)
	fun putDouble(value: Double)
	fun putLong(value: Long)
}

interface ReadWriteBuffer<T> : ReadBuffer<T>, WriteBuffer<T>
interface ReadWriteByteBuffer : ReadWriteBuffer<Byte>, WriteByteBuffer, ReadByteBuffer

// TODO: Endianness

interface NativeReadBuffer<T> : ReadBuffer<T> {

	/**
	 * Returns the underlying native implementation of this Buffer. For JVM it will be an nio.Buffer object, for
	 * js it will be an ArrayBuffer
	 */
	val native: Any
}

interface NativeReadByteBuffer : NativeReadBuffer<Byte>, ReadByteBuffer
interface NativeReadWriteBuffer<T> : NativeReadBuffer<T>, ReadWriteBuffer<T>
interface NativeReadWriteByteBuffer : NativeReadByteBuffer, ReadWriteByteBuffer, NativeReadWriteBuffer<Byte>

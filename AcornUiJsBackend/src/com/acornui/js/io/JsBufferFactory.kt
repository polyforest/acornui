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

package com.acornui.js.io

import com.acornui.core.io.BufferFactory
import com.acornui.io.BufferBase
import com.acornui.io.NativeBuffer
import org.khronos.webgl.*

/**
 * @author nbilyk
 */
class JsBufferFactory : BufferFactory {

	override fun byteBuffer(capacity: Int): NativeBuffer<Byte> {
		return JsByteBuffer(Uint8Array(capacity))
	}

	override fun shortBuffer(capacity: Int): NativeBuffer<Short> {
		return JsShortBuffer(Uint16Array(capacity))
	}

	override fun intBuffer(capacity: Int): NativeBuffer<Int> {
		return JsIntBuffer(Uint32Array(capacity))
	}

	override fun floatBuffer(capacity: Int): NativeBuffer<Float> {
		return JsFloatBuffer(Float32Array(capacity))
	}

	override fun doubleBuffer(capacity: Int): NativeBuffer<Double> {
		return JsDoubleBuffer(Float64Array(capacity))
	}
}

class JsByteBuffer(private val buffer: Uint8Array) : BufferBase<Byte>(buffer.length), NativeBuffer<Byte> {

	override fun get(): Byte {
		return buffer[_position++]
	}

	override fun put(value: Byte) {
		if (_position == _limit) _limit++
		buffer[_position++] = value
	}

	override val native: Any
		get() {
			return if (_limit == _capacity) buffer
			else buffer.subarray(0, _limit)
		}
}

class JsShortBuffer(private val buffer: Uint16Array) : BufferBase<Short>(buffer.length), NativeBuffer<Short> {

	override fun get(): Short {
		return buffer[_position++]
	}

	override fun put(value: Short) {
		if (_position == _limit) _limit++
		buffer[_position++] = value
	}

	override val native: Any
		get() {
			if (_limit == _capacity) return buffer
			else return buffer.subarray(0, _limit)
		}
}


class JsIntBuffer(private val buffer: Uint32Array) : BufferBase<Int>(buffer.length), NativeBuffer<Int> {

	override fun get(): Int {
		return buffer[_position++]
	}

	override fun put(value: Int) {
		if (_position == _limit) _limit++
		buffer[_position++] = value
	}

	override val native: Any
		get() {
			if (_limit == _capacity) return buffer
			else return buffer.subarray(0, _limit)
		}
}


class JsFloatBuffer(private val buffer: Float32Array) : BufferBase<Float>(buffer.length), NativeBuffer<Float> {

	override fun get(): Float {
		return buffer[_position++]
	}

	override fun put(value: Float) {
		if (_position == _limit) _limit++
		buffer[_position++] = value
	}

	override val native: Any
		get() {
			if (_limit == _capacity) return buffer
			else return buffer.subarray(0, _limit)
		}
}


class JsDoubleBuffer(private val buffer: Float64Array) : BufferBase<Double>(buffer.length), NativeBuffer<Double> {

	override fun get(): Double {
		return buffer[_position++]
	}

	override fun put(value: Double) {
		if (_position == _limit) _limit++
		buffer[_position++] = value
	}

	override val native: Any
		get() {
			if (_limit == _capacity) return buffer
			else return buffer.subarray(0, _limit)
		}
}
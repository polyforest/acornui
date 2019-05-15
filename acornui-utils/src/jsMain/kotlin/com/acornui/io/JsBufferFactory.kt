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

import org.khronos.webgl.*

class JsByteBuffer(private val bufferView: Uint8Array) : BufferBase(bufferView.length), NativeReadWriteByteBuffer {

	private val dataView = DataView(bufferView.buffer)
	private val littleEndian = true

	override fun get(): Byte {
		return bufferView[nextPosition()]
	}

	override fun put(value: Byte) {
		bufferView[nextPosition()] = value
	}

	override val native: Any
		get() {
			return if (_limit == capacity) bufferView
			else bufferView.subarray(0, _limit)
		}

	override val dataSize: Int = 1

	override fun getShort(): Short = dataView.getInt16(nextPosition(2), littleEndian)

	override fun getInt(): Int = dataView.getInt32(nextPosition(4), littleEndian)

	override fun getFloat(): Float = dataView.getFloat32(nextPosition(4), littleEndian)

	override fun getDouble(): Double = dataView.getFloat64(nextPosition(8), littleEndian)

	override fun getLong(): Long {
		val int1 = dataView.getInt32(nextPosition(4), littleEndian)
		val int2 = dataView.getInt32(nextPosition(4), littleEndian)
		return (int1.toLong() shl 16) or (int2.toLong())
	}

	override fun putShort(value: Short) {
		dataView.setInt16(nextPosition(2), value, littleEndian)
	}

	override fun putInt(value: Int) {
		dataView.setInt32(nextPosition(4), value, littleEndian)
	}

	override fun putFloat(value: Float) {
		dataView.setFloat32(nextPosition(4), value, littleEndian)
	}

	override fun putDouble(value: Double) {
		dataView.setFloat64(nextPosition(8), value, littleEndian)
	}

	override fun putLong(value: Long) {
		dataView.setInt32(nextPosition(4), (value shr 16).toInt(), littleEndian)
		dataView.setInt32(nextPosition(4), (value and 0xffffffff).toInt(), littleEndian)
	}
}

class JsShortBuffer(private val bufferView: Uint16Array) : BufferBase(bufferView.length), NativeReadWriteBuffer<Short> {

	override val dataSize: Int = 2

	override fun get(): Short {
		return bufferView[nextPosition()]
	}

	override fun put(value: Short) {
		bufferView[nextPosition()] = value
	}

	override val native: Any
		get() {
			return if (_limit == capacity) bufferView
			else bufferView.subarray(0, _limit)
		}
}


class JsIntBuffer(private val bufferView: Uint32Array) : BufferBase(bufferView.length), NativeReadWriteBuffer<Int> {

	override val dataSize: Int = 4

	override fun get(): Int {
		return bufferView[nextPosition()]
	}

	override fun put(value: Int) {
		bufferView[nextPosition()] = value
	}

	override val native: Any
		get() {
			return if (_limit == capacity) bufferView
			else bufferView.subarray(0, _limit)
		}
}

class JsFloatBuffer(private val bufferView: Float32Array) : BufferBase(bufferView.length), NativeReadWriteBuffer<Float> {

	override val dataSize: Int = 4

	override fun get(): Float {
		return bufferView[nextPosition()]
	}

	override fun put(value: Float) {
		bufferView[nextPosition()] = value
	}

	override val native: Any
		get() {
			return if (_limit == capacity) bufferView
			else bufferView.subarray(0, _limit)
		}
}

class JsDoubleBuffer(private val bufferView: Float64Array) : BufferBase(bufferView.length), NativeReadWriteBuffer<Double> {

	override val dataSize: Int = 8

	override fun get(): Double {
		return bufferView[nextPosition()]
	}

	override fun put(value: Double) {
		bufferView[nextPosition()] = value
	}

	override val native: Any
		get() {
			return if (_limit == capacity) bufferView
			else bufferView.subarray(0, _limit)
		}
}

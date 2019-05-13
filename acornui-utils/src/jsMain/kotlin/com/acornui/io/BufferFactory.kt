package com.acornui.io

import org.khronos.webgl.*

/**
 * @author nbilyk
 */
actual object BufferFactory {
	actual fun byteBuffer(capacity: Int): NativeReadWriteByteBuffer {
		return JsByteBuffer(Uint8Array(capacity))
	}

	actual fun shortBuffer(capacity: Int): NativeReadWriteBuffer<Short> {
		return JsShortBuffer(Uint16Array(capacity))
	}

	actual fun intBuffer(capacity: Int): NativeReadWriteBuffer<Int> {
		return JsIntBuffer(Uint32Array(capacity))
	}

	actual fun floatBuffer(capacity: Int): NativeReadWriteBuffer<Float> {
		return JsFloatBuffer(Float32Array(capacity))
	}

	actual fun doubleBuffer(capacity: Int): NativeReadWriteBuffer<Double> {
		return JsDoubleBuffer(Float64Array(capacity))
	}
}
package com.acornui.io

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * @author nbilyk
 */
actual object BufferFactory {

	actual fun byteBuffer(capacity: Int): NativeReadWriteByteBuffer {
		return JvmByteBuffer(createByteBuffer(capacity))
	}

	actual fun shortBuffer(capacity: Int): NativeReadWriteBuffer<Short> {
		return JvmShortBuffer(ByteBuffer.allocateDirect(getAllocationSize(capacity, 1)).order(ByteOrder.nativeOrder()).asShortBuffer())
	}

	actual fun intBuffer(capacity: Int): NativeReadWriteBuffer<Int> {
		return JvmIntBuffer(createByteBuffer(getAllocationSize(capacity, 2)).asIntBuffer())
	}

	actual fun floatBuffer(capacity: Int): NativeReadWriteBuffer<Float> {
		return JvmFloatBuffer(createByteBuffer(getAllocationSize(capacity, 2)).asFloatBuffer())
	}

	actual fun doubleBuffer(capacity: Int): NativeReadWriteBuffer<Double> {
		return JvmDoubleBuffer(createByteBuffer(getAllocationSize(capacity, 3)).asDoubleBuffer())
	}

	private fun createByteBuffer(capacity: Int): ByteBuffer {
		return ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder())
	}

	private fun getAllocationSize(elements: Int, elementShift: Int): Int {
		apiGetBytes(elements, elementShift)
		return elements shl elementShift
	}

	private fun apiGetBytes(elements: Int, elementShift: Int): Long {
        return (elements.toLong() and 0xFFFF_FFFFL) shl elementShift
    }
}
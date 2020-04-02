package com.acornui.io

import com.acornui.test.assertGreaterThan
import com.acornui.test.assertLessThan
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ResizableBufferTest {

	private lateinit var buffer: ResizableBuffer<Float, NativeReadWriteBuffer<Float>>

	@BeforeTest
	fun setup() {
		buffer = resizableFloatBuffer()
	}


	@Test fun put() {
		repeat(100) {
			buffer.put(it.toFloat())
		}
	}

	@Test fun ensureCapacity() {
		buffer.ensureCapacity(10)
		val newActualCapacity = buffer.wrappedCapacity
		assertTrue(newActualCapacity > 10)
		buffer.ensureCapacity(10)
		assertEquals(newActualCapacity, buffer.wrappedCapacity)
		buffer.ensureCapacity(100)
		assertGreaterThan(99, buffer.wrappedCapacity)
		assertLessThan(200, buffer.wrappedCapacity)
	}
}
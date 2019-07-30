package com.acornui.io

import kotlin.test.BeforeTest
import kotlin.test.Test

class ResizableBufferTest {

	private lateinit var buffer: ResizableBuffer<Float, NativeReadWriteBuffer<Float>>

	@BeforeTest
	fun setup() {
		buffer = resizableFloatBuffer()
	}


	@Test fun get() {
		repeat(100) {
			buffer.put(it.toFloat())
		}
	}
}
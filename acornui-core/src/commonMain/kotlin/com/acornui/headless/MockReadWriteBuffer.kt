package com.acornui.headless

import com.acornui.io.Buffer
import com.acornui.io.ReadWriteBuffer

class MockReadWriteBuffer<T> : ReadWriteBuffer<T> {

	override fun flip(): Buffer {
		return this
	}

	override val capacity: Int = 0
	override val limit: Int = 0

	override fun mark(): Buffer {
		return this
	}

	override var position: Int = 0

	override fun reset(): Buffer {
		return this
	}

	override fun rewind(): Buffer {
		return this
	}

	override val dataSize: Int = 0

	override fun get(): T {
		throw UnsupportedOperationException()
	}

	override fun put(value: T) {
	}

	override fun clear(): Buffer {
		return this
	}

	override fun limit(newLimit: Int): Buffer {
		return this
	}
}
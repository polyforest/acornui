package com.acornui.audio

import com.acornui.test.assertListEquals
import com.acornui.time.FrameDriverImpl
import kotlin.test.Test

class AudioManagerImplTest {

	@Test fun registerWithPriority() {
		val m = AudioManagerImpl(FrameDriverImpl(), simultaneousSounds = 4)
		val s0 = MockSound(2f, "s0")
		m.registerSound(s0)
		val s1 = MockSound(2f, "s1")
		m.registerSound(s1)
		val s2 = MockSound(0f, "s2")
		m.registerSound(s2)
		val s3 = MockSound(0f, "s3")
		m.registerSound(s3)
		val s4 = MockSound(0f, "s4")
		m.registerSound(s4)

		assertListEquals(listOf(s3, s4, s0, s1), m.activeSounds)
	}
}

class MockSound(
		override val priority: Float,
		private val name: String
) : Sound {
	override var onCompleted: (() -> Unit)? = null
	override var loop: Boolean = false
	override var volume: Float = 0f

	override fun setPosition(x: Float, y: Float, z: Float) {
	}

	override fun start() {
	}

	override fun stop() {
	}

	override val currentTime: Float = 0f
	override val isPlaying: Boolean = false

	override fun update() {
	}

	override fun dispose() {
	}

	override fun toString(): String {
		return "MockSound($name)"
	}
}
package com.acornui.audio

import com.acornui.test.assertListEquals
import kotlin.test.Test
import kotlin.time.Duration

class AudioManagerImplTest {

	@Test fun registerWithPriority() {
		val m = AudioManagerImpl(simultaneousSounds = 4)
		val s0 = MockSound(2.0, "s0")
		m.registerSound(s0)
		val s1 = MockSound(2.0, "s1")
		m.registerSound(s1)
		val s2 = MockSound(0.0, "s2")
		m.registerSound(s2)
		val s3 = MockSound(0.0, "s3")
		m.registerSound(s3)
		val s4 = MockSound(0.0, "s4")
		m.registerSound(s4)

		assertListEquals(listOf(s3, s4, s0, s1), m.activeSounds)
	}
}

class MockSound(
		override val priority: Double,
		private val name: String
) : Sound {
	override var onCompleted: (() -> Unit)? = null
	override var loop: Boolean = false
	override var volume: Double = 0.0

	override fun setPosition(x: Double, y: Double, z: Double) {
	}

	override fun start() {
	}

	override fun stop() {
	}

	override val currentTime: Duration = Duration.ZERO
	override val isPlaying: Boolean = false

	override fun dispose() {
	}

	override fun toString(): String {
		return "MockSound($name)"
	}
}
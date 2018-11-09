package com.acornui.core.audio

import com.acornui.test.assertListEquals
import kotlin.test.Test
import org.mockito.Mockito
// TODO - MP: Direct dependency on Mockito which is not common friendly
class AudioManagerImplTest {


	@Test fun registerWithPriority() {
		val m = AudioManagerImpl(simultaneousSounds = 4)
		val s0 = createMockSound(2f, "s0")
		m.registerSound(s0)
		val s1 = createMockSound(2f, "s1")
		m.registerSound(s1)
		val s2 = createMockSound(0f, "s2")
		m.registerSound(s2)
		val s3 = createMockSound(0f, "s3")
		m.registerSound(s3)
		val s4 = createMockSound(0f, "s4")
		m.registerSound(s4)

		assertListEquals(arrayOf(s3, s4, s0, s1), m.activeSounds)
	}

	private fun createMockSound(priority: Float, name: String): Sound {
		val s = Mockito.mock(Sound::class.java)
		Mockito.`when`(s.priority).thenReturn(priority)
		Mockito.`when`(s.toString()).thenReturn("MockSound($name)")
		return s
	}
}
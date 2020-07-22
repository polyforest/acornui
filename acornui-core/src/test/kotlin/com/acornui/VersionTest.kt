package com.acornui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class VersionTest {

	@Test fun toVersion() {
		assertEquals(Version(4, 2, 0), "4.2.0".toVersion())
		assertEquals(Version(4, 2, 1), "4.2.1".toVersion())
		assertEquals(Version(4, 2, 1), "4.2.1-SNAPSHOT".toVersion())
		assertEquals(Version(4, 0), "4".toVersion())
		assertEquals(Version(4, 1), "4.1".toVersion())
		assertFailsWith<NumberFormatException> {
			"4.2.SNAPSHOT".toVersion()
		}
	}
}
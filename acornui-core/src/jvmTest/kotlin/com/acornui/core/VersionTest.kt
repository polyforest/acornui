package com.acornui.core

import org.junit.Test
import kotlin.test.assertEquals

class VersionTest {

	@Test fun testFromStr() {
		val v = Version(1, 2, 3, 4)
		assertEquals("1.2.3.4", v.toVersionString())
		val v2 = Version.fromStr(v.toVersionString())
		assertEquals("1.2.3.4", v2.toVersionString())
		assertEquals(v, v2)
	}
}
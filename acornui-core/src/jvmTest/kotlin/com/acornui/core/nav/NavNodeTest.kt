package com.acornui.core.nav

import kotlin.test.Test
import kotlin.test.assertEquals

class NavNodeTest {

	@Test fun fromStr() {
		val n = NavNode.fromStr("foo?val1=1&val2=2")
		assertEquals("foo", n.name)
		assertEquals("1", n.params["val1"])
		assertEquals("2", n.params["val2"])
		assertEquals(2, n.params.size)
	}
}
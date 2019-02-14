package com.acornui.factory

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LazyInstanceTest {

	@Test fun lazyInstance() {
		val i = LazyInstance(this, { 3 })

		assertFalse(i.created)
		i.instance
		assertTrue(i.created)
		assertEquals(3, i.instance)
	}
}
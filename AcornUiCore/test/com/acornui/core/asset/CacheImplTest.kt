package com.acornui.core.asset

import com.acornui.core.TimeDriverConfig
import com.acornui.core.time.TimeDriverImpl
import org.junit.Test
import kotlin.test.assertEquals


class CacheImplTest {
	@Test fun testSet() {
		val timeDriver = TimeDriverImpl(TimeDriverConfig(1f, 1))
		val cache = CacheImpl(timeDriver)
		val key = object : CacheKey<String> {}
		cache[key] = "Test"
		assertEquals("Test", cache[key])
	}

	@Test fun testGc() {
		val timeDriver = TimeDriverImpl(TimeDriverConfig(1f, 1))
		val cache = CacheImpl(timeDriver, gcFrames = 10)
		val key = object : CacheKey<String> {}
		cache[key] = "Test"

		// Ensure keys with a reference are not discarded.
		cache.refInc(key)
		for (i in 0..20) timeDriver.update()
		assertEquals("Test", cache[key])

		// Ensure keys without a reference are discarded, but only after at least gcFrames.
		cache.refDec(key)
		timeDriver.update()
		timeDriver.update()
		assertEquals("Test", cache[key])
		for (i in 0..20) timeDriver.update()
		assertEquals(null, cache[key])

	}
}
package com.acornui.core.asset

import com.acornui.core.time.TimeDriverImpl
import kotlin.test.Test
import kotlin.test.assertEquals


class CacheImplTest {
	@Test fun testSet() {
		val timeDriver = TimeDriverImpl()
		val cache = CacheImpl(timeDriver)
		val key = object : CacheKey<String> {}
		cache[key] = "Test"
		assertEquals("Test", cache[key])
	}

	@Test fun testGc() {
		val timeDriver = TimeDriverImpl()
		val cache = CacheImpl(timeDriver, gcFrames = 10)
		val key = object : CacheKey<String> {}
		cache[key] = "Test"

		// Ensure keys with a reference are not discarded.
		cache.refInc(key)
		for (i in 0..20) timeDriver.update(1f)
		assertEquals("Test", cache[key])

		// Ensure keys without a reference are discarded, but only after at least gcFrames.
		cache.refDec(key)
		timeDriver.update(1f)
		timeDriver.update(1f)
		assertEquals("Test", cache[key])
		for (i in 0..20) timeDriver.update(1f)
		assertEquals(null, cache[key])

	}
}
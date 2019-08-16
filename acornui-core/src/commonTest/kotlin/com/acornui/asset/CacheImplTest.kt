package com.acornui.asset

import com.acornui.time.FrameDriver
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals


class CacheImplTest {

	@BeforeTest
	fun setup() {
		FrameDriver.clearChildren()
	}

	@Test fun testSet() {
		val cache = CacheImpl()
		val key = object : CacheKey<String> {}
		cache[key] = "Test"
		assertEquals("Test", cache[key])
	}

	@Test fun testGc() {
		val cache = CacheImpl(gcFrames = 10)
		val key = object : CacheKey<String> {}
		cache[key] = "Test"

		// Ensure keys with a reference are not discarded.
		cache.refInc(key)
		for (i in 0..20) FrameDriver.update(0f)
		assertEquals("Test", cache[key])

		// Ensure keys without a reference are discarded, but only after at least gcFrames.
		cache.refDec(key)
		FrameDriver.update(0f)
		FrameDriver.update(0f)
		assertEquals("Test", cache[key])
		for (i in 0..20) FrameDriver.update(0f)
		assertEquals(null, cache[key])
	}

	@Test fun testGc2() {
		val cache = CacheImpl(gcFrames = 10)
		val key = object : CacheKey<String> {}
		cache[key] = "Test"

		// Ensure keys with no references are not immediately discarded.
		cache.refInc(key)
		cache.refDec(key)
		FrameDriver.update(0f)
		FrameDriver.update(0f)
		FrameDriver.update(0f)
		assertEquals("Test", cache[key])
		cache.refInc(key)
		for (i in 0..20) FrameDriver.update(0f)
		assertEquals("Test", cache[key])

	}
}
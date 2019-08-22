package com.acornui.asset

import com.acornui.time.FrameDriver
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals


class CacheImplTest {

	@BeforeTest
	fun setup() {
		FrameDriver.clear()
	}

	@Test fun testSet() {
		val cache = CacheImpl()
		val key = "key"
		cache[key] = "Test"
		assertEquals<String?>("Test", cache[key])
	}

	@Test fun testGc() {
		val cache = CacheImpl(gcFrames = 10)
		val key = "key"
		cache[key] = "Test"

		// Ensure keys with a reference are not discarded.
		cache.refInc(key)
		for (i in 0..20) FrameDriver.dispatch(0f)
		assertEquals<String?>("Test", cache[key])

		// Ensure keys without a reference are discarded, but only after at least gcFrames.
		cache.refDec(key)
		FrameDriver.dispatch(0f)
		FrameDriver.dispatch(0f)
		assertEquals<String?>("Test", cache[key])
		for (i in 0..20) FrameDriver.dispatch(0f)
		assertEquals<String?>(null, cache[key])
	}

	@Test fun testGc2() {
		val cache = CacheImpl(gcFrames = 10)
		val key = "key"
		cache[key] = "Test"

		// Ensure keys with no references are not immediately discarded.
		cache.refInc(key)
		cache.refDec(key)
		FrameDriver.dispatch(0f)
		FrameDriver.dispatch(0f)
		FrameDriver.dispatch(0f)
		assertEquals<String?>("Test", cache[key])
		cache.refInc(key)
		for (i in 0..20) FrameDriver.dispatch(0f)
		assertEquals<String?>("Test", cache[key])

	}
}
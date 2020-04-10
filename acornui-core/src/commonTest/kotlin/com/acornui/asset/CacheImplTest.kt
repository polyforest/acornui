package com.acornui.asset

import com.acornui.di.ContextImpl
import com.acornui.time.FrameDriver
import com.acornui.time.FrameDriverImpl
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals


class CacheImplTest {

	private val frameDriver = FrameDriverImpl()
	private val context = ContextImpl(listOf(FrameDriver to frameDriver))

	@BeforeTest
	fun setup() {
		frameDriver.clear()
	}

	@Test fun testSet() {
		val cache = CacheImpl(context)
		val key = "key"
		cache[key] = "Test"
		assertEquals<String?>("Test", cache[key])
	}

	@Test fun testGc() {
		val cache = CacheImpl(context, gcFrames = 10)
		val key = "key"
		cache[key] = "Test"

		// Ensure keys with a reference are not discarded.
		cache.refInc(key)
		for (i in 0..20) frameDriver.dispatch(0f)
		assertEquals<String?>("Test", cache[key])

		// Ensure keys without a reference are discarded, but only after at least gcFrames.
		cache.refDec(key)
		frameDriver.dispatch(0f)
		frameDriver.dispatch(0f)
		assertEquals<String?>("Test", cache[key])
		for (i in 0..20) frameDriver.dispatch(0f)
		assertEquals<String?>(null, cache[key])
	}

	@Test fun testGc2() {
		val cache = CacheImpl(context, gcFrames = 10)
		val key = "key"
		cache[key] = "Test"

		// Ensure keys with no references are not immediately discarded.
		cache.refInc(key)
		cache.refDec(key)
		frameDriver.dispatch(0f)
		frameDriver.dispatch(0f)
		frameDriver.dispatch(0f)
		assertEquals<String?>("Test", cache[key])
		cache.refInc(key)
		for (i in 0..20) frameDriver.dispatch(0f)
		assertEquals<String?>("Test", cache[key])

	}
}
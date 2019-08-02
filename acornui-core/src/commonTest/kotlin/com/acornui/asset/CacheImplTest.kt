package com.acornui.asset

import com.acornui.ChildRo
import com.acornui.ParentRo
import com.acornui.TimeDriverConfig
import com.acornui.UpdatableChild
import com.acornui.collection.ActiveList
import com.acornui.time.TimeDriver
import com.acornui.time.TimeDriverImpl
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals


class CacheImplTest {

	private val timeDriver = object : TimeDriver {
		override val config: TimeDriverConfig
			get() = throw UnsupportedOperationException()

		override fun activate() {
			throw UnsupportedOperationException()
		}

		override fun update() {
			children.forEach { it.update(1f) }
		}
		override val parent: ParentRo<ChildRo>?
			get() = throw UnsupportedOperationException()
		override val children = ActiveList<UpdatableChild>()

		override fun <S : UpdatableChild> addChild(index: Int, child: S): S {
			children.add(index, child)
			return child
		}

		override fun removeChild(index: Int): UpdatableChild {
			return children.removeAt(index)
		}
	}

	@BeforeTest
	fun setup() {
		timeDriver.clearChildren()
	}

	@Test fun testSet() {
		val timeDriver = TimeDriverImpl(TimeDriverConfig(1f, 1))
		val cache = CacheImpl(timeDriver)
		val key = object : CacheKey<String> {}
		cache[key] = "Test"
		assertEquals("Test", cache[key])
	}

	@Test fun testGc() {
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

	@Test fun testGc2() {
		val cache = CacheImpl(timeDriver, gcFrames = 10)
		val key = object : CacheKey<String> {}
		cache[key] = "Test"

		// Ensure keys with no references are not immediately discarded.
		cache.refInc(key)
		cache.refDec(key)
		timeDriver.update()
		timeDriver.update()
		timeDriver.update()
		assertEquals("Test", cache[key])
		cache.refInc(key)
		for (i in 0..20) timeDriver.update()
		assertEquals("Test", cache[key])

	}
}
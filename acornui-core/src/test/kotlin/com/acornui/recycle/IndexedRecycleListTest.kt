/*
 * Copyright 2019 Poly Forest, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.acornui.recycle

import com.acornui.assertionsEnabled
import com.acornui.test.assertUnorderedListEquals
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class IndexedRecycleListTest {

	@BeforeTest
	fun setUp() {
		assertionsEnabled = true
		TestObj.changedCount = 0
		TestObj.constructionCount = 0
	}

	/**
	 * Test that the second obtain / flip set recycles based on index.
	 */
	@Test
	fun smartObtain() {
		val c = IndexedPool(ClearableObjectPool { TestObj() })
		for (i in 5..9) {
			c.obtain(i).value = i
		}
		c.flip()
		assertChangedCount(5)
		assertConstructionCount(5)
		// Second set, the exact same values:
		for (i in 5..9) {
			c.obtain(i).value = i
		}
		c.flip()
		assertChangedCount(0)
		assertConstructionCount(0)

		// Third set, shifted 2:
		for (i in 7..11) {
			c.obtain(i).value = i
		}
		c.flip()
		assertChangedCount(2)
		assertConstructionCount(0)

		// Reversed
		for (i in 11 downTo 7) {
			c.obtain(i).value = i
		}
		c.flip()
		assertChangedCount(0)
		assertConstructionCount(0)

		// Split forward
		for (i in 8..11) {
			c.obtain(i).value = i
		}
		assertChangedCount(0)
		for (i in 7 downTo 5) {
			c.obtain(i).value = i
		}
		assertChangedCount(2)
		assertConstructionCount(2)
		c.flip()

		// Split reversed
		for (i in 8 downTo 11) {
			c.obtain(i).value = i
		}
		assertChangedCount(0)
		for (i in 7 downTo 5) {
			c.obtain(i).value = i
		}
		assertChangedCount(0)
		assertConstructionCount(0)
		c.flip()
	}

	/**
	 * Test that obtaining forwards, but a negative offset or obtaining reversed, but a positive offset is optimal.
	 */
	@Test
	fun smartObtainOppositeDirection() {
		val c = IndexedPool(ClearableObjectPool { TestObj() })
		for (i in 5..9) {
			c.obtain(i).value = i
		}
		c.flip()
		assertChangedCount(5)
		assertConstructionCount(5)

		// Step down one
		for (i in 4..8) {
			c.obtain(i).value = i
		}
		c.flip()
		assertChangedCount(1)
		assertConstructionCount(0)

		// Step up one
		for (i in 9 downTo 5) {
			c.obtain(i).value = i
		}
		c.flip()
		assertChangedCount(1)
		assertConstructionCount(0)
	}


	@Test
	fun testNonSequential1() {
		val c = IndexedPool(ClearableObjectPool { TestObj() })
		c.obtain(9).value = 9
		c.obtain(11).value = 11
		c.flip()
		assertUnorderedListEquals(listOf(9, 11), c.getUnused())
		c.obtain(11)
		assertUnorderedListEquals(listOf(9), c.getUnused())
		c.obtain(9)
		assertUnorderedListEquals(listOf(), c.getUnused())
		assertEquals(Int.MAX_VALUE, c.obtain(12).value)
	}

	@Test
	fun testNonSequential2() {
		val c = IndexedPool(ClearableObjectPool { TestObj() })
		c.obtain(9)
		c.obtain(7)
	}

	@Test
	fun forEach() {
		val c = IndexedPool(ClearableObjectPool { TestObj() })
		for (i in 5..9) {
			c.obtain(i).value = i
		}
		c.flip()

		assertUnorderedListEquals(listOf(5, 6, 7, 8, 9), c.getUnused())
		c.obtain(6)
		assertUnorderedListEquals(listOf(5, 7, 8, 9), c.getUnused())
		c.obtain(7)
		assertUnorderedListEquals(listOf(5, 8, 9), c.getUnused())
		c.obtain(5)
		assertUnorderedListEquals(listOf(8, 9), c.getUnused())
		c.clear()
		assertUnorderedListEquals(listOf(), c.getUnused())
	}

	@Test
	fun getObtainedSerial() {
		val c = IndexedPool(ClearableObjectPool { TestObj() })
		for (i in 5..9) {
			c.obtain(i).value = i
		}
		for (i in 5..9) {
			assertEquals(i, c.getObtainedByIndex(i).value)
		}
		assertFails { c.getObtainedByIndex(4) }
		assertFails { c.getObtainedByIndex(10) }
	}

	private fun IndexedPool<TestObj>.getUnused(): List<Int> {
		val list = ArrayList<Int>()
		forEachUnused { _, it ->
			list.add(it.value)
		}
		return list
	}

	private fun assertChangedCount(i: Int) {
		assertEquals(i, TestObj.changedCount)
		TestObj.changedCount = 0
	}

	private fun assertConstructionCount(i: Int) {
		assertEquals(i, TestObj.constructionCount)
		TestObj.constructionCount = 0
	}
}

private class TestObj : Clearable {

	init {
		constructionCount++
	}

	private var _value: Int = Int.MAX_VALUE

	var value: Int
		get() = _value
		set(value) {
			if (_value == value) return
			_value = value
			changedCount++
		}

	override fun clear() {
		_value = Int.MAX_VALUE
	}

	companion object {
		var changedCount = 0
		var constructionCount = 0
	}
}

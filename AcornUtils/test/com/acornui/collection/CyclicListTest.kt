package com.acornui.collection

import com.acornui.math.MathUtils
import com.acornui.test.assertListEquals
import com.acornui.test.benchmark
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

class CyclicListTest {

	@Test fun unshift() {
		val list = CyclicList<Int>(5)
		list.unshift(1)
		list.unshift(2)
		list.unshift(3)
		list.unshift(4)

		assertListEquals(arrayOf(4, 3, 2, 1), list)

		list.unshift(5) // Will cause list to grow
		list.unshift(6)
		list.unshift(7)
		list.unshift(8)

		assertListEquals(arrayOf(8, 7, 6, 5, 4, 3, 2, 1), list)
	}

	@Test fun shift() {
		var shifted: Int?
		val list = CyclicList<Int>(5)
		list.addAll(1, 2, 3, 4)
		assertListEquals(arrayOf(1, 2, 3, 4), list)

		list.shift()
		assertListEquals(arrayOf(2, 3, 4), list)

		list.shift()
		shifted = list.shift()
		assertEquals(3, shifted)
		assertListEquals(arrayOf(4), list)

		shifted = list.shift()
		assertEquals(4, shifted)
		assertTrue(list.isEmpty())
	}

	@Test fun push() {
		val list = CyclicList<Int>(5)
		list.add(1)
		list.add(2)
		list.add(3)
		list.add(4)

		assertListEquals(arrayOf(1, 2, 3, 4), list)

		list.add(5) // Will cause list to grow
		list.add(6)
		list.add(7)
		list.add(8)

		assertListEquals(arrayOf(1, 2, 3, 4, 5, 6, 7, 8), list)
	}

	@Test fun pop() {
		var popped: Int?
		val list = CyclicList<Int>(5)
		list.unshift(1, 2, 3, 4)
		assertListEquals(arrayOf(4, 3, 2, 1), list)

		popped = list.pop()
		assertEquals(1, popped)
		assertListEquals(arrayOf(4, 3, 2), list)
		assertEquals(3, list.size)

		popped = list.pop()
		assertEquals(2, popped)
		assertListEquals(arrayOf(4, 3), list)

		popped = list.pop()
		assertEquals(3, popped)
		assertListEquals(arrayOf(4), list)

		popped = list.pop()
		assertEquals(4, popped)

	}

	@Test fun getItemAt() {
		val list = CyclicList<Int>(5)
		list.addAll(1, 2, 3, 4)
		assertListEquals(arrayOf(1, 2, 3, 4), list)
		assertEquals(2, list[1])
		assertEquals(1, list[0])
		assertEquals(4, list[3])
		//assertNull(list[4])
	}

	@Test fun size() {
		val list = CyclicList<Int>(5)
		list.addAll(1, 2, 3, 4)
		assertEquals(4, list.size)
		list.addAll(1, 2, 3, 4)
		assertEquals(8, list.size)
		list.shift()
		list.shift()
		list.shift()
		assertEquals(5, list.size)
		list.clear()
		assertEquals(0, list.size)
	}

	@Test fun clear() {
		val list = CyclicList<Int>(5)
		list.addAll(1, 2, 3, 4)
		list.unshift(0)
		list.unshift(-1)
		list.clear()
		assertEquals(0, list.size)

	}

	@Test fun first_returnsFirst() {
		val list = CyclicList<Int>(5)
		list.addAll(1, 2, 3, 4)
		assertTrue(list.first() == 1)
	}

	@Test fun first_empty_returnsNull() {
		val list = CyclicList<Int>(5)
		assertNull(list.firstOrNull())
	}

	@Test fun last_empty_returnsNull() {
		val list = CyclicList<Int>(5)
		assertNull(list.lastOrNull())
	}

	@Test fun last_returnsLast() {
		val list = CyclicList<Int>(5)
		list.addAll(1, 2, 3, 4)
		assertTrue(list.last() == 4)
	}

	/**
	 * regression
	 */
	@Test fun shiftAcrossBoundary_doesNotBreakStartIndex() {
		val list = CyclicList<Int>(4)
		list.add(1)
		list.unshift(2)
		list.shift()
		list.shift()
	}


	@Test fun add() {
		val list = CyclicList<Int>(5)
		list.addAll(1, 2, 4, 5)
		list.add(2, 3)
		assertListEquals(listOf(1, 2, 3, 4, 5), list)
		list.add(0, 0)
		assertListEquals(listOf(0, 1, 2, 3, 4, 5), list)
		list.add(6, 6)
		assertListEquals(listOf(0, 1, 2, 3, 4, 5, 6), list)
		list.add(3, 6)
		assertListEquals(listOf(0, 1, 2, 6, 3, 4, 5, 6), list)
	}

	@Test fun removeAt() {
		val list = CyclicList<Int>(5)
		list.addAll(1, 2, 4, 5, 6, 7, 8, 9)
		list.removeAt(2)
		assertListEquals(listOf(1, 2, 5, 6, 7, 8, 9), list)
		list.removeAt(0)
		assertListEquals(listOf(2, 5, 6, 7, 8, 9), list)
		list.removeAt(3)
		assertListEquals(listOf(2, 5, 6, 8, 9), list)
		list.removeAt(4)
		assertListEquals(listOf(2, 5, 6, 8), list)
	}

	@Test fun speedTest() {
		val n = 1_000
		val list = CyclicList<Int>(n)
		val cyclicListSpeed = benchmark(100) {
			for (i in 0..n - 1) {
				list.unshift(i)
			}
		}
		val list2 = ArrayList<Int>(n)
		val arrayListSpeed = benchmark(100) {
			for (i in 0..n - 1) {
				list2.add(0, i)
			}
		}
		if (cyclicListSpeed * 10f > arrayListSpeed) {
			fail("CyclicList unshift not fast enough. $cyclicListSpeed $arrayListSpeed")
		}

		val cyclicListSpeed2 = benchmark(100) {
			for (i in 0..n - 1) {
				list.shift()
			}
		}
		val arrayListSpeed2 = benchmark(100) {
			for (i in 0..n - 1) {
				list2.removeAt(0)
			}
		}
		if (cyclicListSpeed2 * 10f > arrayListSpeed2) {
			fail("CyclicList shift not fast enough. $cyclicListSpeed2 $arrayListSpeed2")
		}

		val cyclicListSpeed3 = benchmark(100) {
			for (i in 0..n - 1) {
				list.add(i)
			}
		}
		val arrayListSpeed3 = benchmark(100) {
			for (i in 0..n - 1) {
				list2.add(i)
			}
		}
		println("1 $cyclicListSpeed3 2 $arrayListSpeed3")

	}

	@Test fun shiftAll() {
		val list = CyclicList<Int>(16)
		list.addAll(0, 1, 2, 3, 4, 5, 6)
		list.shiftAll(3)
		assertListEquals(listOf(3, 4, 5, 6, 0, 1, 2), list)

		list.shiftAll(-3)
		assertListEquals(listOf(0, 1, 2, 3, 4, 5, 6), list)

	}


}
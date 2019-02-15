package com.acornui.collection

import com.acornui.observe.IndexBinding
import com.acornui.observe.bindIndex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IndexBindingTest {

	@Test fun bind() {
		val list = activeListOf(0, 1, 2, 3, 3, 3, 4, 5)

		val binding = list.bindIndex(4)

		assertEquals(3, binding.element)
		assertEquals(4, binding.index)

		list.removeAt(3)
		assertEquals(3, binding.element)
		assertEquals(3, binding.index)

		list.add(3, 3)
		assertEquals(3, binding.element)
		assertEquals(4, binding.index)

		list.add(3, 4)
		assertEquals(3, binding.element)
		assertEquals(5, binding.index)

		list.add(5, 1)
		assertEquals(3, binding.element)
		assertEquals(6, binding.index)

		list.add(7, 1)
		assertEquals(3, binding.element)
		assertEquals(6, binding.index)

		list.removeAt(7)
		assertEquals(3, binding.element)
		assertEquals(6, binding.index)

		list.removeAt(5)
		assertEquals(3, binding.element)
		assertEquals(5, binding.index)

		list.removeAt(5)
		assertTrue(binding.isEmpty)
		assertEquals(-1, binding.index)
	}

	@Test fun reset() {
		val list = activeListOf(0, 1, 2, 3, 3, 3, 4, 5)

		val binding = list.bindIndex(4, equality = null)

		assertEquals(3, binding.element)
		assertEquals(4, binding.index)

		list.dirty()

		assertTrue(binding.isEmpty)
		assertEquals(-1, binding.index)
	}

	@Test fun resetWithRecover() {
		val list = activeListOf(0, 1, 2, 3, 3, 3, 4, 5)

		val binding = list.bindIndex(4)

		assertEquals(3, binding.element)
		assertEquals(4, binding.index)

		list.dirty()

		assertEquals(3, binding.element)
		assertEquals(4, binding.index)

		// Test a recovery where the element is no longer at the same index.
		list.batchUpdate {
			list.add(0, 0)
			list.add(0, 0)
		}
		assertEquals(3, binding.element)
		// The index at this point could be a position of any of the 3 3 elements.
		assertEquals(list[binding.index], binding.element)

	}

	@Test fun changed() {
		val list = activeListOf(0, 1, 2, 3, 3, 3, 4, 5)

		val binding = list.bindIndex(4)

		assertEquals(binding.element, 3)
		assertEquals(binding.index, 4)

		list[3] = 4
		list[5] = 4

		assertEquals(binding.element, 3)
		assertEquals(binding.index, 4)

		list[4] = 4

		assertTrue(binding.isEmpty)
		assertEquals(-1, binding.index)
	}

	@Test fun equalityRecover() {
		val list = listOf(N(0, 0), N(1, 1), N(2, 2), N(3, 3), N(4, 4), N(5, 5))

		val binding = IndexBinding(list, 3) { a, b -> a.id == b.id }

		assertEquals(binding.element, N(3, 3))
		assertEquals(binding.index, 3)

		binding.data(listOf(N(0, 0), N(3, 5), N(1, 1), N(2, 2), N(4, 4), N(5, 5)))
		assertEquals(binding.index, 1)
		assertEquals(binding.element, N(3, 5))
	}

	@Test fun equalityRecoverActiveList() {
		val source = activeListOf(N(0, 3), N(1, 8), N(2, 4), N(3, 1), N(4, 9), N(5, 10), N(6, 0))
		val list = ListView(source)

		val binding = IndexBinding(list, 3) { a, b -> a.id == b.id }

		assertEquals(N(3, 1), binding.element)
		assertEquals(3, binding.index)

		list.sortComparator = { a, b -> a.value.compareTo(b.value) }
		assertEquals(N(3, 1), binding.element)
		assertEquals(1, binding.index)

		// Added/removed.
		source[3] = N(3, 5)
		// N(id=6, value=0), N(id=0, value=3), N(id=2, value=4), N(id=3, value=5), N(id=1, value=8), N(id=4, value=9), N(id=5, value=10)
		assertEquals(N(3, 5), binding.element)
		assertEquals(3, binding.index)

		// Changed.
		source[3] = N(3, 7)
		// N(id=6, value=0), N(id=0, value=3), N(id=2, value=4), N(id=3, value=7), N(id=1, value=8), N(id=4, value=9), N(id=5, value=10)
		assertEquals(N(3, 7), binding.element)
		assertEquals(3, binding.index)
	}
}

private data class N(val id: Int, val value: Int)
@file:Suppress("UNUSED_ANONYMOUS_PARAMETER")

package com.acornui.collection

import com.acornui.test.assertListEquals
import kotlin.test.Test
import kotlin.test.assertEquals


class ListViewTest {
	
	@Test fun add() {
		val source = ActiveList<Int>()
		val listView = ListView(source)

		for (i in 0..10) {
			source.add(i)
		}
		assertListEquals(arrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10), listView)
	}

	@Test fun filter() {
		val source = ActiveList<Int>()
		val listView = ListView(source)
		listView.filter = { it % 2 == 0 } // Filter out all odd numbers.
		source.addAll(arrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9))

		// Expect the source not to change.
		assertListEquals(arrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9), source)

		// Expect the list view to show only filtered values.
		assertListEquals(arrayOf(2, 4, 6, 8), listView)

		source.add(10)
		source.add(0, 14)
		source.add(0, 15)
		source.add(0, 17)
		source.add(1, 16)
		assertListEquals(arrayOf(16, 14, 2, 4, 6, 8, 10), listView)
		assertListEquals(arrayOf(17, 16, 15, 14, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10), source)

	}

	@Test fun sort() {
		val source = ActiveList<Int>()
		val listView = ListView(source)
		source.addAll(arrayOf(6, 4, 3, 2, 5, 1, 7, 9, 8))

		assertListEquals(arrayOf(6, 4, 3, 2, 5, 1, 7, 9, 8), listView)

		listView.sort()
		assertListEquals(arrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9), listView)

		source.add(10)
		source.add(0, 14)
		source.add(0, 15)
		source.add(0, 17)
		source.add(1, 16)

		assertListEquals(arrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 14, 15, 16, 17), listView)
		assertListEquals(arrayOf(17, 16, 15, 14, 6, 4, 3, 2, 5, 1, 7, 9, 8, 10), source)
	}

	@Test fun sortZeroCompare() {
		val source = ActiveList<Int>()
		val listView = ListView(source)
		source.addAll(arrayOf(6, 4, 3, 2, 5, 1, 7, 9, 8))

		assertListEquals(arrayOf(6, 4, 3, 2, 5, 1, 7, 9, 8), listView)

		listView.sortComparator = { o1, o2 -> 0 }

		// A sort comparator function with a 0 return value should not change the order of the original list.
		assertListEquals(arrayOf(6, 4, 3, 2, 5, 1, 7, 9, 8), listView)

		listView.sort()
		assertListEquals(arrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9), listView)

		// And back again.
		listView.sortComparator = { o1, o2 -> 0 }
		assertListEquals(arrayOf(6, 4, 3, 2, 5, 1, 7, 9, 8), listView)
		listView.sortComparator = null
		assertListEquals(arrayOf(6, 4, 3, 2, 5, 1, 7, 9, 8), listView)
	}

	@Test fun filterAndSort() {
		val source = ActiveList<Int>()
		val listView = ListView(source)
		source.addAll(arrayOf(6, 4, 3, 2, 5, 1, 7, 9, 8))


		listView.sort()
		listView.filter = { it % 2 == 0 }

		assertListEquals(arrayOf(2, 4, 6, 8), listView)

		source.add(10)
		source.add(0, 14)
		source.add(0, 15)
		source.add(0, 17)
		source.add(1, 16)

		assertListEquals(arrayOf(2, 4, 6, 8, 10, 14, 16), listView)
		assertListEquals(arrayOf(17, 16, 15, 14, 6, 4, 3, 2, 5, 1, 7, 9, 8, 10), source)
	}

	@Test fun filterAndSortObj() {
		val source = ActiveList<Foo556>()
		val listView = ListView(source)
		source.addAll(arrayOf(Foo556(6), Foo556(4), Foo556(3), Foo556(2), Foo556(5), Foo556(1), Foo556(7), Foo556(9), Foo556(8)))


		listView.sort()
		listView.filter = { it.i % 2 == 0 }

		assertListEquals(arrayOf(Foo556(2), Foo556(4), Foo556(6), Foo556(8)), listView)

		source.add(Foo556(10))
		source.add(0, Foo556(14))
		source.add(0, Foo556(15))
		source.add(0, Foo556(17))
		source.add(1, Foo556(16))

		assertListEquals(arrayOf(Foo556(2), Foo556(4), Foo556(6), Foo556(8), Foo556(10), Foo556(14), Foo556(16)), listView)
		assertListEquals(arrayOf(Foo556(17), Foo556(16), Foo556(15), Foo556(14), Foo556(6), Foo556(4), Foo556(3), Foo556(2), Foo556(5), Foo556(1), Foo556(7), Foo556(9), Foo556(8), Foo556(10)), source)
	}

	@Test fun notifyElementModified() {
		val source = ActiveList<Foo556>()
		val listView = ListView(source)
		source.addAll(arrayOf(Foo556(6), Foo556(10), Foo556(3), Foo556(2), Foo556(5), Foo556(1), Foo556(7), Foo556(9), Foo556(8)))


		listView.filter = { it.i % 2 == 0 }

		assertListEquals(arrayOf(Foo556(6), Foo556(10), Foo556(2), Foo556(8)), listView) // Note: The read causes a validation.

		source[2].i = 4

		assertListEquals(arrayOf(Foo556(6), Foo556(10), Foo556(2), Foo556(8)), listView) // Assert that the list is stale.
		source.notifyElementModified(2) // Notify update.
		assertListEquals(arrayOf(Foo556(6), Foo556(10), Foo556(4), Foo556(2), Foo556(8)), listView)

		source[2].i = 3
		source.notifyElementModified(2) // Notify update.
		assertListEquals(arrayOf(Foo556(6), Foo556(10), Foo556(2), Foo556(8)), listView)
	}

	@Test fun notifyElementModifiedWithSort() {
		val source = ActiveList<Foo556>()
		val listView = ListView(source)
		source.addAll(arrayOf(Foo556(6), Foo556(10), Foo556(3), Foo556(2), Foo556(5), Foo556(1), Foo556(7), Foo556(9), Foo556(8)))


		listView.filter = { it.i % 2 == 0 }
		listView.sort()

		assertListEquals(arrayOf(Foo556(2), Foo556(6), Foo556(8), Foo556(10)), listView) // Note: The read causes a validation.

		source[2].i = 4

		assertListEquals(arrayOf(Foo556(2), Foo556(6), Foo556(8), Foo556(10)), listView) // Assert that the list is stale.
		source.notifyElementModified(2) // Notify update.
		assertListEquals(arrayOf(Foo556(2), Foo556(4), Foo556(6), Foo556(8), Foo556(10)), listView)

		source[2].i = 12
		source.notifyElementModified(2) // Notify update.
		assertListEquals(arrayOf(Foo556(2), Foo556(6), Foo556(8), Foo556(10), Foo556(12)), listView)

		source[2].i = 3
		source.notifyElementModified(2) // Notify update.
		assertListEquals(arrayOf(Foo556(2), Foo556(6), Foo556(8), Foo556(10)), listView)
	}

	@Test fun elementChangedWFilter() {
		val source = ActiveList<Foo556>()
		val listView = ListView(source)
		source.addAll(arrayOf(Foo556(6), Foo556(10), Foo556(3), Foo556(2), Foo556(5), Foo556(1), Foo556(7), Foo556(9), Foo556(8)))


		listView.filter = { it.i % 2 == 0 }

		assertListEquals(arrayOf(Foo556(6), Foo556(10), Foo556(2), Foo556(8)), listView) // Note: The read causes a validation.

		source[2] = Foo556(4)

		assertListEquals(arrayOf(Foo556(6), Foo556(10), Foo556(4), Foo556(2), Foo556(8)), listView)

		source[2] = Foo556(3)
		assertListEquals(arrayOf(Foo556(6), Foo556(10), Foo556(2), Foo556(8)), listView)

		source[2] = Foo556(7)
		source[4] = Foo556(7)
		assertListEquals(arrayOf(Foo556(6), Foo556(10), Foo556(2), Foo556(8)), listView)
	}

	@Test fun addedHandler() {
		val source = ActiveList<Foo556>()
		val listView = ListView(source)
		listView.filter = { it.i % 2 == 0 }
		var c = 0
		listView.added.add { _, _ -> c++ }
		listView.validate()
		source.addAll(arrayOf(Foo556(6), Foo556(10), Foo556(3), Foo556(2), Foo556(5), Foo556(1), Foo556(7), Foo556(9), Foo556(8)))

		assertEquals(4, c)
		source.add(Foo556(7))
		assertEquals(4, c)
		source.add(Foo556(8))
		assertEquals(5, c)

		listView.sort()
		listView.validate()

		assertEquals(5, c)
		source.add(Foo556(7))
		assertEquals(5, c)
		source.add(Foo556(8))
		assertEquals(6, c)
	}

	@Test fun removedHandler() {
		val source = ActiveList<Foo556>()
		val listView = ListView(source)
		listView.filter = { it.i % 2 == 0 }
		var c = 0
		listView.removed.add { _, _ -> c++ }
		listView.validate()
		source.addAll(arrayOf(Foo556(6), Foo556(10), Foo556(3), Foo556(2), Foo556(5), Foo556(1), Foo556(7), Foo556(9), Foo556(8)))


		source.remove(Foo556(7))
		assertEquals(0, c)
		source.remove(Foo556(8))
		assertEquals(1, c)

		listView.sort()
		listView.validate()

		assertEquals(1, c)
		source.remove(Foo556(3))
		assertEquals(1, c)
		source.remove(Foo556(10))
		assertEquals(2, c)
	}


	@Test fun changedHandler() {
		val source = ActiveList<Foo556>()
		val listView = ListView(source)
		listView.filter = { it.i % 2 == 0 }
		var removedC = 0
		listView.removed.add { _, _ -> removedC++ }
		var addedC = 0
		listView.added.add { _, _ -> addedC++ }
		var changedC = 0
		listView.changed.add { _, _, _ -> changedC++ }
		listView.validate()
		source.addAll(arrayOf(Foo556(6), Foo556(10), Foo556(3), Foo556(2), Foo556(5), Foo556(1), Foo556(7), Foo556(9), Foo556(8)))

		// Removed
		source[source.lastIndex] = Foo556(-1)
		assertEquals(0, changedC)
		assertEquals(1, removedC)
		// Added
		source[2] = Foo556(4)
		assertEquals(5, addedC)

		// Modified
		source[2] = Foo556(6)
		assertEquals(1, removedC)
		assertEquals(5, addedC)
		assertEquals(1, changedC)

		listView.sort()
		listView.validate()

		source[2] = Foo556(0)
		assertEquals(2, removedC)
		assertEquals(6, addedC)
		assertEquals(1, changedC)
	}

	@Test
	fun sourceIndexToLocal() {
		val source = listOf(6, 4, 3, 2, 5, 1, 7, 9, 8)
		val listView = ListView(source)

		listView.sort()
		listView.filter = { it % 2 == 0 }

		// 2, 4, 6, 8

		assertEquals(2, listView.sourceIndexToLocal(0))
		assertEquals(1, listView.sourceIndexToLocal(1))
		assertEquals(-1, listView.sourceIndexToLocal(2))
		assertEquals(0, listView.sourceIndexToLocal(3))

		// 8, 6, 4, 2
		listView.reversed = true
		assertEquals(1, listView.sourceIndexToLocal(0))
		assertEquals(2, listView.sourceIndexToLocal(1))
		assertEquals(-1, listView.sourceIndexToLocal(2))
		assertEquals(3, listView.sourceIndexToLocal(3))


	}

}

private data class Foo556(var i: Int) : Comparable<Foo556> {
	override fun compareTo(other: Foo556): Int {
		return this.i.compareTo(other.i)
	}
}
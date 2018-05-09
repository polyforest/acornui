/*
 * Copyright 2017 Nicholas Bilyk
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

package com.acornui.collection

import com.acornui.test.assertListEquals
import org.junit.Test

import org.junit.Assert.*

class ListUtilsKtTest {
	@Test
	fun arrayCopy() {
	}

	@Test
	fun copy() {
	}

	@Test
	fun addAllUnique() {
	}

	@Test
	fun addAllUnique1() {
	}

	@Test
	fun sortedInsertionIndex() {
	}

	@Test
	fun sortedInsertionIndex1() {
	}

	@Test
	fun addSorted() {
	}

	@Test
	fun addSorted1() {
	}

	@Test
	fun getArrayListPool() {
	}

	@Test
	fun addOrSet() {
	}

	@Test
	fun fill() {
	}

	@Test
	fun addAll2() {
	}

	@Test
	fun addAll21() {
	}

	@Test
	fun toList() {
	}

	@Test
	fun filterTo2() {
	}

	@Test
	fun find2() {
	}

	@Test
	fun indexOfFirst2() {
		val list = arrayListOf(0, 1, 2, 3, 4, 5, 0)
		assertEquals(0, list.indexOfFirst2 { it == 0 })
		assertEquals(-1, list.indexOfFirst2(1, 2) { it == 0 })
		assertEquals(6, list.indexOfFirst2(1, 6) { it == 0 })
		assertEquals(1, list.indexOfFirst2 { it == 1 })
		assertEquals(-1, list.indexOfFirst2 { it == 6 })
		assertEquals(5, list.indexOfFirst2 { it == 5 })
		val list2 = arrayListOf(0)
		assertEquals(0, list2.indexOfFirst2 { it == 0 })
		assertEquals(-1, list2.indexOfFirst2 { it == 6 })
	}

	@Test
	fun indexOfLast2() {
		val list = arrayListOf(0, 1, 2, 3, 4, 5, 0)
		assertEquals(6, list.indexOfLast2 { it == 0 })
		assertEquals(-1, list.indexOfLast2(2, 1) { it == 0 })
		assertEquals(6, list.indexOfLast2(6, 1) { it == 0 })
		assertEquals(1, list.indexOfLast2 { it == 1 })
		assertEquals(-1, list.indexOfLast2 { it == 6 })
		assertEquals(5, list.indexOfLast2 { it == 5 })
		val list2 = arrayListOf(0)
		assertEquals(0, list2.indexOfLast2 { it == 0 })
		assertEquals(-1, list2.indexOfLast2 { it == 6 })
	}

	@Test
	fun emptyList() {
		val list = emptyList<Int>()
		assertEquals(-1, list.indexOfLast2 { it == 0 })
		assertEquals(-1, list.indexOfFirst2 { it == 0 })
		assertEquals(-1, list.indexOfLast2(0, 1) { it == 0 })
		assertEquals(-1, list.indexOfFirst2(0, 1) { it == 0 })
		assertEquals(-1, list.indexOfLast2(1, 1) { it == 0 })
		assertEquals(-1, list.indexOfFirst2(1, 1) { it == 0 })
	}

	@Test
	fun forEach2() {
	}

	@Test
	fun forEachReversed2() {
	}

	@Test
	fun sum2() {
	}

	@Test
	fun addAll() {
	}

	@Test
	fun count2() {
	}

	@Test
	fun removeFirst() {
	}

	@Test
	fun setSize() {
		val list = arrayListOf(1, 2, 3)
		list.setSize(5) { 0 }
		assertListEquals(listOf(1, 2, 3, 0, 0), list)

		list.setSize(3) { 0 }
		assertListEquals(listOf(1, 2, 3), list)
	}

	@Test
	fun replaceRange() {
		val list = arrayListOf(0, 1, 2, 3)
		assertEquals(listOf(0, 1, 5, 6), list.replaceRange(2, 4, listOf(5, 6)))
		assertEquals(listOf(0, 1, 5, 6, 2, 3), list.replaceRange(2, 2, listOf(5, 6)))

	}

}
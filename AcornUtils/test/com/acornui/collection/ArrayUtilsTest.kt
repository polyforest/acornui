/*
 * Copyright 2015 Nicholas Bilyk
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

import org.junit.Test
import kotlin.test.assertEquals

/**
 * @author nbilyk
 */
class ArrayUtilsTest {

	@Test fun binarySearch() {
		val arr = arrayListOf(1, 4, 6, 7, 8)
		assertEquals(2, arr.sortedInsertionIndex(5))
		assertEquals(3, arr.sortedInsertionIndex(6))
		assertEquals(4, arr.sortedInsertionIndex(7))
		assertEquals(5, arr.sortedInsertionIndex(8))
		assertEquals(5, arr.sortedInsertionIndex(9))
	}

	@Test fun binarySearchWithComparator() {
		val comparator = {
			o1: Int?, o2: Int? ->
			if (o1 == null && o2 == null) 0
			else if (o1 == null) -1
			else if (o2 == null) 1
			else o1.compareTo(o2)
		}

		val arr = arrayListOf(1, 4, 6, 7, 8)
		assertEquals(1, arr.sortedInsertionIndex(1, comparator = comparator))
		assertEquals(2, arr.sortedInsertionIndex(5, comparator = comparator))
		assertEquals(3, arr.sortedInsertionIndex(6, comparator = comparator))
		assertEquals(4, arr.sortedInsertionIndex(7, comparator = comparator))
		assertEquals(5, arr.sortedInsertionIndex(8, comparator = comparator))
		assertEquals(5, arr.sortedInsertionIndex(9, comparator = comparator))
	}
}
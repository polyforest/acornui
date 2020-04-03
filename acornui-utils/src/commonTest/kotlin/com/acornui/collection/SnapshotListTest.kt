/*
 * Copyright 2020 Poly Forest, LLC
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
import kotlin.test.Test
import kotlin.test.assertEquals

class SnapshotListTest {

	@Test
	fun singleIteration() {
		val list = snapshotListOf(1, 2, 3, 4, 5)
		val actual = ArrayList<Int>()
		list.forEach2 {
			actual.add(it)
			list.add(0, -it)
		}
		assertListEquals(listOf(1, 2, 3, 4, 5), actual)
		assertListEquals(listOf(-5, -4, -3, -2, -1, 1, 2, 3, 4, 5), list)
		assertEquals(1, list.copyCount)
	}

	@Test
	fun noModificationIteration() {
		val list = snapshotListOf(1, 2, 3, 4, 5)
		val actual = ArrayList<Int>()
		list.forEach2 {
			actual.add(it)
		}
		assertListEquals(listOf(1, 2, 3, 4, 5), actual)
		assertEquals(0, list.copyCount)
	}

	@Test
	fun multiIteration() {
		val list = snapshotListOf(1, 2, 3)
		val actual = ArrayList<Int>()
		list.forEach2 { i ->
			list.forEach2 { j ->
				actual.add(i * 10 + j)
				list.add(-i)
			}
			list.add(0, -i)
		}
		assertListEquals(listOf(11, 12, 13, 21, 22, 23, 31, 32, 33), actual)
		assertListEquals(listOf(-3, -2, -1, 1, 2, 3, -1, -1, -1, -2, -2, -2, -3, -3, -3), list)
		assertEquals(1, list.copyCount)
	}
}
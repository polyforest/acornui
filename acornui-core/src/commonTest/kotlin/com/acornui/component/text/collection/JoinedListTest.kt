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

package com.acornui.component.text.collection

import com.acornui.test.assertListEquals
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class JoinedListTest {

	private lateinit var lists: List<C>
	private lateinit var joined: JoinedList<C, Int>

	@BeforeTest
	fun setup() {
		lists = listOf(C(listOf(1, 2, 3)), C(listOf(4, 5, 6)), C(listOf(7, 8, 9)))
		joined = JoinedList(lists) { it.children }
	}


	@Test
	fun getSize() {
		assertEquals(9, joined.size)

	}

	@Test
	fun get() {
		assertListEquals(listOf(1, 2, 3, 4, 5, 6, 7, 8, 9), joined)
	}

	@Test
	fun dirty() {

		val lists = mutableListOf(C(listOf(1, 2, 3)), C(listOf(4, 5, 6)), C(listOf(7, 8, 9)))
		val joined = JoinedList(lists) { it.children }

		lists[0] = C(listOf(1, 2, 3, 0))
		joined.dirty()
		assertListEquals(listOf(1, 2, 3, 0, 4, 5, 6, 7, 8, 9), joined)
	}

	private class C(val children: List<Int>)
}

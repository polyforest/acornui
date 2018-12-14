/*
 * Copyright 2018 Poly Forest
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

import kotlin.test.Test
import com.acornui.test.assertListEquals

class ConcatListTest {

	@Test
	fun get() {
		val a = arrayListOf(1, 2, 3)
		val b = arrayListOf(4, 5, 6)
		val c = a concat b
		assertListEquals(listOf(1, 2, 3, 4, 5, 6), c)

		a.add(4)
		assertListEquals(listOf(1, 2, 3, 4, 4, 5, 6), c)

		b.add(7)
		assertListEquals(listOf(1, 2, 3, 4, 4, 5, 6, 7), c)

		a.add(0, 0)
		assertListEquals(listOf(0, 1, 2, 3, 4, 4, 5, 6, 7), c)
	}

	@Test
	fun copy() {
		val a = arrayListOf(1, 2, 3)
		val b = arrayListOf(4, 5, 6)
		val c = a concat b

		val copied = c.copy()
		assertListEquals(listOf(1, 2, 3, 4, 5, 6), copied)
		a.add(0)
		assertListEquals(listOf(1, 2, 3, 4, 5, 6), copied)

	}
}
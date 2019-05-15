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

import com.acornui.test.assertUnorderedListEquals
import kotlin.test.Test

class QuadTreeTest {

	@Test
	fun add() {
		val q = QuadTree<Int, Int>()

		q.add(1, 0, 0)
		q.add(2, 1, 0)
		q.add(3, 1, 1)
		q.add(4, 0, 1)
		q.add(5, -1, 0)
		q.add(6, -1, -1)
		q.add(7, 0, -1)

		val arr = ArrayList<Int>()
		q.iterate(0, 0, 2, 2) { arr.add(it) }
		assertUnorderedListEquals(listOf(1, 2, 3, 4), arr)

		arr.clear()
		q.iterate(-2, 0, 2, 2) { arr.add(it) }
		assertUnorderedListEquals(listOf(1, 2, 3, 4, 5), arr)

		arr.clear()
		q.iterate(-2, -2, 0, 0) { arr.add(it) }
		assertUnorderedListEquals(listOf(1, 5, 6, 7), arr)
	}

	@Test
	fun remove() {

		val q = QuadTree<Int, Int>()

		q.add(1, 0, 0)
		q.add(2, 1, 0)
		q.add(3, 1, 1)
		q.add(4, 0, 1)
		q.add(5, -1, 0)
		q.add(6, -1, -1)
		q.add(7, 0, -1)

		q.remove(4, 0, 1)
		q.remove(6, -1, -1)

		val arr = ArrayList<Int>()
		q.iterate(-2, -2, 2, 2) { arr.add(it) }
		assertUnorderedListEquals(listOf(1, 2, 3, 5, 7), arr)

		arr.clear()

		q.remove(1, 0, 0)
		q.iterate(-2, -2, 2, 2) { arr.add(it) }
		assertUnorderedListEquals(listOf(2, 3, 5, 7), arr)

	}

	@Test
	fun update() {
		val q = QuadTree<Int, Int>()

		q.add(1, 0, 0)
		q.add(2, 1, 0)
		q.add(3, 1, 1)
		q.add(4, 0, 1)
		q.add(5, -1, 0)
		q.add(6, -1, -1)
		q.add(7, 0, -1)

		q.remove(4, 0, 1)
		q.remove(6, -1, -1)

		val arr = ArrayList<Int>()
		q.iterate(-2, -2, 2, 2) { arr.add(it) }
		assertUnorderedListEquals(listOf(1, 2, 3, 5, 7), arr)

		arr.clear()
		q.update(2, 1, 0, 0, 4)
		q.iterate(-2, -2, 2, 2) { arr.add(it) }
		assertUnorderedListEquals(listOf(1, 3, 5, 7), arr)
		arr.clear()
		q.iterate(-2, -2, 2, 4) { arr.add(it) }
		assertUnorderedListEquals(listOf(1, 2, 3, 5, 7), arr)
	}

}
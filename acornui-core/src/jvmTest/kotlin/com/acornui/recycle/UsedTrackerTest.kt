/*
 * Copyright 2019 PolyForest
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

import com.acornui.test.assertListEquals
import org.junit.Test

class UsedTrackerTest {

	@Test fun markUsed() {
		val u = UsedTracker<Int>()
		u.markUsed(1)
		u.markUsed(3)
		u.markUsed(5)
		u.markUsed(8)

		assertListEquals(listOf(), u.getUnusedList())
		u.flip()

		assertListEquals(listOf(1, 3, 5, 8), u.getUnusedList())
		u.markUsed(1)
		u.markUsed(3)
		assertListEquals(listOf(5, 8), u.getUnusedList())
		u.flip()
		assertListEquals(listOf(1, 3), u.getUnusedList())
		u.flip()
		assertListEquals(listOf(), u.getUnusedList())
	}

	@Test fun forget() {
		val u = UsedTracker<Int>()
		u.markUsed(1)
		u.markUsed(3)
		u.markUsed(5)
		u.markUsed(8)
		u.forget(5)

		assertListEquals(listOf(), u.getUnusedList())
		u.flip()
		assertListEquals(listOf(1, 3, 8), u.getUnusedList())
	}

	private fun UsedTracker<Int>.getUnusedList(): List<Int> {
		val list = ArrayList<Int>()
		forEachUnused { list.add(it) }
		return list
	}
}
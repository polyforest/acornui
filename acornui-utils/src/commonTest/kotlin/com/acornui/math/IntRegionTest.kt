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

package com.acornui.math

import com.acornui.test.assertListEquals
import kotlin.test.Test
import kotlin.test.fail

class IntRegionTest {

	@Test fun testNoIntersections() {
		val s = IntRegionSet()

		s.add(10, 20, 40, 30)
		assertListEquals(listOf(IntRectangle(10, 20, 40, 30)), s.regions)

		// No intersection
		s.add(50, 20, 40, 30)
		s.add(10, 50, 40, 30)

		assertListEquals(listOf(IntRectangle(10, 20, 40, 30), IntRectangle(50, 20, 40, 30), IntRectangle(10, 50, 40, 30)), s.regions)

	}

	@Test fun testAddedIsContained() {
		val s = IntRegionSet()

		s.add(10, 20, 40, 30)
		s.add(10, 20, 40, 30)
		s.add(15, 25, 10, 10)
		assertListEquals(listOf(IntRectangle(10, 20, 40, 30)), s.regions)
	}

	@Test fun testExistingIsContained() {
		val s = IntRegionSet()

		s.add(40, 45, 10, 5)
		s.add(15, 25, 10, 10)
		s.add(10, 20, 40, 30)
		assertListEquals(listOf(IntRectangle(10, 20, 40, 30)), s.regions)
	}

	@Test fun overlapIsPrevented() {
		val s = IntRegionSet()

		val regionsToAdd = listOf(
				IntRectangle(20, 30, 5, 3),
				IntRectangle(19, 30, 5, 3),
				IntRectangle(19, 30, 5, 3),
				IntRectangle(18, 28, 4, 4),
				IntRectangle(18, 26, 2, 4),
				IntRectangle(18, 29, 2, 2),
				IntRectangle(2, 3, 4, 3),
				IntRectangle(3, 2, 4, 5),
				IntRectangle(4, 3, 1, 1)
		)
		regionsToAdd.forEach { s.add(it) }

		checkValid(regionsToAdd, s.regions)
	}

	private fun checkValid(source: List<IntRectangleRo>, regions: List<IntRectangleRo>) {
		for (i in 0..49) {
			for (j in 0..49) {
				val shouldIntersect = source.any { it.contains(i, j) }
				val c = regions.count { it.contains(i, j) }
				if (c > 1) {
					fail("Found overlap at $i, $j")
				}
				if (shouldIntersect) {
					if (c != 1) {
						fail("Region set was expected to contain a region at $i, $j")
					}
				} else {
					if (c != 0) {
						fail("Region set not was expected to contain a region at $i, $j")
					}
				}
			}
		}
	}
}
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

import com.acornui.serialization.jsonParse
import com.acornui.serialization.jsonStringify
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IntRectangleTest {

	@Test fun serialize() {
		val v = IntFoo987(IntRectangle(1, 2, 3, 4))
		val str = jsonStringify(IntFoo987.serializer(), v)
		assertEquals("""{"rect":[1,2,3,4]}""", str)
		assertEquals(v, jsonParse(IntFoo987.serializer(), """{"rect":[1,2,3,4]}"""))
	}

	@Test fun area() {
		assertEquals(12, IntRectangle(1, 2, 3, 4).area)
	}

	@Test
	fun containsPoint() {
		assertTrue(IntRectangle(1, 2, 3, 4).contains(2, 3))
		assertTrue(IntRectangle(1, 2, 3, 4).contains(3, 5))
		assertFalse(IntRectangle(1, 2, 3, 4).contains(0, 2))
		assertFalse(IntRectangle(1, 2, 3, 4).contains(2, 0))
		assertTrue(IntRectangle(-1, 2, 3, 4).contains(0, 5))
		assertTrue(IntRectangle(-1, -2, 3, 4).contains(1, 1))

		// After Top/Left edge
		assertTrue(IntRectangle(1, 2, 3, 4).contains(1, 2))
		assertTrue(IntRectangle(1, 2, 3, 4).contains(1, 3))

		// After Bottom/Right edge
		assertFalse(IntRectangle(1, 2, 3, 4).contains(4, 6))
		assertFalse(IntRectangle(1, 2, 3, 4).contains(4, 5))
		assertFalse(IntRectangle(1, 2, 3, 4).contains(3, 6))
	}

	@Test
	fun containsRectangle() {
		assertTrue(IntRectangle(1, 2, 3, 4).contains(IntRectangle(1, 2, 3, 4)))
		assertFalse(IntRectangle(1, 2, 3, 4).contains(IntRectangle(1, 2, 3, 5)))
		assertFalse(IntRectangle(1, 2, 3, 4).contains(IntRectangle(1, 2, 4, 4)))
		assertTrue(IntRectangle(1, 2, 3, 4).contains(IntRectangle(1, 2, 2, 3)))
	}

	@Test
	fun intersectsRectangle() {
		assertTrue(IntRectangle(0, 0, 4, 2).intersects(IntRectangle(2, 0, 4, 2)))
		assertFalse(IntRectangle(0, 0, 4, 2).intersects(IntRectangle(4, 0, 4, 2)))
		assertFalse(IntRectangle(0, 0, 4, 2).intersects(IntRectangle(0, 2, 4, 2)))
		assertTrue(IntRectangle(0, 0, 4, 2).intersects(IntRectangle(0, 1, 4, 2)))
		assertTrue(IntRectangle(3, 0, 4, 2).intersects(IntRectangle(0, 0, 4, 2)))

		assertTrue(IntRectangle(59, 59, 2136, 1193).intersects(IntRectangle(-1, -1, 2257, 1397)))
		assertTrue(IntRectangle(-1, -1, 2257, 1397).intersects(IntRectangle(59, 59, 2136, 1193)))
	}

	@Test
	fun intersectsRectangleWithOut() {
		val out = IntRectangle()
		assertTrue(IntRectangle(0, 0, 4, 2).intersects(IntRectangle(2, 0, 4, 2), out))
		assertEquals(IntRectangle(2, 0, 2, 2), out)
		assertTrue(IntRectangle(0, 0, 4, 2).intersects(IntRectangle(0, 1, 4, 2), out))
		assertEquals(IntRectangle(0, 1, 4, 1), out)
		assertTrue(IntRectangle(3, 0, 4, 2).intersects(IntRectangle(0, 0, 4, 2), out))
		assertEquals(IntRectangle(3, 0, 1, 2), out)
		assertTrue(IntRectangle(3, 3, 4, 2).intersects(IntRectangle(0, 0, 4, 4), out))
		assertEquals(IntRectangle(3, 3, 1, 1), out)
	}
}

@Serializable
private data class IntFoo987(val rect: IntRectangleRo)
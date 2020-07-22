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

class RectangleTest {
	@Test
	fun serialize() {
		val v = Foo648(Rectangle(1.1, 2.1, 3.1, 4.1))
		val str = jsonStringify(Foo648.serializer(), v)
		assertEquals("""{"rect":[1.1,2.1,3.1,4.1]}""", str)
		assertEquals(v, jsonParse(Foo648.serializer(), """{"rect":[1.1,2.1,3.1,4.1]}"""))
	}

	@Test
	fun area() {
		assertEquals(12.0, Rectangle(1.0, 2.0, 3.0, 4.0).area)
	}

	@Test
	fun containsPoint() {
		assertTrue(Rectangle(1.0, 2.0, 3.0, 4.0).contains(2.0, 3.0))
		assertTrue(Rectangle(1.0, 2.0, 3.0, 4.0).contains(3.99, 5.99))
		assertFalse(Rectangle(1.0, 2.0, 3.0, 4.0).contains(0.0, 2.0))
		assertFalse(Rectangle(1.0, 2.0, 3.0, 4.0).contains(2.0, 0.0))
		assertTrue(Rectangle(-1.0, 2.0, 3.0, 4.0).contains(0.0, 5.0))
		assertTrue(Rectangle(-1.0, -2.0, 3.0, 4.0).contains(1.0, 1.0))

		// After Top/Left edge
		assertTrue(Rectangle(1.0, 2.0, 3.0, 4.0).contains(1.0, 2.0))
		assertTrue(Rectangle(1.0, 2.0, 3.0, 4.0).contains(1.0, 3.0))

		// After Bottom/Right edge
		assertFalse(Rectangle(1.0, 2.0, 3.0, 4.0).contains(4.0, 6.0))
		assertFalse(Rectangle(1.0, 2.0, 3.0, 4.0).contains(4.0, 5.0))
		assertFalse(Rectangle(1.0, 2.0, 3.0, 4.0).contains(3.0, 6.0))
	}

	@Test
	fun containsRectangle() {
		assertTrue(
			Rectangle(1.0, 2.0, 3.0, 4.0).contains(Rectangle(1.0, 2.0, 3.0, 4.0))
		)
		assertFalse(
			Rectangle(1.0, 2.0, 3.0, 4.0).contains(Rectangle(1.0, 2.0, 3.0, 5.0))
		)
		assertFalse(
			Rectangle(1.0, 2.0, 3.0, 4.0).contains(Rectangle(1.0, 2.0, 4.0, 4.0))
		)
		assertTrue(
			Rectangle(1.0, 2.0, 3.0, 4.0).contains(Rectangle(1.0, 2.0, 2.0, 3.0))
		)
	}

	@Test
	fun intersectsRectangle() {
		assertTrue(
			Rectangle(0.0, 0.0, 4.0, 2.0).intersects(Rectangle(2.0, 0.0, 4.0, 2.0))
		)
		assertFalse(
			Rectangle(0.0, 0.0, 4.0, 2.0).intersects(Rectangle(4.0, 0.0, 4.0, 2.0))
		)
		assertFalse(
			Rectangle(0.0, 0.0, 4.0, 2.0).intersects(Rectangle(0.0, 2.0, 4.0, 2.0))
		)
		assertTrue(
			Rectangle(0.0, 0.0, 4.0, 2.0).intersects(Rectangle(0.0, 1.0, 4.0, 2.0))
		)
		assertTrue(
			Rectangle(3.0, 0.0, 4.0, 2.0).intersects(Rectangle(0.0, 0.0, 3.01, 2.0))
		)
	}

	@Test
	fun intersectsRectangleWithOut() {
		assertEquals(
			Rectangle(2.0, 0.0, 2.0, 2.0),
			Rectangle(0.0, 0.0, 4.0, 2.0).intersection(Rectangle(2.0, 0.0, 4.0, 2.0))
		)
		assertEquals(
			Rectangle(0.0, 1.0, 4.0, 1.0),
			Rectangle(0.0, 0.0, 4.0, 2.0).intersection(Rectangle(0.0, 1.0, 4.0, 2.0))
		)
		assertEquals(
			Rectangle(3.0, 0.0, 1.0, 2.0),
			Rectangle(3.0, 0.0, 4.0, 2.0).intersection(Rectangle(0.0, 0.0, 4.0, 2.0))
		)
		assertEquals(
			Rectangle(3.0, 3.0, 1.0, 1.0),
			Rectangle(3.0, 3.0, 4.0, 2.0).intersection(Rectangle(0.0, 0.0, 4.0, 4.0))
		)
	}

}

@Serializable
private data class Foo648(val rect: Rectangle)
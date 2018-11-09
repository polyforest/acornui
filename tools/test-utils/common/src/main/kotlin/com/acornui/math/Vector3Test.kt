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

package com.acornui.math

import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import com.acornui.test.*

/**
 * @author nbilyk
 */
class Vector3Test {

	@Test fun class_len() {
		assertEquals(1f, Vector3.len(1f, 0f, 0f))
		assertEquals(1f, Vector3.len(0f, 0f, 1f))
		assertEquals(1f, Vector3.len(0f, 1f, 0f))
		assertClose(sqrt(3.0).toFloat(), Vector3.len(-1f, 1f, -1f))
	}

	@Test fun class_len2() {
		assertEquals(1f, Vector3.len2(1f, 0f, 0f))
		assertEquals(1f, Vector3.len2(0f, 0f, 1f))
		assertEquals(1f, Vector3.len2(0f, 1f, 0f))
		assertClose(3.0f, Vector3.len2(-1f, 1f, -1f))
	}

	@Test fun set() {
		val v = Vector3()
		v.set(1f, 2f, 3f)
		assertEquals(Vector3(1f, 2f, 3f), v)
	}

	@Test fun copy() {
		assertEquals(Vector3(1f, 2f, 3f), Vector3(1f, 2f, 3f).copy())
	}

	@Test fun add() {
		assertEquals(Vector3(4f, 5f, 6f), Vector3(1f, -1f, 9f).add(Vector3(3f, 6f, -3f)))
		assertEquals(Vector3(4f, 5f, 6f), Vector3(1f, -1f, 9f).add(3f, 6f, -3f))
	}

	@Test fun sub() {
		assertEquals(Vector3(-2f, -7f, 12f), Vector3(1f, -1f, 9f).sub(Vector3(3f, 6f, -3f)))
		assertEquals(Vector3(-2f, -7f, 12f), Vector3(1f, -1f, 9f).sub(3f, 6f, -3f))
	}

	@Test fun crs() {
		assertEquals(Vector3(1f, 0f, -1f), Vector3(0f, 1f, 0f).crs(Vector3(1f, 0f, 1f)))
		assertEquals(Vector3(1f, 0f, 0f), Vector3(0f, 1f, 0f).crs(Vector3(0f, 0f, 1f)))
	}

	@Test fun equals() {
		assertTrue(Vector3(1f, 2f, 3f) == Vector3(1f, 2f, 3f))
		assertFalse(Vector3(1f, 2f, 3f) == Vector3(1f, 2f, 4f))
	}
}
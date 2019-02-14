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

package com.acornui.math

import org.junit.Test

import org.junit.Assert.*
import org.junit.Before

class MinMaxTest {

	lateinit var m1: MinMax

	@Before
	fun setUp() {
		m1 = MinMax(1f, -3f, 40f, 100f)
	}

	@Test
	fun inf() {
		m1.inf()
		assertEquals(MinMax(), m1)
	}

	@Test
	fun ext() {
		m1.ext(-4f, 2f)
		assertEquals(MinMax(-4f, -3f, 40f, 100f), m1)
		m1.ext(-4f, -5f)
		assertEquals(MinMax(-4f, -5f, 40f, 100f), m1)
		m1.ext(41f, 101f)
		assertEquals(MinMax(-4f, -5f, 41f, 101f), m1)
	}

	@Test
	fun isEmpty() {
		assertFalse(m1.isEmpty())
		m1.inf()
		assertTrue(m1.isEmpty())
		assertTrue(MinMax(23f, 0f, 355f, 0f).isEmpty())
		assertFalse(MinMax(23f, 0f, 355f, 0.1f).isEmpty())
	}

	@Test
	fun scl() {
		m1.scl(2f, 3f)
		assertEquals(MinMax(1f * 2f, -3f * 3f, 40f * 2f, 100f * 3f), m1)
	}

	@Test
	fun inflate() {
		m1.inflate(1f, 2f, 3f, 4f)
		assertEquals(MinMax(1f - 1f, -3f - 2f, 40f + 3f, 100f + 4f), m1)
	}

	@Test
	fun getWidth() {
		assertEquals(39f, m1.width)
	}

	@Test
	fun getHeight() {
		assertEquals(103f, m1.height)
	}

	@Test
	fun set() {
		m1.set(MinMax(23f, 24f, 25f, 26f))
		assertEquals(MinMax(23f, 24f, 25f, 26f), m1)
	}

	@Test
	fun set1() {
		m1.set(23f, 24f, 25f, 26f)
		assertEquals(MinMax(23f, 24f, 25f, 26f), m1)
	}

	@Test
	fun clampPoint() {
		assertEquals(Vector2(1f, -3f), m1.clampPoint(Vector2(0f, -4f)))
		assertEquals(Vector2(2f, -3f), m1.clampPoint(Vector2(2f, -4f)))
		assertEquals(Vector2(40f, 100f), m1.clampPoint(Vector2(42f, 104f)))
	}

	@Test
	fun intersection() {
		m1.intersection(MinMax(-30f, 3f, 30f, 120f))
		assertEquals(MinMax(1f, 3f, 30f, 100f), m1)
	}

	@Test
	fun ext1() {
		m1.ext(MinMax(-30f, -10f, 50f, 120f))
		assertEquals(MinMax(-30f, -10f, 50f, 120f), m1)
		m1.ext(MinMax(-30f, -10f, 40f, 130f))
		assertEquals(MinMax(-30f, -10f, 50f, 130f), m1)
	}

	@Test
	fun copy() {
		val m2 = m1.copy()
		assertEquals(m1, m2)
		assertFalse(m1 === m2)
	}

}
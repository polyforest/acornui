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

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * @author nbilyk
 */
class RayTest {

	@Test fun mul() {
	}

	@Test fun intersects() {
		run {

			val r1 = Ray(Vector3(5f, 5f, 4f), Vector3(5f, 5f, 2f))
			val r2 = Ray(Vector3(5f, 5f, 5f), Vector3(5f, 5f, -2f))

			val out = Vector3()
			r1.intersects(r2, out)
			assertEquals(Vector3(6.25f, 6.25f, 4.5f), out)
		}

		run {
			val r1 = Ray(Vector3(6f, 8f, 4f), Vector3(6f, 7f, 0f))
			val r2 = Ray(Vector3(6f, 8f, 2f), Vector3(6f, 7f, 4f))

			val out = Vector3()
			r1.intersects(r2, out)
			assertEquals(Vector3(9f, 11.5f, 4f), out)
		}

		run {
			val r1 = Ray(Vector3(10f, 20f, 30f), Vector3(0f, -1f, 0f))
			val r2 = Ray(Vector3(0f, 0f, 30f), Vector3(1f, 0f, 0f))

			val out = Vector3()
			assertTrue(r1.intersects(r2, out))
			assertEquals(Vector3(10f, 0f, 30f), out)
		}

		run {
			val r1 = Ray(Vector3(10f, 20f, 30f), Vector3(1f, -1f, 0f))
			val r2 = Ray(Vector3(0f, 0f, 30f), Vector3(1f, 0f, 0f))

			val out = Vector3()
			assertTrue(r1.intersects(r2, out))
			assertEquals(Vector3(30f, 0f, 30f), out)
		}
	}

	@Test fun intersectsTriangle() {
		run {
			val r1 = Ray(Vector3(1f, 2f, -20f), Vector3(0f, 0f, 1f))

			val out = Vector3()
			assertTrue(r1.intersects(Vector3(0f, 0f, 0f), Vector3(10f, 10f, 0f), Vector3(0f, 10f, 0f), out))
			assertEquals(Vector3(1f, 2f, 0f), out)
		}

		run {
			val r1 = Ray(Vector3(-20f, 2f, 1f), Vector3(1f, 0f, 0f))

			val out = Vector3()
			assertTrue(r1.intersects(Vector3(0f, 0f, 0f), Vector3(0f, 10f, 10f), Vector3(0f, 10f, 0f), out))
			assertEquals(Vector3(0f, 2f, 1f), out)
		}

		run {
			val r1 = Ray(Vector3(-20f, 2f, 1f), Vector3(-1f, 0f, 0f))

			val out = Vector3()
			assertFalse(r1.intersects(Vector3(0f, 0f, 0f), Vector3(0f, 10f, 10f), Vector3(0f, 10f, 0f), out))
		}
	}
}
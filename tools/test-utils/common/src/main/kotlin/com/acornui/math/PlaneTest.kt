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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * @author nbilyk
 */
class PlaneTest {

	@Test fun intersects() {
		run {
			val p = Plane(Vector3(0f, 0f, 1f), -10f)
			val r = Ray(Vector3(0f, 0f, 20f), Vector3(0f, 0f, -1f))
			assertEquals(10f, p.distance(r.origin))
			val out = Vector3()
			assertTrue(p.intersects(r, out))
			assertEquals(Vector3(0f, 0f, 10f), out)
		}

		run {
			val p = Plane(Vector3(1f, 0f, 1f), -10f)
			val r = Ray(Vector3(0f, 0f, 20f), Vector3(0f, 0f, -1f))
			val out = Vector3()
			assertTrue(p.intersects(r, out))
			assertEquals(Vector3(0f, 0f, 10f), out)
		}

		run {
			val p = Plane(Vector3(1f, 0f, 0f), -10f)
			val r = Ray(Vector3(0f, 0f, 20f), Vector3(0f, 0f, -1f))
			val out = Vector3()
			assertFalse(p.intersects(r, out))
		}
	}

	@Test fun distance() {
		run {
			val p = Plane(Vector3(0f, 0f, 1f), 0f)
			val pt = Vector3(0f, 0f, 10f)
			assertEquals(10f, p.distance(pt))
		}

		run {
			val p = Plane(Vector3(0f, 0f, 1f), 0f)
			val pt = Vector3(0f, 0f, -10f)
			assertEquals(-10f, p.distance(pt))
		}

		run {
			val p = Plane(Vector3(0f, 0f, 1f), 10f) // A distance of 10 will place the Plane 10 units in the opposite direction of the normal.
			val pt = Vector3(0f, 0f, -10f)
			assertEquals(0f, p.distance(pt))
		}
	}

	@Test fun prj() {
		run {
			// Trivial case
			val p = Plane(Vector3(1f, 0f, 1f), 0f)
			val pt = Vector3(0f, 0f, 0f)
			assertEquals(Vector3(0f, 0f, 0f), p.prj(pt))
		}

		run {
			val p = Plane(Vector3(0f, 0f, 1f), -10f)
			val pt = Vector3(10f, 30f, 20f)
			assertEquals(Vector3(10f, 30f, 10f), p.prj(pt))
		}

		run {
			val p = Plane(Vector3(0f, 1f, 0f), -10f)
			val pt = Vector3(10f, 20f, 30f)
			assertEquals(Vector3(10f, 10f, 30f), p.prj(pt))
		}

		run {
			val p = Plane(Vector3(1f, 0f, 1f), -10f)
			val pt = Vector3(0f, 0f, 0f)
			assertEquals(Vector3(10f, 0f, 10f), p.prj(pt))
		}

		run {
			val p = Plane(Vector3(1f, 0f, 1f), -10f)
			val pt = Vector3(10f, 30f, 20f)
			assertEquals(Vector3(-10f, 30f, 0f), p.prj(pt))
		}

	}
}
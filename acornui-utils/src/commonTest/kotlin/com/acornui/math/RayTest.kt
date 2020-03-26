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

import com.acornui.serialization.jsonParse
import com.acornui.serialization.jsonStringify
import com.acornui.test.assertClose
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// Note: JS will stringify 1.0f as "1" and JVM is "1.0"

/**
 * @author nbilyk
 */
class RayTest {

	@Test fun mul() {
	}

	@Test fun intersectsRay() {
		run {
			val r1 = Ray(vec3(5f, 5f, 4f), vec3(5f, 5f, 2f))
			val r2 = Ray(vec3(5f, 5f, 5f), vec3(5f, 5f, -2f))

			val out = vec3()
			assertTrue(r1.intersectsRay(r2, out))
			assertClose(vec3(6.25f, 6.25f, 4.5f), out)
		}

		run {
			val r1 = Ray(vec3(6f, 8f, 4f), vec3(6f, 7f, 0f))
			val r2 = Ray(vec3(6f, 8f, 2f), vec3(6f, 7f, 4f))

			val out = vec3()
			assertTrue(r1.intersectsRay(r2, out))
			assertClose(vec3(9f, 11.5f, 4f), out)
		}

		run {
			val r1 = Ray(vec3(10f, 20f, 30f), vec3(0f, -1f, 0f))
			val r2 = Ray(vec3(0f, 0f, 30f), vec3(1f, 0f, 0f))

			val out = vec3()
			assertTrue(r1.intersectsRay(r2, out))
			assertClose(vec3(10f, 0f, 30f), out)
		}

		run {
			val r1 = Ray(vec3(10f, 20f, 30f), vec3(1f, -1f, 0f))
			val r2 = Ray(vec3(0f, 0f, 30f), vec3(1f, 0f, 0f))

			val out = vec3()
			assertTrue(r1.intersectsRay(r2, out))
			assertClose(vec3(30f, 0f, 30f), out)
		}

		run {
			val r1 = Ray(vec3(10f, 20f, 30f), vec3(1f, -1f, 0f))
			val r2 = Ray(vec3(10f, 30f, 30f), vec3(1f, -1f, 0f))

			assertFalse(r1.intersectsRay(r2))
		}

		run {
			val r1 = Ray(vec3(10f, 20f, 30f), vec3(1f, -1f, 0f))
			val r2 = Ray(vec3(10f, 30f, 31f), vec3(2f, -3f, 0f))

			assertFalse(r1.intersectsRay(r2))
		}

		run {
			val r1 = Ray(vec3(10f, 20f, 30f), vec3(0f, 1f, 2f))
			val r2 = Ray(vec3(10f, 40f, 50f), vec3(0f, 3f, 4f))

			assertTrue(r1.intersectsRay(r2))
		}

		run {
			val r1 = Ray(vec3(10f, 20f, 30f), vec3(0f, 1f, 2f))
			val r2 = Ray(vec3(11f, 40f, 50f), vec3(0f, 3f, 4f))

			assertFalse(r1.intersectsRay(r2))
		}
	}

	@Test fun intersectsTriangle() {
		run {
			val r1 = Ray(vec3(1f, 2f, -20f), vec3(0f, 0f, 1f))

			val out = vec3()
			assertTrue(r1.intersectsTriangle(vec3(0f, 0f, 0f), vec3(10f, 10f, 0f), vec3(0f, 10f, 0f), out))
			assertEquals(vec3(1f, 2f, 0f), out)
		}

		run {
			val r1 = Ray(vec3(-20f, 2f, 1f), vec3(1f, 0f, 0f))

			val out = vec3()
			assertTrue(r1.intersectsTriangle(vec3(0f, 0f, 0f), vec3(0f, 10f, 10f), vec3(0f, 10f, 0f), out))
			assertEquals(vec3(0f, 2f, 1f), out)
		}

		run {
			val r1 = Ray(vec3(-20f, 2f, 1f), vec3(-1f, 0f, 0f))

			val out = vec3()
			assertFalse(r1.intersectsTriangle(vec3(0f, 0f, 0f), vec3(0f, 10f, 10f), vec3(0f, 10f, 0f), out))
		}
	}

	@Test fun serialize() {
		val ray = Ray(vec3(1.1f, 2.1f, 3.1f), vec3(4.1f, 5.1f, 6.1f))
		val str = jsonStringify(Ray.serializer(), ray)
		val ray2 = jsonParse(Ray.serializer(), str)
		assertClose(ray.origin, ray2.origin)
		assertClose(ray.direction, ray2.direction)
	}
}
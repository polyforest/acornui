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

import com.acornui.serialization.binaryParse
import com.acornui.serialization.jsonParse
import com.acornui.serialization.jsonStringify
import com.acornui.test.assertClose
import kotlin.test.Test
import kotlin.test.assertTrue

// Note: JS will stringify 1.0f as "1" and JVM is "1.0"

/**
 * @author nbilyk
 */
class Ray2Test {

	@Test fun mul() {
	}

	@Test fun intersectsRay() {
		run {
			val r1 = Ray2(Vector2(0f, 0f), Vector2(1f, 0f))
			val r2 = Ray2(Vector2(1f, 1f), Vector2(0f, -1f))

			val out = Vector2()
			assertTrue(r1.intersectsRay(r2, out))
			assertClose(Vector2(1f, 0f), out)
		}

		run {
			val r1 = Ray(Vector3(6f, 8f, 4f), Vector3(6f, 7f, 0f))
			val r2 = Ray(Vector3(6f, 8f, 2f), Vector3(6f, 7f, 4f))

			val out = Vector3()
			r1.intersectsRay(r2, out)
			assertClose(Vector3(9f, 11.5f, 4f), out)
		}

		run {
			val r1 = Ray(Vector3(10f, 20f, 30f), Vector3(0f, -1f, 0f))
			val r2 = Ray(Vector3(0f, 0f, 30f), Vector3(1f, 0f, 0f))

			val out = Vector3()
			assertTrue(r1.intersectsRay(r2, out))
			assertClose(Vector3(10f, 0f, 30f), out)
		}

		run {
			val r1 = Ray(Vector3(10f, 20f, 30f), Vector3(1f, -1f, 0f))
			val r2 = Ray(Vector3(0f, 0f, 30f), Vector3(1f, 0f, 0f))

			val out = Vector3()
			assertTrue(r1.intersectsRay(r2, out))
			assertClose(Vector3(30f, 0f, 30f), out)
		}
	}

	@Test fun serialize() {
		val ray = Ray2(Vector2(1.1f, 2.1f), Vector2(4.1f, 5.1f))
		val str = jsonStringify(Ray2.serializer(), ray)
		val ray2 = jsonParse(Ray2.serializer(), str)
		assertClose(ray.origin, ray2.origin)
		assertClose(ray.direction, ray2.direction)
	}
}
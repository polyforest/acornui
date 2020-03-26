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

import com.acornui.test.assertClose
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * @author nbilyk
 */
class BoxTest {

	@Test fun ext() {
		val boxA = Box()
		val boxB = Box()
		boxB.set(0f, 0f, 0f, 100f, 200f, 300f)
		boxA.ext(boxB)

		assertEquals(boxA, boxB)
		assertEquals(boxA.center, vec3(50f, 100f, 150f))
	}

	@Test fun extTransformed() {
		val boxA = Box()
		val boxB = Box()
		boxB.set(0f, 0f, 0f, 100f, 200f, 300f)
		val mat = Matrix4()
		mat.translate(50f, 100f, 150f)
		mat.scl(0.5f)
		boxA.ext(boxB, mat)

		assertEquals(boxA, boxB)
		assertEquals(boxA.center, vec3(50f, 100f, 150f))
	}

	@Test fun extTransformed2() {
		val boxA = Box()
		val boxB = Box()
		boxB.set(0f, 0f, 0f, 100f, 200f, 300f)
		val mat = Matrix4()
		mat.rotate(Vector3.Y, PI / 2)

		boxA.ext(boxB, mat)

		assertClose(vec3(300f, 200f, 100f), boxA.dimensions)
	}

	@Test fun intersectsRay() {
		run {
			// Axis-aligned ray is directly above an axis aligned box
			val ray = Ray()
			ray.setOrigin(50f, 100f, 150f)
			ray.setDirection(0f, 0f, -1f)

			val boxA = Box()
			boxA.set(0f, 0f, 0f, 100f, 200f, 10f)

			assertTrue(boxA.intersects(ray))

			val out: Vector3 = vec3()
			boxA.intersects(ray, out)
			assertEquals(vec3(50f, 100f, 10f), out)

			// Optimized case for a box with no depth. (Still axis aligned)
			boxA.max.z = 0f // max z == min z
			assertTrue(boxA.intersects(ray))
			boxA.intersects(ray, out)
			assertEquals(vec3(50f, 100f, 0f), out)

		}

		run {
			// Non axis-aligned Ray
			val ray = Ray()
			ray.setOrigin(50f, 100f, 20f)
			ray.setDirection(0.5f, 0.25f, -1f)

			val boxA = Box()
			boxA.set(0f, 0f, 10f, 100f, 200f, 10f)

			val out = vec3()

			assertTrue(boxA.intersects(ray, out))
			assertEquals(vec3(50f + 5f, 100f + 2.5f, 10f), out)

		}
	}

	@Test fun intersectsBackwardsRay() {
		val ray = Ray()
		ray.setOrigin(50f, 100f, -150f)
		ray.setDirection(0f, 0f, 1f)

		val boxA = Box()
		boxA.set(0f, 0f, 0f, 100f, 200f, -10f)
		assertTrue(boxA.intersects(ray))

		// Optimized case for a box with no depth.
		boxA.set(0f, 0f, 0f, 100f, 200f, 0f)
		assertTrue(boxA.intersects(ray))
	}

	@Test fun doesNotIntersectBehindRay() {
		val ray = Ray()
		ray.setOrigin(50f, 100f, 20f)
		ray.setDirection(0f, 0f, 1f)

		val box = Box()
		box.set(0f, 0f, 0f, 100f, 200f, 10f)

		assertFalse(box.intersects(ray))

		// Optimized case for a box with no depth.
		box.set(0f, 0f, 0f, 100f, 200f, 0f)
		assertFalse(box.intersects(ray))
	}

	@Test fun clampMinMax() {
		assertEquals(Box(), Box().clamp(MinMax(0f, 0f, 100f, 50f)))
		assertEquals(Box(vec3(10f, 0f, 0f), vec3(80f, 50f, 20f)), Box(vec3(), vec3(100f, 50f, 20f)).clamp(MinMax(10f, -10f, 80f, 55f)))
		assertEquals(Box(vec3(0f, 10f, 0f), vec3(100f, 40f, 20f)), Box(vec3(), vec3(100f, 50f, 20f)).clamp(MinMax(-10f, 10f, 110f, 40f)))
	}

}


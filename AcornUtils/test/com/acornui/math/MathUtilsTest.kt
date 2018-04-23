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
import com.acornui.test.benchmark
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.fail

/**
 * @author nbilyk
 */
class MathUtilsTest {

	@Test fun sin() {
		var x = -7f
		while (x <= 7f) {
			assertClose(Math.sin(x.toDouble()).toFloat(), MathUtils.sin(x), 0.01f)
			x += 0.01f
		}

		// Test that the common angles match more precisely.
		MathUtils.sin(0f)
		for (i in 0..16) {
			val theta = i * PI2 / 16
			assertClose(Math.sin(theta.toDouble()).toFloat(), MathUtils.sin(theta), 0.000001f)
		}
	}

	@Test fun sinSpeed() {
		val utilsSpeed = benchmark {
			var x = -7f
			while (x <= 7f) {
				MathUtils.sin(x)
				x += 0.01f
			}
		}
		println("MathUtils.sin speed: " + utilsSpeed)

		val nativeSpeed = benchmark {
			var x = -7.0
			while (x <= 7.0) {
				Math.sin(x)
				x += 0.01
			}
		}
		println("Math.sin speed: " + nativeSpeed)

		if (utilsSpeed * 1.5f > nativeSpeed) {
			fail("MathUtils.sin not fast enough $utilsSpeed $nativeSpeed")
		}
	}

	@Test fun tanSpeed() {
		val utilsSpeed = benchmark {
			var x = -7f
			while (x <= 7f) {
				MathUtils.tan(x)
				x += 0.01f
			}
		}
		println("MathUtils.tan speed: " + utilsSpeed)

		val nativeSpeed = benchmark {
			var x = -7.0
			while (x <= 7.0) {
				Math.tan(x)
				x += 0.01
			}
		}
		println("Math.tan speed: " + nativeSpeed)

		if (utilsSpeed * 2.0f > nativeSpeed) {
			fail("MathUtils.tan not fast enough")
		}
	}


	@Test fun cos() {
		var x = -7f
		while (x <= 7f) {
			assertClose(Math.cos(x.toDouble()).toFloat(), MathUtils.cos(x), 0.01f)
			x += 0.01f
		}
	}

	@Test fun acos() {
		var x = -7f
		while (x <= 7f) {
			assertClose(Math.acos(x.toDouble()).toFloat(), MathUtils.acos(x), 0.01f)
			x += 0.01f
		}
	}

	@Test fun atan2() {
		var x = -7f
		while (x <= 7f) {
			var y = -7f
			while (y <= 7f) {
				assertClose(Math.atan2(x.toDouble(), y.toDouble()).toFloat(), MathUtils.atan2(x, y), 0.01f)
				y += 0.1f
			}
			x += 0.1f
		}
	}

	@Test fun clamp() {
		assertEquals(3, MathUtils.clamp(3, 0, 4))
		assertEquals(4, MathUtils.clamp(5, 0, 4))
		assertEquals(4, MathUtils.clamp(1, 4, 8))
		assertEquals(4, MathUtils.clamp(4, 4, 8))
		assertEquals(8, MathUtils.clamp(8, 4, 8))
		assertEquals(-8, MathUtils.clamp(-11, -8, -4))
		assertEquals(-4, MathUtils.clamp(-3, -8, -4))

		assertEquals(3f, MathUtils.clamp(3f, 0f, 4f))
		assertEquals(3.5f, MathUtils.clamp(8.5f, 0f, 3.5f))
		assertEquals(4f, MathUtils.clamp(5f, 0f, 4f))
		assertEquals(4f, MathUtils.clamp(1f, 4f, 8f))
		assertEquals(4f, MathUtils.clamp(4f, 4f, 8f))
		assertEquals(8f, MathUtils.clamp(8f, 4f, 8f))
		assertEquals(-8f, MathUtils.clamp(-11f, -8f, -4f))
		assertEquals(-4f, MathUtils.clamp(-3f, -8f, -4f))

		assertEquals(-4.0, MathUtils.clamp(-3.0, -8.0, -4.0))
	}


	@Test fun angleDiff() {
		assertClose(0f, MathUtils.angleDiff(0f, 0f))
		assertClose(PI / 2f, MathUtils.angleDiff(0f, PI / 2f))
		assertClose(-PI / 2f, MathUtils.angleDiff(PI / 2f, 0f))
		assertClose(-PI, MathUtils.angleDiff(PI, 0f))
		assertClose(-PI, MathUtils.angleDiff(0f, PI))
		assertClose(0f, MathUtils.angleDiff(PI * 4, 0f))
		assertClose(0f, MathUtils.angleDiff(0f, PI * 4))
		assertClose(PI * 2f / 3f, MathUtils.angleDiff(PI * 2f / 3f, PI * 4f / 3f))
		assertClose(-PI * 2f / 3f, MathUtils.angleDiff(PI * 4f / 3f, PI * 2f / 3f))
		assertClose(-PI, MathUtils.angleDiff(PI * 3f / 2f, PI / 2f))
	}

}
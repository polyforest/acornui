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

/**
 * @author nbilyk
 */
class MathUtilsTest {

	@Test fun clamp() {
		assertEquals(3, clamp(3, 0, 4))
		assertEquals(4, clamp(5, 0, 4))
		assertEquals(4, clamp(1, 4, 8))
		assertEquals(4, clamp(4, 4, 8))
		assertEquals(8, clamp(8, 4, 8))
		assertEquals(-8, clamp(-11, -8, -4))
		assertEquals(-4, clamp(-3, -8, -4))

		assertEquals(3.0, clamp(3.0, 0.0, 4.0))
		assertEquals(3.5, clamp(8.5, 0.0, 3.5))
		assertEquals(4.0, clamp(5.0, 0.0, 4.0))
		assertEquals(4.0, clamp(1.0, 4.0, 8.0))
		assertEquals(4.0, clamp(4.0, 4.0, 8.0))
		assertEquals(8.0, clamp(8.0, 4.0, 8.0))
		assertEquals(-8.0, clamp(-11.0, -8.0, -4.0))
		assertEquals(-4.0, clamp(-3.0, -8.0, -4.0))

		assertEquals(-4.0, clamp(-3.0, -8.0, -4.0))
	}


	@Test fun angleDiff() {
        assertClose(0.0, angleDiff(0.0, 0.0))
        assertClose(PI / 2.0, angleDiff(0.0, PI / 2.0))
        assertClose(-PI / 2.0, angleDiff(PI / 2.0, 0.0))
        assertClose(-PI, angleDiff(PI, 0.0))
        assertClose(-PI, angleDiff(0.0, PI))
        assertClose(0.0, angleDiff(PI * 4, 0.0))
        assertClose(0.0, angleDiff(0.0, PI * 4))
        assertClose(PI * 2.0 / 3.0, angleDiff(PI * 2.0 / 3.0, PI * 4.0 / 3.0))
        assertClose(-PI * 2.0 / 3.0, angleDiff(PI * 4.0 / 3.0, PI * 2.0 / 3.0))
        assertClose(-PI, angleDiff(PI * 3.0 / 2.0, PI / 2.0))
	}

	@Test fun modInt() {

		assertEquals(0, mod(0, 4))
		assertEquals(1, mod(1, 4))
		assertEquals(2, mod(2, 4))
		assertEquals(3, mod(3, 4))
		assertEquals(0, mod(4, 4))
		assertEquals(1, mod(5, 4))
		assertEquals(1, mod(5 + 4, 4))

		assertEquals(3, mod(-1, 4))
		assertEquals(2, mod(-2, 4))
		assertEquals(1, mod(-3, 4))
		assertEquals(0, mod(-4, 4))
		assertEquals(3, mod(-5, 4))
		assertEquals(3, mod(-5 - 4, 4))

	}

	@Test fun roundToNearest() {
		assertEquals(100.0, roundToNearest(101.0, 100.0))
		assertEquals(200.0, roundToNearest(150.0, 100.0))
		assertEquals(100.0, roundToNearest(90.0, 100.0))
	}

}
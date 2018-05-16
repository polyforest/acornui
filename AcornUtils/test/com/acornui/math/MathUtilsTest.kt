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

import com.acornui.math.MathUtils.angleDiff
import com.acornui.math.MathUtils.clamp
import com.acornui.math.MathUtils.mod
import com.acornui.test.assertClose
import org.junit.Test
import kotlin.test.assertEquals

/**
 * @author nbilyk
 */
class MathUtilsTest {

	@Test fun clampTest() {
		assertEquals(3, clamp(3, 0, 4))
		assertEquals(4, clamp(5, 0, 4))
		assertEquals(4, clamp(1, 4, 8))
		assertEquals(4, clamp(4, 4, 8))
		assertEquals(8, clamp(8, 4, 8))
		assertEquals(-8, clamp(-11, -8, -4))
		assertEquals(-4, clamp(-3, -8, -4))

		assertEquals(3f, clamp(3f, 0f, 4f))
		assertEquals(3.5f, clamp(8.5f, 0f, 3.5f))
		assertEquals(4f, clamp(5f, 0f, 4f))
		assertEquals(4f, clamp(1f, 4f, 8f))
		assertEquals(4f, clamp(4f, 4f, 8f))
		assertEquals(8f, clamp(8f, 4f, 8f))
		assertEquals(-8f, clamp(-11f, -8f, -4f))
		assertEquals(-4f, clamp(-3f, -8f, -4f))

		assertEquals(-4.0, clamp(-3.0, -8.0, -4.0))
	}


	@Test fun angleDiffTest() {
		assertClose(0f, angleDiff(0f, 0f))
		assertClose(PI / 2f, angleDiff(0f, PI / 2f))
		assertClose(-PI / 2f, angleDiff(PI / 2f, 0f))
		assertClose(-PI, angleDiff(PI, 0f))
		assertClose(-PI, angleDiff(0f, PI))
		assertClose(0f, angleDiff(PI * 4, 0f))
		assertClose(0f, angleDiff(0f, PI * 4))
		assertClose(PI * 2f / 3f, angleDiff(PI * 2f / 3f, PI * 4f / 3f))
		assertClose(-PI * 2f / 3f, angleDiff(PI * 4f / 3f, PI * 2f / 3f))
		assertClose(-PI, angleDiff(PI * 3f / 2f, PI / 2f))
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

}
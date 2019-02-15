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

	@Test fun modInt() {

		assertEquals(0, MathUtils.mod(0, 4))
		assertEquals(1, MathUtils.mod(1, 4))
		assertEquals(2, MathUtils.mod(2, 4))
		assertEquals(3, MathUtils.mod(3, 4))
		assertEquals(0, MathUtils.mod(4, 4))
		assertEquals(1, MathUtils.mod(5, 4))
		assertEquals(1, MathUtils.mod(5 + 4, 4))

		assertEquals(3, MathUtils.mod(-1, 4))
		assertEquals(2, MathUtils.mod(-2, 4))
		assertEquals(1, MathUtils.mod(-3, 4))
		assertEquals(0, MathUtils.mod(-4, 4))
		assertEquals(3, MathUtils.mod(-5, 4))
		assertEquals(3, MathUtils.mod(-5 - 4, 4))

	}

}
/*
 * Copyright 2018 Nicholas Bilyk
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
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class Vector2Test {

	@Test
	fun len() {
        assertClose(3.0, vec2(3.0, 0.0).len())
        assertClose(3.0, vec2(0.0, -3.0).len())
        assertClose(sqrt(18.0), vec2(3.0, -3.0).len())
	}

	@Test
	fun len2() {
        assertClose(9.0, vec2(3.0, 0.0).len2())
        assertClose(9.0, vec2(0.0, -3.0).len2())
        assertClose(18.0, vec2(3.0, -3.0).len2())
	}

	@Test
	fun minus1() {
		assertEquals(vec2(2.0, 3.0), vec2(4.0, 6.0) - vec2(2.0, 3.0))
	}

	@Test
	fun minus2() {
		assertEquals(vec2(2.0, 3.0), vec2(4.0, 6.0).minus(2.0, 3.0))
	}

	@Test
	fun nor() {
		assertEquals(vec2(1.0, 0.0), vec2(5.0, 0.0).nor())
	}

	@Test
	fun plus() {
		assertEquals(vec2(10.0, 11.0), vec2(5.0, 4.0).plus(5.0, 7.0))
	}

	@Test
	fun plus1() {
		assertEquals(vec2(10.0, 11.0), vec2(5.0, 4.0).plus(vec2(5.0, 7.0)))
	}

	@Test
	fun dot() {
		assertEquals(1.0, vec2(1.0, 0.0).dot(1.0, 0.0))
		assertEquals(-1.0, vec2(1.0, 0.0).dot(-1.0, 0.0))
		assertEquals(0.0, vec2(1.0, 0.0).dot(0.0, 1.0))

//		println(Vector2(50.0, 0.0).crs(100.0, 100.0) / Vector2(50.0, 0.0).len())
//		println(Vector2(10.0, 50.0).crs(0.0, 10.0) / Vector2(50.0, 10.0).len())
//		println(Vector2(10.0, 50.0).crs(5.0, 1.0) / Vector2(50.0, 10.0).len())
	}

	@Test
	fun dot1() {
		assertEquals(1.0, vec2(1.0, 0.0).dot(vec2(1.0, 0.0)))
	}

	@Test
	fun scl() {
		assertEquals(vec2(6.0, 6.0), vec2(3.0, 2.0).times(2.0, 3.0))
	}

	@Test
	fun scl1() {
		assertEquals(vec2(6.0, 6.0), vec2(3.0, 2.0).times(vec2(2.0, 3.0)))
	}

	@Test
	fun scl2() {
		assertEquals(vec2(6.0, 4.0), vec2(3.0, 2.0).times(2.0))
	}

	@Test
	fun dst() {
		assertEquals(4.0, vec2(0.0, 0.0).dst(vec2(4.0, 0.0)))
	}

	@Test
	fun dst1() {
		assertEquals(4.0, vec2(0.0, 0.0).dst(4.0, 0.0))
	}

	@Test
	fun dst2() {
		assertEquals(16.0, vec2(0.0, 0.0).dst2(4.0, 0.0))
	}

	@Test
	fun manhattanDst() {
		assertEquals(5.0, vec2(0.0, 0.0).manhattanDst(vec2(4.0, 1.0)))
	}

	@Test
	fun limit() {
		assertEquals(vec2(1.0, 0.0), vec2(3.0, 0.0).limit(1.0))
	}

	@Test
	fun clamp() {
		assertEquals(vec2(3.0, 0.0), vec2(4.0, 0.0).clamp(1.0, 3.0))
		assertEquals(vec2(1.0, 0.0), vec2(0.5, 0.0).clamp(1.0, 3.0))
	}

	@Test
	fun mul() {
	}

	@Test
	fun crs() {
	}

	@Test
	fun crs1() {
	}

	@Test
	fun angle() {
	}

	@Test
	fun setAngleRad() {
	}

	@Test
	fun rotateRad() {
	}

	@Test
	fun lerp() {
	}

	@Test
	fun lerp1() {
	}

	@Test
	fun interpolate() {
	}

	@Test
	fun epsilonEquals() {
	}

	@Test
	fun epsilonEquals1() {
	}

	@Test
	fun isUnit() {
	}

	@Test
	fun isUnit1() {
	}

	@Test
	fun isZero() {
	}

	@Test
	fun isZero1() {
	}

	@Test
	fun isOnLine() {
		assertTrue(vec2(100.0, 0.0).isOnLine(vec2(50.0, 0.0)))
		assertTrue(vec2(100.0, 110.0).isOnLine(vec2(50.0, 55.0)))
	}

	@Test
	fun isOnLine1() {
	}

	@Test
	fun isCollinear() {
	}

	@Test
	fun isCollinear1() {
	}

	@Test
	fun isCollinearOpposite() {
	}

	@Test
	fun isCollinearOpposite1() {
	}

	@Test
	fun isPerpendicular() {
	}

	@Test
	fun isPerpendicular1() {
	}

	@Test
	fun hasSameDirection() {
	}

	@Test
	fun hasOppositeDirection() {
	}

	@Test
	fun ext() {
	}

	@Test
	fun clear() {
	}

	@Test
	fun free() {
	}

	@Test
	fun equalsTest() {
	}

	@Test
	fun hashCodeTest() {
	}

	@Test
	fun toStringTest() {
	}

	@Test
	fun getX() {
	}

	@Test
	fun setX() {
	}

	@Test
	fun getY() {
	}

	@Test
	fun setY() {
	}
}
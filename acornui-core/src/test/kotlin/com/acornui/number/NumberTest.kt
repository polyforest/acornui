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

package com.acornui.number

import com.acornui.number.*
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * @author nbilyk
 */
class NumberTest {

	@Test fun numberOfTrailingZeros() {
		assertEquals(3, 8.numberOfTrailingZeros())
		assertEquals(0, 7.numberOfTrailingZeros())
		assertEquals(32, 0.numberOfTrailingZeros())
		assertEquals(0, 1.numberOfTrailingZeros())
		assertEquals(1, 2.numberOfTrailingZeros())
	}

	@Test fun numberOfLeadingZeros() {
		assertEquals(31, 1.numberOfLeadingZeros())
		assertEquals(4, 0x0fffffff.numberOfLeadingZeros())
	}

	@Test fun zeroPaddingTest() {
		assertEquals("3.0", 3.zeroPadding(0, 1))
		assertEquals("3.1", 3.1.zeroPadding(0, 1))
		assertEquals("3.100", 3.1.zeroPadding(0, 3))
		assertEquals("03.100", 3.1.zeroPadding(2, 3))
		assertEquals("0003.120", 3.12.zeroPadding(4, 3))
		assertEquals("0003.000", 3.zeroPadding(4, 3))
		assertEquals("0003", 3L.zeroPadding(4, 0))
		assertEquals("0003.0", 3L.zeroPadding(4, 1))
		assertEquals("0034.123", 034.123.zeroPadding(4, 1))
	}

	@Test fun nonZeroTest() {
		assertEquals(3.0, 3.0.nonZero())
		assertEquals(-3.0, (-3.0).nonZero())
		assertEquals(Double.MIN_VALUE, 0.0.nonZero())
		assertEquals(Double.MIN_VALUE, (-0.0).nonZero())
		assertEquals(-Double.MIN_VALUE, (-Double.MIN_VALUE).nonZero())
		assertEquals(Double.MIN_VALUE, (Double.MIN_VALUE).nonZero())
	}


	@Test fun cssToDouble() {
		assertEquals(3.0, "3px".cssToDouble())
		assertEquals(0.0, "".cssToDouble())
		assertEquals(200.0, "200px".cssToDouble())
		assertEquals(200.5, "200.5px".cssToDouble())
	}
}
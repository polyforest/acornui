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

package com.acornui.string

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * @author nbilyk
 */
class SubStringTest {

	@Test fun testLength() {
		val s = SubString("Hello World", 3, 5)
		assertEquals(2, s.length)
		assertEquals(2, s.subSequence(0, 8).length)
	}

	@Test fun testCharAt() {
		val s = SubString("Hello World", 2, 6)
		assertEquals('l', s.charAt(0))
		assertEquals('l', s.charAt(1))
		assertEquals('o', s.charAt(2))
		assertEquals(' ', s.charAt(3))
	}

	@Test fun testSubSequence() {
		val s = SubString("Hello World")
		assertEquals("el", s.subSequence(0, 5).subSequence(1, 3).toString())
		assertEquals("lo", s.subSequence(2, 7).subSequence(1, 3).toString())
		assertEquals("lo", s.subSequence(2, 5).subSequence(1, 6).toString())
	}

	@Test fun testToString() {
		val s = SubString("Hello World")
		assertEquals("Hello", s.subSequence(0, 5).toString())

		assertEquals("llo W", s.subSequence(2, 7).toString())
	}

	@Test fun testCompareTo() {
		val s = SubString("Hello World").subSequence(6, 11)
		assertEquals(0, s.compareTo("World"))
		assertEquals("World".compareTo("Xorld"), s.compareTo("Xorld"))
		assertEquals("World".compareTo("Worldb"), s.compareTo("Worldb"))
		assertEquals("World".compareTo("BWorld"), s.compareTo("BWorld"))
		assertEquals("World".compareTo("Borld"), s.compareTo("Borld"))
	}

	@Test fun testEquals() {
		val s = SubString("Hello World")
		assertTrue(s.subSequence(0, 5).equalsStr("Hello"))
		val ello = s.subSequence(1, 5)
		assertTrue(ello.equalsStr("ello"))
		assertFalse(ello.equalsStr("Hello"))
		assertFalse(ello.equalsStr("elo"))
		assertFalse(ello.equalsStr("ell"))
		assertTrue(s.subSequence(1, 1).equalsStr(""))
		assertFalse(s.subSequence(1, 1).equalsStr("e"))
		assertFalse(s.subSequence(1, 2).equalsStr(""))
		assertTrue(s.subSequence(1, 2).equalsStr("e"))

	}

	@Test fun testHashCode() {
		val s = SubString("Hello World")
		assertEquals("Hello".hashCode(), s.subSequence(0, 5).hashCode())
		assertEquals("ello".hashCode(), s.subSequence(1, 5).hashCode())
		assertEquals("".hashCode(), s.subSequence(1, 1).hashCode())
		assertEquals("e".hashCode(), s.subSequence(1, 2).hashCode())
	}

	@Test fun testStartsWith() {
		val s = SubString("Hello World", 6, 11)
		assertTrue(s.startsWith("World"))
		assertTrue(s.startsWith("orl", 1))
		assertFalse(s.startsWith("World", 1))
	}

	@Test fun testIndexOf() {
		val s = SubString("Hello World", 6, 11)
		assertEquals(-1, s.indexOf('H'))
		assertEquals(0, s.indexOf('W'))
		assertEquals(0, s.indexOf("W"))

		assertEquals(1, s.indexOf('o'))
		assertEquals(1, s.indexOf("orld"))
		assertEquals(-1, s.indexOf("orldy"))
	}

	@Test fun testLastIndexOf() {
		val s = SubString("Hello World", 3, 11)
		assertEquals(-1, s.lastIndexOf('H'))
		assertEquals(3, s.lastIndexOf('W'))
		assertEquals(3, s.lastIndexOf("W"))

		assertEquals(4, s.lastIndexOf('o'))
		assertEquals(4, s.lastIndexOf("orld"))
		assertEquals(-1, s.lastIndexOf("orldy"))
	}
}

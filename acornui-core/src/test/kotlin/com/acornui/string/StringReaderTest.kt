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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNull

/**
 * @author nbilyk
 */
class StringReaderTest {

	@Test fun getInt() {
		val parser = StringReader(" 123 434 255")
		parser.white()
		val a = parser.getInt()
		parser.white()
		val b = parser.getInt()
		parser.white()
		val c = parser.getInt()
		assertEquals(123, a)
		assertEquals(434, b)
		assertEquals(255, c)
	}

	@Test fun getDouble() {
		val parser = StringReader(" 123.526 434 255.0 -234.0 77.3.5")
		parser.white()
		assertEquals(123.526, parser.getDouble())
		parser.white()
		assertEquals(434.0, parser.getDouble())
		parser.white()
		assertEquals(255.0, parser.getDouble())
		parser.white()
		assertEquals(-234.0, parser.getDouble())
		parser.white()
		assertEquals(77.3, parser.getDouble())

	}

	@Test fun getQuotedString() {
		assertEquals("Hello World", StringReader("'Hello World'").getQuotedString())
		assertNull(StringReader("'World").getQuotedString())
	}
	
	@Test fun getQuotedString2() {
		val parser = StringReader("Hello = 'World'")
		parser.consumeString("Hello")
		parser.white()
		parser.consumeChar('=')
		parser.white()
		assertEquals("World", parser.getQuotedString())
	}

	@Test fun getInner() {
		val parser = StringReader("Hello (one(two)three) End")
		parser.consumeString("Hello")
		parser.white()
		assertEquals("one(two)three", parser.getInner('(', ')'))
		parser.white()
		assertTrue(parser.consumeString("End"))
	}

	@Test fun consumeThrough() {
		val parser = StringReader("Hello = 'World'")
		parser.consumeThrough('=')
		parser.white()
		assertEquals("World", parser.getQuotedString())
	}

	@Test fun readLine() {
		val windowz = "\r\n"

		val str = """Hello World
This is line 1.
This is line 2${windowz}This is line 3
This is the last line"""

		val parser = StringReader(str)
		assertEquals("Hello World", parser.readLine())
		assertEquals("This is line 1.", parser.readLine())
		assertEquals("This is line 2", parser.readLine())
		assertEquals("This is line 3", parser.readLine())
		assertEquals("This is the last line", parser.readLine())
		assertEquals("", parser.readLine())
		assertEquals("", parser.readLine())

	}
}
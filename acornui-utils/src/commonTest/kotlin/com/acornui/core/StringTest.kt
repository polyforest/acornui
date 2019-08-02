/*
 * Copyright 2017 Nicholas Bilyk
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

package com.acornui

import kotlin.test.Test
import kotlin.test.assertEquals

class StringTest {

	@Test fun testRemoveBackslashes() {
		assertEquals("Test\nMe", removeBackslashes("""Test\nMe"""))
		assertEquals("Test\nMe\nAgain", removeBackslashes("""Test\nMe\nAgain"""))
		assertEquals("Test\nMe\nAgain\\", removeBackslashes("""Test\nMe\nAgain\\"""))
		assertEquals("\tTest\nMe\nAgain\\", removeBackslashes("""\tTest\nMe\nAgain\\"""))
		assertEquals("\t\bTest\nMe\nAgain\\", removeBackslashes("""\t\bTest\nMe\nAgain\\"""))
		assertEquals("\t\bTest\nMe\nAgain\r\\", removeBackslashes("""\t\bTest\nMe\nAgain\r\\"""))
		assertEquals("\t\bTest\nMe\nAgain\r\\\"", removeBackslashes("""\t\bTest\nMe\nAgain\r\\\""""))
		assertEquals("\t\bTest\nMe\nAgain\n\$Again\r\\\"", removeBackslashes("""\t\bTest\nMe\nAgain\n\${'$'}Again\r\\\""""))
		assertEquals("Test\u2345Me", removeBackslashes("""Test\u2345Me"""))
	}

	@Test fun testAddBackslashes() {
		assertEquals("""Test\nMe""", addBackslashes("Test\nMe"))
		assertEquals("""Test\nMe\nAgain""", addBackslashes("Test\nMe\nAgain"))
		assertEquals("""Test\nMe\nAgain\\""", addBackslashes("Test\nMe\nAgain\\"))
		assertEquals("""\tTest\nMe\nAgain\\""", addBackslashes("\tTest\nMe\nAgain\\"))
		assertEquals("""\t\bTest\nMe\nAgain\\""", addBackslashes("\t\bTest\nMe\nAgain\\"))
		assertEquals("""\t\bTest\nMe\nAgain\r\\""", addBackslashes("\t\bTest\nMe\nAgain\r\\"))
		assertEquals("""\t\bTest\nMe\nAgain\r\\\"""", addBackslashes("\t\bTest\nMe\nAgain\r\\\""))
		assertEquals("""\t\bTest\nMe\nAgain\n\${'$'}Again\r\\\"""", addBackslashes("\t\bTest\nMe\nAgain\n\$Again\r\\\""))
	}

}
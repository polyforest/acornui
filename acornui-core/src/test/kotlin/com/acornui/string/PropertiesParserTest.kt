/*
 * Copyright 2020 Poly Forest, LLC
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

import com.acornui.test.benchmark
import kotlin.test.Test
import kotlin.test.assertEquals

class PropertiesParserTest {

	@Test
	fun testParse() {

		assertEquals(
			mapOf("key1" to "value1", "key2" to "value2", "key3" to "This is a \nmultiline \nvalue"),
			PropertiesParser.parse(
				"""
			key1 =value1
 			key2 =value2
 			
 			# This line is commented
 			! This line is also commented
 			
 			key3 =This is a \
multiline \
value
		""")
		)
	}

	@Test
	fun benchmarkParse() {

		val medianTime = benchmark {
			PropertiesParser.parse(
				"""
				key1 =value1
				key2 =value2
				
				# This line is commented
				! This line is also commented
				
				key3 =This is a \
	multiline \
	value
				key5 =anotherTest
				# another comment
				key6=Another
				key7=Another multiline\
				line2\
				line3\
				line4\
				line5\
				line6
				key8=Test
				
			""")
		}

		// Laika, Geekbench 5 single core: 1125, multi core: 4348
		// nodejs: 60.0us
		println("PropertiesParser.parse benchmark: $medianTime")
	}
}
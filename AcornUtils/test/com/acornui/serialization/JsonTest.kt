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

package com.acornui.serialization

import com.acornui.test.assertListEquals
import com.acornui.test.benchmark
import org.junit.Test
import kotlin.test.assertEquals

/**
 * @author nbilyk
 */
class JsonTest {

	private fun jsonParse(str: String): JsonNode {
		return JsonNode(str, 0, str.length)
	}

	@Test fun parse() {
		// language=JSON
		val value = jsonParse("""
		{
		    "bool1": true,
		    "bool2": false,
			"char": "c",
		    "int1": 1,
		    "int2": 2,
		    "double1": -0.1,
		    "str1": "String 0-1",
			"strPadded": "  Test  ",
		    "obj1": {
		    	"bool1": true,
				"bool2": false,
				"int1": 1,
				"int2": 2,
				"double1": 1.1
		    },
		    "obj2": {
		    	"bool1": true,
				"bool2": false,
				"int1": 3,
				"int2": 4,
				"str1": "String 2-1",
				"double1": 2.1
		    },
		    "arr1": [0,1,2,3,4],
		    "arr2": [
		    	[55, 66],
		    	"Test1",
		    	{
		    		"a": 23,
		    		"b": 43
		    	}
		    ]
		 }
		 """)

		assertEquals(true, value["bool1"]!!.bool())
		assertEquals('c', value["char"]!!.char())
		assertEquals(false, value["bool2"]!!.bool())
		assertEquals(1, value["int1"]!!.int())
		assertEquals(2, value["int2"]!!.int())
		assertEquals("String 0-1", value["str1"]!!.string())
		assertEquals(-0.1, value["double1"]!!.double())
		assertEquals(true, value["obj1"]!!["bool1"]!!.bool())
		assertEquals(false, value["obj1"]!!["bool2"]!!.bool())
		assertEquals(2, value["obj1"]!!["int2"]!!.int())
		assertEquals(1.1, value["obj1"]!!["double1"]!!.double())
		assertEquals(4, value["obj2"]!!["int2"]!!.int())
		assertEquals("String 2-1", value["obj2"]!!["str1"]!!.string())
		assertEquals(2.1, value["obj2"]!!["double1"]!!.double())
		assertListEquals(intArrayOf(0, 1, 2, 3, 4), value["arr1"]!!.intArray()!!)
		assertListEquals(intArrayOf(55, 66), value["arr2"]!![0]!!.intArray()!!)
		assertEquals("Test1", value["arr2"]!![1]!!.string())
		assertEquals(23, value["arr2"]!![2]!!["a"]!!.int())
		assertEquals(43, value["arr2"]!![2]!!["b"]!!.int())
		assertEquals("  Test  ", value["strPadded"]!!.string())
	}

	@Test fun parse2() {
		// language=JSON
		val value = jsonParse("""
		 {
		    "str": "str' test",
		    "str2": "str\"' test",
		    "ju'\"nk"   : true
		 }
		 """)

		assertEquals("str' test", value["str"]!!.string())
	}

	@Test fun arrays() {
		// language=JSON
		val value = jsonParse("""
		 {
		    "arr1": [1, 2, 3, 4],
		    "arr2": [5, 6, 7, 8],
		    "arr3": ["a", "b", "c", "d"]
		 }
		 """)

		assertListEquals(intArrayOf(1, 2, 3, 4), value["arr1"]!!.intArray()!!)
		assertListEquals(intArrayOf(5, 6, 7, 8), value["arr2"]!!.intArray()!!)
		assertListEquals(charArrayOf('a', 'b', 'c', 'd'), value["arr3"]!!.charArray()!!)
	}

	@Test fun benchmarkTest() {
		// language=JSON
		val str = """
		{
		    "bool1": true,
		    "bool2": false,
		    "int1": 1,
		    "int2": 2,
		    "double1": -0.1,
		    "str1": "String 0-1",
		    "obj1": {
		    	"bool1": true,
				"bool2": false,
				"int1": 1,
				"int2": 2,
				"double1": 1.1
		    },
		    "obj2": {
		    	"bool1": true,
				"bool2": false,
				"int1": 3,
				"int2": 4,
				"str1": "String 2-1",
				"double1": 2.1
		    }
		 }
		 """
		val v = benchmark {
			val value = jsonParse(str)
			value["bool1"]!!.bool()
			value["bool2"]!!.bool()
			value["int1"]!!.int()
			value["obj1"]!!["int1"]!!.int()
		}
		println(v) //  0.011262276
	}

	@Test fun testWrite() {
		val data = PersonData("Bob", 1956, true,
				arrayOf(PersonData("Nicholas", 1982, true, arrayOf(
						PersonData("Margaret", 2001, false),
						PersonData("Dizzy", 2001, false)
				)),
						PersonData("Alexander", 1985, false),
						PersonData("Joseph", 1987, true),
						PersonData("Christian", 1987, false)))
		val json = JsonSerializer.write(data, PersonDataSerializer)

		// language=JSON
		val expected = """{
	"name": "Bob",
	"born": 1956,
	"married": true,
	"bools": [ true, true, false, false, true ],
	"ints": [ 0, 1, 2, 3, 4 ],
	"char": "a",
	"double": 3.4,
	"object": {
		"foo": "bar",
		"strings": [ "test1", "test2" ]
	},
	"children": [
		{
			"name": "Nicholas",
			"born": 1982,
			"married": true,
			"bools": [ true, true, false, false, true ],
			"ints": [ 0, 1, 2, 3, 4 ],
			"char": "a",
			"double": 3.4,
			"object": {
				"foo": "bar",
				"strings": [ "test1", "test2" ]
			},
			"children": [
				{
					"name": "Margaret",
					"born": 2001,
					"married": false,
					"bools": [ true, true, false, false, true ],
					"ints": [ 0, 1, 2, 3, 4 ],
					"char": "a",
					"double": 3.4,
					"object": {
						"foo": "bar",
						"strings": [ "test1", "test2" ]
					},
					"children": [
					]
				},
				{
					"name": "Dizzy",
					"born": 2001,
					"married": false,
					"bools": [ true, true, false, false, true ],
					"ints": [ 0, 1, 2, 3, 4 ],
					"char": "a",
					"double": 3.4,
					"object": {
						"foo": "bar",
						"strings": [ "test1", "test2" ]
					},
					"children": [
					]
				}
			]
		},
		{
			"name": "Alexander",
			"born": 1985,
			"married": false,
			"bools": [ true, true, false, false, true ],
			"ints": [ 0, 1, 2, 3, 4 ],
			"char": "a",
			"double": 3.4,
			"object": {
				"foo": "bar",
				"strings": [ "test1", "test2" ]
			},
			"children": [
			]
		},
		{
			"name": "Joseph",
			"born": 1987,
			"married": true,
			"bools": [ true, true, false, false, true ],
			"ints": [ 0, 1, 2, 3, 4 ],
			"char": "a",
			"double": 3.4,
			"object": {
				"foo": "bar",
				"strings": [ "test1", "test2" ]
			},
			"children": [
			]
		},
		{
			"name": "Christian",
			"born": 1987,
			"married": false,
			"bools": [ true, true, false, false, true ],
			"ints": [ 0, 1, 2, 3, 4 ],
			"char": "a",
			"double": 3.4,
			"object": {
				"foo": "bar",
				"strings": [ "test1", "test2" ]
			},
			"children": [
			]
		}
	]
}"""
		assertEquals(expected, json)
	}

	@Test fun escaped() {
		val data = PersonData("""B"\ob""", 1956, true)
		val json = JsonSerializer.write(data, PersonDataSerializer)
		assertEquals("""{
	"name": "B\"\\ob",
	"born": 1956,
	"married": true,
	"bools": [ true, true, false, false, true ],
	"ints": [ 0, 1, 2, 3, 4 ],
	"char": "a",
	"double": 3.4,
	"object": {
		"foo": "bar",
		"strings": [ "test1", "test2" ]
	},
	"children": [
	]
}""", json)

	}
}

private data class PersonData(
		val name: String,
		val birthDate: Int,
		val married: Boolean,

		val children: Array<PersonData> = arrayOf()
) {
}

private object PersonDataSerializer : To<PersonData> {

	override fun PersonData.write(writer: Writer) {
		writer.string("name", name)
		writer.int("born", birthDate)
		writer.bool("married", married)
		writer.boolArray("bools", booleanArrayOf(true, true, false, false, true))
		writer.intArray("ints", intArrayOf(0, 1, 2, 3, 4))
		writer.char("char", 'a')
		writer.double("double", 3.4)
		writer.obj("object", true) {
			childWriter ->
			childWriter.string("foo", "bar")
			childWriter.stringArray("strings", arrayOf("test1", "test2"))
		}

		writer.array("children", children, PersonDataSerializer)
	}
}
package com.acornui.time

import com.acornui.serialization.jsonParse
import com.acornui.serialization.jsonStringify
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals

class DateTest {

	@Test fun parseDateTest() {

		val d = utcDate(2019, 2, 3, 4, 5, 6, 7)
		val json = jsonStringify(Date.serializer(), d)
		assertEquals("\"2019-02-03T04:05:06.007Z\"", json)
		assertEquals(d, jsonParse(Date.serializer(), json))

		val m = MyClassWithDate(d)
		val json2 = jsonStringify(MyClassWithDate.serializer(), m)
		assertEquals(m, jsonParse(MyClassWithDate.serializer(), json2))

		val json3 = jsonStringify(MyClassWithDate.serializer(), MyClassWithDate(null))
		assertEquals(MyClassWithDate(null), jsonParse(MyClassWithDate.serializer(), json3))
	}
}

@Serializable
private data class MyClassWithDate(val d: Date?)
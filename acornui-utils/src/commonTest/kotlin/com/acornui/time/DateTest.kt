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
		assertEquals(d, jsonParse(Date.serializer(), json))

		val m = MyClassWithDate(d)
		val json2 = jsonStringify(MyClassWithDate.serializer(), m)
		assertEquals(m, jsonParse(MyClassWithDate.serializer(), json2))
	}
}

@Serializable
private data class MyClassWithDate(val d: Date)
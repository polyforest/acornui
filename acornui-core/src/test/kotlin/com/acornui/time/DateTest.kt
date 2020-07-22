package com.acornui.time

import com.acornui.serialization.jsonParse
import com.acornui.serialization.jsonStringify
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.days
import kotlin.time.milliseconds
import kotlin.time.minutes
import kotlin.time.seconds

class DateTest {

	@Test fun toIsoString() {
		val d = Date.UTC(2019, 2, 3, 4, 5, 6, 7)
		assertEquals("2019-02-03T04:05:06.007Z", d.toIsoString())
	}

	@Test fun serializeIso() {
		val d = Date.UTC(2019, 2, 3, 4, 5, 6, 7)
		val json = jsonStringify(DateSerializer, d)
		assertEquals("\"2019-02-03T04:05:06.007Z\"", json)
		assertEquals(d, jsonParse(DateSerializer, json))

		val m = MyClassWithDate(d)
		val json2 = jsonStringify(MyClassWithDate.serializer(), m)
		assertEquals(m, jsonParse(MyClassWithDate.serializer(), json2))

		val json3 = jsonStringify(MyClassWithDate.serializer(), MyClassWithDate(null))
		assertEquals(MyClassWithDate(null), jsonParse(MyClassWithDate.serializer(), json3))
	}

	@Test fun plus() {
		val d = Date(2019, 2, 3, 4, 5, 6, 7)
		assertEquals(Date(2019, 2, 3, 4, 5, 6, 7 + 30), d + 30.milliseconds)
		assertEquals(Date(2019, 2, 3, 4, 5, 6 + 30, 7), d + 30.seconds)
		assertEquals(Date(2019, 2, 3, 4, 5 + 30, 6, 7), d + 30.minutes)
		assertEquals(Date(2019, 2, 3, 4, 5, 6, 7), d)
		assertEquals(Date(2019, 2, 5), Date(2019, 2, 3) + 2.days)
	}

}

@Serializable
private data class MyClassWithDate(
	@Serializable(with=DateSerializer::class)
	val d: Date?
)
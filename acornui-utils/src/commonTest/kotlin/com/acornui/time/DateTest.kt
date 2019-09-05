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
		val d = utcDate(2019, 2, 3, 4, 5, 6, 7)
		assertEquals("2019-02-03T04:05:06.007Z", d.toIsoString())
	}

	@Test fun serializeIso() {
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

	@Test fun plus() {
		val d = date(2019, 2, 3, 4, 5, 6, 7)
		assertEquals(date(2019, 2, 3, 4, 5, 6, 7 + 30), d + 30.milliseconds)
		assertEquals(date(2019, 2, 3, 4, 5, 6 + 30, 7), d + 30.seconds)
		assertEquals(date(2019, 2, 3, 4, 5 + 30, 6, 7), d + 30.minutes)
		assertEquals(date(2019, 2, 3, 4, 5, 6, 7), d)
		assertEquals(date(2019, 2, 5), date(2019, 2, 3) + 2.days)
	}

	@Test fun plusAssign() {
		val d = date(2019, 2, 3, 4, 5, 6, 7)
		d += 30.milliseconds
		assertEquals(date(2019, 2, 3, 4, 5, 6, 7 + 30), d)
		d += 31.seconds
		assertEquals(date(2019, 2, 3, 4, 5, 6 + 31, 7 + 30), d)
		d += 32.minutes
		assertEquals(date(2019, 2, 3, 4, 5 + 32, 6 + 31, 7 + 30), d)
	}
}

@Serializable
private data class MyClassWithDate(val d: DateRo?)
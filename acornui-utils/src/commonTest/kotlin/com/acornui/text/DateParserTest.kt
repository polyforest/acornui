package com.acornui.text

import com.acornui.i18n.Locale
import com.acornui.time.time
import kotlin.test.Test
import kotlin.test.assertEquals

class DateParserTest {

	@Test
	fun parseDate() {
		val parser = DateTimeParser()
		parser.type = DateTimeFormatType.DATE
		assertEquals(time.date(fullYear = 2048, month = 3, dayOfMonth = 11), parser.parse("2048/3/11"))
		assertEquals(time.date(fullYear = 2048, month = 3, dayOfMonth = 11), parser.parse("2048.3.11"))
		assertEquals(time.date(fullYear = 2048, month = 3, dayOfMonth = 11), parser.parse("2048-3-11"))
		assertEquals(time.date(fullYear = 1950, month = 1, dayOfMonth = 1), parser.parse("1950/1/1"))
		assertEquals(time.date(fullYear = 1950, month = 1, dayOfMonth = 1), parser.parse("1950-1-1"))
		assertEquals(time.date(fullYear = 1950, month = 1, dayOfMonth = 1), parser.parse("1950-01-01"))
		assertEquals(null, parser.parse("3:33 2048/3/11"))
		assertEquals(null, parser.parse("T 2048/3/11"))
		assertEquals(null, parser.parse("19501/1/1"))
		assertEquals(null, parser.parse("1950/1.1"))
		assertEquals(null, parser.parse("1950/1-1"))
		assertEquals(null, parser.parse("195/1/1"))
		assertEquals(null, parser.parse("19/1/11000"))
		assertEquals(null, parser.parse("19/1/1"))
		assertEquals(null, parser.parse("1/1/1"))
		assertEquals(null, parser.parse("1/1"))
		assertEquals(time.date(fullYear = 1978, month = 12, dayOfMonth = 31), parser.parse("1978/12/31"))
		assertEquals(time.date(fullYear = 1978, month = 12, dayOfMonth = 31), parser.parse("12/31/1978"))

		parser.locales = listOf(Locale("de-DE"))
		assertEquals(time.date(fullYear = 1978, month = 12, dayOfMonth = 31), parser.parse("31/12/1978"))
	}

	@Test
	fun parseDateOptionalYear() {
		val currentYear = 2018
		val parser = DateTimeParser()
		parser.currentYear = currentYear
		parser.yearIsOptional = true
		parser.type = DateTimeFormatType.DATE

		assertEquals(time.date(fullYear = 2048, month = 3, dayOfMonth = 11), parser.parse("2048/3/11"))
		assertEquals(time.date(fullYear = 2048, month = 3, dayOfMonth = 11), parser.parse("2048.3.11"))
		assertEquals(time.date(fullYear = 2048, month = 3, dayOfMonth = 11), parser.parse("2048-3-11"))
		assertEquals(time.date(fullYear = 1950, month = 1, dayOfMonth = 1), parser.parse("1950/1/1"))
		assertEquals(time.date(fullYear = 1950, month = 1, dayOfMonth = 1), parser.parse("1950-1-1"))
		assertEquals(time.date(fullYear = currentYear, month = 3, dayOfMonth = 11), parser.parse("3/11"))
		assertEquals(time.date(fullYear = currentYear, month = 3, dayOfMonth = 11), parser.parse("3.11"))
		assertEquals(time.date(fullYear = currentYear, month = 3, dayOfMonth = 11), parser.parse("3-11"))
		assertEquals(time.date(fullYear = currentYear, month = 1, dayOfMonth = 1), parser.parse("1/1"))
		assertEquals(time.date(fullYear = currentYear, month = 1, dayOfMonth = 1), parser.parse("1-1"))
		assertEquals(null, parser.parse("19501/1/1"))
		assertEquals(null, parser.parse("195/1/1"))
		assertEquals(null, parser.parse("19/1/11000"))
		assertEquals(null, parser.parse("19/1/1"))
		assertEquals(null, parser.parse("1/1/1"))
		assertEquals(time.date(fullYear = currentYear, month = 12, dayOfMonth = 31), parser.parse("12/31"))
		assertEquals(time.date(fullYear = currentYear, month = 12, dayOfMonth = 31), parser.parse("12/31"))
		assertEquals(null, parser.parse("13/6/2014"))
		assertEquals(null, parser.parse("-2/6/2014"))
		assertEquals(null, parser.parse("2/.6/2014"))
		assertEquals(null, parser.parse("2/-6/2014"))

		assertEquals(time.date(2014, 7, 13), parser.parse("July 13, 2014"))

		parser.locales = listOf(Locale("de-DE"))
		assertEquals(time.date(fullYear = currentYear, month = 12, dayOfMonth = 31), parser.parse("31/12"))
		assertEquals(time.date(fullYear = 2014, month = 12, dayOfMonth = 31), parser.parse("31/12/2014"))
		assertEquals(null, parser.parse("7/13/2014"))

		assertEquals(null, parser.parse("July 13, 2014"))
		assertEquals(time.date(2014, 7, 13), parser.parse("Juli 13, 2014"))
		assertEquals(time.date(currentYear, 7, 13), parser.parse("Juli 13"))
		parser.yearIsOptional = false
		assertEquals(null, parser.parse("Juli 13"))
	}

	@Test
	fun parseDateAllowTwoDigitYear() {
		val parser = DateTimeParser()
		parser.currentYear = 2000
		parser.allowTwoDigitYears = true
		parser.type = DateTimeFormatType.DATE

		assertEquals(time.date(fullYear = 2050, month = 3, dayOfMonth = 11), parser.parse("3/11/50"))
		assertEquals(time.date(fullYear = 2050, month = 3, dayOfMonth = 11), parser.parse("3/11/2050"))
		assertEquals(time.date(fullYear = 1951, month = 3, dayOfMonth = 11), parser.parse("3.11.51"))
		assertEquals(time.date(fullYear = 2023, month = 3, dayOfMonth = 11), parser.parse("3-11-23"))

		parser.currentYear = 1990
		assertEquals(time.date(fullYear = 1990, month = 3, dayOfMonth = 11), parser.parse("3/11/90"))
		assertEquals(time.date(fullYear = 1990, month = 3, dayOfMonth = 11), parser.parse("3/11/1990"))
		assertEquals(time.date(fullYear = 2040, month = 3, dayOfMonth = 11), parser.parse("3/11/40"))
		assertEquals(time.date(fullYear = 1941, month = 3, dayOfMonth = 11), parser.parse("3/11/41"))

		parser.currentYear = 2099
		assertEquals(time.date(fullYear = 2100, month = 3, dayOfMonth = 11), parser.parse("3/11/00"))
		assertEquals(time.date(fullYear = 2149, month = 3, dayOfMonth = 11), parser.parse("3/11/49"))
		assertEquals(time.date(fullYear = 2050, month = 3, dayOfMonth = 11), parser.parse("3/11/50"))
		assertEquals(time.date(fullYear = 2125, month = 3, dayOfMonth = 11), parser.parse("3/11/25"))
		assertEquals(time.date(fullYear = 2125, month = 3, dayOfMonth = 1), parser.parse("3/1/25"))
		parser.currentYear = 2018
		assertEquals(time.date(fullYear = 2014, month = 1, dayOfMonth = 1), parser.parse("1/1/14"))

		parser.currentYear = 2018
		parser.locales = listOf(Locale("de-DE"))
		assertEquals(null, parser.parse("31/12"))
		assertEquals(time.date(fullYear = 2014, month = 12, dayOfMonth = 31), parser.parse("31/12/2014"))
		assertEquals(time.date(fullYear = 2014, month = 12, dayOfMonth = 31), parser.parse("31/12/14"))
		assertEquals(null, parser.parse("7/13/2014"))
	}

	@Test
	fun parseDateOptionalYearAllowTwoDigit() {
		val parser = DateTimeParser()
		parser.yearIsOptional = true
		parser.currentYear = 2000
		parser.allowTwoDigitYears = true
		parser.type = DateTimeFormatType.DATE

		assertEquals(time.date(fullYear = 2050, month = 3, dayOfMonth = 11), parser.parse("3/11/50"))
		assertEquals(time.date(fullYear = 1951, month = 3, dayOfMonth = 11), parser.parse("3.11.51"))
		assertEquals(time.date(fullYear = 2023, month = 3, dayOfMonth = 11), parser.parse("3-11-23"))

		parser.currentYear = 1990
		assertEquals(time.date(fullYear = 1990, month = 3, dayOfMonth = 11), parser.parse("3/11/90"))
		assertEquals(time.date(fullYear = 2040, month = 3, dayOfMonth = 11), parser.parse("3/11/40"))
		assertEquals(time.date(fullYear = 1941, month = 3, dayOfMonth = 11), parser.parse("3/11/41"))

		parser.currentYear = 2099
		assertEquals(time.date(fullYear = 2100, month = 3, dayOfMonth = 11), parser.parse("3/11/00"))
		assertEquals(time.date(fullYear = 2149, month = 3, dayOfMonth = 11), parser.parse("3/11/49"))
		assertEquals(time.date(fullYear = 2050, month = 3, dayOfMonth = 11), parser.parse("3/11/50"))
		assertEquals(time.date(fullYear = 2125, month = 3, dayOfMonth = 11), parser.parse("3/11/25"))

		parser.currentYear = 1990
		assertEquals(time.date(fullYear = 1990, month = 3, dayOfMonth = 11), parser.parse("3/11"))

		parser.currentYear = 2099
		assertEquals(time.date(fullYear = 2099, month = 3, dayOfMonth = 11), parser.parse("3/11"))

		// One digit month
		parser.currentYear = 2018
		assertEquals(time.date(fullYear = 2014, month = 1, dayOfMonth = 1), parser.parse("1/1/14"))

	}

	@Test
	fun parseTime() {
		val parser = DateTimeParser()
		parser.type = DateTimeFormatType.TIME
		assertEquals(time.time(hour = 3, minute = 33), parser.parse("3:33"))
		assertEquals(time.time(hour = 13, minute = 33), parser.parse("13:33"))
		assertEquals(time.time(hour = 13, minute = 33), parser.parse("1:33 PM"))
		assertEquals(null, parser.parse("24:00"))
		assertEquals(null, parser.parse("13:00 PM"))
		assertEquals(time.time(hour = 23, minute = 33, second = 59), parser.parse("23:33:59"))
		assertEquals(time.time(hour = 23, minute = 0, second = 59, milli = 214), parser.parse("23:00:59.214"))
		assertEquals(time.utcTime(hour = 23, minute = 0, second = 59, milli = 214), parser.parse("23:00:59.214 GMT+0"))
		assertEquals(null, parser.parse("23:00:59.214 GMT+15")) // No GMT+15
		assertEquals(null, parser.parse("23:00:59.214 GMT-15")) // No GMT-15
		assertEquals(time.utcTime(hour = 23 - 8, minute = 0, second = 59, milli = 214), parser.parse("23:00:59.214 GMT-8"))
		assertEquals(time.utcTime(hour = 23 - 8, minute = 0, second = 59, milli = 214), parser.parse(" 23:00:59.214 GMT-8  \t"))
		assertEquals(null, parser.parse(" 23: 00:59.214 GMT-8  \t"))
		assertEquals(time.utcTime(hour = 23 - 8, minute = 0 - 30, second = 59, milli = 214), parser.parse("23:00:59.214 GMT-8:30"))
		assertEquals(time.utcTime(hour = 23 - 8, minute = 0 - 30, second = 59, milli = 214), parser.parse("23:00:59.214 GMT-830"))
		assertEquals(time.utcTime(hour = 23 + 8, minute = 0 + 30, second = 59, milli = 214), parser.parse("23:00:59.214 GMT+830"))
		assertEquals(time.utcTime(hour = 23 + 8, minute = 0 + 30, second = 59, milli = 214), parser.parse("23:00:59.214Z+830"))
	}

	@Test
	fun parseDateTime() {
		val parser = DateTimeParser()
		parser.type = DateTimeFormatType.DATE_TIME
		assertEquals(time.date(fullYear = 2048, month = 3, dayOfMonth = 11, hour = 3, minute = 33), parser.parse("3:33 2048/3/11"))
		assertEquals(time.date(fullYear = 2048, month = 3, dayOfMonth = 11, hour = 3, minute = 33), parser.parse("2048/3/11 3:33"))
		assertEquals(time.utcDate(fullYear = 2048, month = 3, dayOfMonth = 11, hour = 3, minute = 33), parser.parse("2048/3/11 3:33 GMT+0"))
		assertEquals(time.utcDate(fullYear = 1948, month = 3, dayOfMonth = 11, hour = 3, minute = 33), parser.parse("1948-03-11 3:33 GMT+0"))
		assertEquals(time.utcDate(fullYear = 2048, month = 3, dayOfMonth = 11, hour = 2, minute = 33), parser.parse("2048/3/11 3:33 GMT-1"))
		assertEquals(time.utcDate(fullYear = 2048, month = 3, dayOfMonth = 11, hour = 4, minute = 33), parser.parse("2048/3/11 3:33 GMT+1"))
		assertEquals(time.utcDate(fullYear = 2048, month = 3, dayOfMonth = 11, hour = 4, minute = 63), parser.parse("2048/3/11 3:33 GMT+01:30"))
		assertEquals(time.utcDate(fullYear = 2048, month = 3, dayOfMonth = 11, hour = 5 + 13, minute = 63), parser.parse("2048/3/11 5:33 GMT+13:30"))
		assertEquals(null, parser.parse("48/3/11 5:33 GMT+13:30"))
		parser.allowTwoDigitYears = true
		parser.currentYear = 2018
		assertEquals(null, parser.parse("48/3/11 5:33 GMT+13:30")) // YY/MM/DD is not supported for ambiguity reasons.
		assertEquals(time.utcDate(fullYear = 2048, month = 3, dayOfMonth = 11, hour = 5 + 13, minute = 30+33), parser.parse("3/11/48 5:33 GMT+13:30"))
		assertEquals(time.utcDate(fullYear = 2048, month = 3, dayOfMonth = 11, hour = 5, minute = 33), parser.parse("3/11/48 5:33 GMT"))
		assertEquals(time.utcDate(fullYear = 2048, month = 3, dayOfMonth = 11, hour = 5, minute = 33), parser.parse("3/11/48 5:33 Z"))
		assertEquals(time.utcDate(fullYear = 2048, month = 3, dayOfMonth = 11, hour = 5, minute = 33), parser.parse("3/11/48 5:33 UTC"))
		assertEquals(time.utcDate(fullYear = 2017, month = 3, dayOfMonth = 11, hour = 5 - 3, minute = 33 - 40), parser.parse("2017-3-11T5:33Z-340"))
		assertEquals(time.utcDate(fullYear = 2017, month = 3, dayOfMonth = 11, hour = 5 - 1, minute = 33 - 20), parser.parse("2017-3-11T5:33Z-120"))
		assertEquals(time.utcDate(fullYear = 2017, month = 3, dayOfMonth = 11, hour = 5 - 13, minute = 33 - 30), parser.parse("2017-03-11T05:33-1330"))
		assertEquals(null, parser.parse("2017-3-11T5:33-1330")) // Iso format requires leading zeros.
		assertEquals(null, parser.parse("17-3-11T5:33-1330")) // Two year dates not allowed in ISO format.

		parser.yearIsOptional = true
		parser.currentYear = 2018
		assertEquals(time.utcDate(fullYear = 2018, month = 3, dayOfMonth = 11, hour = 5 + 13, minute = 33 + 30), parser.parse("3/11 5:33 GMT+13:30"))
		assertEquals(null, parser.parse("3/11 5:33 GMT+16:30")) // No such gmt offset
		assertEquals(time.utcDate(fullYear = 2018, month = 3, dayOfMonth = 11, hour = 5 + 13, minute = 33 + 30), parser.parse("3/11T5:33Z+1330"))
		assertEquals(time.utcDate(fullYear = 2018, month = 3, dayOfMonth = 11, hour = 5 + 13, minute = 33 + 30), parser.parse("3/11T5:33Z+1330"))
		assertEquals(time.utcDate(fullYear = 2018, month = 3, dayOfMonth = 11, hour = 5 - 13, minute = 33 - 30), parser.parse("3/11T5:33Z-1330"))
		assertEquals(time.utcDate(fullYear = 2018, month = 3, dayOfMonth = 11, hour = 5 - 3, minute = 33 - 40), parser.parse("3/11T5:33Z-340"))
		assertEquals(time.utcDate(fullYear = 2018, month = 3, dayOfMonth = 11, hour = 5 - 1, minute = 33 - 20), parser.parse("3/11T5:33Z-120"))
		assertEquals(null, parser.parse("3-11T5:33-1330")) // Optional year not allowed in ISO format.
		assertEquals(time.utcDate(fullYear = 2018, month = 3, dayOfMonth = 11, hour = 5, minute = 33), parser.parse("3/11 5:33Z"))
		assertEquals(time.utcDate(fullYear = 2018, month = 10, dayOfMonth = 5, hour = 14, minute = 15, second = 50), parser.parse("2018-10-05T14:15:50+00:00"))

		for (i in 0..1000) {
			val t = time.date((27513254 * i).toLong())
			assertEquals(t, parser.parse(t.toIsoString()))
		}
	}
}
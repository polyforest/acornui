package com.acornui.formatters

import com.acornui.i18n.Locale
import com.acornui.i18n.isI18nSupported
import com.acornui.time.Date
import com.acornui.time.time
import com.acornui.time.utcDate
import com.acornui.time.utcTime
import kotlin.test.Test
import kotlin.test.assertEquals

class DateParserTest {

	private val currentYear = 2018


	@Test
	fun parseDate() {
		val parser = DateTimeParser(type = DateTimeFormatType.DATE, currentYear = currentYear)
		assertEquals(Date(year = 2048, month = 3, day = 11), parser.parse("2048/3/11"))
		assertEquals(Date(year = 2048, month = 3, day = 11), parser.parse("2048.3.11"))
		assertEquals(Date(year = 2048, month = 3, day = 11), parser.parse("2048-3-11"))
		assertEquals(Date(year = 1950, month = 0 + 1, day = 1), parser.parse("1950/1/1"))
		assertEquals(Date(year = 1950, month = 0 + 1, day = 1), parser.parse("1950-1-1"))
		assertEquals(Date(year = 1950, month = 0 + 1, day = 1), parser.parse("1950-01-01"))
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
		assertEquals(Date(year = 1978, month = 11 + 1, day = 31), parser.parse("1978/12/31"))
		assertEquals(Date(year = 1978, month = 11 + 1, day = 31), parser.parse("12/31/1978"))
	}

	@Test
	fun parseDateI18n() {
		if (!isI18nSupported) return
		val parser = DateTimeParser(type = DateTimeFormatType.DATE, currentYear = currentYear)
		parser.locales = listOf(Locale("de-DE"))
		assertEquals(Date(year = 1978, month = 11 + 1, day = 31), parser.parse("31/12/1978"))
	}

	@Test
	fun parseDateOptionalYear() {
		val parser = DateTimeParser(type = DateTimeFormatType.DATE, currentYear = currentYear, yearIsOptional = true)

		assertEquals(Date(year = 2048, month = 3, day = 11), parser.parse("2048/3/11"))
		assertEquals(Date(year = 2048, month = 3, day = 11), parser.parse("2048.3.11"))
		assertEquals(Date(year = 2048, month = 3, day = 11), parser.parse("2048-3-11"))
		assertEquals(Date(year = 1950, month = 0 + 1, day = 1), parser.parse("1950/1/1"))
		assertEquals(Date(year = 1950, month = 0 + 1, day = 1), parser.parse("1950-1-1"))
		assertEquals(Date(year = currentYear, month = 3, day = 11), parser.parse("3/11"))
		assertEquals(Date(year = currentYear, month = 3, day = 11), parser.parse("3.11"))
		assertEquals(Date(year = currentYear, month = 3, day = 11), parser.parse("3-11"))
		assertEquals(Date(year = currentYear, month = 0 + 1, day = 1), parser.parse("1/1"))
		assertEquals(Date(year = currentYear, month = 0 + 1, day = 1), parser.parse("1-1"))
		assertEquals(null, parser.parse("19501/1/1"))
		assertEquals(null, parser.parse("195/1/1"))
		assertEquals(null, parser.parse("19/1/11000"))
		assertEquals(null, parser.parse("19/1/1"))
		assertEquals(null, parser.parse("1/1/1"))
		assertEquals(Date(year = currentYear, month = 11 + 1, day = 31), parser.parse("12/31"))
		assertEquals(Date(year = currentYear, month = 11 + 1, day = 31), parser.parse("12/31"))
		assertEquals(null, parser.parse("13/6/2014"))
		assertEquals(null, parser.parse("-2/6/2014"))
		assertEquals(null, parser.parse("2/.6/2014"))
		assertEquals(null, parser.parse("2/-6/2014"))

		assertEquals(Date(2014, 7, 13), parser.parse("July 13, 2014"))
	}

	@Test
	fun parseDateOptionalYearI18n() {
		if (!isI18nSupported) return
		var parser = DateTimeParser(type = DateTimeFormatType.DATE, currentYear = currentYear, yearIsOptional = true, locales = listOf(Locale("de-DE")))
		assertEquals(Date(year = currentYear, month = 11 + 1, day = 31), parser.parse("31/12"))
		assertEquals(Date(year = 2014, month = 11 + 1, day = 31), parser.parse("31/12/2014"))
		assertEquals(null, parser.parse("7/13/2014"))

		assertEquals(null, parser.parse("July 13, 2014"))
		assertEquals(Date(2014, 7, 13), parser.parse("Juli 13, 2014"))
		assertEquals(Date(currentYear, 7, 13), parser.parse("Juli 13"))
		parser = parser.copy(yearIsOptional = false)
		assertEquals(null, parser.parse("Juli 13"))
	}

	@Test
	fun parseDateAllowTwoDigitYear() {
		var parser = DateTimeParser(currentYear = 2000, allowTwoDigitYears = true, type = DateTimeFormatType.DATE)

		assertEquals(Date(year = 2050, month = 3, day = 11), parser.parse("3/11/50"))
		assertEquals(Date(year = 2050, month = 3, day = 11), parser.parse("3/11/2050"))
		assertEquals(Date(year = 1951, month = 3, day = 11), parser.parse("3.11.51"))
		assertEquals(Date(year = 2023, month = 3, day = 11), parser.parse("3-11-23"))

		parser = parser.copy(currentYear = 1990)
		assertEquals(Date(year = 1990, month = 3, day = 11), parser.parse("3/11/90"))
		assertEquals(Date(year = 1990, month = 3, day = 11), parser.parse("3/11/1990"))
		assertEquals(Date(year = 2040, month = 3, day = 11), parser.parse("3/11/40"))
		assertEquals(Date(year = 1941, month = 3, day = 11), parser.parse("3/11/41"))

		parser = parser.copy(currentYear = 2099)
		assertEquals(Date(year = 2100, month = 3, day = 11), parser.parse("3/11/00"))
		assertEquals(Date(year = 2149, month = 3, day = 11), parser.parse("3/11/49"))
		assertEquals(Date(year = 2050, month = 3, day = 11), parser.parse("3/11/50"))
		assertEquals(Date(year = 2125, month = 3, day = 11), parser.parse("3/11/25"))
		assertEquals(Date(year = 2125, month = 3, day = 1), parser.parse("3/1/25"))

		parser = parser.copy(currentYear = 2018)
		assertEquals(Date(year = 2014, month = 0 + 1, day = 1), parser.parse("1/1/14"))
	}

	@Test
	fun parseDateAllowTwoDigitYearI18n() {
		if (!isI18nSupported) return
		val parser = DateTimeParser(currentYear = 2000, allowTwoDigitYears = true, type = DateTimeFormatType.DATE, locales = listOf(Locale("de-DE")))
		assertEquals(null, parser.parse("31/12"))
		assertEquals(Date(year = 2014, month = 11 + 1, day = 31), parser.parse("31/12/2014"))
		assertEquals(Date(year = 2014, month = 11 + 1, day = 31), parser.parse("31/12/14"))
		assertEquals(null, parser.parse("7/13/2014"))
	}

	@Test
	fun parseDateOptionalYearAllowTwoDigit() {
		var parser = DateTimeParser(
			yearIsOptional = true,
			currentYear = 2000,
			allowTwoDigitYears = true,
			type = DateTimeFormatType.DATE
		)

		assertEquals(Date(year = 2050, month = 3, day = 11), parser.parse("3/11/50"))
		assertEquals(Date(year = 1951, month = 3, day = 11), parser.parse("3.11.51"))
		assertEquals(Date(year = 2023, month = 3, day = 11), parser.parse("3-11-23"))

		parser = parser.copy(currentYear = 1990)
		assertEquals(Date(year = 1990, month = 3, day = 11), parser.parse("3/11/90"))
		assertEquals(Date(year = 2040, month = 3, day = 11), parser.parse("3/11/40"))
		assertEquals(Date(year = 1941, month = 3, day = 11), parser.parse("3/11/41"))

		parser = parser.copy(currentYear = 2099)
		assertEquals(Date(year = 2100, month = 3, day = 11), parser.parse("3/11/00"))
		assertEquals(Date(year = 2149, month = 3, day = 11), parser.parse("3/11/49"))
		assertEquals(Date(year = 2050, month = 3, day = 11), parser.parse("3/11/50"))
		assertEquals(Date(year = 2125, month = 3, day = 11), parser.parse("3/11/25"))

		parser = parser.copy(currentYear = 1990)
		assertEquals(Date(year = 1990, month = 3, day = 11), parser.parse("3/11"))

		parser = parser.copy(currentYear = 2099)
		assertEquals(Date(year = 2099, month = 3, day = 11), parser.parse("3/11"))

		// One digit month
		parser = parser.copy(currentYear = 2018)
		assertEquals(Date(year = 2014, month = 0 + 1, day = 1), parser.parse("1/1/14"))
	}

	@Test
	fun parseTime() {
		val parser = DateTimeParser(DateTimeFormatType.TIME)
		assertEquals(time(hour = 3, minute = 33), parser.parse("3:33"))
		assertEquals(3, parser.parse("3:33")?.hours)
		assertEquals(33, parser.parse("3:33")?.minutes)
		assertEquals(time(hour = 13, minute = 33), parser.parse("13:33"))
		assertEquals(time(hour = 13, minute = 33), parser.parse("1:33 PM"))
		//assertEquals("1:33 PM", parser.parse("1:33 PM")?.toLocaleTimeString(options = dateLocaleOptions { second = }))
		assertEquals(null, parser.parse("24:00"))
		assertEquals(null, parser.parse("13:00 PM"))
		assertEquals(time(hour = 23, minute = 33, second = 59), parser.parse("23:33:59"))
		assertEquals(time(hour = 23, minute = 0, second = 59, millisecond = 214), parser.parse("23:00:59.214"))
		assertEquals(utcTime(hour = 23, minute = 0, second = 59, millisecond = 214), parser.parse("23:00:59.214 GMT+0"))
		assertEquals(null, parser.parse("23:00:59.214 GMT+15")) // No GMT+15
		assertEquals(null, parser.parse("23:00:59.214 GMT-15")) // No GMT-15
		assertEquals(
			utcTime(hour = 23 - 8, minute = 0, second = 59, millisecond = 214),
			parser.parse("23:00:59.214 GMT-8")
		)
		assertEquals(
			utcTime(hour = 23 - 8, minute = 0, second = 59, millisecond = 214),
			parser.parse(" 23:00:59.214 GMT-8  \t")
		)
		assertEquals(null, parser.parse(" 23: 00:59.214 GMT-8  \t"))
		assertEquals(
			utcTime(hour = 23 - 8, minute = 0 - 30, second = 59, millisecond = 214),
			parser.parse("23:00:59.214 GMT-8:30")
		)
		assertEquals(
			utcTime(hour = 23 - 8, minute = 0 - 30, second = 59, millisecond = 214),
			parser.parse("23:00:59.214 GMT-830")
		)
		assertEquals(
			utcTime(hour = 23 + 8, minute = 0 + 30, second = 59, millisecond = 214),
			parser.parse("23:00:59.214 GMT+830")
		)
		assertEquals(
			utcTime(hour = 23 + 8, minute = 0 + 30, second = 59, millisecond = 214),
			parser.parse("23:00:59.214Z+830")
		)
	}

	@Test
	fun parseDateTime() {
		var parser = DateTimeParser(DateTimeFormatType.DATE_TIME)
		assertEquals(Date(year = 2048, month = 3, day = 11, hour = 3, minute = 33), parser.parse("3:33 2048/3/11"))
		assertEquals(Date(year = 2048, month = 3, day = 11, hour = 3, minute = 33), parser.parse("2048/3/11 3:33"))
		assertEquals(
			utcDate(year = 2048, month = 3, day = 11, hour = 3, minute = 33),
			parser.parse("2048/3/11 3:33 GMT+0")
		)
		assertEquals(
			utcDate(year = 1948, month = 3, day = 11, hour = 3, minute = 33),
			parser.parse("1948-03-11 3:33 GMT+0")
		)
		assertEquals(
			utcDate(year = 2048, month = 3, day = 11, hour = 2, minute = 33),
			parser.parse("2048/3/11 3:33 GMT-1")
		)
		assertEquals(
			utcDate(year = 2048, month = 3, day = 11, hour = 4, minute = 33),
			parser.parse("2048/3/11 3:33 GMT+1")
		)
		assertEquals(
			utcDate(year = 2048, month = 3, day = 11, hour = 4, minute = 63),
			parser.parse("2048/3/11 3:33 GMT+01:30")
		)
		assertEquals(
			utcDate(year = 2048, month = 3, day = 11, hour = 5 + 13, minute = 63),
			parser.parse("2048/3/11 5:33 GMT+13:30")
		)
		assertEquals(null, parser.parse("48/3/11 5:33 GMT+13:30"))
		parser = parser.copy(allowTwoDigitYears = true, currentYear = 2018)
		assertEquals(null, parser.parse("48/3/11 5:33 GMT+13:30")) // YY/MM/DD is not supported for ambiguity reasons.
		assertEquals(
			utcDate(year = 2048, month = 3, day = 11, hour = 5 + 13, minute = 30 + 33),
			parser.parse("3/11/48 5:33 GMT+13:30")
		)
		assertEquals(utcDate(year = 2048, month = 3, day = 11, hour = 5, minute = 33), parser.parse("3/11/48 5:33 GMT"))
		assertEquals(utcDate(year = 2048, month = 3, day = 11, hour = 5, minute = 33), parser.parse("3/11/48 5:33 Z"))
		assertEquals(utcDate(year = 2048, month = 3, day = 11, hour = 5, minute = 33), parser.parse("3/11/48 5:33 UTC"))
		assertEquals(
			utcDate(year = 2017, month = 3, day = 11, hour = 5 - 3, minute = 33 - 40),
			parser.parse("2017-3-11T5:33Z-340")
		)
		assertEquals(
			utcDate(year = 2017, month = 3, day = 11, hour = 5 - 1, minute = 33 - 20),
			parser.parse("2017-3-11T5:33Z-120")
		)
		assertEquals(
			utcDate(year = 2017, month = 3, day = 11, hour = 5 - 13, minute = 33 - 30),
			parser.parse("2017-03-11T05:33-1330")
		)
		assertEquals(null, parser.parse("2017-3-11T5:33-1330")) // Iso format requires leading zeros.
		assertEquals(null, parser.parse("17-3-11T5:33-1330")) // Two year Dates not allowed in ISO format.

		parser = parser.copy(yearIsOptional = true, currentYear = 2018)
		assertEquals(
			utcDate(year = 2018, month = 3, day = 11, hour = 5 + 13, minute = 33 + 30),
			parser.parse("3/11 5:33 GMT+13:30")
		)
		assertEquals(null, parser.parse("3/11 5:33 GMT+16:30")) // No such gmt offset
		assertEquals(
			utcDate(year = 2018, month = 3, day = 11, hour = 5 + 13, minute = 33 + 30),
			parser.parse("3/11T5:33Z+1330")
		)
		assertEquals(
			utcDate(year = 2018, month = 3, day = 11, hour = 5 + 13, minute = 33 + 30),
			parser.parse("3/11T5:33Z+1330")
		)
		assertEquals(
			utcDate(year = 2018, month = 3, day = 11, hour = 5 - 13, minute = 33 - 30),
			parser.parse("3/11T5:33Z-1330")
		)
		assertEquals(
			utcDate(year = 2018, month = 3, day = 11, hour = 5 - 3, minute = 33 - 40),
			parser.parse("3/11T5:33Z-340")
		)
		assertEquals(
			utcDate(year = 2018, month = 3, day = 11, hour = 5 - 1, minute = 33 - 20),
			parser.parse("3/11T5:33Z-120")
		)
		assertEquals(null, parser.parse("3-11T5:33-1330")) // Optional year not allowed in ISO format.
		assertEquals(utcDate(year = 2018, month = 3, day = 11, hour = 5, minute = 33), parser.parse("3/11 5:33Z"))
		assertEquals(
			utcDate(year = 2018, month = 9 + 1, day = 5, hour = 14, minute = 15, second = 50),
			parser.parse("2018-10-05T14:15:50+00:00")
		)

		for (i in 0..1000) {
			val t = Date((27513254 * i).toLong())
			assertEquals(t, parser.parse(t.toIsoString()))
		}

		assertEquals(
			utcDate(year = 2019, month = 1 + 1, day = 3, hour = 4, minute = 5, second = 6, millisecond = 7),
			parser.parse("2019-02-03T04:05:06.007Z")
		)
	}
}


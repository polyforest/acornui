package com.acornui.jvm.time

import com.acornui.core.UserInfo
import com.acornui.core.i18n.Locale
import com.acornui.core.text.DateTimeFormatType
import com.acornui.core.text.DateTimeParser
import com.acornui.core.text.dateTimeFormatterProvider
import com.acornui.core.text.numberFormatterProvider
import com.acornui.core.time.time
import com.acornui.core.userInfo
import com.acornui.jvm.text.DateTimeFormatterImpl
import com.acornui.jvm.text.NumberFormatterImpl
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class DateParserTest {

	@Before fun setUp() {
		val u = UserInfo(
				isDesktop = true,
				isTouchDevice = false,
				userAgent = "headless",
				platformStr = System.getProperty("os.name") ?: UserInfo.UNKNOWN_PLATFORM,
				systemLocale = listOf(Locale("en-US"))
		)
		userInfo = u
		time = TimeProviderImpl()
		numberFormatterProvider = { NumberFormatterImpl() }
		dateTimeFormatterProvider = { DateTimeFormatterImpl() }
	}

	@Test
	fun parseDate() {
		val parser = DateTimeParser()
		parser.type = DateTimeFormatType.DATE
		assertEquals(time.date(fullYear = 2048, month = 3, dayOfMonth = 11), parser.parse("2048/3/11"))
		assertEquals(time.date(fullYear = 2048, month = 3, dayOfMonth = 11), parser.parse("2048.3.11"))
		assertEquals(time.date(fullYear = 2048, month = 3, dayOfMonth = 11), parser.parse("2048-3-11"))
		assertEquals(time.date(fullYear = 1950, month = 1, dayOfMonth = 1), parser.parse("1950/1/1"))
		assertEquals(time.date(fullYear = 1950, month = 1, dayOfMonth = 1), parser.parse("1950-1-1"))
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
		val currentYear = time.now().fullYear
		val parser = DateTimeParser()
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

		parser.locales = listOf(Locale("de-DE"))
		assertEquals(time.date(fullYear = currentYear, month = 12, dayOfMonth = 31), parser.parse("31/12"))
		assertEquals(time.date(fullYear = 2014, month = 12, dayOfMonth = 31), parser.parse("31/12/2014"))
		assertEquals(null, parser.parse("7/13/2014"))
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

	}

	@Test
	fun parseTime() {
		val parser = DateTimeParser()
		parser.type = DateTimeFormatType.TIME
		val t = time.date(0)
		assertEquals(t.setTimeOfDay(hour = 3, minute = 33), parser.parse("3:33"))
		assertEquals(t.setTimeOfDay(hour = 13, minute = 33), parser.parse("13:33"))
		assertEquals(t.setTimeOfDay(hour = 13, minute = 33), parser.parse("1:33 PM"))
		assertEquals(null, parser.parse("24:00"))
		assertEquals(null, parser.parse("13:00 PM"))
		assertEquals(t.setTimeOfDay(hour = 23, minute = 33, second = 59), parser.parse("23:33:59"))
		assertEquals(t.setTimeOfDay(hour = 23, minute = 0, second = 59, milli = 214), parser.parse("23:00:59.214"))
		assertEquals(t.setUtcTimeOfDay(hour = 23, minute = 0, second = 59, milli = 214), parser.parse("23:00:59.214 GMT+0"))
		assertEquals(null, parser.parse("23:00:59.214 GMT+15")) // No GMT+15
		assertEquals(null, parser.parse("23:00:59.214 GMT-15")) // No GMT-15
		assertEquals(t.setUtcTimeOfDay(hour = 23-8, minute = 0, second = 59, milli = 214), parser.parse("23:00:59.214 GMT-8"))
	}

	@Test
	fun parseDateTime() {
		val parser = DateTimeParser()
		parser.type = DateTimeFormatType.DATE_TIME
		assertEquals(time.date(fullYear = 2048, month = 3, dayOfMonth = 11, hour = 3, minute = 33), parser.parse("3:33 2048/3/11"))
		assertEquals(time.date(fullYear = 2048, month = 3, dayOfMonth = 11, hour = 3, minute = 33), parser.parse("2048/3/11 3:33"))
		assertEquals(time.utcDate(fullYear = 2048, month = 3, dayOfMonth = 11, hour = 3, minute = 33), parser.parse("2048/3/11 3:33 GMT+0"))
		assertEquals(time.utcDate(fullYear = 2048, month = 3, dayOfMonth = 11, hour = 2, minute = 33), parser.parse("2048/3/11 3:33 GMT-1"))
		assertEquals(time.utcDate(fullYear = 2048, month = 3, dayOfMonth = 11, hour = 4, minute = 33), parser.parse("2048/3/11 3:33 GMT+1"))
		assertEquals(time.utcDate(fullYear = 2048, month = 3, dayOfMonth = 11, hour = 4, minute = 63), parser.parse("2048/3/11 3:33 GMT+01:30"))
		assertEquals(time.utcDate(fullYear = 2048, month = 3, dayOfMonth = 11, hour = 5+13, minute = 63), parser.parse("2048/3/11 5:33 GMT+13:30"))
		assertEquals(null, parser.parse("48/3/11 5:33 GMT+13:30"))
		parser.allowTwoDigitYears = true
		parser.currentYear = 2018
		assertEquals(null, parser.parse("48/3/11 5:33 GMT+13:30")) // YY/MM/DD is not supported for ambiguity reasons.
		assertEquals(time.utcDate(fullYear = 2048, month = 3, dayOfMonth = 11, hour = 5+13, minute = 63), parser.parse("3/11/48 5:33 GMT+13:30"))
		parser.yearIsOptional = true
		parser.currentYear = 2018
		assertEquals(time.utcDate(fullYear = 2018, month = 3, dayOfMonth = 11, hour = 5+13, minute = 63), parser.parse("3/11 5:33 GMT+13:30"))

	}
}
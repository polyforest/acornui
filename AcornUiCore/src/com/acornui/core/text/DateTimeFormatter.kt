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

package com.acornui.core.text

import com.acornui.collection.indexOfFirst2
import com.acornui.core.i18n.Locale
import com.acornui.core.time.Date
import com.acornui.core.time.DateRo
import com.acornui.core.time.time

/**
 * This class formats dates into localized string representations.
 */
interface DateTimeFormatter : StringFormatter<DateRo> {

	/**
	 * Whether this should format the [Date] object as time, date, or date and time.
	 */
	var type: DateTimeFormatType

	/**
	 * The exact format of a date or time is dependent on the locale, and is platform specific, but this property will
	 * hint whether the format will be long form, medium form, or short form.
	 * The default is [DateTimeFormatStyle.DEFAULT]
	 */
	var timeStyle: DateTimeFormatStyle

	/**
	 * The exact format of a date or time is dependent on the locale, and is platform specific, but this property will
	 * hint whether the format will be long form, medium form, or short form.
	 * The default is [DateTimeFormatStyle.DEFAULT]
	 */
	var dateStyle: DateTimeFormatStyle

	/**
	 * The time zone for formatting.
	 * The only values this is guaranteed to work with are "UTC" or null.
	 * Other values that will likely work based on browser or jvm implementation are the full TZ code
	 * [https://en.wikipedia.org/wiki/List_of_tz_database_time_zones]
	 * For example, use "America/New_York" as opposed to "EST"
	 * If this is null, the user's timezone will be used.
	 */
	var timeZone: String?

	/**
	 * The ordered locale chain to use for formatting. If this is left null, the user's current locale will be used.
	 *
	 * See [https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl#Locale_identification_and_negotiation]
	 */
	var locales: List<Locale>?

}

lateinit var dateTimeFormatterProvider: ()->DateTimeFormatter

class DateTimeParser : StringParser<Date> {

	/**
	 * Whether this should format the [Date] object as time, date, or date and time.
	 */
	var type = DateTimeFormatType.DATE_TIME

	/**
	 * The time zone of the date string.
	 * The only values this is guaranteed to work with are "UTC" or null.
	 * Other values that will likely work based on browser or jvm implementation are the full TZ code
	 * [https://en.wikipedia.org/wiki/List_of_tz_database_time_zones]
	 * For example, use "America/New_York" as opposed to "EST"
	 * If this is null, the user's timezone will be used.
	 */
	var timeZone: String? = null

	/**
	 * The ordered locale chain to use for formatting. If this is left null, the user's current locale will be used.
	 *
	 * See [https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl#Locale_identification_and_negotiation]
	 */
	var locales: List<Locale>? = null

//	/**
//	 * If true, two digit years will be accepted. This is more flexible, but less safe.
//	 * A two digit year is assumed to be within 50 years of the current year. That is, if the current year is 2001,
//	 * 50 will be 2050 and
//	 */
//	var allowTwoDigitYears = false

	private val timeRegex = Regex("""([0-1][0-9]|[2][0-3]):([0-5][0-9])(:[0-5][0-9](.[0-9][0-9][0-9])?)? ?(am|pm)?""", RegexOption.IGNORE_CASE)

	private fun parseTime(value: String): Date? {
		val str = value.trim()
		val result = timeRegex.matchEntire(str) ?: return null
		val isPm = result.groupValues.getOrNull(5)?.toLowerCase() == "pm"
		val hour = result.groupValues[1].toInt() + if (isPm) 12 else 0
		val minute = result.groupValues[2].toInt()
		val second = result.groupValues.getOrNull(3)?.substring(1)?.toInt() ?: 0
		val milli = result.groupValues.getOrNull(4)?.substring(1)?.toInt() ?: 0
		val t = time.now()
		if (timeZone == null) {
			t.hour = hour
			t.minute = minute
			t.second = second
			t.milli = milli
		} else if (timeZone!!.toLowerCase() == "utc") {
			t.utcHour = hour
			t.utcMinute = minute
			t.utcSecond = second
			t.utcMilli = milli
		} else {
			// Attempt to figure out the provided timezone's GMT offset.
			TODO()
		}
		return t
	}

	private val yMDRegex = Regex("""([0-9]{4})([/.-])(1[0-2]|0?[1-9])\2([1-2][0-9]|3[0-1]|0?[1-9])""")
	private val mDYRegex = Regex("""(1[0-2]|0?[1-9])([/.-])([1-2][0-9]|3[0-1]|0?[1-9])\2([0-9]{4})""")
	private val dMYRegex = Regex("""([1-2][0-9]|3[0-1]|0?[1-9])([/.-])(1[0-2]|0?[1-9])\2([0-9]{4})""")

	private fun parseDate(value: String): Date? {
		val str = value.trim()
		val result = yMDRegex.matchEntire(str)
		if (result != null) {
			val fullYear = result.groupValues[1].toInt()
			val month = result.groupValues[3].toInt()
			val day = result.groupValues[4].toInt()
			return if (timeZone == null) {
				time.date(fullYear, month - 1, day)
			} else if (timeZone!!.toLowerCase() == "utc") {
				time.utcDate(fullYear, month - 1, day)
			} else {
				TODO()
			}
		}
		return null
	}

	override fun parse(value: String): Date? {
		if (type == DateTimeFormatType.DATE) {
			return parseDate(value)
		} else {
			TODO()
		}

//		val date = time.date(1110, 11, 12, 13, 14, 15, 16)
//		val regex = Regex("""[,. :\\-]+""")

//		val valueParts = value.split(regex)
//		if (localizedParts.size != valueParts.size) return null
//		val newDate = time.date(0)
//		for (i in 0..valueParts.lastIndex) {
//			val p = valueParts[i]
//			when (localizedParts[i].toLowerCase()) {
//				"1110" -> {
//					if (valueParts[i].length != 4) return null
//					newDate.fullYear = p.toInt()
//				}
//				"10" -> {
//					if (valueParts[i].length != 2) return null
//					val anchor = (currentYear / 100) * 100
//					val ref = currentYear - anchor
//					val y = p.toInt()
//					newDate.fullYear = if (y > ref + 50) anchor + y - 100
//					else anchor + y
//				}
//				"11" -> newDate.monthIndex = p.toInt()
//				"12" -> newDate.dayOfMonth = p.toInt()
//				"13" -> newDate.hour = p.toInt()
//				"14" -> newDate.minute = p.toInt()
//				"15" -> newDate.second = p.toInt()
//				"16" -> newDate.milli = p.toInt()
//				"pm" -> {
//					if (p == "pm")
//						newDate.hour += 12
//				}
//			}
//		}
//		return newDate
	}

	companion object {
		private val currentYear by lazy {
			time.now().fullYear
		}
	}
}

enum class DateTimeFormatStyle {
	FULL,
	LONG,
	MEDIUM,
	SHORT,
	DEFAULT
}

enum class DateTimeFormatType {
	DATE,
	MONTH,
	WEEKDAY,
	TIME,
	DATE_TIME
}

fun dateFormatter(init: DateTimeFormatter.() -> Unit = {}): DateTimeFormatter {
	return dateTimeFormatterProvider().apply {
		type = DateTimeFormatType.DATE
		init()
	}
}

fun dateTimeFormatter(init: DateTimeFormatter.() -> Unit = {}): DateTimeFormatter {
	return dateTimeFormatterProvider().apply {
		type = DateTimeFormatType.DATE_TIME
		init()
	}
}

fun timeFormatter(init: DateTimeFormatter.() -> Unit = {}): DateTimeFormatter {
	return dateTimeFormatterProvider().apply {
		type = DateTimeFormatType.TIME
		init()
	}
}

/**
 * Returns a date time parser configured to parse a date in local time.
 */
fun dateParser(): DateTimeParser = DateTimeParser().apply { type = DateTimeFormatType.DATE }

/**
 * Returns a date time parser configured to parse a time in local time.
 */
fun timeParser(): DateTimeParser = DateTimeParser().apply { type = DateTimeFormatType.TIME }

/**
 * Returns a date time parser configured to parse a date and time in local time.
 */
fun dateTimeParser(): DateTimeParser = DateTimeParser().apply { type = DateTimeFormatType.DATE_TIME }

/**
 * Parses a string into a Month index, according to the given locale.
 * @param str The month string to parse.
 * @param locales [DateTimeParser.locales]
 */
fun parseMonthIndex(str: String, locales: List<Locale>? = null): Int? {
	val s = str.trim().toLowerCase()
	val shortIndex = getMonths(false, locales).indexOfFirst2 { it.toLowerCase() == s }
	if (shortIndex != -1) return shortIndex
	val longIndex = getMonths(true, locales).indexOfFirst2 { it.toLowerCase() == s }
	return if (longIndex != -1) longIndex else null
}

/**
 * Parses a string into a day of week, 0 - Sunday, 6 - Saturday, according to the given locale.
 * @param str The day of the week string to parse.
 * @param locales [DateTimeParser.locales]
 */
fun parseWeekday(str: String, locales: List<Locale>? = null): Int? {
	val s = str.trim().toLowerCase()
	val shortIndex = getDaysOfWeek(false, locales).indexOfFirst2 { it.toLowerCase() == s }
	if (shortIndex != -1) return shortIndex
	val longIndex = getDaysOfWeek(true, locales).indexOfFirst2 { it.toLowerCase() == s }
	return if (longIndex != -1) longIndex else null
}

/**
 * Parses a string into a date, with the time being 0:00:00.0000 according to the given [timeZone].
 * @param str The date string to parse. This may be any [DateTimeFormatStyle].
 * @param timeZone [DateTimeParser.timeZone]
 * @param locales [DateTimeParser.locales]
 */
fun parseDate(str: String, timeZone: String? = null, locales: List<Locale>? = null): Date? {
	val parser = DateTimeParser()
	parser.type = DateTimeFormatType.DATE
	parser.timeZone = timeZone
	parser.locales = locales
	return parser.parse(str)
}

/**
 * Parses a string into a date-time.
 * @param str The date string to parse.
 * @param timeZone [DateTimeParser.timeZone]
 * @param locales [DateTimeParser.locales]
 */
fun parseDateTime(str: String, timeZone: String? = null, locales: List<Locale>? = null): Date? {
	val parser = DateTimeParser()
	parser.type = DateTimeFormatType.DATE_TIME
	parser.timeZone = timeZone
	parser.locales = locales
	return parser.parse(str)
}

/**
 * Parses a string into a time, with the day being the unix epoch.
 * @param str The date string to parse.
 * @param timeZone [DateTimeParser.timeZone]
 * @param locales [DateTimeParser.locales]
 */
fun parseTime(str: String, timeZone: String? = null, locales: List<Locale>? = null): Date? {
	val parser = DateTimeParser()
	parser.type = DateTimeFormatType.TIME
	parser.timeZone = timeZone
	parser.locales = locales
	return parser.parse(str)
}

/**
 * Parses a string into a date-time, a time, or a date, whichever format works.
 * @param str The date string to parse.
 * @param timeZone [DateTimeParser.timeZone]
 * @param locales [DateTimeParser.locales]
 */
fun parseDateOrTime(str: String, timeZone: String? = null, locales: List<Locale>? = null): Date? {
	return parseDateTime(str, timeZone, locales) ?: parseTime(str, timeZone, locales) ?: parseDate(str, timeZone, locales)
}

private val daysOfWeekCache = HashMap<Pair<Boolean, List<Locale>?>, List<String>>()

/**
 * Returns a list of the localized days of the week.
 * @param locales The locales to use for lookup. Use null for the user's locale.
 */
fun getDaysOfWeek(longFormat: Boolean, locales: List<Locale>? = null): List<String> {
	val cacheKey = longFormat to locales
	if (daysOfWeekCache.containsKey(cacheKey)) return daysOfWeekCache[cacheKey]!!
	val d = time.date(0)
	val list = ArrayList<String>(7)
	val formatter = dateFormatter {
		this.dateStyle = if (longFormat) DateTimeFormatStyle.FULL else DateTimeFormatStyle.SHORT
		this.locales = locales
		type = DateTimeFormatType.WEEKDAY
	}
	val offset = d.dayOfMonth - d.dayOfWeek
	for (i in 0..11) {
		d.dayOfMonth = i + offset
		list.add(formatter.format(d))
	}
	daysOfWeekCache[cacheKey] = list
	return list
}


private val monthsOfYearCache = HashMap<Pair<Boolean, List<Locale>?>, List<String>>()

/**
 * Returns a list of the localized months of the year.
 * @param locales
 */
fun getMonths(longFormat: Boolean, locales: List<Locale>? = null): List<String> {
	val cacheKey = longFormat to locales
	if (monthsOfYearCache.containsKey(cacheKey)) return monthsOfYearCache[cacheKey]!!
	val d = time.date(0)
	val list = ArrayList<String>(12)
	val formatter = dateFormatter {
		this.dateStyle = if (longFormat) DateTimeFormatStyle.FULL else DateTimeFormatStyle.SHORT
		this.locales = locales
		type = DateTimeFormatType.MONTH
	}
	for (i in 0..11) {
		d.monthIndex = i
		list.add(formatter.format(d))
	}
	monthsOfYearCache[cacheKey] = list
	return list
}
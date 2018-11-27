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

lateinit var dateTimeFormatterProvider: () -> DateTimeFormatter

class DateTimeParser : StringParser<Date> {

	/**
	 * Whether this should format the [Date] object as time, date, or date and time.
	 */
	var type = DateTimeFormatType.DATE_TIME

	/**
	 * If true, the date will be parsed as a UTC time if a GMT offset was not supplied in the string.
	 */
	var isUtc = false

	/**
	 * The ordered locale chain to use for formatting. If this is left null, the user's current locale will be used.
	 *
	 * See [https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl#Locale_identification_and_negotiation]
	 */
	var locales: List<Locale>? = null

	/**
	 * If true, two digit years will be accepted. This is more flexible, but less safe.
	 * A two digit year is assumed to be within 50 years after, and 49 years before [currentYear].
	 */
	var allowTwoDigitYears: Boolean = false

	/**
	 * If true, a date without a year will be considered to be the user's current year.
	 */
	var yearIsOptional: Boolean = false

	/**
	 * If the parsed date is a two digit year and [allowTwoDigitYears] is true, then this year is used as the anchor.
	 */
	var currentYear = time.now().fullYear

	/**
	 * Parses the timezone offset
	 */
	private fun parseTimezoneOffset(value: String): Pair<IntRange, Int>? {
		val result = gmtOffsetRegex.find(value) ?: return null
		val sign = if (result.groupValues[1] == "-") -1 else 1
		val hour = result.groupValues[2].toIntOrNull() ?: 0
		val minute = result.groupValues[3].toIntOrNull() ?: 0
		return result.range to sign * (hour * 60 + minute)
	}

	private fun parseTime(value: String, timezoneOffset: Int?): Pair<IntRange, Date>? {
		val time12Result = time12Regex.find(value)
		val isPm = if (time12Result == null) false else time12Result.groupValues[5].notBlank()?.toLowerCase() == "pm"
		val result = time12Result ?: time24Regex.find(value) ?: return null

		val hour = result.groupValues[1].toInt() + if (isPm) 12 else 0
		val minute = result.groupValues[2].toInt()
		val second = result.groupValues[3].toIntOrNull() ?: 0
		val milli = result.groupValues[4].toTimeMilli()

		val t = time.date(0)
		if (timezoneOffset != null || isUtc) {
			t.setUtcTimeOfDay(hour, minute + (timezoneOffset ?: 0), second, milli)
		} else {
			t.setTimeOfDay(hour, minute, second, milli)
		}
		return result.range to t
	}

	private fun parseDate(value: String, timezoneOffset: Int?): Pair<IntRange, Date>? {
		val result = yMDRegex.find(value)
		val year: Int
		val month: Int
		val day: Int
		val range: IntRange
		if (result != null) {
			year = result.groupValues[1].toInt()
			month = result.groupValues[3].toInt()
			day = result.groupValues[4].toInt()
			range = result.range
		} else {
			// Check for a written out date.
			val mMDYRegexResult = mMDYRegex.find(value)
			if (mMDYRegexResult != null) {
				val monthIndex = parseMonthIndex(mMDYRegexResult.groupValues[1], locales) ?: return null
				month = monthIndex + 1
				day = mMDYRegexResult.groupValues[2].toInt()
				val yearCheck = mMDYRegexResult.groupValues[3].toIntOrNull()
				year = if (yearCheck == null) {
					if (!yearIsOptional) return null
					currentYear
				} else {
					yearCheck
				}
				range = mMDYRegexResult.range
			} else {
				// Figure out whether based on locale month or day is expected to be first.
				val localeFormat = dateFormatter {
					locales = this@DateTimeParser.locales
					dateStyle = DateTimeFormatStyle.SHORT
				}.format(time.date(fullYear = 1110, month = 7, dayOfMonth = 8))
				val monthFirst = (localeFormat.indexOf("7") < localeFormat.indexOf("8"))
				if (monthFirst) {
					val mDYResult = mDYRegex.find(value)
					if (mDYResult == null) {
						if (yearIsOptional) {
							// Try again without the year
							val mDResult = mDRegex.find(value) ?: return null
							month = mDResult.groupValues[1].toInt()
							day = mDResult.groupValues[2].toInt()
							year = currentYear
							range = mDResult.range
						} else return null
					} else {
						month = mDYResult.groupValues[1].toInt()
						day = mDYResult.groupValues[3].toInt()
						year = mDYResult.groupValues[4].toInt()
						range = mDYResult.range
					}
				} else {
					val dMYResult = dMYRegex.find(value)
					if (dMYResult == null) {
						if (yearIsOptional) {
							// Try again without the year
							val dMResult = dMRegex.find(value) ?: return null
							day = dMResult.groupValues[1].toInt()
							month = dMResult.groupValues[2].toInt()
							year = currentYear
							range = dMResult.range
						} else return null
					} else {
						day = dMYResult.groupValues[1].toInt()
						month = dMYResult.groupValues[3].toInt()
						year = dMYResult.groupValues[4].toInt()
						range = dMYResult.range
					}
				}
			}
		}
		val fullYear = if (year < 100) {
			// Two digit year
			if (!allowTwoDigitYears) return null
			val window = (currentYear + 50) % 100
			if (year <= window) {
				year + ((currentYear + 50) / 100) * 100
			} else {
				year + ((currentYear - 49) / 100) * 100
			}
		} else {
			year
		}
		return if (timezoneOffset != null || isUtc) {
			range to time.utcDate(fullYear, month, day, 0, timezoneOffset ?: 0)
		} else {
			range to time.date(fullYear, month, day)
		}
	}

	/**
	 * The date is first attempted to be parsed as ISO8601, and if that doesn't pass, it is broken down into
	 * 3 optional segments. Date, Time, and Time Zone.
	 */
	override fun parse(value: String): Date? {
		var str = value.trim()

		// Try with an iso8601 format first.
		val isoResult = iso8601Regex.matchEntire(value)
		if (isoResult != null) {
			val fullYear = isoResult.groupValues[1].toInt()
			val month = isoResult.groupValues[2].toIntOrNull() ?: 1
			val day = isoResult.groupValues[3].toIntOrNull() ?: 1
			val hour = isoResult.groupValues[4].toIntOrNull() ?: 0
			val minute = isoResult.groupValues[5].toIntOrNull() ?: 1
			val second = isoResult.groupValues[6].toIntOrNull() ?: 0
			val milli = isoResult.groupValues[7].toTimeMilli()
			val sign = if (isoResult.groupValues[8] == "-") -1 else 1
			val offsetHour = isoResult.groupValues[9].toIntOrNull() ?: 0
			val offsetMinute = isoResult.groupValues[10].toIntOrNull() ?: 0

			return time.utcDate(fullYear, month, day, hour, minute + sign * (offsetHour * 60 + offsetMinute), second, milli)
		}

		val ret = when (type) {
			DateTimeFormatType.DATE -> {
				val timeZoneOffset = parseTimezoneOffset(str)
				str -= timeZoneOffset?.first
				val d = parseDate(str, timeZoneOffset?.second) ?: return null
				str -= d.first
				d.second
			}
			DateTimeFormatType.MONTH -> throw Exception("type MONTH not supported, use parseMonthIndex")
			DateTimeFormatType.WEEKDAY -> throw Exception("type WEEKDAY not supported, use parseWeekday")
			DateTimeFormatType.TIME -> {
				val timeZoneOffset = parseTimezoneOffset(str)
				str -= timeZoneOffset?.first
				val d = parseTime(str, timeZoneOffset?.second) ?: return null
				str -= d.first
				d.second
			}
			DateTimeFormatType.DATE_TIME -> {
				val timeZoneOffset = parseTimezoneOffset(str)
				str -= timeZoneOffset?.first
				val dR = parseDate(str, timeZoneOffset?.second) ?: return null
				str -= dR.first
				val tR = parseTime(str, 0) ?: return null
				str -= tR.first
				val separator = timeDateSeparatorRegex.find(str)
				if (separator != null)
					str -= separator.range
				time.date(dR.second.time + tR.second.time)
			}
		}
		if (str.isNotBlank()) return null
		return ret
	}

	/**
	 * A utility method to cut a range from a string.
	 */
	private operator fun String.minus(range: IntRange?): String {
		if (range == null) return this
		return substring(0, range.start) + substring(range.endInclusive + 1, length)
	}

	/**
	 * "2" becomes 200
	 * "02" becomes 20
	 * "002" becomes 200
	 * "200" becomes 200
	 * "20" becomes 200
	 */
	private fun String.toTimeMilli(): Int {
		val i = toIntOrNull() ?: return 0
		return when (length) {
			3 -> i
			2 -> i * 10
			1 -> i * 100
			else -> throw Exception()
		}
	}

	/**
	 * If this string is blank, returns null, otherwise, returns this string.
	 */
	private fun String.notBlank(): String? {
		return if (isBlank()) null else this
	}

	companion object {
		private val iso8601Regex = Regex("""(\d{4})(?:-(\d{2}))?(?:-(\d{2}))?(?:T(\d{2}):(\d{2})(?::(\d{2}))?(?:\.(\d{0,3}))?(?:([+\-])(1[0-4]|0?[0-9]):?([0-5][0-9])|Z)?)?""", RegexOption.IGNORE_CASE)

		private val timeDateSeparatorRegex = Regex("""[\s,]+""", RegexOption.IGNORE_CASE)
		private val gmtOffsetRegex = Regex("""(?:gmt|utc|z)(?:([+\-])(1[0-4]|0?[0-9])(?::?([0-5][0-9])?))?\s*$""", RegexOption.IGNORE_CASE)
		private val time12Regex = Regex("""t?(1[0-2]|0?[0-9]):([0-5][0-9])(?::([0-5][0-9])(?:.([0-9]{1,3}))?)?\s?(am|pm)""", RegexOption.IGNORE_CASE)
		private val time24Regex = Regex("""t?([2][0-3]|[0-1][0-9]|[0-9]):([0-5][0-9])(?::([0-5][0-9])(?:.([0-9]{1,3}))?)?""", RegexOption.IGNORE_CASE)

		private val yMDRegex = Regex("""d?(\d{4})([/.-])(1[0-2]|0?[1-9])\2([1-2]\d|3[0-1]|0?[1-9])""", RegexOption.IGNORE_CASE)
		private val mDYRegex = Regex("""d?(1[0-2]|0?[1-9])([/.-])([1-2]\d|3[0-1]|0?[1-9])\2(\d{2}(?:\d{2})?)""", RegexOption.IGNORE_CASE)
		private val dMYRegex = Regex("""d?([1-2]\d|3[0-1]|0?[1-9])([/.-])(1[0-2]|0?[1-9])\2(\d{2}(?:\d{2})?)""", RegexOption.IGNORE_CASE)
		private val mDRegex = Regex("""d?(1[0-2]|0?[1-9])[/.-]([1-2]\d|3[0-1]|0?[1-9])""", RegexOption.IGNORE_CASE)
		private val dMRegex = Regex("""d?([1-2]\d|3[0-1]|0?[1-9])[/.-](1[0-2]|0?[1-9])""", RegexOption.IGNORE_CASE)

		private val mMDYRegex = Regex("""[^\d\W]*?[, ]*([^\d\W]+)[, ]+([1-2]\d|3[0-1]|0?[1-9])[^\d\W]{0,3}[, ]*(\d{2}(?:\d{2})?)?""", RegexOption.IGNORE_CASE)
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
 * Returns a new date time parser configured to parse a date in local time.
 */
fun dateParser(): DateTimeParser = DateTimeParser().apply { type = DateTimeFormatType.DATE }

/**
 * Returns a date time parser configured to parse a time in local time.
 */
fun timeParser(): DateTimeParser = DateTimeParser().apply { type = DateTimeFormatType.TIME }

/**
 * Returns a date time parser configured to parse a date and time in local time.
 */
fun dateTimeParser(): DateTimeParser = DateTimeParser()

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

private val parser by lazy { DateTimeParser() }

/**
 * Parses a string into a date with the time being 00:00:00 either in local time or universal time depending on the
 * [isUtc] flag.
 * @param str The date string to parse.
 * @param isUtc [DateTimeParser.isUtc]
 * @param locales [DateTimeParser.locales]
 * @see DateTimeParser.parse
 */
fun parseDate(str: String?, isUtc: Boolean = false, locales: List<Locale>? = null): Date? {
	if (str == null) return null
	parser.type = DateTimeFormatType.DATE
	parser.isUtc = isUtc
	parser.locales = locales
	return parser.parse(str)
}

/**
 * Parses a string into a date-time either in local time or universal time depending on the [isUtc] flag.
 * @param str The date string to parse.
 * @param isUtc [DateTimeParser.isUtc]
 * @param locales [DateTimeParser.locales]
 * @see DateTimeParser.parse
 */
fun parseDateTime(str: String?, isUtc: Boolean = false, locales: List<Locale>? = null): Date? {
	if (str == null) return null
	parser.type = DateTimeFormatType.DATE_TIME
	parser.isUtc = isUtc
	parser.locales = locales
	return parser.parse(str)
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
		this.dateStyle = if (longFormat) DateTimeFormatStyle.FULL else DateTimeFormatStyle.LONG
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
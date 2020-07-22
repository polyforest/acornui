/*
 * Copyright 2020 Poly Forest, LLC
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

package com.acornui.formatters

import com.acornui.i18n.Locale
import com.acornui.time.Date
import kotlin.js.Date as JsDate


data class DateTimeParser(

	/**
	 * Whether this should format the [JsDate] object as time, date, or date and time.
	 */
	val type: DateTimeFormatType = DateTimeFormatType.DATE_TIME,

	/**
	 * If true, the date will be parsed as a UTC time if a GMT offset was not supplied in the string.
	 */
	val isUtc: Boolean = false,

	/**
	 * The ordered locale chain to use for formatting. If this is left null, the user's current locale will be used.
	 *
	 * See [https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl#Locale_identification_and_negotiation]
	 */
	var locales: List<Locale>? = null,

	/**
	 * If true, two digit years will be accepted. This is more flexible, but less safe.
	 * A two digit year is assumed to be within 50 years after, and 49 years before [currentYear].
	 */
	val allowTwoDigitYears: Boolean = false,

	/**
	 * If true, a date without a year will be considered to be the user's current year.
	 */
	val yearIsOptional: Boolean = false,

	/**
	 * If the parsed date is a two digit year and [allowTwoDigitYears] is true, then this year is used as the anchor.
	 */
	val currentYear: Int = Date().fullYear

) : StringParser<Date> {

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

		val t = if (timezoneOffset != null || isUtc) {
			Date.UTC(1970, 1, 1, hour, minute + (timezoneOffset ?: 0), second, milli)
		} else {
			Date(1970, 1, 1, hour, minute, second, milli)
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
				month = (parseMonthIndex(mMDYRegexResult.groupValues[1], locales) ?: return null) + 1
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
				val localeFormat =
					DateTimeFormatter(dateStyle = DateTimeStyle.SHORT, locales = locales)
						.format(Date(year = 1110, month = 7, day = 8))
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
		val fullYear = calculateFullYear(year, allowTwoDigitYears, currentYear) ?: return null
		return if (timezoneOffset != null || isUtc) {
			range to Date.UTC(fullYear, month, day, 0, timezoneOffset ?: 0)
		} else {
			range to Date(fullYear, month, day)
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
			val minute = isoResult.groupValues[5].toIntOrNull() ?: 0
			val second = isoResult.groupValues[6].toIntOrNull() ?: 0
			val milli = isoResult.groupValues[7].toTimeMilli()
			val sign = if (isoResult.groupValues[8] == "-") -1 else 1
			val offsetHour = isoResult.groupValues[9].toIntOrNull() ?: 0
			val offsetMinute = isoResult.groupValues[10].toIntOrNull() ?: 0

			return if (isUtc || isoResult.groupValues[4].isNotBlank()) {
				Date.UTC(
					fullYear,
					month,
					day,
					hour,
					minute + sign * (offsetHour * 60 + offsetMinute),
					second,
					milli
				)
			} else {
				// No time is provided, use midnight in the local time.
				Date(fullYear, month, day, hour, minute + sign * (offsetHour * 60 + offsetMinute), second, milli)
			}
		}

		val ret = when (type) {
			DateTimeFormatType.DATE -> {
				val timeZoneOffset = parseTimezoneOffset(str)
				str -= timeZoneOffset?.first
				val d = parseDate(str, timeZoneOffset?.second) ?: return null
				str -= d.first
				d.second
			}
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
				val date = dR.second
				val time = tR.second
				Date(date.time + time.time)
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
		return substring(0, range.first) + substring(range.last + 1, length)
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
		private val iso8601Regex = Regex(
			"""(\d{4})(?:-(\d{2}))?(?:-(\d{2}))?(?:T(\d{2}):(\d{2})(?::(\d{2}))?(?:\.(\d{0,3}))?(?:([+\-])(1[0-4]|0?[0-9]):?([0-5][0-9])|Z)?)?""",
			RegexOption.IGNORE_CASE
		)

		private val timeDateSeparatorRegex = Regex("""[\s,]+""", RegexOption.IGNORE_CASE)
		private val gmtOffsetRegex =
			Regex("""(?:gmt|utc|z)(?:([+\-])(1[0-4]|0?[0-9])(?::?([0-5][0-9])?))?\s*$""", RegexOption.IGNORE_CASE)
		private val time12Regex = Regex(
			"""t?(1[0-2]|0?[0-9]):([0-5][0-9])(?::([0-5][0-9])(?:.([0-9]{1,3}))?)?\s?(am|pm)""",
			RegexOption.IGNORE_CASE
		)
		private val time24Regex = Regex(
			"""t?([2][0-3]|[0-1][0-9]|[0-9]):([0-5][0-9])(?::([0-5][0-9])(?:.([0-9]{1,3}))?)?""",
			RegexOption.IGNORE_CASE
		)

		private val yMDRegex =
			Regex("""d?(\d{4})([/.-])(1[0-2]|0?[1-9])\2([1-2]\d|3[0-1]|0?[1-9])""", RegexOption.IGNORE_CASE)
		private val mDYRegex =
			Regex("""d?(1[0-2]|0?[1-9])([/.-])([1-2]\d|3[0-1]|0?[1-9])\2(\d{2}(?:\d{2})?)""", RegexOption.IGNORE_CASE)
		private val dMYRegex =
			Regex("""d?([1-2]\d|3[0-1]|0?[1-9])([/.-])(1[0-2]|0?[1-9])\2(\d{2}(?:\d{2})?)""", RegexOption.IGNORE_CASE)
		private val mDRegex = Regex("""d?(1[0-2]|0?[1-9])[/.-]([1-2]\d|3[0-1]|0?[1-9])""", RegexOption.IGNORE_CASE)
		private val dMRegex = Regex("""d?([1-2]\d|3[0-1]|0?[1-9])[/.-](1[0-2]|0?[1-9])""", RegexOption.IGNORE_CASE)

		private val mMDYRegex = Regex(
			"""[^\d\W]*?[, ]*([^\d\W]+)[, ]+([1-2]\d|3[0-1]|0?[1-9])[^\d\W]{0,3}[, ]*(\d{2}(?:\d{2})?)?""",
			RegexOption.IGNORE_CASE
		)
	}
}

enum class DateTimeFormatType {
	DATE,
	TIME,
	DATE_TIME
}


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
	val parser = DateTimeParser(type = DateTimeFormatType.DATE, isUtc = isUtc, locales = locales)
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
	val parser = DateTimeParser(type = DateTimeFormatType.DATE_TIME, isUtc = isUtc, locales = locales)
	return parser.parse(str)
}


/**
 * Returns a new date time parser configured to parse a date in local time.
 */
fun dateParser(): DateTimeParser = DateTimeParser(type = DateTimeFormatType.DATE)

/**
 * Returns a date time parser configured to parse a time in local time.
 */
fun timeParser(init: DateTimeParser.() -> Unit = {}): DateTimeParser = DateTimeParser(type = DateTimeFormatType.TIME)

/**
 * Parses a string into a day of week, 0 - Sunday, 6 - Saturday, according to the given locale.
 * @param str The day of the week string to parse.
 * @param locales [DateTimeParser.locales]
 */
fun parseWeekday(str: String, locales: List<Locale>? = null): Int? {
	val s = str.trim().toLowerCase()
	val shortIndex = getDaysOfWeek(false, locales).indexOfFirst { it.toLowerCase() == s }
	if (shortIndex != -1) return shortIndex
	val longIndex = getDaysOfWeek(true, locales).indexOfFirst { it.toLowerCase() == s }
	return if (longIndex != -1) longIndex else null
}

/**
 * Parses a string into a Month index, according to the given locale.
 *
 * @param str The month string to parse.
 * @param locales [DateTimeParser.locales]
 * @return Returns the month index, or null if the month could not be parsed. January - 0, December - 11
 */
fun parseMonthIndex(str: String, locales: List<Locale>? = null): Int? {
	val s = str.trim().toLowerCase()
	val shortIndex = getMonths(false, locales).indexOfFirst { it.toLowerCase() == s }
	if (shortIndex != -1) return shortIndex
	val longIndex = getMonths(true, locales).indexOfFirst { it.toLowerCase() == s }
	return if (longIndex != -1) longIndex else null
}

/**
 *
 */
fun parseYear(str: String, allowTwoDigitYears: Boolean = true, currentYear: Int = JsDate().getFullYear()): Int? {
	val year = str.trim().toIntOrNull() ?: return null
	return calculateFullYear(year, allowTwoDigitYears, currentYear)
}
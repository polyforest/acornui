/*
 * Copyright 2019 Poly Forest, LLC
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

import com.acornui.obj.removeNullValues
import com.acornui.i18n.Locale
import com.acornui.system.userInfo
import com.acornui.time.Date
import kotlin.js.Date as JsDate

/**
 * This class formats dates into localized string representations.
 */
class DateTimeFormatter(

	/**
	 * The date formatting style to use when called.
	 */
	dateStyle: DateTimeStyle? = null,

	/**
	 * The time formatting style to use when called.
	 */
	timeStyle: DateTimeStyle? = null,

	/**
	 * The number of fractional seconds to apply when calling format(). Valid values are 0-3.
	 */
	fractionalSecondsDigit: Int? = null,

	calendar: Calendar? = null,

	dayPeriod: DayPeriod? = null,

	numberingSystem: NumberingSystem? = null,

	/**
	 * The locale matching algorithm to use.
	 */
	localeMatcher: LocaleMatcher = LocaleMatcher.BEST_FIT,

	/**
	 * The time zone for formatting.
	 * The only values this is guaranteed to work with are "UTC" or null.
	 * Other values that will likely work based on browser or jvm implementation are the full TZ code
	 * [https://en.wikipedia.org/wiki/List_of_tz_database_time_zones]
	 * For example, use "America/New_York" as opposed to "EST"
	 * If this is null, the user's timezone will be used.
	 */
	timeZone: String? = null,

	/**
	 * Whether to use 12-hour time (as opposed to 24-hour time). Possible values are true and false; the default is
	 * locale dependent. This option overrides the hc language tag and/or the hourCycle option in case both are present.
	 */
	hour12: Boolean? = null,

	/**
	 * The hour cycle to use. This option overrides the hc language tag, if both are present, and the hour12 option
	 * takes precedence in case both options have been specified.
	 */
	hourCycle: HourCycle? = null,

	/**
	 * The format matching algorithm to use.
	 * See the following paragraphs for information about the use of this property.
	 */
	formatMatcher: FormatMatcher? = null,

	weekday: WeekdayFormat? = null,

	era: EraFormat? = null,

	year: YearFormat? = null,

	month: MonthFormat? = null,

	day: TimePartFormat? = null,

	hour: TimePartFormat? = null,

	minute: TimePartFormat? = null,

	second: TimePartFormat? = null,

	timeZoneName: TimezoneNameFormat? = null,

	/**
	 * The ordered locale chain to use for formatting. If this is left null, the user's current locale will be used.
	 *
	 * See [Locale Identification](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl#Locale_identification_and_negotiation)
	 */
	locales: List<Locale>? = null

) : StringFormatter<Date> {

	private val formatter: dynamic

	init {
		@Suppress("LocalVariableName")
		val JsDateTimeFormat = js("Intl.DateTimeFormat")
		val options = js("({})")

		options.dateStyle = dateStyle?.name.toJsValue()
		options.timeStyle = timeStyle?.name.toJsValue()
		options.fractionalSecondsDigit = fractionalSecondsDigit
		options.calendar = calendar?.name.toJsValue()
		options.dayPeriod = dayPeriod?.name.toJsValue()
		options.numberingSystem = numberingSystem?.name.toJsValue()
		options.localeMatcher = localeMatcher.name.toJsValue()
		options.timeZone = timeZone
		options.hour12 = hour12
		options.hourCycle = hourCycle?.name.toJsValue()
		options.formatMatcher = formatMatcher?.name.toJsValue()
		options.weekday = weekday?.name.toJsValue()
		options.era = era?.name.toJsValue()
		options.year = year?.name.toJsValue()
		options.month = month?.name.toJsValue()
		options.day = day?.name.toJsValue()
		options.hour = hour?.name.toJsValue()
		options.minute = minute?.name.toJsValue()
		options.second = second?.name.toJsValue()
		options.timeZoneName = timeZoneName?.name.toJsValue()

		removeNullValues(options)

		val loc = (locales ?: userInfo.systemLocale).map { it.value }.toTypedArray()
		formatter = JsDateTimeFormat(loc, options)
	}

	override fun format(value: Date): String =
		formatter!!.format(value.jsDate).unsafeCast<String>()

}

enum class DateTimeStyle {
	FULL,
	LONG,
	MEDIUM,
	SHORT
}

enum class DayPeriod {
	NARROW,
	SHORT,
	LONG
}

enum class NumberingSystem {
	ARAB,
	ARABEXT,
	BALI,
	BENG,
	DEVA,
	FULLWIDE,
	GUJR,
	GURU,
	HANIDEC,
	KHMR,
	KNDA,
	LAOO,
	LATN,
	LIMB,
	MLYM,
	MONG,
	MYMR,
	ORYA,
	TAMLDEC,
	TELU,
	THAI,
	TIBT
}

enum class LocaleMatcher {
	LOOKUP,
	BEST_FIT
}

enum class FormatMatcher {
	BASIC,
	BEST_FIT
}

enum class HourCycle {
	H11,
	H12,
	H23,
	H24
}

enum class DateTimeFormatStyle {
	FULL,
	LONG,
	MEDIUM,
	SHORT,
	DEFAULT
}

enum class Calendar {
	BUDDHIST,
	CHINESE,
	COPTIC,
	ETHIOPIA,
	ETHIOPIC,
	GREGORY,
	HEBREW,
	INDIAN,
	ISLAMIC,
	ISO8601,
	JAPANESE,
	PERSIAN,
	ROC
}

enum class WeekdayFormat {

	/**
	 * E.g. Thursday
	 */
	LONG,

	/**
	 * E.g. Thu
	 */
	SHORT,

	/**
	 * E.g. T
	 */
	NARROW
}

enum class EraFormat {

	/**
	 * E.g. Anno Domini
	 */
	LONG,

	/**
	 * E.g. AD
	 */
	SHORT,

	/**
	 * E.g. A
	 */
	NARROW
}

enum class YearFormat {

	/**
	 * E.g. 2012
	 */
	NUMERIC,

	/**
	 * E.g. 12
	 */
	TWO_DIGIT
}

enum class MonthFormat {

	/**
	 * E.g. 2
	 */
	NUMERIC,

	/**
	 * E.g. 02
	 */
	TWO_DIGIT,

	/**
	 * E.g. March
	 */
	LONG,

	/**
	 * E.g. Mar
	 */
	SHORT,

	/**
	 * E.g. M
	 */
	NARROW
}

enum class TimePartFormat {

	/**
	 * E.g. 1
	 */
	NUMERIC,

	/**
	 * E.g. 01
	 */
	TWO_DIGIT
}

enum class TimezoneNameFormat {

	/**
	 * E.g. British Summer Time
	 */
	LONG,

	/**
	 * E.g. GMT+1
	 */
	SHORT
}

/**
 * Converts a two digit year to a four digit year, relative to [currentYear].
 * @param year If this is not a two digit year, it will be returned as is. Otherwise, it will be considered relative
 * to [currentYear]. That is, the year returned will be in the century of the span of
 * `currentYear - 49 to currentYear + 50`.
 */
fun calculateFullYear(year: Int, allowTwoDigitYears: Boolean = true, currentYear: Int = JsDate().getFullYear()): Int? {
	return if (year < 100) {
		// Two digit year
		if (!allowTwoDigitYears) return null
		val window = (currentYear + 50) % 100
		if (year <= window) {
			year + ((currentYear + 50) / 100) * 100
		} else {
			year + ((currentYear - 49) / 100) * 100
		}
	} else year
}

private val daysOfWeekCache = HashMap<Pair<Boolean, List<Locale>?>, List<String>>()

/**
 * Returns a list of the localized days of the week.
 * @param locales The locales to use for lookup. Use null for the user's locale.
 */
fun getDaysOfWeek(longFormat: Boolean, locales: List<Locale>? = null): List<String> {
	val cacheKey = longFormat to locales
	if (daysOfWeekCache.containsKey(cacheKey)) return daysOfWeekCache[cacheKey]!!
	val list = ArrayList<String>(7)
	val formatter = DateTimeFormatter(
		weekday = if (longFormat) WeekdayFormat.LONG else WeekdayFormat.SHORT,
		locales = locales
	)
	val d = Date(0)
	val offset = d.dayOfMonth - d.dayOfWeek
	for (i in 0..11) {
		list.add(formatter.format(Date(year = 0, month = 1, day = i + offset)))
	}
	daysOfWeekCache[cacheKey] = list
	return list
}


private val monthsOfYearCache = HashMap<Pair<Boolean, List<Locale>?>, List<String>>()

/**
 * Returns a list of the localized months of the year.
 *
 * @param longFormat If true, the whole month names will be returned instead of the abbreviations.
 * @param locales The locale chain to use for parsing. If this is null, then [com.acornui.system.UserInfo.currentLocale]
 * will be used from [com.acornui.system.userInfo].
 */
fun getMonths(longFormat: Boolean, locales: List<Locale>? = null): List<String> {
	val cacheKey = longFormat to locales
	if (monthsOfYearCache.containsKey(cacheKey)) return monthsOfYearCache[cacheKey]!!
	val list = ArrayList<String>(12)
	val formatter = DateTimeFormatter(
		month = if (longFormat) MonthFormat.LONG else MonthFormat.SHORT,
		locales = locales
	)
	for (i in 1..12) {
		list.add(formatter.format(Date(year = 0, month = i, day = 1)))
	}
	monthsOfYearCache[cacheKey] = list
	return list
}


private fun String?.toJsValue(): String? {
	if (this == null) return null
	return if (this == "TWO_DIGIT") "2-digit" // Special case.
	else toLowerCase().replace('_', ' ')
}
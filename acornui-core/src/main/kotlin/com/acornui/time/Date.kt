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

package com.acornui.time

import com.acornui.formatters.parseDate
import com.acornui.number.zeroPadding
import kotlinx.serialization.*
import kotlin.time.Duration
import kotlin.js.Date as JsDate

/**
 * A wrapper to a JS Date object that does the following:
 *
 * - Converts get methods to property getters.
 * - Implements Comparable
 * - Allows equality
 * - Serialization
 * - Months are one indexed. (Use monthIndex for zero-indexed month access)
 */
@Serializable(with = DateSerializer::class)
class Date(val jsDate: JsDate) : Comparable<Date> {

	constructor() : this(JsDate())

	constructor(milliseconds: Number) : this(JsDate(milliseconds))

	constructor(dateString: String) : this(JsDate(dateString))

	constructor(year: Int, month: Int) : this(JsDate(year, month - 1))

	constructor(year: Int, month: Int, day: Int) : this(JsDate(year, month - 1, day))

	constructor(year: Int, month: Int, day: Int, hour: Int) : this(JsDate(year, month - 1, day, hour))

	constructor(year: Int, month: Int, day: Int, hour: Int, minute: Int) : this(
		JsDate(
			year,
			month - 1,
			day,
			hour,
			minute
		)
	)

	constructor(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int) : this(
		JsDate(
			year,
			month - 1,
			day,
			hour,
			minute,
			second
		)
	)

	constructor(
		year: Int,
		month: Int,
		day: Int,
		hour: Int,
		minute: Int,
		second: Int,
		millisecond: Number
	) : this(JsDate(year, month - 1, day, hour, minute, second, millisecond))

	/**
	 * The numeric value of the specified date as the number of milliseconds since January 1, 1970,
	 * 00:00:00 UTC (negative for prior times).
	 */
	val time: Double
		get() = jsDate.getTime()

	/**
	 * The full year according to local time.  (e.g. 1999, not 99)
	 */
	val fullYear: Int
		get() = jsDate.getFullYear()

	/**
	 * The [fullYear] according to universal time.
	 */
	val utcFullYear: Int
		get() = jsDate.getUTCFullYear()

	/**
	 * The 0 indexed month according to local time. 0 - January, 11 - December
	 * @see Months
	 */
	val monthIndex: Int
		get() = jsDate.getMonth()

	/**
	 * The 1 indexed month according to local time.
	 */
	val month: Int
		get() = monthIndex + 1

	/**
	 * The [monthIndex] according to universal time.
	 */
	val utcMonthIndex: Int
		get() = jsDate.getUTCMonth()

	/**
	 * The [month] according to universal time.
	 * This is 1 indexed.
	 * 1 - January, 12 - December
	 */
	val utcMonth: Int
		get() = utcMonthIndex + 1

	/**
	 * The 1 indexed day of the month according to local time. 1st - 1, 31st - 31
	 */
	val dayOfMonth: Int
		get() = jsDate.getDate()

	/**
	 * The [dayOfMonth] according to universal time.
	 */
	val utcDayOfMonth: Int
		get() = jsDate.getUTCDate()

	/**
	 * The day of the week (0-6) according to local time.
	 * 0 - Sunday, 6 - Saturday
	 */
	val dayOfWeek: Int
		get() = jsDate.getDay()

	/**
	 * The [dayOfWeek] according to universal time.
	 */
	val utcDayOfWeek: Int
		get() = jsDate.getUTCDay()

	/**
	 * Hour of the day using 24-hour clock according to local time.
	 * At 3:14:12.330 PM the hour is 15.
	 */
	val hours: Int
		get() = jsDate.getHours()

	/**
	 * The [hours] according to universal time.
	 */
	val utcHours: Int
		get() = jsDate.getUTCHours()

	/**
	 * The minute within the hour according to local time.
	 */
	val minutes: Int
		get() = jsDate.getMinutes()

	/**
	 * The [minutes] according to universal time.
	 */
	val utcMinutes: Int
		get() = jsDate.getUTCMinutes()

	/**
	 * The second within the minute according to local time.
	 * At 3:14:12.330 PM the second is 12.
	 */
	val seconds: Int
		get() = jsDate.getSeconds()

	/**
	 * The [seconds] according to universal time.
	 */
	val utcSeconds: Int
		get() = jsDate.getUTCSeconds()

	/**
	 * The millisecond within the second according to local time.
	 * At 3:14:12.330 PM the milli is 330.
	 */
	val milliseconds: Int
		get() = jsDate.getMilliseconds()

	/**
	 * The [milliseconds] according to universal time.
	 */
	val utcMilliseconds: Int
		get() = jsDate.getUTCMilliseconds()

	/**
	 * The timezone offset from local time to GMT in minutes.
	 */
	val timezoneOffset: Int
		get() = jsDate.getTimezoneOffset()

	/**
	 * Returns a mutable copy of this date.
	 */
	fun copy(): Date {
		return Date(time)
	}

	/**
	 * Outputs to the ISO-8601 standard. The format is: YYYY-MM-DDTHH:mm:ss.sssZ
	 */
	fun toIsoString(): String {
		return "${utcFullYear.zeroPadding(4)}-${utcMonth.zeroPadding(2)}-${utcDayOfMonth.zeroPadding(2)}T${utcHours.zeroPadding(
			2
		)}:${utcMinutes.zeroPadding(2)}:${utcSeconds.zeroPadding(2)}.${utcMilliseconds.zeroPadding(3)}Z"
	}

	override fun compareTo(other: Date): Int {
		return time.compareTo(other.time)
	}


	override fun toString(): String = jsDate.toDateString()

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other == null || this::class.js != other::class.js) return false
		other as Date
		if (time != other.time) return false
		return true
	}

	override fun hashCode(): Int = time.hashCode()

	companion object {

		fun UTC(year: Int, month: Int) = Date(JsDate(JsDate.UTC(year, month - 1)))

		fun UTC(year: Int, month: Int, day: Int) = Date(JsDate(JsDate.UTC(year, month - 1, day)))

		fun UTC(year: Int, month: Int, day: Int, hour: Int) = Date(JsDate(JsDate.UTC(year, month - 1, day, hour)))

		fun UTC(year: Int, month: Int, day: Int, hour: Int, minute: Int) =
			Date(JsDate(JsDate.UTC(year, month - 1, day, hour, minute)))

		fun UTC(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int) =
			Date(JsDate(JsDate.UTC(year, month - 1, day, hour, minute, second)))

		fun UTC(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int, millisecond: Number) =
			Date(JsDate(JsDate.UTC(year, month - 1, day, hour, minute, second, millisecond)))
	}

}

/**
 * Returns true if the two dates are the same day.
 */
fun Date.isSameDate(o: Date): Boolean {
	return this.dayOfMonth == o.dayOfMonth && this.monthIndex == o.monthIndex && this.fullYear == o.fullYear
}

/**
 * The normalized year of the gregorian cutover in Gregorian, with
 * 0 representing 1 BCE, -1 representing 2 BCE, etc.
 */
const val GREGORIAN_CUTOVER_YEAR = 1582

/**
 * Determines if the given year is a leap year. Returns `true` if
 * the given year is a leap year. To specify BC year numbers,
 * `1 - year number` must be given. For example, year BC 4 is
 * specified as -3.
 *
 * @param year the given year.
 * @return `true` if the given year is a leap year; `false` otherwise.
 */
fun isLeapYear(year: Int): Boolean {
	if ((year and 3) != 0) {
		return false
	}
	return if (year >= GREGORIAN_CUTOVER_YEAR) {
		(year % 100 != 0) || (year % 400 == 0) // Gregorian
	} else true // Julian calendar had no correction.
}

/**
 * Returns true if this Date's year is a leap year.
 */
val Date.isLeapYear: Boolean
	get() = isLeapYear(fullYear)


enum class Era {

	/**
	 * Before common era (Before christ)
	 */
	BCE,

	/**
	 * Common era (Anno domini)
	 */
	CE
}

object Months {

	val JANUARY: Int = 0

	val FEBRUARY: Int = 1

	val MARCH: Int = 2

	val APRIL: Int = 3

	val MAY: Int = 4

	val JUNE: Int = 5

	val JULY: Int = 6

	val AUGUST: Int = 7

	val SEPTEMBER: Int = 8

	val OCTOBER: Int = 9

	val NOVEMBER: Int = 10

	val DECEMBER: Int = 11
}

fun time(hour: Int = 0, minute: Int = 0, second: Int = 0, millisecond: Number = 0): Date =
	Date(1970, 1, 1, hour, minute, second, millisecond)

fun utcTime(hour: Int = 0, minute: Int = 0, second: Int = 0, millisecond: Number = 0): Date =
	Date.UTC(1970, 1, 1, hour, minute, second, millisecond)

fun utcDate(
	year: Int,
	month: Int,
	day: Int,
	hour: Int = 0,
	minute: Int = 0,
	second: Int = 0,
	millisecond: Number = 0
): Date = Date.UTC(year, month, day, hour, minute, second, millisecond)

@Serializer(forClass = Date::class)
object DateSerializer : KSerializer<Date> {

	override val descriptor: SerialDescriptor = PrimitiveDescriptor("Date", PrimitiveKind.STRING)

	override fun serialize(encoder: Encoder, value: Date) {
		encoder.encodeString(value.toIsoString())
	}

	override fun deserialize(decoder: Decoder): Date {
		return parseDate(decoder.decodeString())!!
	}
}

/**
 * Returns a new date, incrementing the time by [duration].
 */
operator fun Date.plus(duration: Duration): Date {
	return Date(time + duration.toLongMilliseconds())
}
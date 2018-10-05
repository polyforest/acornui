/*
 * Copyright 2014 Nicholas Bilyk
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

package com.acornui.core.time

import com.acornui.serialization.From
import com.acornui.serialization.Reader
import com.acornui.serialization.To
import com.acornui.serialization.Writer

interface DateRo : Comparable<DateRo> {

	/**
	 * The numeric value of the specified date as the number of milliseconds since January 1, 1970,
	 * 00:00:00 UTC (negative for prior times).
	 */
	val time: Long

	/**
	 * The full year according to local time.  (e.g. 1999, not 99)
	 */
	val fullYear: Int

	/**
	 * The [fullYear] according to universal time.
	 */
	val utcFullYear: Int

	/**
	 * The 0 indexed month according to local time. 0 - January, 11 - December
	 * @see Months
	 */
	val monthIndex: Int

	/**
	 * The 1 indexed month according to local time.
	 */
	val month: Int
		get() = monthIndex + 1

	/**
	 * The [monthIndex] according to universal time.
	 */
	val utcMonthIndex: Int

	/**
	 * The [month] according to universal time.
	 */
	val utcMonth: Int
		get() = utcMonthIndex + 1

	/**
	 * The 1 indexed day of the month according to local time. 1st - 1, 31st - 31
	 */
	val dayOfMonth: Int

	/**
	 * The [dayOfMonth] according to universal time.
	 */
	val utcDayOfMonth: Int

	/**
	 * The day of the week (0-6) according to local time.
	 * 0 - Sunday, 6 - Saturday
	 */
	val dayOfWeek: Int

	/**
	 * The [dayOfWeek] according to universal time.
	 */
	val utcDayOfWeek: Int

	/**
	 * Hour of the day using 24-hour clock according to local time.
	 * At 3:14:12.330 PM the hour is 15.
	 */
	val hour: Int

	/**
	 * The [hour] according to universal time.
	 */
	val utcHour: Int

	/**
	 * The minute within the hour according to local time.
	 */
	val minute: Int

	/**
	 * The [minute] according to universal time.
	 */
	val utcMinute: Int

	/**
	 * The second within the minute according to local time.
	 * At 3:14:12.330 PM the second is 12.
	 */
	val second: Int

	/**
	 * The [second] according to universal time.
	 */
	val utcSecond: Int

	/**
	 * The millisecond within the second according to local time.
	 * At 3:14:12.330 PM the milli is 330.
	 */
	val milli: Int

	/**
	 * The [milli] according to universal time.
	 */
	val utcMilli: Int

	/**
	 * The timezone offset from local time to GMT in minutes.
	 */
	val timezoneOffset: Int

	/**
	 * Returns a mutable copy of this date.
	 */
	fun copy(): Date

	override fun compareTo(other: DateRo): Int {
		return time.compareTo(other.time)
	}

}

/**
 * Returns true if the two dates are the same day.
 */
fun DateRo.isSameDate(o: DateRo): Boolean {
	return this.dayOfMonth == o.dayOfMonth && this.monthIndex == o.monthIndex && this.fullYear == o.fullYear
}

/**
 * Returns true if this Date's year is a leap year.
 */
val DateRo.isLeapYear: Boolean
	get() = DateUtil.isLeapYear(fullYear)

/**
 * A Date object.
 * To convert this date to a string, use [com.acornui.core.text.DateTimeFormatter]
 * @author nbilyk
 */
interface Date : DateRo {

	override var time: Long

	override var fullYear: Int
	override var utcFullYear: Int

	override var monthIndex: Int
	override var month: Int
		get() = monthIndex + 1
		set(value) {
			monthIndex = value - 1
		}

	override var utcMonthIndex: Int
	override var utcMonth: Int
		get() = utcMonthIndex + 1
		set(value) {
			utcMonthIndex = value - 1
		}

	override var dayOfMonth: Int
	override var utcDayOfMonth: Int

	override var hour: Int
	override var utcHour: Int

	override var minute: Int
	override var utcMinute: Int

	override var second: Int
	override var utcSecond: Int

	override var milli: Int
	override var utcMilli: Int

	/**
	 * A convenience function for setting the time of day on a Date object.
	 * Note that if the time provided is beyond the range of a day, the date will change.
	 */
	fun setTimeOfDay(hour: Int, minute: Int, second: Int = 0, milli: Int = 0): Date {
		this.hour = hour
		this.minute = minute
		this.second = second
		this.milli = milli
		return this
	}

	/**
	 * A convenience function for setting the UTC time of day on a Date object.
	 * Note that if the time provided is beyond the range of a day, the date will change.
	 */
	fun setUtcTimeOfDay(hour: Int, minute: Int, second: Int = 0, milli: Int = 0): Date {
		this.utcHour = hour
		this.utcMinute = minute
		this.utcSecond = second
		this.utcMilli = milli
		return this
	}

	/**
	 * A convenience function for setting the date on a Date object.
	 */
	fun setDate(fullYear: Int, month: Int, dayOfMonth: Int): Date {
		this.fullYear = fullYear
		this.month = month
		this.dayOfMonth = dayOfMonth
		return this
	}

	/**
	 * A convenience function for setting the utc date on a Date object.
	 */
	fun setUtcDate(fullYear: Int, month: Int, dayOfMonth: Int): Date {
		this.utcFullYear = fullYear
		this.utcMonth = month
		this.utcDayOfMonth = dayOfMonth
		return this
	}

	/**
	 * Sets this date object to match the time of the other date object.
	 */
	fun set(other: DateRo): Date {
		this.time = other.time
		return this
	}

}

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

// TODO:
object DateSerializer : To<Date?>, From<Date?> {
	override fun read(reader: Reader): Date? {
		return null
	}

	override fun Date?.write(writer: Writer) {
	}
}
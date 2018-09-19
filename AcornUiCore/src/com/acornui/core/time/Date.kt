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
	 * The time, in UTC milliseconds from the Unix epoch.
	 */
	val time: Long

	/**
	 * The full 4 digit year.
	 */
	val year: Int

	/**
	 * The 0 indexed month. 0 - January, 11 - December
	 * @see Months
	 */
	val month: Int

	/**
	 * The 1 indexed day of the month. 1st - 1, 31st - 31
	 */
	val dayOfMonth: Int

	/**
	 * The day of the week (0-6) for the specified date.
	 * 0 - Sunday, 6 - Saturday
	 */
	val dayOfWeek: Int

	/**
	 * Hour of the day using 24-hour clock.
	 * At 3:14:12.330 PM the hour is 15.
	 */
	val hour: Int

	/**
	 * The minute within the hour.
	 */
	val minute: Int

	/**
	 * The second within the minute.
	 * At 3:14:12.330 PM the second is 12.
	 */
	val second: Int

	/**
	 * The millisecond within the second.
	 * At 3:14:12.330 PM the milli is 330.
	 */
	val milli: Int

	/**
	 * Returns a mutable copy of this date.
	 */
	fun clone(): Date

	override fun compareTo(other: DateRo): Int {
		return time.compareTo(other.time)
	}
}

/**
 * Returns true if the two dates are the same day.
 */
fun DateRo.isSameDate(o: DateRo): Boolean {
	return this.dayOfMonth == o.dayOfMonth && this.month == o.month && this.year == o.year
}

/**
 * Returns true if this Date's year is a leap year.
 */
val DateRo.isLeapYear: Boolean
	get() = DateUtil.isLeapYear(year)

/**
 * @author nbilyk
 */
interface Date : DateRo {

	override var time: Long

	override var year: Int

	override var month: Int

	override var dayOfMonth: Int

	override var hour: Int

	override var minute: Int

	override var second: Int

	override var milli: Int

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
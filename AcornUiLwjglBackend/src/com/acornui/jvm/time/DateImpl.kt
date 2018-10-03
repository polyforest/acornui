/*
 * Copyright 2015 Nicholas Bilyk
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

package com.acornui.jvm.time

import com.acornui.core.time.Date
import com.acornui.core.time.DateRo
import com.acornui.core.zeroPadding
import java.time.ZoneOffset
import java.util.*


/**
 * @author nbilyk
 */
class DateImpl : Date {

	private var localDateIsValid = true
	private var utcDateIsValid = false

	private val _date = Calendar.getInstance()
	private val date: Calendar
		get() {
			if (!localDateIsValid) {
				localDateIsValid = true
				_date.time = _dateUtc.time
			}
			utcDateIsValid = false
			return _date
		}

	private val _dateUtc by lazy { Calendar.getInstance(TimeZone.getTimeZone(ZoneOffset.UTC)) }
	private val dateUtc: Calendar
		get() {
			// The UTC methods first mirror the local date's time, then returns that date.
			if (!utcDateIsValid) {
				utcDateIsValid = true
				_dateUtc.time = _date.time
			}
			localDateIsValid = false
			return _dateUtc
		}

	override var time: Long
		get() = date.timeInMillis
		set(value) {
			date.timeInMillis = value
		}

	override var fullYear: Int
		get() = date.get(Calendar.YEAR)
		set(value) {
			date.set(Calendar.YEAR, value)
		}

	override var utcFullYear: Int
		get() {
			return dateUtc.get(Calendar.YEAR)
		}
		set(value) {
			dateUtc.set(Calendar.YEAR, value)
		}

	override var monthIndex: Int
		get() = date.get(Calendar.MONTH)
		set(value) {
			date.set(Calendar.MONTH, value)
		}

	override var utcMonthIndex: Int
		get() = dateUtc.get(Calendar.MONTH)
		set(value) {
			dateUtc.set(Calendar.MONTH, value)
		}

	override var dayOfMonth: Int
		get() = date.get(Calendar.DAY_OF_MONTH)
		set(value) {
			date.set(Calendar.DAY_OF_MONTH, value)
		}

	override var utcDayOfMonth: Int
		get() = dateUtc.get(Calendar.DAY_OF_MONTH)
		set(value) {
			dateUtc.set(Calendar.DAY_OF_MONTH, value)
		}

	override val dayOfWeek: Int
		get() = date.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY

	override val utcDayOfWeek: Int
		get() = dateUtc.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY

	override var hour: Int
		get() = date.get(Calendar.HOUR_OF_DAY)
		set(value) {
			date.set(Calendar.HOUR_OF_DAY, value)
		}

	override var utcHour: Int
		get() = dateUtc.get(Calendar.HOUR_OF_DAY)
		set(value) {
			dateUtc.set(Calendar.HOUR_OF_DAY, value)
		}

	override var minute: Int
		get() = date.get(Calendar.MINUTE)
		set(value) {
			date.set(Calendar.MINUTE, value)
		}

	override var utcMinute: Int
		get() = dateUtc.get(Calendar.MINUTE)
		set(value) {
			dateUtc.set(Calendar.MINUTE, value)
		}

	override var second: Int
		get() = date.get(Calendar.SECOND)
		set(value) {
			date.set(Calendar.SECOND, value)
		}

	override var utcSecond: Int
		get() = dateUtc.get(Calendar.SECOND)
		set(value) {
			dateUtc.set(Calendar.SECOND, value)
		}

	override var milli: Int
		get() = date.get(Calendar.MILLISECOND)
		set(value) {
			date.set(Calendar.MILLISECOND, value)
		}

	override var utcMilli: Int
		get() = dateUtc.get(Calendar.MILLISECOND)
		set(value) {
			dateUtc.set(Calendar.MILLISECOND, value)
		}

	override val timezoneOffset: Int
		get() = date.get(Calendar.ZONE_OFFSET) / 60000

	override fun copy(): Date {
		val newDate = DateImpl()
		newDate.time = time
		return newDate
	}

	override fun equals(other: Any?): Boolean {
		if (other == null) return false
		other as? DateRo ?: return false
		return time == other.time
	}

	override fun hashCode(): Int {
		return time.hashCode()
	}

	override fun toString(): String {
		return "Date($fullYear/$month/$dayOfMonth $hour:${minute.zeroPadding(2)}:${second.zeroPadding(2)}.$milli)"
	}
}
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

import com.acornui.core.zeroPadding

/**
 * @author nbilyk
 */
actual class DateImpl : Date {

	val jsDate = js("new Date();")

	override var time: Long
		get() {
			return (jsDate.getTime() as Number).toLong()
		}
		set(value) {
			val t = value.asDynamic().toNumber()
			jsDate.setTime(t)
		}

	override var fullYear: Int
		get() {
			return jsDate.getFullYear()
		}
		set(value) {
			jsDate.setFullYear(value)
		}

	override var monthIndex: Int
		get() = jsDate.getMonth()
		set(value) {
			jsDate.setMonth(value)
		}

	override var dayOfMonth: Int
		get() = jsDate.getDate()
		set(value) {
			jsDate.setDate(value)
		}

	override val dayOfWeek: Int
		get() = jsDate.getDay()


	override var hour: Int
		get() = jsDate.getHours()
		set(value) {
			jsDate.setHours(value)
		}

	override var minute: Int
		get() = jsDate.getMinutes()
		set(value) {
			jsDate.setMinutes(value)
		}

	override var second: Int
		get() = jsDate.getSeconds()
		set(value) {
			jsDate.setSeconds(value)
		}

	override var milli: Int
		get() = jsDate.getMilliseconds()
		set(value) {
			jsDate.setMilliseconds(value)
		}

	override var utcFullYear: Int
		get() {
			return jsDate.getUTCFullYear()
		}
		set(value) {
			jsDate.setUTCFullYear(value)
		}

	override var utcMonthIndex: Int
		get() = jsDate.getUTCMonth()
		set(value) {
			jsDate.setUTCMonth(value)
		}

	override var utcDayOfMonth: Int
		get() = jsDate.getUTCDate()
		set(value) {
			jsDate.setUTCDate(value)
		}

	override val utcDayOfWeek: Int
		get() = jsDate.getUTCDay()


	override var utcHour: Int
		get() = jsDate.getUTCHours()
		set(value) {
			jsDate.setUTCHours(value)
		}

	override var utcMinute: Int
		get() = jsDate.getUTCMinutes()
		set(value) {
			jsDate.setUTCMinutes(value)
		}

	override var utcSecond: Int
		get() = jsDate.getUTCSeconds()
		set(value) {
			jsDate.setUTCSeconds(value)
		}

	override var utcMilli: Int
		get() = jsDate.getUTCMilliseconds()
		set(value) {
			jsDate.setUTCMilliseconds(value)
		}

	override val timezoneOffset: Int
		get() = jsDate.getTimezoneOffset()

	override fun copy(): Date {
		val newDate = DateImpl()
		newDate.time = time
		return newDate
	}

	override fun equals(other: Any?): Boolean {
		if (other == null) return false
		if (other !is DateRo) return false
		return time == other.time
	}

	override fun hashCode(): Int {
		return time.hashCode()
	}

	override fun toString(): String {
		return "Date($fullYear/$month/$dayOfMonth $hour:${minute.zeroPadding(2)}:${second.zeroPadding(2)}.$milli)"
	}
}

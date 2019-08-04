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

import com.acornui.zeroPadding
import kotlinx.serialization.Serializable

/**
 * @author nbilyk
 */
@Serializable(with = DateSerializer::class)
actual class Date actual constructor() : DateRo {

	val jsDate = js("new Date();")

	actual override var time: Long
		get() {
			return (jsDate.getTime() as Number).toLong()
		}
		set(value) {
			val t = value.asDynamic().toNumber()
			jsDate.setTime(t)
		}

	actual override var fullYear: Int
		get() {
			return jsDate.getFullYear()
		}
		set(value) {
			jsDate.setFullYear(value)
		}

	actual override var monthIndex: Int
		get() = jsDate.getMonth()
		set(value) {
			jsDate.setMonth(value)
		}

	actual override var month: Int
		get() = monthIndex + 1
		set(value) {
			monthIndex = value - 1
		}

	actual override var dayOfMonth: Int
		get() = jsDate.getDate()
		set(value) {
			jsDate.setDate(value)
		}

	override val dayOfWeek: Int
		get() = jsDate.getDay()


	actual override var hour: Int
		get() = jsDate.getHours()
		set(value) {
			jsDate.setHours(value)
		}

	actual override var minute: Int
		get() = jsDate.getMinutes()
		set(value) {
			jsDate.setMinutes(value)
		}

	actual override var second: Int
		get() = jsDate.getSeconds()
		set(value) {
			jsDate.setSeconds(value)
		}

	actual override var milli: Int
		get() = jsDate.getMilliseconds()
		set(value) {
			jsDate.setMilliseconds(value)
		}

	actual override var utcFullYear: Int
		get() {
			return jsDate.getUTCFullYear()
		}
		set(value) {
			jsDate.setUTCFullYear(value)
		}

	actual override var utcMonthIndex: Int
		get() = jsDate.getUTCMonth()
		set(value) {
			jsDate.setUTCMonth(value)
		}

	actual override var utcMonth: Int
		get() = utcMonthIndex + 1
		set(value) {
			utcMonthIndex = value - 1
		}

	actual override var utcDayOfMonth: Int
		get() = jsDate.getUTCDate()
		set(value) {
			jsDate.setUTCDate(value)
		}

	override val utcDayOfWeek: Int
		get() = jsDate.getUTCDay()
	
	actual override var utcHour: Int
		get() = jsDate.getUTCHours()
		set(value) {
			jsDate.setUTCHours(value)
		}

	actual override var utcMinute: Int
		get() = jsDate.getUTCMinutes()
		set(value) {
			jsDate.setUTCMinutes(value)
		}

	actual override var utcSecond: Int
		get() = jsDate.getUTCSeconds()
		set(value) {
			jsDate.setUTCSeconds(value)
		}

	actual override var utcMilli: Int
		get() = jsDate.getUTCMilliseconds()
		set(value) {
			jsDate.setUTCMilliseconds(value)
		}

	override val timezoneOffset: Int
		get() = jsDate.getTimezoneOffset()

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

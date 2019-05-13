package com.acornui.core.time

import com.acornui.core.zeroPadding

/**
 * @author nbilyk
 */
actual class DateImpl : Date {

	internal val date = js("new Date();")

	override var time: Long
		get() {
			return (date.getTime() as Number).toLong()
		}
		set(value) {
			val t = value.asDynamic().toNumber()
			date.setTime(t)
		}

	override var fullYear: Int
		get() {
			return date.getFullYear()
		}
		set(value) {
			date.setFullYear(value)
		}

	override var monthIndex: Int
		get() = date.getMonth()
		set(value) {
			date.setMonth(value)
		}

	override var dayOfMonth: Int
		get() = date.getDate()
		set(value) {
			date.setDate(value)
		}

	override val dayOfWeek: Int
		get() = date.getDay()


	override var hour: Int
		get() = date.getHours()
		set(value) {
			date.setHours(value)
		}

	override var minute: Int
		get() = date.getMinutes()
		set(value) {
			date.setMinutes(value)
		}

	override var second: Int
		get() = date.getSeconds()
		set(value) {
			date.setSeconds(value)
		}

	override var milli: Int
		get() = date.getMilliseconds()
		set(value) {
			date.setMilliseconds(value)
		}

	override var utcFullYear: Int
		get() {
			return date.getUTCFullYear()
		}
		set(value) {
			date.setUTCFullYear(value)
		}

	override var utcMonthIndex: Int
		get() = date.getUTCMonth()
		set(value) {
			date.setUTCMonth(value)
		}

	override var utcDayOfMonth: Int
		get() = date.getUTCDate()
		set(value) {
			date.setUTCDate(value)
		}

	override val utcDayOfWeek: Int
		get() = date.getUTCDay()


	override var utcHour: Int
		get() = date.getUTCHours()
		set(value) {
			date.setUTCHours(value)
		}

	override var utcMinute: Int
		get() = date.getUTCMinutes()
		set(value) {
			date.setUTCMinutes(value)
		}

	override var utcSecond: Int
		get() = date.getUTCSeconds()
		set(value) {
			date.setUTCSeconds(value)
		}

	override var utcMilli: Int
		get() = date.getUTCMilliseconds()
		set(value) {
			date.setUTCMilliseconds(value)
		}

	override val timezoneOffset: Int
		get() = date.getTimezoneOffset()

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
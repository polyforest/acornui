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

package com.acornui.js.text

import com.acornui.collection.copy
import com.acornui.core.di.Injector
import com.acornui.core.di.Scoped
import com.acornui.core.di.inject
import com.acornui.core.i18n.I18n
import com.acornui.core.i18n.Locale
import com.acornui.core.text.DateTimeFormatStyle.*
import com.acornui.core.text.DateTimeFormatType
import com.acornui.core.text.DateTimeFormatter
import com.acornui.core.time.Date
import com.acornui.core.time.DateRo
import com.acornui.js.time.DateImpl
import com.acornui.reflect.observable
import kotlin.properties.ReadWriteProperty

class DateTimeFormatterImpl(override val injector: Injector) : DateTimeFormatter, Scoped {

	override var type by watched(DateTimeFormatType.DATE_TIME)
	override var timeStyle by watched(DEFAULT)
	override var dateStyle by watched(DEFAULT)
	override var timeZone: String? by watched(null)
	override var locales: List<Locale>? by watched(null)

	private var lastLocales: List<Locale> = listOf()
	private var _formatter: dynamic = null
	private val formatter: dynamic
		get() {
			if (locales == null && lastLocales != inject(I18n).currentLocales) {
				_formatter = null
				lastLocales = inject(I18n).currentLocales.copy()
			}
			if (_formatter == null) {
				val locales = (locales ?: lastLocales).map { it.value }
				val JsDateTimeFormat = js("Intl.DateTimeFormat")
				val options = js("({})")
				if (timeZone != null) {
					options.timeZone = timeZone
				}
				if (type == DateTimeFormatType.TIME || type == DateTimeFormatType.DATE_TIME) {
					if (timeStyle == FULL || timeStyle == LONG)
						options.timeZoneName = "short"
					options.hour = "numeric"
					options.minute = "numeric"
					if (timeStyle != SHORT) options.second = "numeric"
				}
				if (type == DateTimeFormatType.DATE || type == DateTimeFormatType.DATE_TIME) {
					if (dateStyle == FULL) options.weekday = "long"
					options.day = "numeric"
					options.month = when (dateStyle) {
						FULL -> "long"
						LONG -> "short"
						else -> "numeric"
					}
					options.year = if (dateStyle == SHORT) "2-digit" else "numeric"
				} else if (type == DateTimeFormatType.MONTH) {
					options.month = when (dateStyle) {
						FULL -> "long"
						LONG -> "short"
						else -> "numeric"
					}
				} else if (type == DateTimeFormatType.WEEKDAY) {
					options.weekday = when (dateStyle) {
						FULL, LONG -> "long"
						MEDIUM, SHORT -> "short"
						DEFAULT -> "long"
					}
				}

				_formatter = JsDateTimeFormat(locales.toTypedArray(), options)
			}
			return _formatter!!
		}

	override fun format(value: DateRo): String {
		value as DateImpl
		return formatter!!.format(value.date)
	}

	override fun parse(value: String): Date? {
		val date = js("new Date(Date.UTC(1110, 11, 12, 13, 14, 15, 16));")
		val regex = Regex("[^0-9]")
		val localizedOrder = (formatter!!.format(date) as String).replace(regex, " ").split(" ")
		val numberParts = value.replace(regex, " ").split(" ")
		if (localizedOrder.size != numberParts.size) return null
		val newDate = DateImpl()
		for (i in 0..numberParts.lastIndex) {
			val num = numberParts[i].toInt()
			when (localizedOrder[i]) {
				"1110" -> newDate.fullYear = num
				"11" -> newDate.monthIndex = num
				"12" -> newDate.dayOfMonth = num
				"13" -> newDate.hour = num
				"14" -> newDate.minute = num
				"15" -> newDate.second = num
				"16" -> newDate.milli = num
			}
		}
		return newDate
	}

	private fun <T> watched(initial: T): ReadWriteProperty<Any?, T> {
		return observable(initial) {
			_formatter = null
		}
	}
}
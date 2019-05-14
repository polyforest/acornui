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

package com.acornui.core.text


import com.acornui.collection.copy
import com.acornui.core.i18n.Locale
import com.acornui.core.text.DateTimeFormatStyle.*
import com.acornui.core.time.DateImpl
import com.acornui.core.time.DateRo
import com.acornui.core.userInfo
import com.acornui.reflect.observable
import kotlin.properties.ReadWriteProperty

actual class DateTimeFormatter : StringFormatter<DateRo> {

	actual var type by watched(DateTimeFormatType.DATE_TIME)
	actual var timeStyle by watched(DEFAULT)
	actual var dateStyle by watched(DEFAULT)
	actual var timeZone: String? by watched(null)
	actual var locales: List<Locale>? by watched(null)

	private var currentUserLocales: List<Locale> = listOf()
	private var _formatter: dynamic = null
	private val formatter: dynamic
		get() {
			if (locales == null && currentUserLocales != userInfo.currentLocale.value) {
				_formatter = null
				currentUserLocales = userInfo.currentLocale.value.copy()
			}
			if (_formatter == null) {
				val locales = (locales ?: currentUserLocales).map { it.value }
				@Suppress("LocalVariableName")
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
				} else if (type == DateTimeFormatType.YEAR) {
					options.year = when (dateStyle) {
						FULL, LONG -> "numeric"
						else -> "2-digit"
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
		return formatter!!.format(value.date) as String
	}

	private fun <T> watched(initial: T): ReadWriteProperty<Any?, T> {
		return observable(initial) {
			_formatter = null
		}
	}
}

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
import com.acornui.core.time.Date
import com.acornui.core.text.DateTimeFormatType
import kotlin.properties.Delegates
import kotlin.properties.ReadWriteProperty
import com.acornui.core.text.DateTimeFormatStyle.FULL
import com.acornui.core.text.DateTimeFormatStyle.LONG
import com.acornui.core.text.DateTimeFormatStyle.SHORT
import com.acornui.core.text.DateTimeFormatStyle.DEFAULT
import com.acornui.core.text.DateTimeFormatter
import com.acornui.js.time.DateImpl
import com.acornui.reflect.observable

class DateTimeFormatterImpl(override val injector: Injector) : DateTimeFormatter, Scoped {

	override var type by watched(DateTimeFormatType.DATE_TIME)
	override var timeStyle by watched(DEFAULT)
	override var dateStyle by watched(DEFAULT)
	override var timeZone: String? by watched(null)
	override var locales: List<Locale>? by watched(null)

	private var lastLocales: List<Locale> = listOf()
	private var formatter: dynamic = null

	override fun format(value: Date): String {
		value as DateImpl
		if (locales == null && lastLocales != inject(I18n).currentLocales) {
			formatter = null
			lastLocales = inject(I18n).currentLocales.copy()
		}
		if (formatter == null) {
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
			}

			formatter = JsDateTimeFormat(locales.joinToString(","), options)

		}

		return formatter!!.format(value.date)
	}


	private fun <T> watched(initial: T): ReadWriteProperty<Any?, T> {
		return observable(initial) {
			formatter = null
		}
	}
}
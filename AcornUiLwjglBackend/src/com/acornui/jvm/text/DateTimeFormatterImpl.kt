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

package com.acornui.jvm.text

import com.acornui.collection.copy
import com.acornui.core.di.Injector
import com.acornui.core.di.Scoped
import com.acornui.core.di.inject
import com.acornui.core.i18n.I18n
import com.acornui.core.i18n.Locale
import com.acornui.core.text.DateTimeFormatStyle
import com.acornui.core.text.DateTimeFormatType
import com.acornui.core.text.DateTimeFormatter
import com.acornui.core.time.Date
import com.acornui.core.time.DateRo
import com.acornui.jvm.time.DateImpl
import com.acornui.logging.Log
import com.acornui.reflect.observable
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.properties.ReadWriteProperty
import java.util.Locale as JvmLocale

class DateTimeFormatterImpl(override val injector: Injector) : DateTimeFormatter, Scoped {

	override var type by watched(DateTimeFormatType.DATE_TIME)
	override var timeStyle by watched(DateTimeFormatStyle.DEFAULT)
	override var dateStyle by watched(DateTimeFormatStyle.DEFAULT)
	override var timeZone: String? by watched(null)
	override var locales: List<Locale>? by watched(null)

	private var lastLocales: List<Locale> = listOf()
	private var _formatter: DateFormat? = null
	private val formatter: DateFormat
		get() {
			if (locales == null && lastLocales != inject(I18n).currentLocales) {
				_formatter = null
				lastLocales = inject(I18n).currentLocales.copy()
			}
			if (_formatter == null) {
				val locales = locales ?: lastLocales
				for (locale in locales) {
					val jvmLocale = java.util.Locale.Builder().setLanguageTag(locale.value).build()
					_formatter = getFormatterForLocale(jvmLocale)
					if (_formatter != null) break
				}
				if (_formatter == null) {
					Log.warn("Could not create a date formatter for the current language chain.")
					_formatter = getFormatterForLocale(java.util.Locale.getDefault())
				}
				_formatter!!.timeZone = if (timeZone == null) TimeZone.getDefault() else TimeZone.getTimeZone(timeZone)
			}
			return _formatter!!
		}

	override fun format(value: DateRo): String {
		value as DateImpl
		return formatter.format(value.time)
	}

	private fun getFormatterForLocale(jvmLocale: java.util.Locale): DateFormat {
		return when (type) {
			DateTimeFormatType.DATE -> DateFormat.getDateInstance(dateStyle.toInt(), jvmLocale)
			DateTimeFormatType.TIME -> DateFormat.getTimeInstance(timeStyle.toInt(), jvmLocale)
			DateTimeFormatType.DATE_TIME -> DateFormat.getDateTimeInstance(dateStyle.toInt(), timeStyle.toInt(), jvmLocale)
			DateTimeFormatType.MONTH -> SimpleDateFormat(when (dateStyle) {
				DateTimeFormatStyle.FULL -> "MMMMM"
				DateTimeFormatStyle.LONG -> "MMM"
				DateTimeFormatStyle.MEDIUM -> "MM"
				DateTimeFormatStyle.SHORT -> "M"
				DateTimeFormatStyle.DEFAULT -> "MM"
			})
			DateTimeFormatType.WEEKDAY -> SimpleDateFormat(when (dateStyle) {
				DateTimeFormatStyle.FULL, DateTimeFormatStyle.LONG -> "EEEEE"
				DateTimeFormatStyle.MEDIUM -> "EEE"
				DateTimeFormatStyle.SHORT -> "EE"
				DateTimeFormatStyle.DEFAULT -> "EE"
			})
		}
	}

	private fun DateTimeFormatStyle.toInt(): Int {
		return when (this) {
			DateTimeFormatStyle.FULL -> DateFormat.FULL
			DateTimeFormatStyle.LONG -> DateFormat.LONG
			DateTimeFormatStyle.MEDIUM -> DateFormat.MEDIUM
			DateTimeFormatStyle.SHORT -> DateFormat.SHORT
			DateTimeFormatStyle.DEFAULT -> DateFormat.DEFAULT
		}
	}

	private fun <T> watched(initial: T): ReadWriteProperty<Any?, T> {
		return observable(initial) {
			_formatter = null
		}
	}

	override fun parse(value: String): Date? {
		return try {
			val date = formatter.parse(value)
			val d = DateImpl()
			d.time = date.time
			d
		} catch (e: ParseException) {
			null
		}
	}
}
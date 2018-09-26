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

package com.acornui.core.text

import com.acornui.core.di.*
import com.acornui.core.i18n.Locale
import com.acornui.core.time.Date
import com.acornui.core.time.DateRo

/**
 * This class formats dates into localized string representations.
 */
interface DateTimeFormatter : StringFormatter<DateRo>, StringParser<Date> {

	/**
	 * Whether this should format the [Date] object as time, date, or date and time.
	 */
	var type: DateTimeFormatType

	/**
	 * The exact format of a date or time is dependent on the locale, and is platform specific, but this property will
	 * hint whether the format will be long form, medium form, or short form.
	 * The default is [DateTimeFormatStyle.DEFAULT]
	 */
	var timeStyle: DateTimeFormatStyle

	/**
	 * The exact format of a date or time is dependent on the locale, and is platform specific, but this property will
	 * hint whether the format will be long form, medium form, or short form.
	 * The default is [DateTimeFormatStyle.DEFAULT]
	 */
	var dateStyle: DateTimeFormatStyle

	/**
	 * The time zone for formatting.
	 * The only values this is guaranteed to work with are "UTC" or null.
	 * Other values that will likely work based on browser or jvm implementation are the full TZ code
	 * [https://en.wikipedia.org/wiki/List_of_tz_database_time_zones]
	 * For example, use "America/New_York" as opposed to "EST"
	 * If this is null, the user's timezone will be used.
	 */
	var timeZone: String?

	/**
	 * The ordered locale chain to use for formatting. If this is left null, the user's current locale will be used.
	 *
	 * See [https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl#Locale_identification_and_negotiation]
	 */
	var locales: List<Locale>?

	companion object {
		val FACTORY_KEY = dKey<(injector: Injector) -> DateTimeFormatter>()
	}
}

enum class DateTimeFormatStyle {
	FULL,
	LONG,
	MEDIUM,
	SHORT,
	DEFAULT
}

enum class DateTimeFormatType {
	DATE,
	MONTH,
	WEEKDAY,
	TIME,
	DATE_TIME
}

fun Scoped.dateFormatter(init: DateTimeFormatter.() -> Unit = {}): DateTimeFormatter {
	return inject(DateTimeFormatter.FACTORY_KEY)(injector).apply {
		type = DateTimeFormatType.DATE
		init()
	}
}

fun Scoped.dateTimeFormatter(init: DateTimeFormatter.() -> Unit = {}): DateTimeFormatter {
	return inject(DateTimeFormatter.FACTORY_KEY)(injector).apply {
		type = DateTimeFormatType.DATE_TIME
		init()
	}
}

fun Scoped.timeFormatter(init: DateTimeFormatter.() -> Unit = {}): DateTimeFormatter {
	return inject(DateTimeFormatter.FACTORY_KEY)(injector).apply {
		type = DateTimeFormatType.TIME
		init()
	}
}

fun Scoped.dateParser(): DateTimeFormatter = dateFormatter()

fun Scoped.dateTimeParser(): DateTimeFormatter = dateTimeFormatter()

fun Scoped.timeParser(): DateTimeFormatter = timeFormatter()
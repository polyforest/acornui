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

package com.acornui.formatters

import com.acornui.i18n.Locale
import com.acornui.properties.afterChange
import com.acornui.system.userInfo
import kotlin.properties.ReadWriteProperty

/**
 * This class formats numbers into localized string representations.
 */
class NumberFormatter() : StringFormatter<Number?>, StringParser<Double> {

	var type by watched(NumberFormatType.NUMBER)

	/**
	 * The ordered locale chain to use for formatting. If this is left null, the user's current locale will be used.
	 *
	 * See [https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl#Locale_identification_and_negotiation]
	 */
	var locales: List<Locale>? by watched(null)

	var minIntegerDigits: Int by watched(1)
	var maxIntegerDigits: Int by watched(40)
	var minFractionDigits: Int by watched(0)
	var maxFractionDigits: Int by watched(3)

	/**
	 * Whether to use grouping separators, such as thousands separators or thousand/lakh/crore separators.
	 * Default is true.
	 */
	var useGrouping: Boolean by watched(true)

	/**
	 * The ISO 4217 code of the currency.
	 * Used only if [type] == [NumberFormatType.CURRENCY]
	 */
	var currencyCode: String by watched("USD")

	private var formatter: dynamic = null

	override fun format(value: Number?): String {
		if (value == null) return ""
		if (formatter == null) {
			val locales = (locales ?: userInfo.systemLocale).map { it.value }
			val JsNumberFormat = js("Intl.NumberFormat")
			val options = js("({})")

			options.minimumIntegerDigits = minIntegerDigits
			options.maximumIntegerDigits = maxIntegerDigits
			options.minimumFractionDigits = minFractionDigits
			options.maximumFractionDigits = maxFractionDigits
			options.useGrouping = useGrouping
			if (type == NumberFormatType.CURRENCY) {
				options.style = "currency"
				options.currency = currencyCode
			} else if (type == NumberFormatType.PERCENT) {
				options.style = "percent"
			}

			formatter = JsNumberFormat(locales.toTypedArray(), options)
		}
		return formatter!!.format(value)
	}

	private fun <T> watched(initial: T): ReadWriteProperty<Any?, T> {
		return afterChange(initial) {
			formatter = null
		}
	}

	override fun parse(value: String): Double? {
		val thousandSeparator = format(1111).replace("1", "")
		val decimalSeparator = format(1.1).replace("1", "")
		return value.replace(thousandSeparator, "").replace(decimalSeparator, ".").toDoubleOrNull()
	}
}

enum class NumberFormatType {
	NUMBER,
	CURRENCY,
	PERCENT
}

fun numberFormatter(init: NumberFormatter.() -> Unit = {}): NumberFormatter {
	val formatter = NumberFormatter()
	formatter.init()
	return formatter
}

/**
 * @pstsm currencyCode the ISO 4217 code of the currency
 */
fun currencyFormatter(currencyCode: String, init: NumberFormatter.() -> Unit = {}): NumberFormatter {
	return NumberFormatter().apply {
		type = NumberFormatType.CURRENCY
		minFractionDigits = 2
		this.currencyCode = currencyCode
		init()
	}
}

/**
 * Percent formatter will format a number as a percent value.
 * E.g. 0.23 will be formatted as 23%
 */
fun percentFormatter(init: NumberFormatter.() -> Unit = {}): NumberFormatter {
	return NumberFormatter().apply {
		type = NumberFormatType.PERCENT
		init()
	}
}

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

import com.acornui.core.di.Injector
import com.acornui.core.di.Scoped
import com.acornui.core.di.dKey
import com.acornui.core.di.inject
import com.acornui.core.i18n.Locale

/**
 * This class formats numbers into localized string representations.
 */
interface NumberFormatter : StringFormatter<Number?>, StringParser<Double> {

	var type: NumberFormatType

	var minIntegerDigits: Int
	var maxIntegerDigits: Int

	var minFractionDigits: Int
	var maxFractionDigits: Int

	/**
	 * Whether to use grouping separators, such as thousands separators or thousand/lakh/crore separators.
	 * Default is true.
	 */
	var useGrouping: Boolean

	/**
	 * The ISO 4217 code of the currency.
	 * Used only if [type] == [NumberFormatType.CURRENCY]
	 */
	var currencyCode: String

	/**
	 * The ordered locale chain to use for formatting. If this is left null, the user's current locale will be used.
	 *
	 * See [https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl#Locale_identification_and_negotiation]
	 */
	var locales: List<Locale>?

	override fun parse(value: String): Double? {
		val thousandSeparator = format(1111).replace("1", "")
		val decimalSeparator = format(1.1).replace("1", ".")
		return value.replace(thousandSeparator, "").replace(decimalSeparator, ".").toDoubleOrNull()
	}

	companion object {
		val FACTORY_KEY = dKey<(injector: Injector) -> NumberFormatter>()
	}
}

enum class NumberFormatType {
	NUMBER,
	CURRENCY,
	PERCENT
}

fun Scoped.numberFormatter(): NumberFormatter {
	return inject(NumberFormatter.FACTORY_KEY)(injector)
}

/**
 * @pstsm currencyCode the ISO 4217 code of the currency
 */
fun Scoped.currencyFormatter(currencyCode: String): NumberFormatter {
	return inject(NumberFormatter.FACTORY_KEY)(injector).apply {
		type = NumberFormatType.CURRENCY
		minFractionDigits = 2
		this.currencyCode = currencyCode
	}
}

/**
 * Percent formatter will format a number as a percent value.
 * E.g. 0.23 will be formatted as 23%
 */
fun Scoped.percentFormatter(): NumberFormatter {
	return inject(NumberFormatter.FACTORY_KEY)(injector).apply {
		type = NumberFormatType.PERCENT
	}
}
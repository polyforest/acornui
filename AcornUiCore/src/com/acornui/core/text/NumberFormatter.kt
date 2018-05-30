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
import com.acornui.math.fpart
import kotlin.math.pow

/**
 * This class formats numbers into localized string representations.
 */
interface NumberFormatter : StringFormatter<Number?> {

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

fun Scoped.intFormatter(): NumberFormatter {
	return numberFormatter().apply {
		maxFractionDigits = 0
	}
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
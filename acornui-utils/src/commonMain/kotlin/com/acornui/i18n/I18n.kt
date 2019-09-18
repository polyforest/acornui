package com.acornui.i18n

import com.acornui.logging.Log
import com.acornui.text.getMonths

/**
 * An object representing a locale key.
 * In the future, this may be parsed into lang, region, variant, etc.
 *
 * @param value The locale key. E.g. en-US
 */
data class Locale(val value: String)

val isI18nSupported: Boolean by lazy {
	val r = try {
		val months = getMonths(longFormat = true, locales = listOf(Locale("es")))
		months.firstOrNull() == "enero"
	} catch (e: Throwable) {
		false
	}
	if (!r) Log.warn("i18n is not supported for this platform.")
	r
}
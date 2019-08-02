package com.acornui.i18n

/**
 * An object representing a locale key.
 * In the future, this may be parsed into lang, region, variant, etc.
 *
 * @param value The locale key. E.g. en-US
 */
data class Locale(val value: String)
package com.acornui.core.text

interface StringFormatter<in T> {

	/**
	 * Converts the given value into a String.
	 */
	fun format(value: T): String
}

interface StringParser<out T> {

	/**
	 * @return Returns the parsed value, or null if it could not be parsed.
	 */
	fun parse(value: String): T?
}

object ToStringFormatter : StringFormatter<Any> {
	override fun format(value: Any): String {
		return value.toString()
	}
}


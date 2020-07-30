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

interface StringFormatter<in T> {

	/**
	 * Converts the given value into a String.
	 */
	fun format(value: T): String
}

class WithNullFormatter<in T>(
		private val inner: StringFormatter<T>,
		var nullString: String = ""
) : StringFormatter<T?> {
	override fun format(value: T?): String = if (value == null) nullString else inner.format(value)
}

fun <T> StringFormatter<T>.withNull(nullString: String = "") = WithNullFormatter(this, nullString)

interface StringParser<out T> {

	/**
	 * @return Returns the parsed value, or null if it could not be parsed.
	 */
	fun parse(value: String): T?
}

object ToStringFormatter : StringFormatter<Any?> {
	override fun format(value: Any?): String {
		return value.toString()
	}
}

// TODO: Remove in 1.4 with SAM conversions
fun <E> stringFormatter(valueToString: (E) -> String): StringFormatter<E> = object : StringFormatter<E> {
	override fun format(value: E): String {
		return valueToString(value)
	}
}
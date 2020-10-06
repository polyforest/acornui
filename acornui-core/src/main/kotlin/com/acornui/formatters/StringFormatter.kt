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

fun interface StringFormatter<in T> {

	/**
	 * Converts the given value into a String.
	 */
	fun format(value: T): String
}

fun interface StringParser<out T> {

	/**
	 * @return Returns the parsed value, or null if it could not be parsed.
	 */
	fun parse(value: String): T?
}

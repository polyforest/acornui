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

package com.acornui.browser

import kotlinx.serialization.Serializable

fun String.toUrlParams(): UrlParams {
	val items = ArrayList<Pair<String, String>>()
	val split = split("&")
	for (entry in split) {
		val i = entry.indexOf("=")
		if (i != -1)
			items.add(entry.substring(0, i) to decodeUriComponent2(entry.substring(i + 1)))
	}
	return UrlParams(items)
}

@Serializable
data class UrlParams(val items: List<Pair<String, String>>) {

	/**
	 * Retrieves the first parameter with the given name.
	 */
	fun get(name: String): String? {
		return items.firstOrNull { it.first == name }?.second
	}

	/**
	 * Retrieves all [items] with the given name.
	 */
	fun getAll(name: String): List<String> {
		return items.filter { it.first == name }.map { it.second }
	}

	fun contains(name: String): Boolean {
		return items.firstOrNull { it.first == name } != null
	}

	fun toQueryString(): String {
		val result = StringBuilder()
		for ((key, value) in items) {
			result.append(encodeUriComponent2(key))
			result.append("=")
			result.append(encodeUriComponent2(value))
			result.append("&")
		}
		val resultString = result.toString()
		return if (resultString.isNotEmpty())
			resultString.substring(0, resultString.length - 1)
		else
			resultString
	}
}

expect fun encodeUriComponent2(str: String): String
expect fun decodeUriComponent2(str: String): String

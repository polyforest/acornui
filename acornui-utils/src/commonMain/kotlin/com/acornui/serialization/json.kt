/*
 * Copyright 2015 Nicholas Bilyk
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

@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.acornui.serialization

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration

@Deprecated("use kotlinx serialization")
expect val json: Serializer<String>

@Deprecated("use kotlinx serialization")
fun <T> parseJson(jsonStr: String, factory: From<T>): T {
	return json.read(jsonStr, factory)
}

@Deprecated("use kotlinx serialization")
fun <T> toJson(value: T, factory: To<T>): String {
	return json.write(value, factory)
}

private val jsonx = Json(JsonConfiguration.Default.copy(encodeDefaults = false), context = dataModule)

fun <T> jsonParse(deserializer: DeserializationStrategy<T>, jsonStr: String): T {
	return jsonx.parse(deserializer, jsonStr)
}

fun <T> jsonStringify(serializer: SerializationStrategy<T>, value: T): String {
	return jsonx.stringify(serializer, value)
}
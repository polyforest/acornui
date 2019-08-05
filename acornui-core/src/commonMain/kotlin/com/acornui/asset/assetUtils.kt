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

package com.acornui.asset

import com.acornui.async.Deferred
import com.acornui.async.async
import com.acornui.di.Scoped
import com.acornui.di.inject
import com.acornui.io.toByteArray
import com.acornui.serialization.From
import com.acornui.serialization.parseBinary
import com.acornui.serialization.parseJson
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json

/**
 * A Collection of utilities for making common asset loading tasks more terse.
 */

/**
 * Loads a json file, then parses it into the target.
 * This is not cached.
 */
@Deprecated("use kotlinx serialization")
fun <T> Scoped.loadJson(path:String, factory: From<T>): Deferred<T> = async {
	val json = inject(AssetManager).load(path, AssetType.TEXT)
	parseJson(json.await(), factory)
}

fun <T> Scoped.loadJson(path:String, deserializer: DeserializationStrategy<T>): Deferred<T> = async {
	val json = inject(AssetManager).load(path, AssetType.TEXT)
	Json.parse(deserializer, json.await())
}

@Deprecated("use kotlinx serialization")
fun <T> Scoped.loadBinary(path: String, factory: From<T>): Deferred<T> = async {
	val binary = inject(AssetManager).load(path, AssetType.BINARY)
	parseBinary(binary.await(), factory)
}

fun <T> Scoped.loadBinary(path: String, deserializer: DeserializationStrategy<T>): Deferred<T> = async {
	val binary = inject(AssetManager).load(path, AssetType.BINARY)
	parseBinary(binary.await().toByteArray(), deserializer)
}
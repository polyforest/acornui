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

package com.acornui.asset

import com.acornui.di.Context
import com.acornui.io.UrlRequestData
import com.acornui.io.toUrlRequestData
import com.acornui.serialization.jsonParse
import kotlinx.coroutines.Deferred
import kotlinx.serialization.DeserializationStrategy

suspend fun <R : Any> Context.loadAndCacheJson(deserializer: DeserializationStrategy<R>, path: String, group: CacheSet = cacheSet()): R =
		loadAndCacheJsonAsync(deserializer, path.toUrlRequestData(), group).await()

suspend fun <R : Any> Context.loadAndCacheJson(deserializer: DeserializationStrategy<R>, request: UrlRequestData, group: CacheSet = cacheSet()): R =
		loadAndCacheJsonAsync(deserializer, request, group).await()

fun <R : Any> Context.loadAndCacheJsonAsync(deserializer: DeserializationStrategy<R>, path: String, group: CacheSet = cacheSet()): Deferred<R> =
		loadAndCacheJsonAsync(deserializer, path, group)

fun <R : Any> Context.loadAndCacheJsonAsync(deserializer: DeserializationStrategy<R>, request: UrlRequestData, group: CacheSet = cacheSet()): Deferred<R> {
	return group.getOrPutAsync(request) {
		jsonParse(deserializer, loadText(request))
	}
}
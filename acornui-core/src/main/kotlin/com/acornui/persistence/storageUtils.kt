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

package com.acornui.persistence

import com.acornui.logging.Log
import com.acornui.serialization.jsonParse
import kotlinx.serialization.DeserializationStrategy
import org.w3c.dom.Storage

/**
 * A utility method for retrieving an item from the persistence map and deserializing it.
 * This will return null if there was an error in deserialization.
 */
fun <T> Storage.getItem(key: String, deserializationStrategy: DeserializationStrategy<T>, errorHandler: (e: Throwable) -> Unit = Log::error): T? {
	if (!containsItem(key)) return null
	return try {
		jsonParse(deserializationStrategy, getItem(key)!!)
	} catch (e: Throwable) {
		errorHandler(e)
		null
	}
}

fun Storage.containsItem(key: String): Boolean = getItem(key) != null
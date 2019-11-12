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

import com.acornui.Version
import com.acornui.di.DKey
import com.acornui.logging.Log
import com.acornui.recycle.Clearable
import com.acornui.serialization.jsonParse
import kotlinx.serialization.DeserializationStrategy

interface Persistence : Clearable {

	/**
	 * The version of the application as described in [com.acornui.version] when the persistence was
	 * last saved. This can be useful for storage migration.
	 * This will be null if there was nothing loaded.
	 */
	val version: Version?

	val allowed: Boolean

	/**
	 * Returns an integer representing the number of data items stored.
	 */
	val length: Int

	/**
	 * Returns the key at the specified index.
	 */
	fun key(index: Int): String?

	fun getItem(key: String): String?
	fun containsItem(key: String): Boolean = getItem(key) != null
	fun setItem(key: String, value: String)
	fun removeItem(key: String)
	override fun clear()

	/**
	 * Ensures that the persistence is written to disk. Note that not all implementations necessarily wait for this.
	 */
	fun flush()

	companion object : DKey<Persistence>
}

/**
 * A utility method for retrieving an item from the persistence map and deserializing it.
 * This will return null if there was an error in deserialization.
 */
fun <T> Persistence.getItem(key: String, deserializationStrategy: DeserializationStrategy<T>, errorHandler: (e: Throwable) -> Unit = Log::error): T? {
	if (!containsItem(key)) return null
	return try {
		jsonParse(deserializationStrategy, getItem(key)!!)
	} catch (e: Throwable) {
		errorHandler(e)
		null
	}
}
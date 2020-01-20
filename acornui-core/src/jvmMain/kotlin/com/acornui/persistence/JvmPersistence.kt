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

@file:Suppress("LoopToCallChain")

package com.acornui.persistence

import com.acornui.Version
import com.acornui.logging.Log
import com.acornui.serialization.jsonParse
import com.acornui.serialization.jsonStringify
import kotlinx.serialization.Serializable
import java.io.File

open class JvmPersistence(
		private val currentVersion: Version,
		name: String,
		persistenceDir: File = File(System.getProperty("user.home") + "/.prefs")
) : Persistence {

	private val file = File(persistenceDir, "$name.data")
	private val data: PersistenceData

	override val version: Version?
		get() = data.version

	init {
		Log.info("Preferences location: ${file.absolutePath}")
		file.parentFile.mkdirs()

		data = if (file.exists()) {
			// Load the saved data.
			val jsonData = file.readText()
			jsonParse(PersistenceData.serializer(), jsonData)
		} else {
			PersistenceData()
		}
	}

	override val allowed: Boolean = true
	override val length: Int
		get() = data.size

	override fun key(index: Int): String? {
		if (index >= data.size) return null
		for ((c, key) in data.keys.withIndex()) {
			if (c == index) return key
		}
		return null
	}

	override fun getItem(key: String): String? {
		return data[key]
	}

	override fun setItem(key: String, value: String) {
		data[key] = value
		write()
	}

	override fun removeItem(key: String) {
		data.remove(key)
		write()
	}

	override fun clear() {
		data.clear()
		file.delete()
	}

	private fun write() {
		data.version = currentVersion
		val jsonStr = jsonStringify(PersistenceData.serializer(), data)
		file.writeText(jsonStr)
	}
}

@Serializable
private data class PersistenceData(
		val map: MutableMap<String, String> = HashMap(),
		var version: Version? = null
) : MutableMap<String, String> by map
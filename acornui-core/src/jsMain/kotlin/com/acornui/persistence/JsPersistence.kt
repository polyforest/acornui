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
import com.acornui.logging.Log
import kotlin.browser.localStorage

class JsPersistence(private val currentVersion: Version) : Persistence {

	private var _version: Version?

	override val version: Version?
		get() = _version

	private val storageAllowed: Boolean = js("typeof(Storage) !== \"undefined\"") as Boolean

	private var currentVersionWritten: Boolean = false

	init {
		if (!storageAllowed) Log.warn("Storage not allowed.")
		val versionStr = getItem("__version")
		_version = if (versionStr == null)
			null
		else
			Version.fromStr(versionStr)
	}

	override val allowed: Boolean
		get() = storageAllowed

	override val length: Int
		get() {
			if (!storageAllowed) return 0
			return localStorage.length
		}

	override fun key(index: Int): String? {
		if (!storageAllowed) return null
		return localStorage.key(index)
	}

	override fun getItem(key: String): String? {
		if (!storageAllowed) return null
		return localStorage.getItem(key)
	}

	override fun setItem(key: String, value: String) {
		if (!storageAllowed) return
		localStorage.setItem(key, value)
		if (!currentVersionWritten) {
			currentVersionWritten = true
			localStorage.setItem("__version", currentVersion.toVersionString())
			_version = currentVersion
		}
	}

	override fun removeItem(key: String) {
		if (!storageAllowed) return
		localStorage.removeItem(key)
	}

	override fun clear() {
		if (!storageAllowed) return
		localStorage.clear()
	}

	override fun flush() {
	}
}

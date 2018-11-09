package com.acornui.js.persistance

import com.acornui.core.Version
import com.acornui.core.persistance.Persistence
import kotlin.browser.localStorage

class JsPersistence(private val currentVersion: Version) : Persistence {

	private var _version: Version?

	override val version: Version?
		get() = _version

	private val storageAllowed: Boolean = js("typeof(Storage) !== \"undefined\"") as Boolean

	private var currentVersionWritten: Boolean = false

	init {
		if (!storageAllowed) println("Storage not allowed.")
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
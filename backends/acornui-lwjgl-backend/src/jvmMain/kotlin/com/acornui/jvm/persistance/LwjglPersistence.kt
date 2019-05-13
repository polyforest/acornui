@file:Suppress("LoopToCallChain")

package com.acornui.jvm.persistance

import com.acornui.collection.stringMapOf
import com.acornui.core.Version
import com.acornui.core.persistance.Persistence
import com.acornui.serialization.*
import java.io.File

open class LwjglPersistence(
		private val currentVersion: Version,
		name: String,
		persistenceDir: String = System.getProperty("user.home") + "/.prefs"
) : Persistence {

	private val file = File(persistenceDir, name + ".data")
	private val data: PersistenceData

	override val version: Version?
		get() = data.version

	init {
		println("Prefs location ${file.absolutePath}")
		file.parentFile.mkdirs()

		if (file.exists()) {
			// Load the saved data.
			val jsonData = file.readText()
			data = json.read(jsonData, PersistenceDataSerializer)
		} else {
			data = PersistenceData()
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
	}

	override fun removeItem(key: String) {
		data.remove(key)
	}

	override fun clear() {
		data.clear()
	}

	override fun flush() {
		data.version = currentVersion
		val jsonStr = json.write(data, PersistenceDataSerializer)
		file.writeText(jsonStr)
	}
}

private class PersistenceData(
		val map: MutableMap<String, String> = HashMap(),
		var version: Version? = null

) : MutableMap<String, String> by map

private object PersistenceDataSerializer : From<PersistenceData>, To<PersistenceData> {

	override fun read(reader: Reader): PersistenceData {
		val versionStr = reader.string("version")
		val version = if (versionStr == null) null else Version.fromStr(versionStr)

		val map = stringMapOf<String>()
		reader["map"]?.forEach { s, reader2 ->
			map[s] = reader2.string()!!
		}
		return PersistenceData(map, version)
	}

	override fun PersistenceData.write(writer: Writer) {
		writer.string("version", version?.toVersionString())
		writer.obj("map", true) {
			for ((key, value) in map) {
				it.string(key, value)
			}
		}
	}
}

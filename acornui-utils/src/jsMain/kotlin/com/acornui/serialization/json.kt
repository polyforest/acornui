/*
 * Copyright 2018 Nicholas Bilyk
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

package com.acornui.serialization

import com.acornui.core.addBackslashes

/**
 * A factory that provides a Reader and Writer for JSON
 * @author nbilyk
 */
private object JsonSerializer : Serializer<String> {

	override fun read(data: String): Reader {
		return JsonNode(JSON.parse(data))
	}

	override fun write(callback: (Writer) -> Unit): String = write(callback, "\t", "\n")

	fun write(callback: (Writer) -> Unit, tabStr: String, returnStr: String): String {
		val buffer = StringBuilder()
		val writer = JsonWriter(buffer, "", tabStr, returnStr)
		callback(writer)
		return buffer.toString()
	}
}

/**
 * @author nbilyk
 */
class JsonNode(private val source: dynamic
) : Reader {

	private val _properties: Map<String, Reader> by lazy {
		val m = stringMapOf<Reader>()
		if (source != null) {
			for (name in keys(source)) {
				if (source.hasOwnProperty(name) == true) {
					m[name] = JsonNode(source[name])
				}
			}
		}
		m
	}

	private val _elements: List<Reader> by lazy {
		val e = ArrayList<Reader>()
		if (source != null) {
			for (element in source) {
				e.add(JsonNode(element))
			}
		}
		e
	}

	override fun properties(): Map<String, Reader> {
		return _properties
	}

	override fun elements(): List<Reader> {
		return _elements
	}

	override val isNull: Boolean
		get() = source == null

	override fun byte(): Byte? {
		return source as? Byte?
	}

	override fun bool(): Boolean? {
		return source as? Boolean?
	}

	override fun char(): Char? {
		return source as? Char?
	}

	override fun string(): String? {
		return source as? String?
	}

	override fun short(): Short? {
		return source as? Short?
	}

	override fun int(): Int? {
		return source as? Int?
	}

	override fun long(): Long? {
		return (source as? Number)?.toLong()
	}

	override fun float(): Float? {
		return source as? Float?
	}

	override fun double(): Double? {
		return source as? Double?
	}

	override fun byteArray(): ByteArray? {
		if (source == null) return null
		return base64.decodeFromString(source as String)
	}

	override fun toString(): String {
		return source.toString()
	}

	@Suppress("NOTHING_TO_INLINE")
	private inline fun keys(json: dynamic) = js("Object").keys(json).unsafeCast<Array<String>>()
}

/**
 * A simple JSON writer
 */
class JsonWriter(
		val builder: StringBuilder,
		val indentStr: String,
		val tabStr: String,
		val returnStr: String
) : Writer {

	private var size: Int = 0

	override fun property(name: String): Writer {
		if (size++ > 0)
			builder.append(",$returnStr")

		builder.append(indentStr)
		builder.append('"')
		builder.append(name)
		builder.append("\": ")
		return JsonWriter(builder, indentStr, tabStr, returnStr)
	}

	override fun element(): Writer {
		if (size++ > 0)
			builder.append(",$returnStr")
		builder.append(indentStr)
		return JsonWriter(builder, indentStr, tabStr, returnStr)
	}

	override fun byte(value: Byte?) {
		if (value == null) return writeNull()
		builder.append(value)
	}

	override fun bool(value: Boolean?) {
		if (value == null) return writeNull()
		if (value) builder.append("true")
		else builder.append("false")
	}

	override fun string(value: String?) {
		if (value == null) return writeNull()
		builder.append('"')
		builder.append(addBackslashes(value))
		builder.append('"')
	}

	override fun int(value: Int?) {
		if (value == null) return writeNull()
		builder.append(value)
	}

	override fun long(value: Long?) {
		if (value == null) return writeNull()
		builder.append(value)
	}

	override fun float(value: Float?) {
		if (value == null) return writeNull()
		builder.append(value)
	}

	override fun double(value: Double?) {
		if (value == null) return writeNull()
		builder.append(value)
	}

	override fun char(value: Char?) {
		if (value == null) return writeNull()
		builder.append('"')
		builder.append(value)
		builder.append('"')
	}

	override fun obj(complex: Boolean, contents: (Writer) -> Unit) {
		val r = if (complex) returnStr else " "
		builder.append("{$r")
		val childIndent = if (complex) indentStr + tabStr else ""
		val childWriter = JsonWriter(builder, childIndent, tabStr, r)
		contents(childWriter)
		if (childWriter.size > 0) {
			builder.append(r)
		}
		if (complex) builder.append(indentStr)
		builder.append('}')
	}

	override fun array(complex: Boolean, contents: (Writer) -> Unit) {
		val r = if (complex) returnStr else " "
		builder.append("[$r")
		val childIndent = if (complex) indentStr + tabStr else ""
		val childWriter = JsonWriter(builder, childIndent, tabStr, r)
		contents(childWriter)
		if (childWriter.size > 0) {
			builder.append(r)
		}
		if (complex) builder.append(indentStr)
		builder.append(']')
	}

	override fun byteArray(value: ByteArray?) {
		if (value == null) return writeNull()
		string(base64.encodeToString(value))
	}

	override fun writeNull() {
		builder.append("null")
	}
}

actual val json: Serializer<String> = JsonSerializer
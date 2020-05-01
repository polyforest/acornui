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

@file:Suppress("DEPRECATION")

package com.acornui.serialization

import com.acornui.collection.peek
import com.acornui.collection.pop
import com.acornui.addBackslashes
import com.acornui.removeBackslashes
import com.acornui.string.SubString

/**
 * A factory that provides a Reader and Writer for JSON
 * @author nbilyk
 */
private object JsonSerializer : Serializer<String> {

	override fun read(data: String): Reader {
		return JsonNode(data, 0, data.length)
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
class JsonNode(private val source: String,
			   fromIndex: Int,
			   toIndex: Int
) : Reader {

	private var _properties: Map<String, Reader> = emptyMap()
	private var _elements: List<Reader> = emptyList()

	private var isParsed: Boolean = false
	private val fromIndex: Int
	private val toIndex: Int
	private val subStr: SubString

	init {
		// Trim leading and trailing whitespace.
		var fromTrimmed = fromIndex
		var toTrimmed = toIndex
		while (fromTrimmed < toTrimmed && source[fromTrimmed].isWhitespace()) {
			fromTrimmed++
		}
		while (fromTrimmed < toTrimmed && source[toTrimmed - 1].isWhitespace()) {
			toTrimmed--
		}
		this.fromIndex = fromTrimmed
		this.toIndex = toTrimmed
		subStr = SubString(source, fromTrimmed, toTrimmed)
	}

	private var marker: Int = 0

	private fun parseObject() {
		if (isParsed) return
		isParsed = true
		if (source.isEmpty()) return
		val isObject = source[fromIndex] == '{'
		if (!isObject && source[fromIndex] != '[') return

		// Set the _properties and _elements values only if necessary.
		val properties: MutableMap<String, Reader>?
		val elements: MutableList<Reader>?
		if (isObject) {
			properties = HashMap()
			elements = null
			_properties = properties
		} else {
			properties = null
			elements = ArrayList()
			_elements = elements
		}
		marker = fromIndex + 1
		val tagStack: MutableList<Char> = ArrayList()

		while (marker < toIndex) {
			consumeWhitespace()
			while (source[marker] == ',') {
				marker++
				consumeWhitespace()
			}
			val char = source[marker]
			if (char == '}' || char == ']') break
			if (!isObject || char == '"') {
				var identifier: SubString? = null
				if (isObject) {
					marker++
					val identifierStartIndex = marker
					while (!(source[marker] == '"' && source[marker - 1] != '\\')) {
						if (marker >= toIndex)
							throw Exception("Expected '\"', but reached end of stream")
						marker++
					}
					identifier = SubString(source, identifierStartIndex, marker++)
				}

				consumeWhitespace()
				if (isObject) {
					if (source[marker++] != ':')
						throw Exception("Expected ':', but instead found: ${source.subSequence(marker, marker + 20)}")
				}

				val valueStartIndex = marker
				tagStack.clear()
				var isString = false
				while (marker < toIndex) {
					// Seek to the next property or element demarcation
					val vC = source[marker]
					if (tagStack.isEmpty() && (vC == '}' || vC == ']')) {
						break
					}
					val last = tagStack.peek()
					if (last != null && vC == last && (!isString || source[marker - 1] != '\\')) {
						tagStack.pop()
						isString = false
					} else if (!isString) {
						if (vC == '{') {
							tagStack.add('}')
						} else if (vC == '[') {
							tagStack.add(']')
						} else if (vC == '\'') {
							tagStack.add('\'')
							isString = true
						} else if (vC == '"') {
							tagStack.add('"')
							isString = true
						}
					}
					if (tagStack.isEmpty() && vC == ',') {
						break
					}
					marker++
				}
				if (tagStack.isNotEmpty())
					throw Exception("Expected ${tagStack.peek()}, but reached end of stream")
				if (isObject) {
					properties!![identifier!!.toString()] = JsonNode(source, valueStartIndex, marker)
				} else {
					elements!!.add(JsonNode(source, valueStartIndex, marker))
				}
			} else {
				if (isObject)
					throw Exception("Expected \", but instead found: ${source.subSequence(marker, marker + 20)}")
				else
					throw Exception("Unexpected character ${source[marker]}.  ${source.subSequence(marker, marker + 20)}")
			}
		}
	}

	private fun consumeWhitespace() {
		while (marker < toIndex && source[marker].isWhitespace()) {
			marker++
		}
	}

	override fun properties(): Map<String, Reader> {
		parseObject()
		return _properties
	}

	override fun elements(): List<Reader> {
		parseObject()
		return _elements
	}

	fun entries(): Set<Map.Entry<String, Reader>> {
		parseObject()
		return _properties.entries
	}

	override val isNull: Boolean
		get() = subStr.equalsStr("null")

	override fun byte(): Byte? {
		if (isNull) return null
		return subStr.toString().toByteOrNull()
	}

	override fun bool(): Boolean? {
		if (isNull) return null
		return subStr.equalsStr("true") || subStr.equalsStr("1")
	}

	override fun char(): Char? {
		if (isNull) return null
		return subStr.charAt(1)
	}

	override fun string(): String? {
		if (isNull) return null
		return removeBackslashes(subStr.subSequence(1, subStr.length - 1).toString())
	}

	override fun short(): Short? {
		if (isNull) return null
		return subStr.toString().toShortOrNull()
	}

	override fun int(): Int? {
		if (isNull) return null
		return subStr.toString().toIntOrNull()
	}

	override fun long(): Long? {
		if (isNull) return null
		return subStr.toString().toLongOrNull()
	}

	override fun float(): Float? {
		return double()?.toFloat()
	}

	override fun double(): Double? {
		if (isNull) return null
		return subStr.toString().toDoubleOrNull()
	}

	override fun byteArray(): ByteArray? {
		if (isNull) return null
		return base64.decodeFromString(subStr.toString())
	}

	override fun toString(): String {
		return subStr.toString()
	}
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

	override fun writeNull() {
		builder.append("null")
	}

	override fun byteArray(value: ByteArray?) {
		if (value == null) return writeNull()
		builder.append(base64.encodeToString(value))
	}
}

actual val json: Serializer<String> = JsonSerializer
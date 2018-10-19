/*
 * Copyright 2017 Nicholas Bilyk
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

import com.acornui.collection.stringMapOf
import com.acornui.core.htmlEntitiesDecode
import com.acornui.core.isWhitespace2
import com.acornui.core.replace2
import com.acornui.string.SubString

// TODO: WIP

/**
 * A factory that provides a Reader and Writer for XML
 * @author nbilyk
 */
object XmlSerializer : Serializer<String> {

	override fun read(data: String): Reader {
		return XmlNode(data, 0, data.length, parent = null)
	}

	override fun write(callback: (Writer) -> Unit): String = write(callback, "\t", "\n")

	fun write(callback: (Writer) -> Unit, tabStr: String, returnStr: String): String {
		val buffer = StringBuilder()
		val writer = XmlWriter(buffer, "", tabStr, returnStr)
		callback(writer)
		return buffer.toString()
	}

	fun <E> write(value: E, to: To<E>, tabStr: String, returnStr: String): String {
		return write({
			it.obj(true) {
				to.write2(value, it)
			}
		}, tabStr, returnStr)
	}
}

/**
 * @author nbilyk
 */
class XmlNode(
		private val source: String,
		val fromIndex: Int,
		val toIndex: Int,
		val parent: XmlNode?
) : Reader {

	private val _properties: MutableMap<String, Reader> = stringMapOf()
	private val _elements: MutableList<Reader> = ArrayList()

	private var isParsed: Boolean = false
	private val subStr = SubString(source, fromIndex, toIndex)

	init {
	}

	private var marker: Int = 0

	private fun parseObject() {
		if (isParsed) return
		isParsed = true

		// Trim leading and trailing whitespace.
		var fromTrimmed = fromIndex
		var toTrimmed = toIndex
		while (fromTrimmed < toTrimmed && source[fromTrimmed].isWhitespace2()) {
			fromTrimmed++
		}
		while (fromTrimmed < toTrimmed && source[toTrimmed - 1].isWhitespace2()) {
			toTrimmed--
		}
		if (fromTrimmed >= toTrimmed) return

		marker = fromTrimmed

		val isObject = source[marker++] == '<'
		if (!isObject) return

		_properties["@type"] = when (source[marker]) {
			'!' -> COMMENT
			'?' -> INSTRUCTION
			else -> ELEMENT
		}
		consumeNonWhitespace()
		_properties["@fullName"] = XmlNode(source, fromIndex + 1, marker, this)

		println("Type ${_properties["@type"]!!.string()} Name ${_properties["@fullName"]!!.string()}")

		consumeWhitespace()

		while (source[marker] != '>') {
			val identifierStartIndex = marker
			while (marker < toIndex && source[marker].isIdentifierPart()) {
				marker++
			}
			val identifier = source.substring(identifierStartIndex, marker)
			consumeWhitespace()
			if (source[marker] == '=') {
				// Attribute has a value.
				marker++
				consumeWhitespace()
				val valueStartIndex = marker
				val startChar = source[marker]
				if (startChar == '"' || startChar == '\'') {
					consumeUntil(startChar)
					_properties[identifier] = XmlNode(source, valueStartIndex + 1, marker, this)
				} else {
					consumeNonWhitespace()
					_properties[identifier] = XmlNode(source, valueStartIndex, marker, this)
				}
				consumeWhitespace()
			} else {
				_properties[identifier] = NullNode
			}
			println("identifier $identifier = ${_properties[identifier]?.string()}")


			consumeWhitespace()

		}

	}

	private fun Char.isIdentifierPart(): Boolean {
		return !isWhitespace2() && this != '=' && this != '>'
	}

	private fun consumeWhitespace() {
		while (marker < toIndex && source[marker].isWhitespace2()) {
			marker++
		}
	}

	private fun consumeNonWhitespace() {
		while (marker < toIndex && !source[marker].isWhitespace2()) {
			marker++
		}
	}

	private fun consumeUntil(char: Char) {
		while (marker < toIndex && source[marker] != char) {
			marker++
		}
	}

	private fun consume(char: Char) {
		if (source[marker] == char)
			marker++
	}

	private fun whileNot(str: String, inner: ()->Unit) {
		for (i in 0..str.lastIndex) {
			if (source[marker + i] != str[i]) {
				inner()
				return
			}
		}
		marker += str.length
	}

	override fun contains(name: String): Boolean {
		parseObject()
		return _properties.contains(name)
	}

	override fun contains(index: Int): Boolean {
		parseObject()
		return index < _elements.size
	}

	override fun properties(): MutableMap<String, Reader> {
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

	override fun bool(): Boolean? {
		if (isNull) return null
		return subStr.equalsStr("true") || subStr.equalsStr("1")
	}

	override fun byte(): Byte? {
		if (isNull) return null
		return subStr.toString().toByteOrNull()
	}

	override fun char(): Char? {
		if (isNull) return null
		return subStr.charAt(1)
	}

	override fun string(): String? {
		if (isNull) return null
		return htmlEntitiesDecode(subStr.toString())
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

	override fun toString(): String {
		return subStr.toString()
	}

	companion object {
		val COMMENT = StringNode("comment")
		val INSTRUCTION = StringNode("instruction")
		val ELEMENT = StringNode("element")
	}
}

fun Reader.type(): ElementType {
	return when (get("@type")) {
		XmlNode.COMMENT -> ElementType.COMMENT
		XmlNode.INSTRUCTION -> ElementType.INSTRUCTION
		XmlNode.ELEMENT -> ElementType.ELEMENT
		else -> throw Exception("Unknown element type.")
	}
}

fun Reader.name(): String {
	return get("@fullName")!!.string()!!.substringAfter(":")
}

enum class ElementType {
	COMMENT,
	ELEMENT,
	INSTRUCTION
}

/**
 * A simple XML writer
 */
class XmlWriter(
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
		return XmlWriter(builder, indentStr, tabStr, returnStr)
	}

	override fun element(): Writer {
		if (size++ > 0)
			builder.append(",$returnStr")
		builder.append(indentStr)
		return XmlWriter(builder, indentStr, tabStr, returnStr)
	}

	override fun bool(value: Boolean?) {
		if (value == null) return writeNull()
		if (value) builder.append("true")
		else builder.append("false")
	}

	override fun byte(value: Byte?) {
		if (value == null) return writeNull()
		builder.append(value)
	}

	override fun string(value: String?) {
		if (value == null) return writeNull()
		builder.append('"')
		builder.append(escape(value))
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
		val childWriter = XmlWriter(builder, childIndent, tabStr, r)
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
		val childWriter = XmlWriter(builder, childIndent, tabStr, r)
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

	private fun escape(value: String): String {
		return value.replace2("\\", "\\\\").replace2("\r", "\\r").replace2("\n", "\\n").replace2("\t", "\\t").replace2("\"", "\\\"")
	}
}
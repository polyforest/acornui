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

package com.acornui.string

import com.acornui.core.isWhitespace2

@Deprecated("Renamed to StringReader", ReplaceWith("StringReader(data)"))
fun StringParser(data: String): StringReader = StringReader(data)

/**
 * Using a source string of data, StringReader provides a way to parse the string as if it were a buffer.
 * Example:
 *
 * ```
 * val parser = StringReader(" 123 434 true")
 * parser.white()
 * parser.getInt() // 123
 * parser.white()
 * parser.getInt() // 434
 * parser.white()
 * parser.getBool() // 255
 * ```
 *
 * @author nbilyk
 */
class StringReader(val data: String) {

	val hasNext: Boolean
		get() = position < length

	var position: Int = 0
	val length: Int = data.length

	fun white(): String {
		return getString { it.isWhitespace2() }
	}

	fun notWhite(): String {
		return getString { !it.isWhitespace2() }
	}

	fun getBool(): Boolean? {
		val char = data[position]
		if (char == '1') {
			position++
			return true
		} else if (char == '0') {
			position++
			return false
		} else if (char == 't' || char == 'T') {
			val found = consumeString("true", true)
			if (!found) position++
			return true
		} else if (char == 'f' || char == 'F') {
			val found = consumeString("false", true)
			if (!found) position++
			return true
		}
		return null
	}

	/**
	 * Returns the unquoted string at the current position until we hit a character that is neither a letter or digit.
	 */
	fun getString(): String {
		return getString { it.isLetterOrDigit2() }
	}

	/**
	 * Parses a string wrapped in quotes. The string may be single or double quoted, and escaped quotes \" \' are
	 * ignored.
	 */
	fun getQuotedString(): String? {
		var foundQuoteEnd = false
		var quoteStart: Char? = null
		var escaped = false
		var p = position
		while (p < length) {
			val it = data[p]
			if (quoteStart == null) {
				if (it.isWhitespace2()) {
				} else if (it == '"' || it == '\'') {
					quoteStart = it
				} else {
					break
				}
			} else {
				if (escaped) {
					escaped = false
				} else if (it == quoteStart) {
					foundQuoteEnd = true
					p++
					break
				} else if (it == '\\') {
					escaped = true
				}
			}
			p++
		}
		return if (foundQuoteEnd) {
			val subString = data.substring(position + 1, p - 1)
			position = p
			subString
		} else null
	}

	fun getString(predicate: (Char)->Boolean): String {
		return data.substring(consumeWhile(predicate), position)
	}

	fun getDouble(): Double? {
		var p = position
		var foundDecimalMark = false
		while (p < length) {
			val it = data[p]
			if (!it.isDigit2() && !(p == position && it == '-')) {
				if (!foundDecimalMark && it == '.') {
					foundDecimalMark = true
				} else {
					break
				}
			}
			p++
		}
		if (position == p) return null
		val subString = data.substring(position, p)
		if (subString.length == 1 && subString == "-") return null
		position = p
		return subString.toDoubleOrNull()
	}

	fun getFloat(): Float? {
		return getDouble()?.toFloat()
	}

	fun getInt(): Int? {
		var p = position
		while (p < length) {
			val it = data[p]
			if (!it.isDigit2() && !(p == position && it == '-')) {
				break
			}
			p++
		}
		if (position == p) return null
		val subString = data.substring(position, p)
		if (subString.length == 1 && subString == "-") return null
		position = p
		return subString.toIntOrNull()
	}

	fun getChar(): Char? {
		if (position >= length) return null
		return data[position++]
	}

	fun consumeString(string: String, ignoreCase: Boolean = false): Boolean {
		val n = string.length
		var p = position
		if (p + n >= length) return false
		var index = 0
		while (p < length && index < n && (data[p] == string[index] ||
				(ignoreCase && data[p] == string[index].toLowerCase()))) {
			index++
			p++
		}
		if (index < n) return false
		position = p
		return true
	}

	fun consumeThrough(string: String): Boolean {
		val index = data.indexOf(string, position)
		if (index == -1) return false
		position = index + string.length
		return true
	}

	fun consumeThrough(char: Char): Boolean {
		val index = data.indexOf(char, position)
		if (index == -1) return false
		position = index + 1
		return true
	}

	/**
	 * Reads until either \r\n or \n, returning the line (not including the line feed or carriage return)
	 * If the position is at the end, an empty string will be returned. Use [hasNext] to determine eof.
	 */
	fun readLine(): String {
		val str = getString { it != '\r' && it != '\n' }
		consumeChar('\r')
		consumeChar('\n')
		return str
	}

	/**
	 * Consumes the provided character.
	 * @return Returns true if the character was consumed, false if it wasn't at the current position.
	 */
	fun consumeChar(char: Char): Boolean {
		return consumeWhile { char == it } < position
	}

	/**
	 * Consumes any of the characters provided in the [chars] string.
	 * @return Returns true if any of the characters were found and the position has progressed.
	 */
	fun consumeChars(chars: String): Boolean {
		return consumeWhile { chars.indexOf(it) >= 0 } < position
	}

	/**
	 * Consumes characters until the predicate function returns false.
	 *
	 * @return Returns the starting position.
	 */
	inline fun consumeWhile(predicate: (Char)->Boolean): Int {
		val mark = position
		while (position < length) {
			if (!predicate(data[position])) break
			position++
		}
		return mark
	}

	fun reset() {
		position = 0
	}

}

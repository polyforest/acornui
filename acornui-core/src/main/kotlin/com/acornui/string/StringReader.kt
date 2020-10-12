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

	/**
	 * Returns true if we have not hit the [data] length.
	 */
	fun hasNext(): Boolean =
		position < length

	/**
	 * The current reader's position.
	 */
	var position: Int = 0

	/**
	 * The string length.
	 */
	val length: Int = data.length

	/**
	 * Gets the character at the current position, without advancing [position].
	 */
	val current: Char
		get() = data[position]

	/**
	 * Consumes while the characters are whitespace.
	 */
	fun white(): String {
		return getString { it.isWhitespace() }
	}

	/**
	 * Consumes while the characters are not whitespace.
	 */
	fun notWhite(): String {
		return getString { !it.isWhitespace() }
	}

	/**
	 * Consumes the boolean at the current position.
	 * The current position is advanced if the boolean is found.
	 */
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
	 * Returns the unquoted string at the current position until we hit a character that is not a word character.
	 */
	fun getString(): String {
		return getString { it.isWord() }
	}

	/**
	 * Parses a string wrapped in quotes. The string may be single or double quoted, and escaped quotes \" \' are
	 * ignored.
	 */
	fun getQuotedString(): String? {
		val quoteStart: Char = current
		if (quoteStart != '"' && quoteStart != '\'') return null
		return getInner(quoteStart, quoteStart)
	}


	/**
	 * Given starting and ending terminals, returns the inner contents.
	 * Examples:
	 *
	 * `StringReader("'one'").getInner('\'', '\'') // "one"
	 * `StringReader("(one(two)three)").getInner('(', ')') // "one(two)three"
	 */
	fun getInner(start: Char, end: Char): String? {
		if (current != start) return null
		var c = 1
		var escaped = false
		var p = position + 1
		while (p < length) {
			val it = data[p++]
			if (escaped) {
				escaped = false
			} else if (it == end) {
				c--
				if (c == 0)
					break
			} else if (it == start) {
				c++
			} else if (it == '\\') {
				escaped = true
			}
		}
		return if (c == 0) {
			val subString = data.substring(position + 1, p - 1)
			position = p
			subString
		} else null
	}

	/**
	 * Consumes the string until [predicate] returns false.
	 */
	fun getString(predicate: (Char) -> Boolean): String {
		return data.substring(consumeWhile(predicate), position)
	}

	/**
	 * Consumes the double at the current position.
	 * The current position is advanced if a double is found.
	 */
	fun getDouble(): Double? {
		var p = position
		var foundDecimalMark = false
		while (p < length) {
			val it = data[p]
			if (!it.isDigit() && !(p == position && it == '-')) {
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
		val d = subString.toDoubleOrNull()
		if (d != null)
			position = p
		return d
	}

	/**
	 * Consumes the float at the current position.
	 */
	fun getFloat(): Float? {
		return getDouble()?.toFloat()
	}

	/**
	 * Consumes the integer at the current position.
	 * The current position is advanced if an integer is found.
	 */
	fun getInt(): Int? {
		var p = position
		while (p < length) {
			val it = data[p]
			if (!it.isDigit() && !(p == position && it == '-')) {
				break
			}
			p++
		}
		if (position == p) return null
		val subString = data.substring(position, p)
		if (subString.length == 1 && subString == "-") return null
		val i = subString.toIntOrNull()
		if (i != null)
		position = p
		return i
	}

	/**
	 * Gets the character at the current position, incrementing [position].
	 */
	fun getChar(): Char? {
		if (position >= length) return null
		return data[position++]
	}

	/**
	 * If the given string is found at the current position, advance to the end of the string.
	 */
	fun consumeString(string: String, ignoreCase: Boolean = false): Boolean {
		val found = data.startsWith(string, position, ignoreCase)
		if (!found) return false
		position += string.length
		return true
	}

	/**
	 * Finds the next instance of [string] and consumes until the end of that string.
	 */
	fun consumeThrough(string: String, ignoreCase: Boolean = false): Boolean {
		val index = data.indexOf(string, position, ignoreCase)
		if (index == -1) return false
		position = index + string.length
		return true
	}

	/**
	 * Finds the next instance of [char] and consumes until the end of that character.
	 */
	fun consumeThrough(char: Char, ignoreCase: Boolean = false): Boolean {
		val index = data.indexOf(char, position, ignoreCase)
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
		val found = data[position]
		if (found != char) return false
		position++
		return true
	}

	/**
	 * Consumes characters until the predicate function returns false.
	 *
	 * @return Returns the starting position.
	 */
	fun consumeWhile(predicate: (Char) -> Boolean): Int {
		val mark = position
		while (true) {
			if (consumeIf(predicate) == null) break
		}
		return mark
	}

	/**
	 * Consumes a single character if it matches the given predicate.
	 * @return Returns the character consumed.
	 */
	fun consumeIf(predicate: (Char) -> Boolean): Char? {
		if (position >= length) return null
		val char = data[position]
		if (!predicate(char)) return null
		position++
		return char
	}

	fun reset() {
		position = 0
	}

}


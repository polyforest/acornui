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

package com.acornui.core

/**
 * Replaces {0}, {1}, {2}, ... {n} with the values from the tokens array.
 */
fun String.replaceTokens(vararg tokens: Any): String {
	var str = this
	for (i in 0..tokens.lastIndex) {
		str = str.replace2("{$i}", tokens[i].toString())
	}
	return str
}

fun String.replace2(target: CharSequence, replacement: CharSequence): String {
	return this.split2(target.toString()).join2(replacement.toString())
}

fun String.replace2(target: Char, replacement: Char): String {
	return this.split2(target).join2(replacement)
}

fun Array<String>.join2(delim: Char): String {
	val builder = StringBuilder()
	for (i in 0..lastIndex) {
		if (i != 0) builder.append(delim)
		builder.append(this[i])
	}
	return builder.toString()
}

fun String.split2(delim: Char): Array<String> {
	val size = count({ it == delim }) + 1
	var index = 0
	return Array(size, {
		val nextIndex = indexOf(delim, index)
		val sub: String
		if (nextIndex == -1) {
			sub = substring(index)
		} else {
			sub = substring(index, nextIndex)
		}
		index = nextIndex + 1
		sub
	})
}

fun Array<String>.join2(delim: CharSequence): String {
	val builder = StringBuilder()
	for (i in 0..lastIndex) {
		if (i != 0) builder.append(delim)
		builder.append(this[i])
	}
	return builder.toString()
}

fun String.split2(delim: String): Array<String> {
	val len = delim.length
	var size = 0
	var index = 0
	while (true) {
		index = indexOf(delim, index)
		size++
		if (index == -1) break
		index += len
	}

	index = 0
	return Array(size, {
		val nextIndex = indexOf(delim, index)
		val sub: String
		if (nextIndex == -1) {
			sub = substring(index)
		} else {
			sub = substring(index, nextIndex)
		}
		index = nextIndex + len
		sub
	})
}

fun String.startsWith2(prefix: String, offset: Int = 0): Boolean {
	var to = offset
	var po = 0
	var pc = prefix.length
	// Note: offset might be near -1>>>1.
	if ((offset < 0) || (offset > length - pc)) {
		return false
	}
	while (--pc >= 0) {
		if (this[to++] != prefix[po++]) {
			return false
		}
	}
	return true
}

fun String.endsWith2(suffix: String): Boolean {
	return startsWith(suffix, length - suffix.length)
}

/**
 * Returns a string containing this char sequence repeated [n] times.
 */
fun CharSequence.repeat2(n: Int): String {
	if (n < 0)
		throw IllegalArgumentException("Value should be non-negative, but was $n")

	val sb = StringBuilder(n * length)
	for (i in 1..n) {
		sb.append(this)
	}
	return sb.toString()
}

/**
 * May be set by the back-ends.
 * JVM sets to System.lineSeparator()
 */
var lineSeparator: String = "\n"

fun htmlEntities(value: String): String {
	@Suppress("name_shadowing")
	var value = value
	value = value.replace("&", "&amp;")
	value = value.replace("<", "&lt;")
	value = value.replace(">", "&gt;")
	value = value.replace("\"", "&quot;")
	value = value.replace("\'", "&apos;")
	return value
}

fun htmlEntitiesDecode(value: String): String {
	@Suppress("name_shadowing")
	var value = value
	value = value.replace("&amp;", "&")
	value = value.replace("&lt;", "<")
	value = value.replace("&gt;", ">")
	value = value.replace("&quot;", "\"")
	value = value.replace("&apos;", "'")
	return value
}

/**
 * Converts backslash escapes into their corresponding characters.
 * \t, \b, \n, \r, \', \", \\, \/, \$, \uFF00
 */
fun removeBackslashes(value: String): String {
	val unescaped = StringBuilder()
	var lastIndex = 0
	var i = 0
	val n = value.length
	while (i < n) {
		if (value[i] == '\\' && i + 1 < n) {
			val next = value[++i]
			val newChar = when (next) {
				't' -> '\t'
				'b' -> '\b'
				'n' -> '\n'
				'r' -> '\r'
				'\'' -> '\''
				'\"' -> '\"'
				'/' -> '/'
				'\\' -> '\\'
				'\$' -> '\$'
				'u' -> {
					if (i + 5 <= n) {
						val digits = value.substring(i + 1, i + 5).toInt(radix = 16)
						unescaped.append(value.substring(lastIndex, i - 1))
						unescaped.append(digits.toChar())
						i += 4
						lastIndex = i + 1
					}
					null
				}
				else -> null
			}
			if (newChar != null) {
				unescaped.append(value.substring(lastIndex, i - 1))
				unescaped.append(newChar)
				lastIndex = i + 1
			}
		}
		i++
	}
	unescaped.append(value.substring(lastIndex))
	return unescaped.toString()
}

/**
 * Adds a backslash before the following characters:
 * t, b, n, r, ', ", \, $
 */
fun addBackslashes(value: String): String {
	val escaped = StringBuilder()
	var i = 0
	val n = value.length
	while (i < n) {
		val char = value[i]
		escaped.append(when (char) {
			'\t' -> "\\t"
			'\b' -> "\\b"
			'\n' -> "\\n"
			'\r' -> "\\r"
			'\'' -> "\\'"
			'\"' -> "\\\""
			'\\' -> "\\\\"
			'\$' -> "\\$"
			else -> char
		})
		i++
	}
	return escaped.toString()
}

fun String.compareTo2(other: String, ignoreCase: Boolean = false): Int {
	return if (ignoreCase)
		toLowerCase().compareTo(other.toLowerCase())
	else
		compareTo(other)
}

fun String.toUnderscoreCase(): String {
	return replace(Regex("([a-z])([A-Z]+)"), "$1_$2").toLowerCase()
}

fun String.toHyphenCase(): String {
	return replace(Regex("([a-z])([A-Z]+)"), "$1_$2").toLowerCase()
}

fun String.toFirstLowerCase(): String {
	return this[0].toLowerCase() + substring(1)
}

private val whitespaceChars = mapOf(
		0x0009.toChar() to true,
		0x000A.toChar() to true,
		0x000B.toChar() to true,
		0x000C.toChar() to true,
		0x000D.toChar() to true,
		0x0020.toChar() to true,
		0x0085.toChar() to true,
		0x00A0.toChar() to true,
		0x1680.toChar() to true,
		0x180E.toChar() to true,
		0x2000.toChar() to true,
		0x2001.toChar() to true,
		0x2002.toChar() to true,
		0x2003.toChar() to true,
		0x2004.toChar() to true,
		0x2005.toChar() to true,
		0x2006.toChar() to true,
		0x2007.toChar() to true,
		0x2008.toChar() to true,
		0x2009.toChar() to true,
		0x200A.toChar() to true,
		0x2028.toChar() to true,
		0x2029.toChar() to true,
		0x202F.toChar() to true,
		0x205F.toChar() to true,
		0x3000.toChar() to true
)

fun Char.isWhitespace2(): Boolean {
	return whitespaceChars.containsKey(this)
}
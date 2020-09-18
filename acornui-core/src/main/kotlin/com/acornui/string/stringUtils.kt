/*
 * Copyright 2020 Poly Forest, LLC
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

import com.acornui.collection.removeFirst
import com.acornui.math.clamp
import kotlinx.serialization.Serializable

/**
 * Replaces {0}, {1}, {2}, ... {n} with the values from the tokens array.
 */
fun String.replaceTokens(vararg tokens: Any): String {
	var str = this
	for (i in 0..tokens.lastIndex) {
		str = str.replace("{$i}", tokens[i].toString())
	}
	return str
}

/**
 *
 */
val lineSeparator: String = "\n"

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
 * \t, \b, \n, \r, \", \\, \/, \$, \uFF00
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
 * t, b, n, r, ", \, $
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

private val wordSplitter = Regex("""([a-z]+|\d+|(?:[A-Z][a-z]+)|(?:[A-Z]+(?=(?:[A-Z][a-z])|[^A-Za-z]|[$\d\n]|\b)))([\W_]*)""")

/**
 * Converts a string to underscore_case.
 * E.g.
 * thisIsATest becomes this_is_a_test
 *
 * This does not support unicode characters.
 * See unit tests for more case examples.
 */
fun String.toUnderscoreCase(): String {
	return replace(wordSplitter, "$1_").trimEnd('_').toLowerCase()
}

/**
 * Converts a string to hyphen-case.
 * E.g.
 * thisIsATest becomes this-is-a-test
 *
 * This does not support unicode characters.
 * See unit tests for more case examples.
 */
fun String.toHyphenCase(): String {
	return replace(wordSplitter, "$1-").trimEnd('-').toLowerCase()
}

/**
 * Converts a string to camelCase.
 * E.g.
 * this-is-a-test becomes thisIsATest
 *
 * This does not support unicode characters.
 * See unit tests for more case examples.
 */
fun String.toCamelCase(): String {
	return wordSplitter.replace(this) {
		it.groupValues[1].toLowerCase().capitalize()
	}.decapitalize()
}

private val whitespaceChars = mapOf(
		0x0009.toChar() to true,
		0x000a.toChar() to true,
		0x000b.toChar() to true,
		0x000c.toChar() to true,
		0x000d.toChar() to true,
		0x0020.toChar() to true,
		0x0085.toChar() to true,
		0x00a0.toChar() to true,
		0x1680.toChar() to true,
		0x180e.toChar() to true,
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
		0x200a.toChar() to true,
		0x2028.toChar() to true,
		0x2029.toChar() to true,
		0x202f.toChar() to true,
		0x205f.toChar() to true,
		0x3000.toChar() to true
)

@Deprecated("Kotlin MPP now supports isWhitespace", ReplaceWith("this.isWhitespace()"))
fun Char.isWhitespace2(): Boolean {
	return whitespaceChars.containsKey(this)
}

/**
 *
 */
fun Iterable<String>.filterWithWords(wordList: List<String>): List<String> {
	return filter { haystackWord ->
		val words = haystackWord.toUnderscoreCase().split(Regex("[\\W_]")).filter { it.isNotEmpty() }.toMutableList()
		for (needleWord in wordList) {
			if (needleWord.isEmpty()) continue
			words.removeFirst { it.equals(needleWord, ignoreCase = true) } ?: return@filter false
		}
		words.isEmpty()
	}

}

fun String.substringInRange(startIndex: Int, endIndex: Int = length): String {
	if (startIndex >= endIndex) return this
	return substring(clamp(startIndex, 0, length), clamp(endIndex, 0, length))
}

@Suppress("NOTHING_TO_INLINE", "UnsafeCastFromDynamic")
internal inline fun String.startsWith(s: String, position: Int): Boolean = asDynamic().startsWith(s, position)

@Suppress("NOTHING_TO_INLINE", "UnsafeCastFromDynamic")
internal inline fun String.endsWith(s: String): Boolean = asDynamic().endsWith(s)

/**
 * Given an index within a string, returns the row and column it represents.
 *
 * @return Returns
 */
fun String.indexToRowAndColumn(index: Int, tabSize: Int = 4): RowAndColumn {
	val sub = substring(0, index)
	val colStart = sub.indexOfLast { it == '\n' }
	val colFrag = substring(colStart + 1, index)
	val numTabs = colFrag.count { it == '\t' }
	val row = sub.count { it == '\n' }
	val col = colFrag.length + (tabSize - 1) * numTabs
	return RowAndColumn(row, col)
}

/**
 * A zero-indexed representation of a row / col position.
 */
@Serializable
data class RowAndColumn(val row: Int, val col: Int)

/**
 * Truncates this string if it exceeds [limit] characters.
 * The returned string will be this string if the limit is not exceeded.
 * Otherwise returns a string with a total length of [limit], ending in [truncationIndicator].
 */
fun String.truncate(limit: Int, truncationIndicator: String = "…"): String {
	require(truncationIndicator.length < limit)
	if (length <= limit) return this
	return substring(0, limit - truncationIndicator.length) + truncationIndicator
}

/**
 * Similar to [String.split] except only splits on the first instance of [delimiter].
 *
 * Examples:
 * "test=me".splitFirst("=") // "test" to "me"
 * "test=me=again".splitFirst("=") // "test" to "me=again"
 * "test=me".splitFirst("?") // "test=me" to ""
 *
 * If [delimiter] is not found, [Pair.first] will be this string, and [Pair.second] will be empty.
 */
fun String.splitFirst(delimiter: String): Pair<String, String> {
	val i = indexOf(delimiter)
	return if (i == -1) this to ""
	else substring(0, i) to substring(i + delimiter.length)
}
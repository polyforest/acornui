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
 * @param str
 * @param delimiters
 */
class StringTokenizer(
		private val str: String,
		private val delimiters: String = " \t\n\r"
) {

	private var currentPosition: Int = 0
	private var nextPosition: Int = 0
	private val maxPosition: Int = str.length

	/**
	 * Stores the value of the delimiter character with the
	 * highest value. It is used to optimize the detection of delimiter
	 * characters.
	 */
	private var maxDelimiterCharCode: Int = 0

	/**
	 * Set maxDelimiterCodePoint to the highest char in the delimiter set.
	 */
	private fun calculateMaxDelimiterCharCode() {
		val n = delimiters.length
		var m = 0
		var c: Int
		for (i in 0..n - 1) {
			c = delimiters[i].toInt()
			if (c > m) m = c
		}
		maxDelimiterCharCode = m
	}

	init {
		currentPosition = 0
		nextPosition = -1
		calculateMaxDelimiterCharCode()
	}

	/**
	 * Skips delimiters starting from the specified position.
	 */
	private fun skipDelimiters(startPos: Int): Int {
		var position = startPos
		while (position < maxPosition) {
			val c = str[position]
			if (c.toInt() > maxDelimiterCharCode || delimiters.indexOf(c) < 0)
				break
			position++
		}
		return position
	}

	/**
	 * Skips ahead from startPos and returns the index of the next delimiter
	 * character encountered, or maxPosition if no such delimiter is found.
	 */
	private fun scanToken(startPos: Int): Int {
		var position = startPos
		while (position < maxPosition) {
			val c = str[position]
			if ((c.toInt() <= maxDelimiterCharCode) && (delimiters.indexOf(c) >= 0))
				break
			position++
		}
		return position
	}

	/**
	 * Tests if there are more tokens available from this tokenizer's string.
	 * If this method returns true, then a subsequent call to
	 * nextToken with no argument will successfully return a token.

	 * @return  `true` if and only if there is at least one token
	 * *          in the string after the current position; `false`
	 * *          otherwise.
	 */
	fun hasMoreTokens(): Boolean {
		/*
         * Temporarily store this position and use it in the following
         * nextToken() method only if the delimiters haven't been changed in
         * that nextToken() invocation.
         */
		nextPosition = skipDelimiters(currentPosition)
		return nextPosition < maxPosition
	}

	/**
	 * Returns the next token from this string tokenizer.

	 * @return     the next token from this string tokenizer.
	 * *
	 * @exception  NoSuchElementException  if there are no more tokens in this
	 * *               tokenizer's string.
	 */
	fun nextToken(): String {
		currentPosition = if (nextPosition >= 0)
			nextPosition
		else
			skipDelimiters(currentPosition)

		nextPosition = -1

		if (currentPosition >= maxPosition)
			throw NoSuchElementException()
		val start = currentPosition
		currentPosition = scanToken(currentPosition)
		return str.substring(start, currentPosition)
	}

	/**
	 * Calculates the number of times that this tokenizer's
	 * `nextToken` method can be called before it generates an
	 * exception. The current position is not advanced.

	 * @return  the number of tokens remaining in the string using the current
	 * *          delimiter set.
	 * *
	 * @see nextToken
	 */
	fun countTokens(): Int {
		var count = 0
		var pos = currentPosition
		while (pos < maxPosition) {
			pos = skipDelimiters(pos)
			if (pos >= maxPosition)
				break
			pos = scanToken(pos)
			count++
		}
		return count
	}
}

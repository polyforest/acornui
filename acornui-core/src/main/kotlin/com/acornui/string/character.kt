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
 * Returns true if this is any numeric.
 */
fun Char.isDigit(): Boolean = toString().matches("[\\d]")

/**
 * Returns true if this is any alphanumeric or underscore character.
 */
fun Char.isWord(): Boolean = toString().matches("[\\w]")

/**
 * Characters where text is wrapped.
 */
var breakingChars: CharArray = charArrayOf('-', ' ', '\n', '\t')

fun Char.isBreaking(): Boolean {
	return breakingChars.indexOf(this) >= 0
}

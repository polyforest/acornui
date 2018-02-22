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

package com.acornui.string



fun Char.isDigit2(): Boolean {
	return this >= '0' && this <= '9'
}

fun Char.isLetter2(): Boolean {
	return (this >= 'a' && this <= 'z') || (this >= 'A' && this <= 'Z')
}

fun Char.isLetterOrDigit2(): Boolean {
	return isLetter2() || isDigit2()
}

/**
 * Characters where text is wrapped.
 */
var breakingChars: CharArray = charArrayOf('-', ' ', '\n', '\t')

fun Char.isBreaking(): Boolean {
	return breakingChars.indexOf(this) >= 0
}
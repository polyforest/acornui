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

package com.acornui

var assertionsEnabled: Boolean = debug

/**
 * assert method calls that work in both jvm and js.
 */
@Deprecated("use check", ReplaceWith("check(value) { message }"))
fun _assert(value: Boolean, message: Any = "Assertion failed") {
	if (assertionsEnabled && !value) {
		throw Exception("" + message)
	}
}

@Deprecated("use check", ReplaceWith("check(value, lazyMessage)"))
inline fun _assert(value: Boolean, lazyMessage: () -> Any) {
	if (assertionsEnabled && !value) {
		val message = lazyMessage()
		throw Exception("" + message)
	}
}

inline fun _assert(lazyValue: () -> Boolean, message: Any = "Assertion failed") {
	if (assertionsEnabled && !lazyValue()) {
		throw Exception("" + message)
	}
}

inline fun _assert(lazyValue: () -> Boolean, lazyMessage: () -> Any) {
	if (assertionsEnabled && !lazyValue()) {
		val message = lazyMessage()
		throw Exception("" + message)
	}
}

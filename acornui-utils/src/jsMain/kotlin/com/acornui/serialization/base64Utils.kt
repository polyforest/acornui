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

package com.acornui.serialization

import kotlin.browser.window

object JsBase64 : Base64 {

	override fun encodeToString(src: ByteArray): String {
		val str = buildString(src.size) {
			src.forEach { append(it.toChar()) }
		}
		return window.btoa(str)
	}

	override fun decodeFromString(str: String): ByteArray {
		val decoded = window.atob(str)
		return ByteArray(decoded.length) { decoded[it].toByte() }
	}
}

actual val base64: Base64 = JsBase64
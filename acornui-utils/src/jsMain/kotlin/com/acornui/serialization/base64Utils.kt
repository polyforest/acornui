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

import com.acornui.system.userInfo
import kotlin.browser.window

object JsBase64 : Base64 {

	override fun encodeToString(src: ByteArray): String {
		val str = buildString(src.size) {
			src.forEach { append(it.toChar()) }
		}
		return if (userInfo.isBrowser) window.btoa(str) else Buffer.from(str, "binary").toString("base64") as String
	}

	override fun decodeFromString(str: String): ByteArray {
		val decoded = if (userInfo.isBrowser) window.atob(str) else Buffer.from(str, "base64").toString()
		return ByteArray(decoded.length) { decoded[it].toByte() }
	}
}

actual val base64: Base64 = JsBase64

external object Buffer {
	fun from(string: String, encoding: String): dynamic
}
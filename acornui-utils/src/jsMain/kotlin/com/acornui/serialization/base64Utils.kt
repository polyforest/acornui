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
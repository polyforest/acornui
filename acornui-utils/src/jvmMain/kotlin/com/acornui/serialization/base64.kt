package com.acornui.serialization

import java.nio.charset.StandardCharsets

object JvmBase64 : Base64 {

	override fun encodeToString(src: ByteArray): String {
		val encoder = java.util.Base64.getEncoder()
		return String(encoder.encode(src), StandardCharsets.ISO_8859_1)
	}

	override fun decodeFromString(str: String): ByteArray {
		return java.util.Base64.getDecoder().decode(str.toByteArray(StandardCharsets.ISO_8859_1))
	}
}

actual val base64: Base64 = JvmBase64
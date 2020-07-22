package com.acornui.serialization

import kotlin.test.Test
import kotlin.test.assertEquals

class Base64Test {
	@Test
	fun testEncodeToString() {
		checkEncodeToString("Kotlin is awesome", "S290bGluIGlzIGF3ZXNvbWU=")
	}

	@Test
	fun testPaddedStrings() {
		checkEncodeToString("", "")
		checkEncodeToString("1", "MQ==")
		checkEncodeToString("22", "MjI=")
		checkEncodeToString("333", "MzMz")
		checkEncodeToString("4444", "NDQ0NA==")

		checkDecodeToString("", "")
		checkDecodeToString("MQ==", "1")
		checkDecodeToString("MjI=", "22")
		checkDecodeToString("MzMz", "333")
		checkDecodeToString("NDQ0NA==", "4444")
	}

	private fun checkEncodeToString(input: String, expectedOutput: String) {
		assertEquals(expectedOutput, base64.encodeToUtf8String(input))
	}

	private fun checkDecodeToString(input: String, expectedOutput: String) {
		val decoded = base64.decodeFromString(input)
		val str = buildString(decoded.size) {
			decoded.forEach { append(it.toChar()) }
		}
		assertEquals(expectedOutput, str)
	}
}
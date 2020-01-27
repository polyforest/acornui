@file:Suppress("MapGetWithNotNullAssertionOperator")

package com.acornui.font

import com.acornui.collection.copy
import com.acornui.collection.removeFirst
import com.acornui.component.text.FontFamily
import com.acornui.component.text.FontStyle
import com.acornui.component.text.FontWeight
import com.acornui.serialization.jsonParse
import java.io.File
import kotlin.test.*

class ProcessFontsKtTest {

	@Test
	fun process() {
		val input = File(ProcessFontsKtTest::class.java.getResource("/fonts_unprocessedFonts").file)
		val output = input.resolveSibling("out/fonts")
		assertTrue(output.deleteRecursively())
		processFonts(input, output)

		val rubik = jsonParse(FontFamily.serializer(), output.resolve("Rubik/fonts.json").readText())
		val roboto = jsonParse(FontFamily.serializer(), output.resolve("Roboto/fonts.json").readText())

		checkFonts(roboto, listOf(36, 44), listOf(FontWeight.BLACK), listOf(FontStyle.NORMAL, FontStyle.ITALIC), "Roboto")
		checkFonts(rubik, listOf(36, 44), listOf(FontWeight.REGULAR), listOf(FontStyle.NORMAL), "Rubik")
	}

	private fun checkFonts(family: FontFamily, expectedSizes: List<Int>, expectedWeights: List<String>, expectedStyles: List<String>, face: String) {
		assertEquals(face, family.family)
		val remaining = family.fonts.copy()
		for (expectedSize in expectedSizes) {
			for (expectedWeight in expectedWeights) {
				for (expectedStyle in expectedStyles) {
					val found = remaining.removeFirst { it.weight == expectedWeight && it.style == expectedStyle && it.size == expectedSize }
					assertNotNull(found, "Expected font <face: $face, weight: $expectedWeight, style: $expectedStyle, size: $expectedSize")
				}
			}
		}
		if (remaining.isNotEmpty()) {
			fail("Unexpected fonts created $remaining")
		}
	}
}
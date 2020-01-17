@file:Suppress("MapGetWithNotNullAssertionOperator")

package com.acornui.font

import com.acornui.collection.copy
import com.acornui.collection.removeFirst
import com.acornui.component.text.FontStyle
import com.acornui.component.text.FontWeight
import com.acornui.component.text.FontsManifest
import com.acornui.serialization.jsonParse
import com.acornui.test.assertUnorderedListEquals
import kotlin.test.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.fail

class ProcessFontsKtTest {

	@Test
	fun process() {
		val input = File("build/processedResources/jvm/test/fonts_unprocessedFonts")
		val output = File("build/processedResources/jvm/test/out/fonts")
		output.deleteRecursively()
		processFonts(input, output)

		val fontsManifest = jsonParse(FontsManifest.serializer(), output.resolve("fonts.json").readText())
		assertUnorderedListEquals(listOf("Rubik", "Roboto"), fontsManifest.sets.keys)

		checkFonts(fontsManifest, listOf(36, 44), listOf(FontWeight.BLACK), listOf(FontStyle.NORMAL, FontStyle.ITALIC), "Roboto")
		checkFonts(fontsManifest, listOf(36, 44), listOf(FontWeight.REGULAR), listOf(FontStyle.NORMAL), "Rubik")
	}

	private fun checkFonts(manifest: FontsManifest, expectedSizes: List<Int>, expectedWeights: List<String>, expectedStyles: List<String>, face: String) {
		val remaining = manifest.sets[face]!!.fonts.copy()
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
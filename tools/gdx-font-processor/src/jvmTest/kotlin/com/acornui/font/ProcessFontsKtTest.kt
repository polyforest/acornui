package com.acornui.font

import com.acornui.component.text.FontsManifest
import com.acornui.serialization.jsonParse
import kotlin.test.Test
import java.io.File
import kotlin.test.assertEquals

class ProcessFontsKtTest {

	@Test
	fun process() {
		val input = File("build/processedResources/jvm/test/fonts_unprocessedFonts")
		val output = File("build/processedResources/jvm/test/out/fonts")
		output.deleteRecursively()
		processFonts(input, output)

		val fontsManifest = jsonParse(FontsManifest.serializer(), output.resolve("fonts.json").readText())
		val fontsManifestExpected = jsonParse(FontsManifest.serializer(), """{"sets":{"fonts":{"family":"fonts","sizes":[36,44],"fonts":[]},"Roboto":{"family":"Roboto","sizes":[36,44],"fonts":[{"path":"Roboto/Roboto-Black_36.fnt","weight":"black","style":"normal","size":36},{"path":"Roboto/Roboto-Black_44.fnt","weight":"black","style":"normal","size":44},{"path":"Roboto/Roboto-BlackItalic_36.fnt","weight":"black","style":"italic","size":36},{"path":"Roboto/Roboto-BlackItalic_44.fnt","weight":"black","style":"italic","size":44}]},"Rubik":{"family":"Rubik","sizes":[36,44],"fonts":[{"path":"Rubik/Rubik-Regular_36.fnt","weight":"regular","style":"normal","size":36},{"path":"Rubik/Rubik-Regular_44.fnt","weight":"regular","style":"normal","size":44}]}}}""")
		assertEquals(fontsManifestExpected, fontsManifest)
	}
}
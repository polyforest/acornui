package com.acornui.font

import org.junit.Test
import java.io.File

class ProcessFontsKtTest {

	@Test
	fun process() {
		val input = File("build/resources/test/fonts_unprocessedFonts")
		if (input.exists())
			processFonts(File("build/resources/test/fonts_unprocessedFonts"), File("build/resources/test/out/fonts"))
	}
}
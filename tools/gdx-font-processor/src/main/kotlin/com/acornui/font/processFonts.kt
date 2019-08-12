package com.acornui.font

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.PixmapPacker
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter
import com.badlogic.gdx.tools.bmfont.BitmapFontWriter
import com.badlogic.gdx.utils.Json
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
	processFonts(File(args[0]), File(args[1]))
	exitProcess(0)
}

/**
 * Processes the fonts in the given [inputDir] and produces bitmap fonts usable by acorn in [outputDir].
 * There is expected to be a settings.json file in the [inputDir] describing the settings to use for processing.
 */
fun processFonts(inputDir: File, outputDir: File) {
	val app = HeadlessApplication(object : ApplicationAdapter() {})

	val info = BitmapFontWriter.FontInfo()
	val settingsFile = inputDir.resolve("settings.json")
	if (!settingsFile.exists()) {
		error("Settings file does not exist. ${settingsFile.path}")
	}
	val json = Json()
	val settings = json.fromJson(FontGeneratorSettings::class.java, settingsFile.readText())
	check(settings.sizes.isNotEmpty()) { "font sizes must not be empty." }

	info.padding = BitmapFontWriter.Padding(settings.padding, settings.padding, settings.padding, settings.padding)
	info.spacing.horizontal = settings.spacing
	info.spacing.vertical = settings.spacing

	val folders = inputDir.listFiles { dir, name ->
		File(dir, name).isDirectory
	}!! + inputDir

	val fontSources = ArrayList<File>()
	for (folder in folders) {
		fontSources += folder.listFiles { _, name ->
			name.endsWith(".ttf", ignoreCase = true)
		}!!
	}

	for (fontSource in fontSources) {
		val folder = fontSource.parentFile.relativeTo(inputDir).path
		val outputFolder = File(outputDir, folder)
		val imagesDir = File(outputFolder, settings.imagesDir)
		val name = fontSource.nameWithoutExtension

		val param: FreeTypeFontParameter = settings.defaultFontSettings ?: FreeTypeFontParameter()

		for (size in settings.sizes) {
			param.size = size
			param.packer = PixmapPacker(settings.pageWidth, settings.pageHeight, Pixmap.Format.RGBA8888, 2, false, PixmapPacker.SkylineStrategy())

			val generator = FreeTypeFontGenerator(Gdx.files.absolute(fontSource.path))
			val data = generator.generateData(param)
			info.face = name
			info.bold = name.contains("bold", ignoreCase = true)
			info.italic = name.contains("italic", ignoreCase = true)
			info.size = param.size
			val fontName = if (settings.sizes.isEmpty()) name else "${name}_$size"
			val pngFiles = BitmapFontWriter.writePixmaps(param.packer.pages, Gdx.files.absolute(imagesDir.absolutePath), fontName)
			BitmapFontWriter.writeFont(data, pngFiles,
					Gdx.files.absolute("${outputFolder.absolutePath}/$fontName.fnt"), info, settings.pageWidth, settings.pageHeight)
		}
	}
	app.exit()
}

class FontGeneratorSettings {

	var imagesDir: String = "."
	var pageWidth: Int = 512
	var pageHeight: Int = 512
	var padding: Int = 2
	var spacing: Int = 2

	var sizes: List<Int> = listOf(14)
	var defaultFontSettings: FreeTypeFontParameter? = null
}
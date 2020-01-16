package com.acornui.font

import com.acornui.component.text.*
import com.acornui.io.file.Path
import com.acornui.serialization.jsonStringify
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

fun main(args: Array<String>) {
	processFonts(File(args[0]), File(args[1]))
}

/**
 * Processes the fonts in the given [inputDir] and produces bitmap fonts usable by acorn in [outputDir].
 * There is expected to be a settings.json file in either the [inputDir] or the font folder describing the settings to
 * use for processing.
 */
fun processFonts(inputDir: File, outputDir: File, fontsManifestFilename: String = "fonts.json") {
	val app = HeadlessApplication(object : ApplicationAdapter() {})

	val info = BitmapFontWriter.FontInfo()
	val settingsFile = inputDir.resolve("settings.json")
	val json = Json()
	val settings = if (settingsFile.exists()) json.fromJson(FontGeneratorSettings::class.java, settingsFile.readText()) else FontGeneratorSettings()
	check(settings.sizes.isNotEmpty()) { "font sizes must not be empty." }

	info.padding = BitmapFontWriter.Padding(settings.padding, settings.padding, settings.padding, settings.padding)
	info.spacing.horizontal = settings.spacing
	info.spacing.vertical = settings.spacing

	val fontSets = ArrayList<FontFamily>()

	inputDir.walkTopDown().forEach { folder ->
		if (!folder.isDirectory) return@forEach

		// Settings file override
		val settingsFileOverride = inputDir.resolve("settings.json")
		val settingsFinal = if (settingsFileOverride.exists()) json.fromJson(FontGeneratorSettings::class.java, settingsFileOverride.readText()) else settings
		val folderRelPath = folder.relativeTo(inputDir).path

		val fonts = ArrayList<Font>()

		// Process all .ttf files
		folder.listFiles { _, name ->
			name.endsWith(".ttf", ignoreCase = true)
		}!!.forEach { fontSource ->
			val outputFolder = File(outputDir, folderRelPath)
			val imagesDir = File(outputFolder, settingsFinal.imagesDir)
			val name = fontSource.nameWithoutExtension

			val param: FreeTypeFontParameter = settingsFinal.defaultFontSettings ?: FreeTypeFontParameter()

			val weight = FontWeight.values.find { name.contains(it, ignoreCase = true) } ?: FontWeight.REGULAR
			val style = if (name.contains("italic", ignoreCase = true)) FontStyle.ITALIC else FontStyle.NORMAL
			val isItalic = style == FontStyle.ITALIC
			val isBold = FontWeight.values.indexOf(weight) > FontWeight.values.indexOf(FontWeight.REGULAR)

			for (size in settingsFinal.sizes) {
				param.size = size
				param.packer = PixmapPacker(settingsFinal.pageWidth, settingsFinal.pageHeight, Pixmap.Format.RGBA8888, 2, false, PixmapPacker.SkylineStrategy())

				val generator = FreeTypeFontGenerator(Gdx.files.absolute(fontSource.path))
				val data = generator.generateData(param)
				info.face = name
				info.bold = isBold
				info.italic = isItalic
				info.size = param.size
				val fontName = if (settingsFinal.sizes.isEmpty()) name else "${name}_$size"
				val pngFiles = BitmapFontWriter.writePixmaps(param.packer.pages, Gdx.files.absolute(imagesDir.absolutePath), fontName)
				BitmapFontWriter.writeFont(data, pngFiles,
						Gdx.files.absolute("${outputFolder.absolutePath}/$fontName.fnt"), info, settingsFinal.pageWidth, settingsFinal.pageHeight)

				fonts.add(Font(path = Path(folderRelPath, "$fontName.fnt" ).value, weight = weight, style = style, size = size))
			}
		}

		// Write a fontSet descriptor
		val fontSet = FontFamily(
				family = folder.name.removeSuffix("_unprocessedFonts"),
				sizes = settingsFinal.sizes,
				fonts = fonts
		)
		fontSets.add(fontSet)
	}
	outputDir.resolve(fontsManifestFilename).writeText(jsonStringify(
			FontsManifest.serializer(),
			FontsManifest(fontSets.map { font -> font.family to font }.toMap())
	))

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
package com.acornui.skins

import com.acornui.collection.sortedInsertionIndex
import com.acornui.component.text.BitmapFontRequest
import com.acornui.component.text.FontSize
import com.acornui.component.text.FontStyle
import com.acornui.component.text.FontWeight
import com.acornui.filterWithWords
import com.acornui.io.file.Directory
import com.acornui.io.file.FileEntry
import com.acornui.io.file.Files
import com.acornui.math.MathUtils

object FontPathResolver {

	var fontsDir = "assets/fonts/"

	private val fileSizeRegex = Regex("""_(\d+)$""")

	/**
	 * The size check regex will provide the size of the font based on the filename.
	 */
	var sizeCheck: (filename: String) -> Int = {
		fileSizeRegex.find(it)?.groups?.get(1)?.value?.toInt() ?: -1
	}

	private val supportedSizesCache = HashMap<Directory, List<Int>>()

	/**
	 * Returns the path to the font with the given characteristics.
	 */
	fun getPath(theme: Theme, files: Files, request: BitmapFontRequest): FileEntry? {
		val dir = files.getDir(fontsDir)?.getDir(request.family) ?: return null

		val supportedSizes = supportedSizesCache.getOrPut(dir) {
			val foundSizes = mutableSetOf<Int>()
			dir.files.values.forEach {
				if (it.hasExtension("fnt")) {
					foundSizes.add(sizeCheck(it.nameNoExtension))
				}
			}
			foundSizes.toList().filter { it > 0 }.sorted()
		}

		val desiredPt = theme.fontSizes[request.size] ?: error("Unknown size: ${request.size}")
		val scaledSize = (desiredPt.toFloat() * request.fontPixelDensity).toInt()
		val nearestSupportedIndex = MathUtils.clamp(supportedSizes.sortedInsertionIndex(scaledSize, matchForwards = false), 0, supportedSizes.lastIndex)
		val nearestSize = supportedSizes[nearestSupportedIndex]

		val matchedFont = dir.files.keys.filterWithWords(listOf(
				if (request.style == FontStyle.NORMAL) "" else request.style,
				nearestSize.toString(),
				"fnt"
		) + request.family.split("_") + if (request.weight == FontWeight.REGULAR && request.style != FontStyle.NORMAL) emptyList() else request.weight.split("-")).firstOrNull()
				?: return null
		return dir.getFile(matchedFont)
	}
}
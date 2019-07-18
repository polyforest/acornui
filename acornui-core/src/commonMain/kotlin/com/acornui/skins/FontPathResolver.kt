package com.acornui.skins

import com.acornui.collection.sortedInsertionIndex
import com.acornui.component.text.BitmapFontRequest
import com.acornui.component.text.FontSize
import com.acornui.component.text.FontStyle
import com.acornui.component.text.FontWeight
import com.acornui.core.filterWithWords
import com.acornui.core.io.file.FileEntry
import com.acornui.core.io.file.Files
import com.acornui.math.MathUtils

object FontPathResolver {

	var fontsDir = "assets/uiskin/fonts/"

	var sizeToPtMap = mutableMapOf(
			FontSize.EXTRA_SMALL to 10,
			FontSize.SMALL to 14,
			FontSize.REGULAR to 18,
			FontSize.LARGE to 22,
			FontSize.EXTRA_LARGE to 32
	)

	/**
	 * The font sizes available.
	 */
	var supportedSizes = listOf(10, 14, 18, 20, 22, 28, 32, 36, 44, 64)

	/**
	 * Returns the path to the font with the given characteristics.
	 */
	fun getPath(files: Files, request: BitmapFontRequest): FileEntry? {
		val dir = files.getDir(fontsDir)?.getDir(request.family) ?: return null

		val desiredPt = sizeToPtMap[request.size] ?: error("Unknown size: ${request.size}")
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
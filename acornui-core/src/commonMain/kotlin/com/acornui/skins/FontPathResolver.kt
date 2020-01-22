package com.acornui.skins

import com.acornui.asset.loadAndCacheJsonAsync
import com.acornui.collection.sortedInsertionIndex
import com.acornui.component.text.BitmapFontRequest
import com.acornui.component.text.FontFamily
import com.acornui.di.Scoped
import com.acornui.io.file.Path
import com.acornui.math.MathUtils

object FontPathResolver {

	var fontsDir = "assets/fonts/{family}"
	var manifestFilename = "fonts.json"

	/**
	 * Returns the path to the font with the given characteristics.
	 */
	suspend fun getPath(scope: Scoped, theme: Theme, request: BitmapFontRequest): String? {
		val familyPath = fontsDir.replace("{family}", request.family)
		val set = scope.loadAndCacheJsonAsync(FontFamily.serializer(), Path(familyPath, manifestFilename).value).await()
		val desiredPt = theme.fontSizes[request.size] ?: error("Unknown size: ${request.size}")
		val scaledSize = (desiredPt.toFloat() * request.fontPixelDensity).toInt()
		val nearestSupportedIndex = MathUtils.clamp(set.sizes.sortedInsertionIndex(scaledSize, matchForwards = false), 0, set.sizes.lastIndex)
		val nearestSize = set.sizes[nearestSupportedIndex]
		val found = set.fonts.find { it.size == nearestSize && it.style == request.style && it.weight == request.weight } ?: return null
		return Path(familyPath, found.path).value
	}
}
package com.acornui.skins

import com.acornui.asset.loadAndCacheJsonAsync
import com.acornui.collection.sortedInsertionIndex
import com.acornui.component.text.BitmapFontRequest
import com.acornui.component.text.FontFamily
import com.acornui.di.Context
import com.acornui.io.file.Path
import com.acornui.math.MathUtils

object FontPathResolver {

	var fontsDir = "assets/fonts/{family}"
	var manifestFilename = "fonts.json"

	suspend fun loadFamily(scope: Context, family: String): FontFamily {
		val facePath = fontsDir.replace("{family}", family)
		return scope.loadAndCacheJsonAsync(FontFamily.serializer(), Path(facePath, manifestFilename).value).await()
	}

	/**
	 * Returns the path to the font with the given characteristics.
	 */
	suspend fun getPath(scope: Context, theme: Theme, request: BitmapFontRequest): String? {
		val familyPath = fontsDir.replace("{family}", request.family)
		val set: FontFamily = loadFamily(scope, request.family)
		val desiredPt = theme.fontSizes[request.size] ?: error("Unknown size: ${request.size}")
		val scaledSize = (desiredPt.toFloat() * request.fontPixelDensity).toInt()
		val nearestSupportedIndex = MathUtils.clamp(set.sizes.sortedInsertionIndex(scaledSize, matchForwards = false), 0, set.sizes.lastIndex)
		val nearestSize = set.sizes[nearestSupportedIndex]
		val found = set.fonts.find { it.size == nearestSize && it.style == request.style && it.weight == request.weight } ?: return null
		return Path(familyPath, found.path).value
	}
}
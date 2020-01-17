package com.acornui.skins

import com.acornui.asset.loadAndCacheJsonAsync
import com.acornui.collection.sortedInsertionIndex
import com.acornui.component.text.BitmapFontRequest
import com.acornui.component.text.FontsManifest
import com.acornui.di.Scoped
import com.acornui.io.file.Path
import com.acornui.math.MathUtils

object FontPathResolver {

	var fontsDir = "assets/fonts/"
	var manifestFilename = "fonts.json"

	private val fileSizeRegex = Regex("""_(\d+)$""")

	/**
	 * The size check regex will provide the size of the font based on the filename.
	 */
	var sizeCheck: (filename: String) -> Int = {
		fileSizeRegex.find(it)?.groups?.get(1)?.value?.toInt() ?: -1
	}

	/**
	 * Returns the path to the font with the given characteristics.
	 */
	suspend fun getPath(scope: Scoped, theme: Theme, request: BitmapFontRequest): String? {
		val manifest = scope.loadAndCacheJsonAsync(FontsManifest.serializer(), Path(fontsDir, manifestFilename).value).await()
		val set = manifest.sets[request.family] ?: return null
		val desiredPt = theme.fontSizes[request.size] ?: error("Unknown size: ${request.size}")
		val scaledSize = (desiredPt.toFloat() * request.fontPixelDensity).toInt()
		val nearestSupportedIndex = MathUtils.clamp(set.sizes.sortedInsertionIndex(scaledSize, matchForwards = false), 0, set.sizes.lastIndex)
		val nearestSize = set.sizes[nearestSupportedIndex]
		val found = set.fonts.find { it.size == nearestSize && it.style == request.style && it.weight == request.weight } ?: return null
		return Path(fontsDir, found.path).value
	}
}
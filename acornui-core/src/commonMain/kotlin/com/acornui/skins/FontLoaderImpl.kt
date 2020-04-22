package com.acornui.skins

import com.acornui.asset.loadAndCacheJsonAsync
import com.acornui.component.text.*
import com.acornui.di.Context
import com.acornui.di.ContextImpl
import com.acornui.io.file.Path
import com.acornui.logging.Log
import kotlin.math.abs

class FontLoaderImpl(owner: Context,

					 /**
					  * The path to load fonts.
					  * This may have the token `{family}` which will be replaced by the requested font family name.
					  */
					 private val fontsDir: String = "assets/fonts/{family}",
					 private val manifestFilename: String = "fonts.json"
) : FontLoader, ContextImpl(owner) {

	override suspend fun loadAndCacheFamily(family: String): FontFamily {
		val familyPath = fontsDir.replace("{family}", family)
		return loadAndCacheJsonAsync(FontFamily.serializer(), Path(familyPath, manifestFilename).value).await()
	}

	override suspend fun loadAndCacheFont(family: FontFamily, sizePx: Int, weight: String, style: String): BitmapFont {
		if (family.fonts.isEmpty()) error("Fonts in family '${family.familyDisplayName}' are empty.")
		val familyPath = fontsDir.replace("{family}", family.family)
		val match = family.fonts.bestMatch(sizePx, weight, style)
		if (!match.equals(sizePx, weight, style)) {
			Log.warn("Exact font not found family: ${family.family} size: $sizePx weight: $weight, style: $style, using fallback: $match")
		}
		return loadAndCacheFontFromDir(Path(familyPath, match.path).value)

	}
}

private fun List<Font>.bestMatch(sizePx: Int, weight: String, style: String): Font {
	var score = 0f
	var best: Font = first()
	for (i in 0..lastIndex) {
		val font = this[i]
		var iScore = 0f
		if (font.weight == weight) iScore += 1f
		else if (abs(FontWeight.values.indexOf(font.weight) - FontWeight.values.indexOf(weight)) <= 1) iScore += 0.5f // Partial credit if the weight is close.
		if (font.style == style) iScore += 1f
		if (font.size == sizePx) iScore += 1f
		else if (abs(font.size - sizePx) <= 2) iScore += 0.5f // Partial credit if the size is close.
		if (iScore > score) {
			best = font
			score = iScore
		}
	}
	return best
}

private fun Font.equals(sizePx: Int, weight: String, style: String): Boolean =
		this.size == sizePx && this.style == style && this.weight == weight
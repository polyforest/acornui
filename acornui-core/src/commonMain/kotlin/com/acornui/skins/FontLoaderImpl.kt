package com.acornui.skins

import com.acornui.asset.loadAndCacheJsonAsync
import com.acornui.component.text.BitmapFont
import com.acornui.component.text.FontFamily
import com.acornui.component.text.FontLoader
import com.acornui.component.text.loadAndCacheFontFromDir
import com.acornui.di.Context
import com.acornui.di.ContextImpl
import com.acornui.io.file.Path

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
		val familyPath = fontsDir.replace("{family}", family.family)
		val found = family.fonts.find { it.size == sizePx && it.style == style && it.weight == weight }
				?: error("Font not found {family: ${family.family} size: $sizePx weight: $weight, style: $style")
		return loadAndCacheFontFromDir(Path(familyPath, found.path).value)

	}
}
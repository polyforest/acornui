/*
 * Copyright 2019 Poly Forest, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.acornui.component.text

import com.acornui.asset.*
import com.acornui.collection.sortedInsertionIndex
import com.acornui.di.Context
import com.acornui.di.dependencyFactory
import com.acornui.gl.core.TextureMagFilter
import com.acornui.gl.core.TextureMinFilter
import com.acornui.graphic.Texture
import com.acornui.graphic.TextureAtlasData
import com.acornui.graphic.configure
import com.acornui.graphic.scaleIfPastAffordance
import com.acornui.io.file.Path
import com.acornui.logging.Log
import com.acornui.math.IntRectangle
import com.acornui.math.IntRectangleRo
import com.acornui.skins.FontLoaderImpl
import kotlinx.coroutines.*

/**
 * @author nbilyk
 */
class BitmapFont(

		val data: BitmapFontData,

		/**
		 * The textures corresponding to [GlyphData.page]
		 */
		val pages: List<Texture>,

		/**
		 * The glyph texture information.
		 */
		val glyphs: Map<Char, Glyph?>,

		val premultipliedAlpha: Boolean

) {

	/**
	 * Returns the glyph associated with the provided [char]. If the glyph is not found, if the [char] is whitespace,
	 * an empty glyph will be returned. If the glyph is not found and is not whitespace, a missing glyph placeholder
	 * will be returned.
	 */
	fun getGlyphSafe(char: Char): Glyph {
		val existing = glyphs[char]
		if (existing != null) return existing
		if (char.isWhitespace()) {
			return glyphs[GlyphData.EMPTY_CHAR]!!
		}
		return glyphs[char]
				?: glyphs[GlyphData.WHITE_SQUARE]!!
	}

}

/**
 * The data required to render a glyph inside of a texture.
 */
class Glyph(

		val data: GlyphData,

		/**
		 * The x offset of the top-left u,v coordinate.
		 * In pixels..
		 */
		val offsetX: Int,

		/**
		 * The y offset of the top-left u,v coordinate.
		 * In pixels..
		 */
		val offsetY: Int,

		/**
		 * The untransformed width of the glyph.
		 * In pixels..
		 */
		val width: Int,

		/**
		 * The untransformed height of the glyph.
		 * In pixels..
		 */
		val height: Int,

		/**
		 * How much the current position should be advanced after drawing the character.
		 * In pixels..
		 */
		val advanceX: Int,

		/**
		 * If not rotated, the uv coordinates are:
		 * Top-left: (u, v), Top-right: (u2, v), Bottom-right: (u2, v2), Bottom-left: (u, v2)
		 * If rotated, the uv coordinates are:
		 * Top-left: (u2, v), Top-right: (u2, v2), Bottom-right: (u, v2), Bottom-left: (u, v)
		 */
		val isRotated: Boolean,

		/**
		 * The region within the [texture].
		 * In pixels..
		 */
		val region: IntRectangleRo,

		val texture: Texture,

		val premultipliedAlpha: Boolean
) {

	/**
	 * Gets the kerning of this glyph for the given character.
	 * This is the horizontal adjustment.
	 * In pixels.
	 */
	fun getKerning(ch: Char): Int = data.getKerning(ch)
}

/**
 * An overload of [loadAndCacheFontFromDir] where the images directory is assumed to be the parent directory of the font data file.
 */
suspend fun Context.loadAndCacheFontFromDir(fontPath: String, cacheSet: CacheSet = cacheSet()): BitmapFont {
	return loadAndCacheFontFromDir(fontPath, Path(fontPath).parent.value, cacheSet)
}

/**
 * Loads and caches a font where the images are standalone files in the specified directory.
 * @param fontPath The path of the font data file. (By default the font data loader expects a .fnt file
 * in the AngelCode format, but this can be changed by associating a different type of loader for the
 * BitmapFontData asset type.)
 * @param imagesDir The directory of images.
 * @param cacheSet The caching group, to allow the loaded assets to be disposed as one.
 * @return Returns the loaded [BitmapFont].
 */
suspend fun Context.loadAndCacheFontFromDir(fontPath: String, imagesDir: String, cacheSet: CacheSet = cacheSet()): BitmapFont {
	return cacheSet.getOrPutAsync(fontPath) {
		loadFontFromDir(fontPath, imagesDir)
	}.await()
}

/**
 * Loads a font where the images are standalone files in the specified directory.
 * @param fontPath The path of the font data file. (By default the font data loader expects a .fnt file
 * in the AngelCode format, but this can be changed by associating a different type of loader for the
 * BitmapFontData asset type.)
 * @param imagesDir The directory of images.
 * @return Returns the loaded [BitmapFont].
 */
suspend fun Context.loadFontFromDir(fontPath: String, imagesDir: String): BitmapFont = withContext(Dispatchers.Default) {
	val dir = Path(imagesDir)
	val bitmapFontData = AngelCodeParser.parse(loadText(fontPath))

	val n = bitmapFontData.pages.size
	val pageTextureLoaders = ArrayList<Deferred<Texture>>()
	for (i in 0 until n) {
		val page = bitmapFontData.pages[i]
		val imageFile = dir.resolve(page.imagePath)
		pageTextureLoaders.add(async {
			configureFontTexture(loadTexture(imageFile.value))
		})
	}
	// Finished loading the font and all its textures.
	val glyphs = HashMap<Char, Glyph>()
	// Calculate the uv coordinates for each glyph.
	for (glyphData in bitmapFontData.glyphs.values) {
		val texture = pageTextureLoaders[glyphData.page].await()
		glyphs[glyphData.char] = Glyph(
				data = glyphData,
				offsetX = glyphData.offsetX,
				offsetY = glyphData.offsetY,
				width = glyphData.region.width,
				height = glyphData.region.height,
				advanceX = glyphData.advanceX,
				isRotated = false,
				region = glyphData.region.copy(),
				texture = texture,
				premultipliedAlpha = false
		)
	}

	val font = BitmapFont(
			bitmapFontData,
			pages = pageTextureLoaders.awaitAll(),
			premultipliedAlpha = false,
			glyphs = glyphs
	)
	Log.info("Font loaded $fontPath")
	font
}


@Deprecated("Not used")
suspend fun Context.loadFontFromAtlas(fontKey: String, atlasPath: String, cacheSet: CacheSet = cacheSet()): BitmapFont {
	val atlasFile = Path(atlasPath)

	val bitmapFontData = cacheSet.getOrPutAsync(fontKey) {
		AngelCodeParser.parse(loadText(fontKey))
	}.await()
	val atlasData = loadAndCacheJsonAsync(TextureAtlasData.serializer(), atlasPath, cacheSet).await()
	val atlasPageTextures = ArrayList<Deferred<Texture>>()

	for (atlasPageIndex in 0..atlasData.pages.lastIndex) {
		val atlasPageData = atlasData.pages[atlasPageIndex]
		val textureEntry = atlasFile.sibling(atlasPageData.texturePath)
		atlasPageTextures.add(cacheSet.getOrPutAsync(textureEntry.value) {
			atlasPageData.configure(loadTexture(textureEntry.value))
		})
	}

	// Finished loading the font and all its textures.

	// Calculate the uv coordinates for each glyph.
	val glyphs = HashMap<Char, Glyph>()
	for (glyphData in bitmapFontData.glyphs.values) {
		val fontPageData = bitmapFontData.pages[glyphData.page]
		val (page, region) = atlasData.findRegion(fontPageData.imagePath)!!

		val leftPadding = region.padding[0]
		val topPadding = region.padding[1]

		var regionX: Int
		var regionY: Int
		var regionW: Int
		var regionH: Int
		var offsetX = 0
		var offsetY = 0
		var offsetR = 0

		if (region.isRotated) {
			regionX = (region.bounds.right + topPadding) - glyphData.region.bottom
			regionY = glyphData.region.x - leftPadding + region.bounds.y
			regionW = glyphData.region.height
			regionH = glyphData.region.width
		} else {
			regionX = glyphData.region.x + region.bounds.x - leftPadding
			regionY = glyphData.region.y + region.bounds.y - topPadding
			regionW = glyphData.region.width
			regionH = glyphData.region.height
		}

		// Account for glyph regions being cut off from whitespace stripping.

		if (regionX < region.bounds.x) {
			val diff = region.bounds.x - regionX
			regionW -= diff
			regionX += diff
			offsetX += diff
		}
		if (regionY < region.bounds.y) {
			val diff = region.bounds.y - regionY
			regionH -= diff
			regionY += diff
			offsetY += diff
		}
		if (regionX + regionW > region.bounds.right) {
			val diff = regionX + regionW - region.bounds.right
			regionW -= diff
			offsetR += diff
		}
		if (regionY + regionH > region.bounds.bottom) {
			val diff = regionY + regionH - region.bounds.bottom
			regionH -= diff
		}

		val pageIndex = atlasData.pages.indexOf(page)
		val atlasTexture = atlasPageTextures[pageIndex]

		glyphs[glyphData.char] = Glyph(
				data = glyphData,
				offsetX = glyphData.offsetX + if (region.isRotated) offsetY else offsetX,
				offsetY = glyphData.offsetY + if (region.isRotated) offsetR else offsetY,
				width = if (region.isRotated) regionH else regionW,
				height = if (region.isRotated) regionW else regionH,
				advanceX = glyphData.advanceX,
				isRotated = region.isRotated,
				region = IntRectangle(regionX, regionY, regionW, regionH),
				texture = atlasTexture.await(),
				premultipliedAlpha = page.premultipliedAlpha
		)
	}
	return BitmapFont(
			bitmapFontData,
			pages = atlasPageTextures.map { it.await() },
			premultipliedAlpha = false,
			glyphs = glyphs
	)
}

interface FontLoader {

	/**
	 * Given a desired font size in px, returns the closest supported size (in px).
	 */
	fun getBestPxSize(sizePx: Int, family: FontFamily): Int {
		val nearestSupportedIndex = minOf(family.sizes.sortedInsertionIndex(sizePx, matchForwards = false), family.sizes.lastIndex)
		return family.sizes[nearestSupportedIndex]
	}

	suspend fun loadAndCacheFamily(family: String): FontFamily

	/**
	 * @param family
	 */
	suspend fun loadAndCacheFont(family: FontFamily, sizePx: Int, weight: String, style: String): BitmapFont

	companion object : Context.Key<FontLoader> {

		override val factory = dependencyFactory {
			FontLoaderImpl(it)
		}
	}
}

/**
 * A utility method around font loading that calculates the font face based off of a resolved [CharStyle] object.
 * @return A
 */
suspend fun FontLoader.loadAndCacheFont(charStyle: CharStyle): FontAndDpiScaling {
	val screenDensity = maxOf(charStyle.scaleX, charStyle.scaleY)

	// Calculate the desired font size in pixels based off the pixel density and font size key.
	val desiredDp = (charStyle.fontSizes[charStyle.fontSize] ?: error("Unknown size: ${charStyle.fontSize}")).toFloat()
	val desiredPx = (desiredDp * screenDensity + 0.5f).toInt()

	val fontFamily = loadAndCacheFamily(charStyle.fontFamily
			?: error("fontFamily is expected to be provided by the skin."))
	val actualPx = getBestPxSize(desiredPx, fontFamily)
	val actualDp = actualPx.toFloat() / screenDensity
	val font = loadAndCacheFont(fontFamily, actualPx, charStyle.fontWeight, charStyle.fontStyle)

	val scale = scaleIfPastAffordance(actualDp, desiredDp, charStyle.scalingSnapAffordance)
	return FontAndDpiScaling(font, charStyle.scaleX * scale, charStyle.scaleY * scale)
}

data class FontAndDpiScaling(

		/**
		 * The loaded bitmap font.
		 */
		val font: BitmapFont,

		/**
		 * The x scaling of dp to pixels.
		 */
		val scaleX: Float,

		/**
		 * The y scaling of dp to pixels.
		 */
		val scaleY: Float)

private fun configureFontTexture(target: Texture): Texture {
	target.filterMin = TextureMinFilter.LINEAR_MIPMAP_LINEAR
	target.filterMag = TextureMagFilter.LINEAR
	target.hasWhitePixel = false
	return target
}
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

import com.acornui.action.Decorator
import com.acornui.async.Deferred
import com.acornui.async.then
import com.acornui.collection.*
import com.acornui.recycle.Clearable
import com.acornui.core.Disposable
import com.acornui.core.asset.*
import com.acornui.core.di.Scoped
import com.acornui.core.di.inject
import com.acornui.core.graphic.AtlasPageDecorator
import com.acornui.core.graphic.Texture
import com.acornui.core.graphic.TextureAtlasDataSerializer
import com.acornui.core.io.file.Files
import com.acornui.core.isWhitespace2
import com.acornui.gl.core.TextureMagFilter
import com.acornui.gl.core.TextureMinFilter
import com.acornui.logging.Log
import com.acornui.math.IntRectangle
import com.acornui.math.IntRectangleRo

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
		if (char.isWhitespace2() || char.toInt() > 0xFF) {
			return glyphs[GlyphData.EMPTY_CHAR]!!
		}
		return glyphs[char] ?: glyphs[GlyphData.UNKNOWN_CHAR]!!
	}

}

/**
 * The data required to render a glyph inside of a texture.
 */
class Glyph(

		val data: GlyphData,

		/**
		 * The x offset of the top-left u,v coordinate.
		 */
		val offsetX: Int,

		/**
		 * The y offset of the top-left u,v coordinate.
		 */
		val offsetY: Int,

		/**
		 * The untransformed width of the glyph.
		 */
		val width: Int,

		/**
		 * The untransformed height of the glyph.
		 */
		val height: Int,

		/**
		 * How much the current position should be advanced after drawing the character.
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
		 */
		val region: IntRectangleRo,

		val texture: Texture,

		val premultipliedAlpha: Boolean
) {
	fun getKerning(ch: Char): Int = data.getKerning(ch)
}

/**
 * An overload of [loadFontFromDir] where the images directory is assumed to be the parent directory of the font data file.
 */
suspend fun Scoped.loadFontFromDir(fontPath: String, group: CachedGroup = cachedGroup()): BitmapFont {
	val files = inject(Files)
	val fontFile = files.getFile(fontPath) ?: throw Exception("$fontPath not found.")
	return loadFontFromDir(fontPath, fontFile.parent!!.path, group)
}

/**
 * Loads a font where the images are standalone files in the specified directory.
 * @param fontPath The path of the font data file. (By default the font data loader expects a .fnt file
 * in the AngelCode format, but this can be changed by associating a different type of loader for the
 * BitmapFontData asset type.)
 * @param imagesDir The directory of images.
 * @param group The caching group, to allow the loaded assets to be disposed as one.
 */
suspend fun Scoped.loadFontFromDir(fontPath: String, imagesDir: String, group: CachedGroup = cachedGroup()): BitmapFont {
	val files = inject(Files)
	val dir = files.getDir(imagesDir) ?: throw Exception("Directory not found: $imagesDir")
	val bitmapFontData = loadAndCache(fontPath, AssetType.TEXT, AngelCodeParser, group).await()

	val n = bitmapFontData.pages.size
	val pageTextures = ArrayList<Deferred<Texture>>()
	for (i in 0..n - 1) {
		val page = bitmapFontData.pages[i]
		val imageFile = dir.getFile(page.imagePath)
				?: throw Exception("Font image file not found: ${page.imagePath}")
		pageTextures.add(loadAndCache(imageFile.path, AssetType.TEXTURE, FontTextureDecorator, group))
	}
	// Finished loading the font and all its textures.
	val glyphs = HashMap<Char, Glyph>()
	// Calculate the uv coordinates for each glyph.
	for (glyphData in bitmapFontData.glyphs.values) {
		val texture = pageTextures[glyphData.page].await()
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
			pages = pageTextures.map { it.await() },
			premultipliedAlpha = false,
			glyphs = glyphs
	)
	Log.info("Font loaded $fontPath")
	return font
}

suspend fun Scoped.loadFontFromAtlas(fontKey: String, atlasPath: String, group: CachedGroup = cachedGroup()): BitmapFont {
	val files = inject(Files)
	val atlasFile = files.getFile(atlasPath) ?: throw Exception("File not found: $atlasPath")

	val bitmapFontData = loadAndCache(fontKey, AssetType.TEXT, AngelCodeParser, group).await()
	val atlasData = loadAndCacheJson(atlasPath, TextureAtlasDataSerializer, group).await()
	val atlasPageTextures = ArrayList<Deferred<Texture>>()

	for (atlasPageIndex in 0..atlasData.pages.lastIndex) {
		val atlasPageData = atlasData.pages[atlasPageIndex]
		val textureEntry = atlasFile.siblingFile(atlasPageData.texturePath)
				?: throw Exception("File not found: ${atlasPageData.texturePath} relative to: $atlasPath")
		atlasPageTextures.add(loadAndCache(textureEntry.path, AssetType.TEXTURE, AtlasPageDecorator(atlasPageData), group))
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
	val font = BitmapFont(
			bitmapFontData,
			pages = atlasPageTextures.map { it.await() },
			premultipliedAlpha = false,
			glyphs = glyphs
	)
	return font
}


typealias FontResolver = (family: String, size: String, weight: String, style: String) -> Deferred<BitmapFont>?

object BitmapFontRegistry : Clearable, Disposable {

	/**
	 * family, size, weight, style -> Deferred<BitmapFont>
	 */
	private val registry: MutableMultiMap4<String, String, String, String, Deferred<BitmapFont>> = multiMap4()

	/**
	 * If set, when a font is requested that isn't registered, this font resolver will be used to load the font with
	 * the given key.
	 */
	var fontResolver: FontResolver? = null

	/**
	 * Registers a bitmap font to a font path.
	 */
	private fun register(family: String, size: String, weight: String, style: String, fontPromise: Deferred<BitmapFont>) {
		registry[family][size][weight][style]?.let { oldFontPromise ->
			oldFontPromise.then {
				for (page in it.pages) {
					page.refDec()
				}
			}
		}
		registry[family][size][weight][style] = fontPromise
		fontPromise.then {
			for (page in it.pages) {
				page.refInc()
			}
		}
	}

	/**
	 * Returns the font registered to the given style. Throws an exception if the font is not registered.
	 *
	 * @param family
	 * @param size
	 * @param weight
	 * @param style
	 */
	fun getFont(family: String, size: String, weight: String, style: String): Deferred<BitmapFont>? {
		var found = registry[family][size][weight][style]
		if (found != null) return found
		if (fontResolver != null) {
			val resolved: Deferred<BitmapFont>? = fontResolver!!.invoke(family, size, weight, style)
			if (resolved != null)
				register(family, size, weight, style, resolved)
			found = resolved
		}
		return found
	}

	override fun clear() {
		registry.iterateValues4 { bitmapFont ->
			bitmapFont.then {
				for (page in it.pages) {
					page.refDec()
				}
			}
		}
		registry.clear()
	}

	override fun dispose() {
		clear()
	}

}

private object FontTextureDecorator : Decorator<Texture, Texture> {
	override fun decorate(target: Texture): Texture {
		target.filterMin = TextureMinFilter.LINEAR_MIPMAP_LINEAR
		target.filterMag = TextureMagFilter.LINEAR
		target.hasWhitePixel = false
		return target
	}
}

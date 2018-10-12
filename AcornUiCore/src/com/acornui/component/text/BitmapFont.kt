/*
 * Copyright 2018 Poly Forest
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

import com.acornui.action.noopDecorator
import com.acornui.async.Deferred
import com.acornui.async.async
import com.acornui.async.launch
import com.acornui.collection.Clearable
import com.acornui.collection.copy
import com.acornui.core.Disposable
import com.acornui.core.assets.*
import com.acornui.core.di.Owned
import com.acornui.core.di.Scoped
import com.acornui.core.di.inject
import com.acornui.core.graphics.AtlasPageDecorator
import com.acornui.core.graphics.Texture
import com.acornui.core.graphics.TextureAtlasDataSerializer
import com.acornui.core.io.file.Files
import com.acornui.core.isWhitespace2
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
			return glyphs[0.toChar()]!!
		}
		return glyphs[char] ?: glyphs[(-1).toChar()]!!
	}

}

interface FontStyleRo {

	/**
	 * The name of the font face. (case sensitive)
	 */
	val face: String

	/**
	 * The size of the font.
	 */
	val size: Int

	val bold: Boolean

	val italic: Boolean
}

/**
 * A data class representing the criteria for what separates a bitmap font set.
 */
data class FontStyle(

		/**
		 * The name of the font face. (case sensitive)
		 */
		override var face: String = "[Unknown]",

		/**
		 * The size of the font.
		 */
		override var size: Int = 0,

		override var bold: Boolean = false,

		override var italic: Boolean = false
) : FontStyleRo

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
fun Scoped.loadFontFromDir(fontStyle: FontStyleRo, fntPath: String, group: CachedGroup): Deferred<BitmapFont> {
	val files = inject(Files)
	val fontFile = files.getFile(fntPath) ?: throw Exception("$fntPath not found.")
	return loadFontFromDir(fontStyle, fntPath, fontFile.parent!!.path, group)
}

/**
 * Loads a font where the images are standalone files in the specified directory.
 * @param fntPath The path of the font data file. (By default the font data loader expects a .fnt file
 * in the AngelCode format, but this can be changed by associating a different type of loader for the
 * BitmapFontData asset type.)
 * @param imagesDir The directory of images
 * @param group The caching group, to allow the loaded assets to be disposed as one.
 */
fun Scoped.loadFontFromDir(fontStyle: FontStyleRo, fntPath: String, imagesDir: String, group: CachedGroup): Deferred<BitmapFont> {
	val loader = async {
		val files = inject(Files)
		val dir = files.getDir(imagesDir) ?: throw Exception("Directory not found: $imagesDir")
		val bitmapFontData = loadAndCache(fntPath, AssetType.TEXT, AngelCodeParser, group).await()

		val n = bitmapFontData.pages.size
		val pageTextures = ArrayList<Deferred<Texture>>()
		for (i in 0..n - 1) {
			val page = bitmapFontData.pages[i]
			val imageFile = dir.getFile(page.imagePath)
					?: throw Exception("Font image file not found: ${page.imagePath}")
			pageTextures.add(loadAndCache(imageFile.path, AssetType.TEXTURE, noopDecorator(), group))
		}
		// Finished loading the font and all its textures.
		val glyphs = HashMap<Char, Glyph>()
		// Calculate the uv coordinates for each glyph.
		for (glyphData in bitmapFontData.glyphs.values) {
			glyphs[glyphData.char] = Glyph(
					data = glyphData,
					offsetX = glyphData.offsetX,
					offsetY = glyphData.offsetY,
					width = glyphData.region.width,
					height = glyphData.region.height,
					advanceX = glyphData.advanceX,
					isRotated = false,
					region = glyphData.region.copy(),
					texture = pageTextures[glyphData.page].await(),
					premultipliedAlpha = false
			)
		}

		val font = BitmapFont(
				bitmapFontData,
				pages = pageTextures.map { it.await() },
				premultipliedAlpha = false,
				glyphs = glyphs
		)
		font
	}
	Log.info("Font loaded $fontStyle")
	BitmapFontRegistry.register(fontStyle, loader)
	return loader
}

fun Scoped.loadFontFromAtlas(fontStyle: FontStyleRo, fntPath: String, atlasPath: String, group: CachedGroup): Deferred<BitmapFont> {
	val loader = async {
		val files = inject(Files)
		val atlasFile = files.getFile(atlasPath) ?: throw Exception("File not found: $atlasPath")

		val bitmapFontData = loadAndCache(fntPath, AssetType.TEXT, AngelCodeParser, group).await()
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
		font
	}
	BitmapFontRegistry.register(fontStyle, loader)
	return loader
}


object BitmapFontRegistry : Clearable, Disposable {

	private val registry: HashMap<FontStyleRo, Deferred<BitmapFont>> = HashMap()
	private val warnedFontStyles = HashMap<FontStyleRo, Boolean>()

	/**
	 * Registers a font loader to a font style.
	 * If the loaded font does not match the style used as the key, a warning will be logged.
	 * The loaded texture pages will have their use counts incremented via [Texture.refInc]
	 */
	fun register(fontStyle: FontStyleRo, bitmapFont: Deferred<BitmapFont>) {
		if (registry.containsKey(fontStyle)) return
		registry[fontStyle] = bitmapFont
		launch {
			val f = bitmapFont.await()
			if (f.data.fontStyle != fontStyle)
				Log.warn("Loaded font did not have a font style that match the registered style. Requested: $fontStyle Found: ${f.data.fontStyle}")
			for (page in f.pages) {
				page.refInc()
			}
		}
	}

	/**
	 * Unregisters a font loader that was registered via [register].
	 * The texture pages will have their use counts decremented via [Texture.refDec]
	 */
	fun unregister(fontStyle: FontStyleRo): Boolean {
		val removed = registry.remove(fontStyle) ?: return false
		launch {
			for (page in removed.await().pages) {
				page.refDec()
			}
		}
		return true
	}

	/**
	 * Returns true if this registry contains the given font.
	 */
	fun containsFont(fontStyle: FontStyleRo): Boolean {
		return registry.containsKey(fontStyle)
	}

	/**
	 * Returns the font registered to the given style. Throws an exception if the font is not registered.
	 *
	 * @param fontStyle The style used as a key via [register]
	 * @param warnOnNotFound If warnOnNotFound is true and the font style was never registered, a logger warning will
	 * be issued. This warning will only occur once per font style.
	 * @see containsFont
	 */
	fun getFont(fontStyle: FontStyleRo, warnOnNotFound: Boolean = true): Deferred<BitmapFont>? {
		val found = registry[fontStyle]
		if (found == null && warnOnNotFound) {
			if (!warnedFontStyles.containsKey(fontStyle)) {
				warnedFontStyles[fontStyle] = true
				Log.warn("Requested a font not being loaded $fontStyle")
			}
		}
		return found
	}

	override fun clear() {
		val values = registry.values.copy()
		registry.clear()
		launch {
			for (bitmapFont in values) {
				for (page in bitmapFont.await().pages) {
					page.refDec()
				}
			}
		}
	}

	override fun dispose() {
		clear()
	}

}

private val tmp = FontStyle()

/**
 * Retrieves the bitmap font based on the char style.
 */
fun getFont(charStyle: CharStyle, warnOnNotFound: Boolean = true): Deferred<BitmapFont>? {
	tmp.face = charStyle.face
	tmp.size = charStyle.size
	tmp.bold = charStyle.bold
	tmp.italic = charStyle.italic
	return BitmapFontRegistry.getFont(tmp, warnOnNotFound)
}
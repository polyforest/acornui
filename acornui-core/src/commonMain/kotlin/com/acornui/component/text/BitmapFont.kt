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
import com.acornui.async.MainDispatcher
import com.acornui.di.Context
import com.acornui.di.ContextImpl
import com.acornui.di.dependencyFactory
import com.acornui.gl.core.TextureMagFilter
import com.acornui.gl.core.TextureMinFilter
import com.acornui.graphic.Texture
import com.acornui.graphic.TextureAtlasData
import com.acornui.graphic.configure
import com.acornui.io.file.Path
import com.acornui.isWhitespace2
import com.acornui.logging.Log
import com.acornui.math.IntRectangle
import com.acornui.math.IntRectangleRo
import com.acornui.recycle.Clearable
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
 * An overload of [loadFontFromDir] where the images directory is assumed to be the parent directory of the font data file.
 */
suspend fun Context.loadFontFromDir(fontPath: String, cacheSet: CacheSet = cacheSet()): BitmapFont {
	return loadFontFromDir(fontPath, Path(fontPath).parent.value, cacheSet)
}

/**
 * Loads a font where the images are standalone files in the specified directory.
 * @param fontPath The path of the font data file. (By default the font data loader expects a .fnt file
 * in the AngelCode format, but this can be changed by associating a different type of loader for the
 * BitmapFontData asset type.)
 * @param imagesDir The directory of images.
 * @param cacheSet The caching group, to allow the loaded assets to be disposed as one.
 */
suspend fun Context.loadFontFromDir(fontPath: String, imagesDir: String, cacheSet: CacheSet = cacheSet()): BitmapFont = withContext(Dispatchers.Default) {
	val dir = Path(imagesDir)
	val bitmapFontData = cacheSet.getOrPutAsync(fontPath) {
		AngelCodeParser.parse(loadText(fontPath))
	}.await()

	val n = bitmapFontData.pages.size
	val pageTextureLoaders = ArrayList<Deferred<Texture>>()
	for (i in 0 until n) {
		val page = bitmapFontData.pages[i]
		val imageFile = dir.resolve(page.imagePath)
		pageTextureLoaders.add(cacheSet.getOrPutAsync(imageFile.value) {
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

	val pageTextures = pageTextureLoaders.awaitAll()
	withContext(MainDispatcher) {
		pageTextures.forEach {
			it.refInc()
		}
	}

	val font = BitmapFont(
			bitmapFontData,
			pages = pageTextures,
			premultipliedAlpha = false,
			glyphs = glyphs
	)
	Log.info("Font loaded $fontPath")
	font
}

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

@Suppress("EqualsOrHashCode")
data class BitmapFontRequest(

		/**
		 * The font type face.
		 */
		val family: String,

		/**
		 * The name of the font size to be passed to the [BitmapFontRegistry.fontResolver].
		 * @see FontSize
		 */
		val size: String,

		/**
		 * The name of the font weight to be passed to the [BitmapFontRegistry.fontResolver].
		 * @see FontWeight
		 */
		val weight: String,

		/**
		 * The name of the font style to be passed to the [BitmapFontRegistry.fontResolver].
		 * @see FontStyle
		 */
		val style: String,

		/**
		 * The dpi scaling for the font.
		 */
		val fontPixelDensity: Float
) {

	// Cache the hashcode; this data class is immutable.
	private val hashCode by lazy {
		var result = family.hashCode()
		result = 31 * result + size.hashCode()
		result = 31 * result + weight.hashCode()
		result = 31 * result + style.hashCode()
		result = 31 * result + fontPixelDensity.hashCode()
		result
	}

	override fun hashCode(): Int {
		return hashCode
	}
}

typealias FontResolver = suspend (request: BitmapFontRequest) -> BitmapFont

interface BitmapFontRegistry {


	/**
	 * If set, when a font is requested that isn't registered, this font resolver will be used to load the font with
	 * the given key.
	 */
	fun setFontResolver(value: FontResolver)

	/**
	 * Returns the font registered to the given style.
	 */
	suspend fun getFont(request: BitmapFontRequest): BitmapFont

	/**
	 * Removes a font from the registry, decrementing the references to its textures.
	 * @return Returns true if the font was found.
	 */
	fun clearFont(request: BitmapFontRequest): Boolean

	companion object : Context.Key<BitmapFontRegistry> {

		override val factory = dependencyFactory {
			BitmapFontRegistryImpl(it)
		}
	}
}

class BitmapFontRegistryImpl(owner: Context) : ContextImpl(owner), BitmapFontRegistry, Clearable {

	/**
	 * family, size, weight, style -> Deferred<BitmapFont>
	 */
	private val registry = HashMap<BitmapFontRequest, Deferred<BitmapFont>>()

	/**
	 * If set, when a font is requested that isn't registered, this font resolver will be used to load the font with
	 * the given key.
	 */
	private var fontResolver: FontResolver? = null

	override fun setFontResolver(value: FontResolver) {
		fontResolver = value
	}

	/**
	 * Returns the font registered to the given style. Logs an exception if the font is not registered.
	 */
	override suspend fun getFont(request: BitmapFontRequest): BitmapFont {
		return registry.getOrPut(request) {
			async {
				fontResolver!!.invoke(request).apply {
					pages.forEach(Texture::refInc)
				}
			}
		}.await()
	}

	@UseExperimental(ExperimentalCoroutinesApi::class)
	override fun clearFont(request: BitmapFontRequest): Boolean {
		return registry[request]?.apply {
			invokeOnCompletion {
				if (it != null)
					getCompleted().pages.forEach(Texture::refDec)
			}
			cancel()
		} != null
	}

	override fun clear() {
		registry.keys.forEach { clearFont(it) }
		registry.clear()
	}

	override fun dispose() {
		clear()
		super.dispose()
	}

}

private fun configureFontTexture(target: Texture): Texture {
	target.filterMin = TextureMinFilter.LINEAR_MIPMAP_LINEAR
	target.filterMag = TextureMagFilter.LINEAR
	target.hasWhitePixel = false
	return target
}
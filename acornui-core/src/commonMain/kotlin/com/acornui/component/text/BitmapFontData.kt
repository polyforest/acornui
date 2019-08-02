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

import com.acornui.isWhitespace2
import com.acornui.math.IntPadRo
import com.acornui.math.IntRectangle
import com.acornui.math.IntRectangleRo


/**
 * Data representing a bitmap font.
 * @author nbilyk
 */
data class BitmapFontData(

		val info: BitmapFontInfo,

		val pages: List<BitmapFontPageData>,

		val glyphs: Map<Char, GlyphData>,

		/**
		 * The distance from one line of text to the next.
		 * In pixels.
		 */
		val lineHeight: Int,

		/**
		 * The baseline is the line upon which most letters "sit" and below which descenders extend.
		 * In pixels.
		 */
		val baseline: Int,

		/**
		 * The width of the texture pages.
		 * In pixels.
		 */
		val pageW: Int,

		/**
		 * The height of the texture pages.
		 * In pixels.
		 */
		val pageH: Int

) {

	/**
	 * Returns the glyph associated with the provided [char]. If the glyph is not found, if the [char] is whitespace,
	 * an empty glyph will be returned. If the glyph is not found and is not whitespace, a missing glyph placeholder
	 * will be returned.
	 */
	fun getGlyphSafe(char: Char): GlyphData {
		val existing = glyphs[char]
		if (existing != null) return existing
		if (char.isWhitespace2() || char.toInt() > 0xFF) {
			return glyphs[0.toChar()]!!
		}
		return glyphs[(-1).toChar()]!!
	}
}

data class BitmapFontInfo(

		/**
		 * The font family.
		 */
		val face: String,

		/**
		 * The size of the font, in pixels.
		 */
		val size: Int,

		/**
		 * The font is bold.
		 */
		val bold: Boolean,

		/**
		 * The font is italic.
		 */
		val italic: Boolean,

		/**
		 * The name of the OEM charset used (when not unicode).
		 */
		val charset: String,

		/**
		 * Set to 1 if it is the unicode charset.
		 */
		val unicode: Boolean,

		/**
		 * The font height stretch in percentage. 100 means no stretch.
		 */
		val stretchH: Int,

		/**
		 * Set to 1 if smoothing was turned on.
		 */
		val smooth: Boolean,

		/**
		 * The supersampling level used. 1 means no supersampling was used.
		 */
		val antialiasing: Int,

		/**
		 * The padding for each character.
		 */
		val padding: IntPadRo,

		/**
		 * The horizontal spacing for each character.
		 */
		val spacingX: Int,

		/**
		 * The vertical spacing for each character.
		 */
		val spacingY: Int
)

data class BitmapFontPageData(
		val id: Int,
		val imagePath: String
)

/**
 * Represents a single character in a font page.
 */
data class GlyphData(

		val char: Char,

		/**
		 * The x, y, width, height bounds within the original image.
		 * The [Glyph] object contains the region in the final, packed image.
		 * In pixels.
		 */
		val region: IntRectangleRo = IntRectangle(),

		/**
		 * How much the current position should be offset when copying the image from the texture to the screen.
		 * In pixels.
		 */
		val offsetX: Int = 0,

		/**
		 * How much the current position should be offset when copying the image from the texture to the screen.
		 * In pixels.
		 */
		val offsetY: Int = 0,

		/**
		 * How much the current position should be advanced after drawing the character.
		 * In pixels.
		 */
		val advanceX: Int = 0,

		/**
		 * The index to the texture page that holds this glyph.
		 */
		var page: Int = 0,

		/**
		 * Kerning pairs.
		 * In pixels.
		 */
		val kerning: Map<Char, Int> = HashMap()
) {


	fun getKerning(ch: Char): Int {
		return kerning[ch] ?: 0
	}

	/**
	 * If the region is outside of the page (the out of bounds area assumed to be clear), then return this glyph
	 * with the region clamped.
	 */
	fun clampRegion(pageWidth: Int, pageHeight: Int): GlyphData {
		return clampRegionX(pageWidth).clampRegionY(pageHeight)
	}

	private fun clampRegionX(pageWidth: Int): GlyphData {
		return if (region.x < 0) {
			copy(
					region = region.copy(x = 0, width = region.width + region.x),
					offsetX = offsetX - region.x
			)
		} else if (region.right > pageWidth) {
			val diff = region.right - pageWidth
			copy(
					region = region.copy(width = region.width - diff)
			)
		} else {
			this
		}
	}

	private fun clampRegionY(pageHeight: Int): GlyphData {
		return if (region.y < 0) {
			copy(
					region = region.copy(y = 0, width = region.height + region.y),
					offsetY = offsetY - region.y
			)
		} else if (region.bottom > pageHeight) {
			val diff = region.bottom - pageHeight
			copy(
					region = region.copy(height = region.height - diff)
			)
		} else {
			this
		}
	}

	companion object {

		const val EMPTY_CHAR = (-2).toChar()

		const val UNKNOWN_CHAR = (-1).toChar()
	}

}

/**
 * Measures the width of a line with this font.
 * Does not consider line breaks.
 */
fun BitmapFontData.measureLineWidth(text: String): Int {
	var x = 0
	var previousGlyph: GlyphData? = null
	for (i in 0..text.lastIndex) {
		val char = text[i]
		val glyph = getGlyphSafe(char)
		if (previousGlyph != null) {
			x += previousGlyph.kerning[char] ?: 0
		}
		x += glyph.advanceX
		previousGlyph = glyph
	}
	return x
}

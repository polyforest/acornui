/*
* Derived from LibGDX BitmapFont by Nicholas Bilyk
* https://github.com/libgdx
* Copyright 2011 See https://github.com/libgdx/libgdx/blob/master/AUTHORS
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

package com.acornui.gl.component.text

import com.acornui.math.IntRectangle
import com.acornui.math.IntRectangleRo


/**
 * Data representing a bitmap font.
 * @author nbilyk
 */
data class BitmapFontData(

		/**
		 * The style of the glyphs packed in this font. (font face, size, bold, italic)
		 */
		val fontStyle: FontStyleRo,

		val pages: List<BitmapFontPageData>,

		val glyphs: Map<Char, GlyphData>,

		/**
		 * The distance from one line of text to the next.
		 */
		val lineHeight: Int,

		/**
		 * The baseline is the line upon which most letters "sit" and below which descenders extend.
		 */
		val baseline: Int,

		/**
		 * The width of the texture pages.
		 */
		val pageW: Int,

		/**
		 * The height of the texture pages.
		 */
		val pageH: Int

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
		 */
		val region: IntRectangleRo = IntRectangle(),

		/**
		 * How much the current position should be offset when copying the image from the texture to the screen.
		 */
		val offsetX: Int = 0,

		/**
		 * How much the current position should be offset when copying the image from the texture to the screen.
		 */
		val offsetY: Int = 0,

		/**
		 * How much the current position should be advanced after drawing the character.
		 */
		val advanceX: Int = 0,

		/**
		 * The index to the texture page that holds this glyph.
		 */
		var page: Int = 0,

		/**
		 * Kerning pairs.
		 */
		val kerning: Map<Char, Int> = HashMap()
) {

	fun getKerning(ch: Char): Int {
		return kerning[ch] ?: 0
	}

}
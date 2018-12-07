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

import com.acornui.action.Decorator
import com.acornui.component.text.GlyphData.Companion.EMPTY_CHAR
import com.acornui.component.text.GlyphData.Companion.UNKNOWN_CHAR
import com.acornui.math.IntRectangle
import com.acornui.string.StringReader
import com.acornui.core.replace2
import com.acornui.math.IntPad
import kotlin.math.abs

/**
 * http://www.angelcode.com/products/bmfont/doc/file_format.html
 *
 * @author nbilyk
 */
object AngelCodeParser : Decorator<String, BitmapFontData> {

	fun parse(str: String): BitmapFontData {
		val parser = StringReader(str)

		// Info
		parser.white()
		parser.consumeString("info")

		val face = parseQuotedStringProp(parser, "face")
		val size = abs(parseIntProp(parser, "size"))
		val bold = parseBoolProp(parser, "bold")
		val italic = parseBoolProp(parser, "italic")
		val charset = parseQuotedStringProp(parser, "charset")
		val unicode = parseBoolProp(parser, "unicode")
		val stretchH = parseIntProp(parser, "stretchH")
		val smooth = parseBoolProp(parser, "smooth")
		val antialiasing = parseIntProp(parser, "aa")
		val padding = IntPad(parseIntArrayProp(parser, "padding"))
		val spacing = parseIntArrayProp(parser, "spacing")

		val info = BitmapFontInfo(
				face,
				size,
				bold,
				italic,
				charset,
				unicode,
				stretchH,
				smooth,
				antialiasing,
				padding,
				spacing[0],
				spacing[1]
		)

		// Common
		nextLine(parser)
		parser.consumeString("common")
		val lineHeight = parseIntProp(parser, "lineHeight")
		val baseline = parseIntProp(parser, "base")
		val pageW = parseIntProp(parser, "scaleW")
		val pageH = parseIntProp(parser, "scaleH")
		val totalPages = parseIntProp(parser, "pages")

		val pages = Array(totalPages) {
			// Read each "page" info line.
			nextLine(parser)
			if (!parser.consumeString("page")) throw Exception("Missing page definition.")

			val id = parseIntProp(parser, "id")
			val fileName: String = parseQuotedStringProp(parser, "file")
			val imagePath = fileName.replace2('\\', '/')
			BitmapFontPageData(id, imagePath)
		}

		// Chars
		nextLine(parser)
		if (!parser.consumeString("chars")) throw Exception("Expected chars block")
		// The chars count from Hiero is off, so we're not going to use it.
		val glyphs = HashMap<Char, GlyphData>()
		val kernings = HashMap<Char, MutableMap<Char, Int>>()

		// Read each glyph definition.
		while (true) {
			nextLine(parser)
			if (!parser.consumeString("char")) break

			val ch = parseIntProp(parser, "id").toChar()
			if (ch > Char.MAX_SURROGATE) continue

			val regionX = parseIntProp(parser, "x")
			val regionY = parseIntProp(parser, "y")
			val regionW = parseIntProp(parser, "width")
			val regionH = parseIntProp(parser, "height")
			val offsetX = parseIntProp(parser, "xoffset")
			val offsetY = parseIntProp(parser, "yoffset")
			val xAdvance = parseIntProp(parser, "xadvance")
			val page = parseIntProp(parser, "page")
			val kerning = HashMap<Char, Int>()
			kernings[ch] = kerning
			glyphs[ch] = GlyphData(
					char = ch,
					region = IntRectangle(regionX, regionY, regionW, regionH).reduce(info.padding),
					offsetX = offsetX + info.padding.left,
					offsetY = offsetY + info.padding.top,
					advanceX = xAdvance,
					page = page,
					kerning = kerning
			).clampRegion(pageW, pageH)
		}

		parser.consumeString("kernings")

		while (true) {
			nextLine(parser)
			if (!parser.consumeString("kerning")) break

			val first = parseIntProp(parser, "first")
			val second = parseIntProp(parser, "second")
			if (first < 0 || second < 0) continue
			val amount = parseIntProp(parser, "amount")
			if (glyphs.containsKey(first.toChar())) {
				// Kerning pairs may exist for glyphs not contained in the font.
				kernings[first.toChar()]!![second.toChar()] = amount
			}
		}

		glyphs.addMissingCommonGlyphs()

		return BitmapFontData(
				info = info,
				pages = pages.toList(),
				glyphs = glyphs,
				lineHeight = lineHeight,
				baseline = baseline,
				pageW = pageW,
				pageH = pageH
		)
	}

	private fun MutableMap<Char, GlyphData>.addMissingCommonGlyphs() {
		var space = this[' ']
		if (space == null) {
			var copy = this['n']
			if (copy == null) copy = this.values.firstOrNull()
			if (copy == null) return // There's no hope
			space = GlyphData(char = ' ', advanceX = copy.advanceX)
			this[' '] = space
		}
		for (char in arrayOf('\t', '\n', '\r')) {
			// Invisible characters that take up no space.
			if (this[char] == null) {
				this[char] = GlyphData(char)
			}
		}
		this[EMPTY_CHAR] = GlyphData(EMPTY_CHAR)
		val nbspChar = '�'
		if (this[nbspChar] == null) {
			this[nbspChar] = space.copy(char = nbspChar)
		}
		this[UNKNOWN_CHAR] = (this['?'] ?: space).copy(char = UNKNOWN_CHAR)
	}

	private fun nextLine(parser: StringReader): Boolean {
		val found = parser.consumeThrough('\n')
		if (found) parser.white()
		return found
	}

	private fun parseFloatProp(parser: StringReader, property: String): Float {
		consumeProperty(parser, property)
		return parser.getFloat()!!
	}

	private fun parseBoolProp(parser: StringReader, property: String): Boolean {
		consumeProperty(parser, property)
		return parser.getBool()!!
	}

	private fun parseIntProp(parser: StringReader, property: String): Int {
		consumeProperty(parser, property)
		return parser.getInt()!!
	}

	private fun parseIntArrayProp(parser: StringReader, property: String): Array<Int> {
		consumeProperty(parser, property)
		return parser.notWhite().split(",").map { it.toInt() }.toTypedArray()
	}

	private fun parseQuotedStringProp(parser: StringReader, property: String): String {
		consumeProperty(parser, property)
		return parser.getQuotedString()!!
	}

	private fun consumeProperty(parser: StringReader, property: String, required: Boolean = true): Boolean {
		parser.white()
		val found = parser.consumeString(property)
		if (!found) {
			if (required) throw Exception("Property not found: $property at: ${parser.data.substring(parser.position, minOf(parser.length, parser.position + 20))}")
			else return false
		}
		parser.white()
		parser.consumeChar('=')
		parser.white()
		return true
	}

	override fun decorate(target: String): BitmapFontData = parse(target)
}
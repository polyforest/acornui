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

import com.acornui.collection.addAll
import com.acornui.component.ComponentInit
import com.acornui.component.layout.algorithm.FlowHAlign
import com.acornui.component.layout.algorithm.FlowVAlign
import com.acornui.component.style.*
import com.acornui.di.Owned
import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
import com.acornui.graphic.color
import com.acornui.math.Corners
import com.acornui.math.Pad
import com.acornui.math.PadRo
import com.acornui.serialization.*
import kotlinx.coroutines.Deferred
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

var TextField.strong by StyleTagToggle(TextStyleTags.strong)

var TextField.emphasis by StyleTagToggle(TextStyleTags.emphasis)

var TextField.selectable: Boolean
	get(): Boolean = charStyle.selectable
	set(value) {
		charStyle.selectable = value
	}

object TextStyleTags {

	val heading = styleTag()

	val extraSmall = styleTag()
	val small = styleTag()
	val normal = styleTag()
	val large = styleTag()
	val extraLarge = styleTag()

	val regular = styleTag()
	val strong = styleTag()
	val emphasis = styleTag()

	val error = styleTag()
	val warning = styleTag()
	val info = styleTag()
}

/**
 * A shortcut to creating a text field with the [TextStyleTags.heading] tag.
 */
inline fun Owned.headingText(text: String = "", init: ComponentInit<TextField> = {}): TextField  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val t = TextFieldImpl(this)
	t.styleTags.add(TextStyleTags.heading)
	t.text = text
	t.init()
	return t
}

/**
 * A shortcut to creating a text field with the [TextStyleTags.large] tag.
 */
inline fun Owned.largeText(text: String = "", init: ComponentInit<TextField> = {}): TextField  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val t = TextFieldImpl(this)
	t.styleTags.add(TextStyleTags.large)
	t.text = text
	t.init()
	return t
}

/**
 * A shortcut to creating a text field with the [TextStyleTags.small] tag.
 */
inline fun Owned.smallText(text: String = "", init: ComponentInit<TextField> = {}): TextField  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val t = TextFieldImpl(this)
	t.styleTags.add(TextStyleTags.small)
	t.text = text
	t.init()
	return t
}


/**
 * A shortcut to creating a text field with the [TextStyleTags.strong] tag.
 */
inline fun Owned.strongText(text: String = "", init: ComponentInit<TextField> = {}): TextField  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val t = TextFieldImpl(this)
	t.styleTags.add(TextStyleTags.strong)
	t.text = text
	t.init()
	return t
}

/**
 * A shortcut to creating a text field with the [TextStyleTags.emphasis] tag.
 */
inline fun Owned.emphasizedText(text: String = "", init: ComponentInit<TextField> = {}): TextField  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val t = TextFieldImpl(this)
	t.styleTags.add(TextStyleTags.emphasis)
	t.text = text
	t.init()
	return t
}

/**
 * A shortcut to creating a text field with the [TextStyleTags.emphasis] and [TextStyleTags.strong] tags.
 */
inline fun Owned.strongEmphasizedText(text: String = "", init: ComponentInit<TextField> = {}): TextField  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val t = TextFieldImpl(this)
	t.styleTags.addAll(TextStyleTags.emphasis, TextStyleTags.strong)
	t.text = text
	t.init()
	return t
}

/**
 * A shortcut to creating a text field with the [TextStyleTags.error] tag.
 */
inline fun Owned.errorText(text: String = "", init: ComponentInit<TextField> = {}): TextField  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val t = TextFieldImpl(this)
	t.styleTags.add(TextStyleTags.error)
	t.text = text
	t.init()
	return t
}

/**
 * A shortcut to creating a text field with the [TextStyleTags.warning] tag.
 */
inline fun Owned.warningText(text: String = "", init: ComponentInit<TextField> = {}): TextField  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val t = TextFieldImpl(this)
	t.styleTags.add(TextStyleTags.warning)
	t.text = text
	t.init()
	return t
}

/**
 * A shortcut to creating a text field with the [TextStyleTags.info] tag.
 */
inline fun Owned.infoText(text: String = "", init: ComponentInit<TextField> = {}): TextField  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val t = TextFieldImpl(this)
	t.styleTags.add(TextStyleTags.info)
	t.text = text
	t.init()
	return t
}

/**
 * The glyph characteristics.
 */
class CharStyle : StyleBase() {

	override val type: StyleType<CharStyle> = CharStyle

	/**
	 * The font family. This is not typically set directly, but provided by the skin.
	 * It should match the font family in [BitmapFontRegistry].
	 */
	var fontFamily by prop<String?>(null)

	/**
	 * The size of the font. This should be a string that matches the what the [FontResolver] expects in the
	 * [BitmapFontRegistry]
	 *
	 * This is not typically set directly, but provided by the skin. Use the corresponding tags in [TextStyleTags].
	 *
	 * @see TextStyleTags.extraSmall
	 * @see TextStyleTags.small
	 * @see TextStyleTags.normal
	 * @see TextStyleTags.large
	 * @see TextStyleTags.extraLarge
	 *
	 * @see FontSize
	 */
	var fontSize by prop(FontSize.REGULAR)

	/**
	 * Which font style to use.
	 *
	 * This is not typically set directly, but provided by the skin. Use the corresponding tags in [TextStyleTags].
	 *
	 * @see TextStyleTags.emphasis
	 *
	 * @see FontStyle
	 */
	var fontStyle by prop(FontStyle.NORMAL)

	/**
	 * Which font weight to use.
	 *
	 * This is not typically set directly, but provided by the skin. Use the corresponding tags in [TextStyleTags].
	 *
	 * @see TextStyleTags.strong
	 * @see TextStyleTags.regular
	 *
	 * @see FontWeight
	 */
	var fontWeight by prop(FontWeight.REGULAR)

	/**
	 * True if the characters should draw an line at the baseline.
	 */
	var underlined by prop(false)

	/**
	 * True if the characters should draw a line through the middle.
	 */
	var strikeThrough by prop(false)

	/**
	 * The line thickness of the underline or strike through if there is one.
	 * @see underlined
	 * @see strikeThrough
	 */
	var lineThickness by prop(1f)

	var colorTint: ColorRo by prop(Color(1f, 1f, 1f, 1f))
	var backgroundColor: ColorRo by prop(Color())

	var selectedColorTint: ColorRo by prop(Color(1f, 1f, 1f, 1f))
	var selectedBackgroundColor: ColorRo by prop(Color(0.12f, 0.25f, 0.5f, 1f))

	/**
	 * The text is selectable by the user. This does not affect whether or not the text can be selected
	 * programmatically, nor does setting this to false de-select the text if it's already selected.
	 */
	var selectable by prop(true)

	/**
	 * The scaling of points to pixels.
	 * This should correspond to the [com.acornui.graphic.Window.scaleX].
	 */
	var scaleX: Float by prop(1f)

	/**
	 * The scaling of points to pixels.
	 * This should correspond to the [com.acornui.graphic.Window.scaleY].
	 */
	var scaleY: Float by prop(1f)

	companion object : StyleType<CharStyle>
}

fun CharStyle.getFont(): Deferred<BitmapFont>? {
	val family = fontFamily ?: return null
	return BitmapFontRegistry.getFont(BitmapFontRequest(
			family,
			fontSize,
			fontWeight,
			fontStyle,
			fontPixelDensity = scaleY
	))
}


fun charStyle(init: CharStyle.() -> Unit = {}): CharStyle {
	val c = CharStyle()
	c.init()
	return c
}

object CharStyleSerializer : To<CharStyle>, From<CharStyle> {

	override fun CharStyle.write(writer: Writer) {
		writer.styleProperty(this, ::fontFamily)?.string(fontFamily)
		writer.styleProperty(this, ::fontWeight)?.string(fontWeight)
		writer.styleProperty(this, ::fontStyle)?.string(fontStyle)
		writer.styleProperty(this, ::fontSize)?.string(fontSize)
		writer.styleProperty(this, ::underlined)?.bool(underlined)
		writer.styleProperty(this, ::colorTint)?.color(colorTint)
		writer.styleProperty(this, ::backgroundColor)?.color(backgroundColor)
		writer.styleProperty(this, ::selectedColorTint)?.color(selectedColorTint)
		writer.styleProperty(this, ::selectedBackgroundColor)?.color(selectedBackgroundColor)
		writer.styleProperty(this, ::selectable)?.bool(selectable)
	}

	override fun read(reader: Reader): CharStyle {
		val c = CharStyle()
		reader.contains(c::fontFamily.name) { c.fontFamily = it.string()!! }
		reader.contains(c::fontWeight.name) { c.fontWeight = it.string()!! }
		reader.contains(c::fontStyle.name) { c.fontStyle = it.string()!! }
		reader.contains(c::fontSize.name) { c.fontSize = it.string()!! }
		reader.contains(c::underlined.name) { c.underlined = it.bool()!! }
		reader.contains(c::colorTint.name) { c.colorTint = it.color()!! }
		reader.contains(c::backgroundColor.name) { c.backgroundColor = it.color()!! }
		reader.contains(c::selectedColorTint.name) { c.selectedColorTint = it.color()!! }
		reader.contains(c::selectedBackgroundColor.name) { c.selectedBackgroundColor = it.color()!! }
		reader.contains(c::selectable.name) { c.selectable = it.bool()!! }
		return c
	}
}

object FontWeight {
	const val THIN = "thin"
	const val EXTRA_LIGHT = "extra-light"
	const val LIGHT = "light"
	const val REGULAR = "regular"
	const val MEDIUM = "medium"
	const val SEMI_BOLD = "semi-bold"
	const val BOLD = "bold"
	const val BLACK = "black"

	val values = listOf(THIN, EXTRA_LIGHT, LIGHT, REGULAR, MEDIUM, SEMI_BOLD, BOLD, BLACK)
}

object FontStyle {
	const val NORMAL = "normal"
	const val ITALIC = "italic"
}

object FontSize {
	const val EXTRA_SMALL = "extra-small"
	const val SMALL = "small"
	const val REGULAR = "regular"
	const val LARGE = "large"
	const val EXTRA_LARGE = "extra-large"

	val values = listOf(EXTRA_SMALL, SMALL, REGULAR, LARGE, EXTRA_LARGE)
}

class TextFlowStyle : StyleBase() {

	override val type = Companion

	/**
	 * The vertical gap between lines.
	 */
	var verticalGap by prop(0f)

	/**
	 * The Padding object with left, bottom, top, and right padding.
	 */
	var padding: PadRo by prop(Pad())

	/**
	 * The number of space char widths a tab should occupy.
	 */
	var tabSize: Int by prop(4)

	var horizontalAlign by prop(FlowHAlign.LEFT)
	var verticalAlign by prop(FlowVAlign.BASELINE)
	var multiline by prop(true)

	companion object : StyleType<TextFlowStyle>
}

class TextInputStyle : StyleBase() {
	override val type = Companion

	var defaultWidth by prop(180f)

	/**
	 * The cursor blinks, this is color one.
	 */
	var cursorColorOne: ColorRo by prop(Color(0.1f, 0.1f, 0.1f, 1f))

	/**
	 * The cursor blinks, this is color two.
	 */
	var cursorColorTwo: ColorRo by prop(Color(0.9f, 0.9f, 0.9f, 1f))

	/**
	 * The number of seconds between cursor blinks.
	 */
	var cursorBlinkSpeed: Float by prop(0.5f)

	/**
	 * The background of this text input.
	 */
	var background: OptionalSkinPart by prop(noSkinOptional)

	/**
	 * Whitespace between the bounds of this text input and the contents.
	 */
	var margin: PadRo by prop(Pad())

	/**
	 * Whitespace between the background and text.
	 */
	var padding: PadRo by prop(Pad())

	/**
	 * Used for clipping, this should match that of the background border radius.
	 */
	var borderRadii by prop(Corners())

	companion object : StyleType<TextInputStyle>
}

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

@file:Suppress("unused")

package com.acornui.component.text

import com.acornui.collection.addAll
import com.acornui.component.ComponentInit
import com.acornui.component.layout.algorithm.FlowHAlign
import com.acornui.component.layout.algorithm.FlowVAlign
import com.acornui.component.style.*
import com.acornui.di.Context
import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
import com.acornui.graphic.DpiStyle
import com.acornui.math.Corners
import com.acornui.math.CornersRo
import com.acornui.math.MathUtils.clamp
import com.acornui.math.Pad
import com.acornui.math.PadRo
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
inline fun Context.headingText(text: String = "", init: ComponentInit<TextField> = {}): TextField {
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
inline fun Context.largeText(text: String = "", init: ComponentInit<TextField> = {}): TextField {
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
inline fun Context.smallText(text: String = "", init: ComponentInit<TextField> = {}): TextField {
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
inline fun Context.strongText(text: String = "", init: ComponentInit<TextField> = {}): TextField {
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
inline fun Context.emphasizedText(text: String = "", init: ComponentInit<TextField> = {}): TextField {
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
inline fun Context.strongEmphasizedText(text: String = "", init: ComponentInit<TextField> = {}): TextField {
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
inline fun Context.errorText(text: String = "", init: ComponentInit<TextField> = {}): TextField {
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
inline fun Context.warningText(text: String = "", init: ComponentInit<TextField> = {}): TextField {
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
inline fun Context.infoText(text: String = "", init: ComponentInit<TextField> = {}): TextField {
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
class CharStyle : DpiStyle() {

	override val type: StyleType<CharStyle> = CharStyle

	/**
	 * The font family.
	 */
	var fontFamily by prop<String?>(null)

	/**
	 * The size of the font. This should be a string that matches a key in the [fontSizes] map.
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

	var colorTint by prop<ColorRo>(Color(1f, 1f, 1f, 1f))
	var backgroundColor by prop<ColorRo>(Color())

	var selectedColorTint by prop<ColorRo>(Color(1f, 1f, 1f, 1f))
	var selectedBackgroundColor by prop<ColorRo>(Color(0.12f, 0.25f, 0.5f, 1f))

	/**
	 * The text is selectable by the user. This does not affect whether or not the text can be selected
	 * programmatically, nor does setting this to false de-select the text if it's already selected.
	 */
	var selectable by prop(true)

	/**
	 * A map of font size key [FontSize] to dp (density independent pixels).
	 *
	 * The actual px size chosen will be as follows:
	 * - A size key matching the [FontSize] key value is set on [com.acornui.component.text.CharStyle.fontSize].
	 * - The dp value is retrieved from this map.
	 * - The requested point size is multiplied by the requested pixel density scaling to get the desired px.
	 * - The next lower size from the available font is chosen. The sizes available are built in the font processor
	 * from the settings.json within fonts_unprocessedFonts.
	 */
	var fontSizes: Map<String, Int> by prop(emptyMap())

	companion object : StyleType<CharStyle> {

		override val extends: StyleType<*>? = DpiStyle
	}
}

inline fun charStyle(init: CharStyle.() -> Unit = {}): CharStyle {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return CharStyle().apply(init)
}

object FontWeight {
	const val HAIRLINE = "hairline"
	const val THIN = "thin"
	const val EXTRA_LIGHT = "extra-light"
	const val LIGHT = "light"
	const val REGULAR = "regular"
	const val MEDIUM = "medium"
	const val SEMI_BOLD = "semi-bold"
	const val BOLD = "bold"
	const val BLACK = "black"

	val values = listOf(HAIRLINE, THIN, EXTRA_LIGHT, LIGHT, REGULAR, MEDIUM, SEMI_BOLD, BOLD, BLACK)
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

	/**
	 * Returns a clamped relative size.
	 */
	fun relativeSize(size: String, delta: Int): String {
		val index = values.indexOf(size)
		if (index == -1) return REGULAR
		return values[clamp(index + delta, 0, values.lastIndex)]
	}
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
	var padding by prop<PadRo>(Pad())

	/**
	 * The number of space char widths a tab should occupy.
	 */
	var tabSize by prop(4)

	var horizontalAlign by prop(FlowHAlign.LEFT)

	var verticalAlign by prop(FlowVAlign.BASELINE)

	var multiline by prop(true)

	/**
	 * If false (default), the measured size be the max of the explicit size if set.
	 */
	var sizeToContents by prop(false)

	/**
	 * If true (default), this component will be clipped to the explicit size.
	 * The measured size will be the min of the explicit and measured dimensions.
	 */
	var allowClipping by prop(true)

	companion object : StyleType<TextFlowStyle>
}

class TextInputStyle : StyleBase() {
	override val type = Companion

	var defaultWidth by prop(180f)

	/**
	 * The cursor blinks, this is color one.
	 */
	var cursorColorOne by prop<ColorRo>(Color(0.1f, 0.1f, 0.1f, 1f))

	/**
	 * The cursor blinks, this is color two.
	 */
	var cursorColorTwo by prop<ColorRo>(Color(0.9f, 0.9f, 0.9f, 1f))

	/**
	 * The number of seconds between cursor blinks.
	 */
	var cursorBlinkSpeed by prop(0.5f)

	/**
	 * The background of this text input.
	 */
	var background by prop(noSkinOptional)

	/**
	 * Whitespace between the bounds of this text input and the contents.
	 */
	var margin by prop<PadRo>(Pad())

	/**
	 * Whitespace between the background and text.
	 */
	var padding by prop<PadRo>(Pad())

	/**
	 * Used for clipping, this should match that of the background border radius.
	 */
	var borderRadii by prop<CornersRo>(Corners())

	companion object : StyleType<TextInputStyle>
}

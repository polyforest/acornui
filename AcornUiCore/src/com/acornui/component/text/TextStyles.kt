/*
 * Copyright 2018 PolyForest
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
import com.acornui.component.style.StyleBase
import com.acornui.component.style.StyleType
import com.acornui.component.style.styleProperty
import com.acornui.component.style.styleTag
import com.acornui.core.di.Owned
import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
import com.acornui.graphic.color
import com.acornui.math.Pad
import com.acornui.math.PadRo
import com.acornui.serialization.*

var TextField.strong: Boolean
	get() = styleTags.contains(TextStyleTags.strong)
	set(value) {
		if (!styleTags.contains(TextStyleTags.strong))
			styleTags.add(TextStyleTags.strong)
	}

var TextField.emphasis: Boolean
	get() = styleTags.contains(TextStyleTags.emphasis)
	set(value) {
		if (!styleTags.contains(TextStyleTags.emphasis))
			styleTags.add(TextStyleTags.emphasis)
	}


var TextField.selectable: Boolean
	get(): Boolean = charStyle.selectable
	set(value) {
		charStyle.selectable = value
	}

object TextStyleTags {

	val h1 = styleTag()
	val h2 = styleTag()
	val h3 = styleTag()
	val h4 = styleTag()
	val strong = styleTag()
	val emphasis = styleTag()

	val error = styleTag()
	val warning = styleTag()
	val info = styleTag()
}

/**
 * A shortcut to creating a text field with the [TextStyleTags.h1] tag.
 */
fun Owned.h1(text: String = "", init: ComponentInit<TextField> = {}): TextField {
	val t = TextFieldImpl(this)
	t.styleTags.add(TextStyleTags.h1)
	t.text = text
	t.init()
	return t
}

/**
 * A shortcut to creating a text field with the [TextStyleTags.h2] tag.
 */
fun Owned.h2(text: String = "", init: ComponentInit<TextField> = {}): TextField {
	val t = TextFieldImpl(this)
	t.styleTags.add(TextStyleTags.h2)
	t.text = text
	t.init()
	return t
}

/**
 * A shortcut to creating a text field with the [TextStyleTags.h3] tag.
 */
fun Owned.h3(text: String = "", init: ComponentInit<TextField> = {}): TextField {
	val t = TextFieldImpl(this)
	t.styleTags.add(TextStyleTags.h3)
	t.text = text
	t.init()
	return t
}

/**
 * A shortcut to creating a text field with the [TextStyleTags.h4] tag.
 */
fun Owned.h4(text: String = "", init: ComponentInit<TextField> = {}): TextField {
	val t = TextFieldImpl(this)
	t.styleTags.add(TextStyleTags.h4)
	t.text = text
	t.init()
	return t
}

/**
 * A shortcut to creating a text field with the [TextStyleTags.strong] tag.
 */
fun Owned.strong(text: String = "", init: ComponentInit<TextField> = {}): TextField {
	val t = TextFieldImpl(this)
	t.styleTags.add(TextStyleTags.strong)
	t.text = text
	t.init()
	return t
}

/**
 * A shortcut to creating a text field with the [TextStyleTags.emphasis] tag.
 */
fun Owned.em(text: String = "", init: ComponentInit<TextField> = {}): TextField {
	val t = TextFieldImpl(this)
	t.styleTags.add(TextStyleTags.emphasis)
	t.text = text
	t.init()
	return t
}

/**
 * A shortcut to creating a text field with the [TextStyleTags.emphasis] and [TextStyleTags.strong] tags.
 */
fun Owned.strongEm(text: String = "", init: ComponentInit<TextField> = {}): TextField {
	val t = TextFieldImpl(this)
	t.styleTags.addAll(TextStyleTags.emphasis, TextStyleTags.strong)
	t.text = text
	t.init()
	return t
}

/**
 * A shortcut to creating a text field with the [TextStyleTags.error] tag.
 */
fun Owned.errorText(text: String = "", init: ComponentInit<TextField> = {}): TextField {
	val t = TextFieldImpl(this)
	t.styleTags.add(TextStyleTags.error)
	t.text = text
	t.init()
	return t
}

/**
 * A shortcut to creating a text field with the [TextStyleTags.warning] tag.
 */
fun Owned.warningText(text: String = "", init: ComponentInit<TextField> = {}): TextField {
	val t = TextFieldImpl(this)
	t.styleTags.add(TextStyleTags.warning)
	t.text = text
	t.init()
	return t
}

/**
 * A shortcut to creating a text field with the [TextStyleTags.info] tag.
 */
fun Owned.infoText(text: String = "", init: ComponentInit<TextField> = {}): TextField {
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
	 * The key of the font, as it was registered on the [BitmapFontRegistry] in the skin.
	 */
	var fontKey by prop<String?>(null)

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

	companion object : StyleType<CharStyle>
}

val CharStyle.font: BitmapFont?
	get() {
		val fontKey = fontKey ?: return null
		return BitmapFontRegistry.getFont(fontKey)
	}


fun charStyle(init: CharStyle.() -> Unit = {}): CharStyle {
	val c = CharStyle()
	c.init()
	return c
}

object CharStyleSerializer : To<CharStyle>, From<CharStyle> {

	override fun CharStyle.write(writer: Writer) {
		writer.styleProperty(this, this::fontKey)?.string(fontKey)
		writer.styleProperty(this, this::underlined)?.bool(underlined)
		writer.styleProperty(this, this::colorTint)?.color(colorTint)
		writer.styleProperty(this, this::backgroundColor)?.color(backgroundColor)
		writer.styleProperty(this, this::selectedColorTint)?.color(selectedColorTint)
		writer.styleProperty(this, this::selectedBackgroundColor)?.color(selectedBackgroundColor)
		writer.styleProperty(this, this::selectable)?.bool(selectable)
	}

	override fun read(reader: Reader): CharStyle {
		val c = CharStyle()
		reader.contains(c::fontKey.name) { c.fontKey = it.string()!! }
		reader.contains(c::underlined.name) { c.underlined = it.bool()!! }
		reader.contains(c::colorTint.name) { c.colorTint = it.color()!! }
		reader.contains(c::backgroundColor.name) { c.backgroundColor = it.color()!! }
		reader.contains(c::selectedColorTint.name) { c.selectedColorTint = it.color()!! }
		reader.contains(c::selectedBackgroundColor.name) { c.selectedBackgroundColor = it.color()!! }
		reader.contains(c::selectable.name) { c.selectable = it.bool()!! }
		return c
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

	companion object : StyleType<TextInputStyle>
}
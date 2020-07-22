/*
 * Copyright 2020 Poly Forest, LLC
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

package com.acornui.skins

import com.acornui.component.*
import com.acornui.component.layout.LayoutContainer
import com.acornui.component.layout.VAlign
import com.acornui.component.layout.algorithm.*
import com.acornui.component.text.TextField
import com.acornui.component.text.charStyle
import com.acornui.component.text.selectable
import com.acornui.component.text.text
import com.acornui.di.Context
import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
import com.acornui.math.*
import com.acornui.properties.afterChangeWithInit

private class BasicButtonSkin(
		owner: Context,
		private val theme: Theme,
		borderRadius: CornersRo,
		borderThickness: PadRo
) : LayoutContainer<CanvasLayoutStyle, CanvasLayoutData>(owner, CanvasLayout()), ButtonSkin {

	constructor(owner: Context, theme: Theme) : this(owner, theme, Corners(theme.borderRadius), Pad(theme.strokeThickness))

	override var label: String = ""

	override var buttonState: ButtonState by validationProp(ButtonState.UP, ValidationFlags.PROPERTIES)

	private val fill: Rect
	private val upShine: Rect
	private val downShine: Rect

	init {
		defaultWidth = 100.0
		defaultHeight = 50.0
		validation.addNode(ValidationFlags.PROPERTIES, 0, ValidationFlags.STYLES, ::updateProperties)
		fill = +rect {
			style.apply {
				val bT = borderThickness.copy()
				this.borderThicknesses = bT
				this.borderRadii = borderRadius
			}
		} layout { fill() }

		upShine = +rect {
			style.apply {
				backgroundColor = Color.WHITE
				margin = Pad(left = borderThickness.left, top = borderThickness.top, right = borderThickness.right, bottom = 0.0)
				this.borderRadii = Corners(
						topLeft = vec2(borderRadius.topLeft.x - borderThickness.left, borderRadius.topLeft.y - borderThickness.top),
						topRight = vec2(borderRadius.topRight.x - borderThickness.right, borderRadius.topRight.y - borderThickness.top),
						bottomLeft = vec2(), bottomRight = vec2()
				)
			}
		} layout {
			widthPercent = 1.0
			heightPercent = 0.5
		}

		downShine = +rect {
			snapToPixel = false
			style.apply {
				backgroundColor = Color.WHITE
				margin = Pad(left = borderThickness.left, top = 0.0, right = borderThickness.right, bottom = borderThickness.bottom)
				this.borderRadii = Corners(
						topLeft = vec2(), topRight = vec2(),
						bottomLeft = vec2(borderRadius.bottomLeft.x - borderThickness.left, borderRadius.bottomLeft.y - borderThickness.bottom),
						bottomRight = vec2(borderRadius.bottomRight.x - borderThickness.right, borderRadius.bottomRight.y - borderThickness.bottom)
				)
			}
		} layout {
			widthPercent = 1.0
			verticalCenter = 0.0
			bottom = 0.0
		}
	}

	private fun updateProperties() {
		val buttonState = buttonState
		fill.style.apply {
			backgroundColor = theme.getButtonFillColor(buttonState)
			borderColors = BorderColors(theme.getButtonStrokeColor(buttonState))
		}
		upShine.visible = !buttonState.isDown && buttonState != ButtonState.DISABLED
		downShine.visible = buttonState.isDown && buttonState != ButtonState.DISABLED
		val shineColor = if (buttonState.isToggled) theme.fillToggledShine else theme.fillShine
		upShine.colorTint = shineColor
		downShine.colorTint = shineColor
	}
}

fun Context.basicButtonSkin(
		theme: Theme,
		borderRadii: CornersRo = Corners(theme.borderRadius),
		borderThickness: PadRo = Pad(theme.strokeThickness)
): ButtonSkin {
	return BasicButtonSkin(this, theme, borderRadii, borderThickness)
}

/**
 * A typical implementation of a skin part for a labelable button state.
 */
private class BasicLabelButtonSkin(
		owner: Context,
		private val texture: ButtonSkin,
		private val padding: PadRo,
		val textColors: Map<ButtonState, ColorRo>
) : ContainerImpl(owner), ButtonSkin {

	val charStyle = bind(charStyle())

	private val textField: TextField = text {
		charStyle.colorTint = Color.WHITE
		selectable = false
	}

	override var buttonState: ButtonState by afterChangeWithInit(ButtonState.UP) { value ->
		texture.buttonState = value
		refreshTextColor(value)
	}

	init {
		addChild(texture)
		addChild(textField)
		watch(charStyle) { refreshTextColor(buttonState) }
		textField.flowStyle.horizontalAlign = FlowHAlign.CENTER
	}

	private fun refreshTextColor(buttonState: ButtonState) {
		textField.colorTint = buttonState.fallbackWalk { state ->
			textColors[state]
		} ?: charStyle.colorTint
	}

	override var label: String by afterChangeWithInit("") { value ->
		textField.label = value
		texture.label = value
	}

	override fun updateLayout(explicitBounds: ExplicitBounds): Bounds {
		textField.size(padding.reduceWidth(explicitWidth), null)
		var h = maxOf(minHeight, padding.expandHeight(textField.height))
		val w = maxOf(h, explicitWidth ?: maxOf(minWidth, padding.expandWidth(textField.width)))
		if (explicitHeight != null && explicitHeight > h) h = explicitHeight
		texture.size(w, h)
		textField.position((padding.reduceWidth(w) - textField.width) * 0.5 + padding.left, (padding.reduceHeight(h) - textField.height) * 0.5 + padding.top)
		out.set(texture.width, texture.height, textField.baselineY)
	}
}

fun Context.basicLabelButtonSkin(texture: ButtonSkin, padding: PadRo, textColors: Map<ButtonState, ColorRo>): ButtonSkin = BasicLabelButtonSkin(this, texture, padding, textColors)
fun Context.basicLabelButtonSkin(theme: Theme): ButtonSkin = BasicLabelButtonSkin(this, basicButtonSkin(theme), theme.buttonPad, mapOf(ButtonState.DISABLED to theme.textDisabledColor))

/**
 * A typical implementation of a skin part for a labelable button state.
 */
private class BasicCheckboxSkin(
		owner: Context,
		private val box: ButtonSkin
) : HorizontalLayoutContainer<UiComponent>(owner), ButtonSkin {

	private val textField: TextField

	init {
		style.gap = 4.0
		style.verticalAlign = VAlign.MIDDLE
		+box
		textField = +text("") {
			selectable = false
		} layout {
			widthPercent = 1.0
		}
	}

	override var buttonState: ButtonState by afterChangeWithInit(ButtonState.UP) { value ->
		box.buttonState = value
		textField.styleTags.clear()
	}

	override var label: String by afterChangeWithInit("") { value ->
		textField.label = value
		textField.visible = value.isNotEmpty()
		box.label = value
	}

	override fun updateLayout(explicitBounds: ExplicitBounds): Bounds {
		super.updateLayout(explicitWidth, explicitHeight, out)
		// If the text field is visible, use it as a baseline.
		// The vertical alignment is MIDDLE,
		out.baseline = if (textField.visible) textField.baselineY else box.baselineY
	}
}

private class BasicCheckboxBox(
		owner: Context,
		theme: Theme,
		upRegion: String,
		toggledRegion: String,
		indeterminateRegion: String
) : ButtonSkin, StackLayoutContainer<UiComponent>(owner) {

	private val indeterminateState = +iconAtlas(theme.atlasPaths, indeterminateRegion)
	private val toggledState = +iconAtlas(theme.atlasPaths, toggledRegion)
	private val upState = +iconAtlas(theme.atlasPaths, upRegion)

	override var label: String = ""

	init {
		style.padding = Pad(-3.0) // The icon is only 18px and has 3px of padding around it.
		baselineOverride = 14.0 // To line up with the check mark
	}

	override var buttonState: ButtonState by afterChangeWithInit(ButtonState.UP) { value ->
		upState.visible = false
		toggledState.visible = false
		indeterminateState.visible = false
		if (value.isIndeterminate) {
			indeterminateState.visible = true
		} else {
			if (value.isToggled) {
				toggledState.visible = true
			} else {
				upState.visible = true
			}
		}
	}
}

fun Context.basicCheckboxSkin(theme: Theme): ButtonSkin {
	return BasicCheckboxSkin(this, BasicCheckboxBox(
			this,
			theme,
			upRegion = "ic_check_box_outline_blank_white_24dp",
			toggledRegion = "ic_check_box_white_24dp",
			indeterminateRegion = "ic_indeterminate_check_box_white_24dp"
	))
}

fun Context.basicRadioButtonSkin(theme: Theme): ButtonSkin {
	return BasicCheckboxSkin(this, BasicCheckboxBox(
			this,
			theme,
			upRegion = "ic_radio_button_unchecked_white_24dp",
			toggledRegion = "ic_radio_button_checked_white_24dp",
			indeterminateRegion = "ic_indeterminate_check_box_white_24dp"
	))
}

fun Context.collapseButtonSkin(theme: Theme): ButtonSkin {
	return BasicCheckboxSkin(this, BasicCheckboxBox(
			this,
			theme,
			upRegion = "ic_chevron_right_white_24dp",
			toggledRegion = "ic_expand_more_white_24dp",
			indeterminateRegion = "ic_indeterminate_check_box_white_24dp"
	))
}

private class BasicTabSkin(owner: Context, theme: Theme) : SingleElementContainerImpl<UiComponent>(owner), ButtonSkin {

	private val notToggled: IconButtonSkin
	private val toggled: IconButtonSkin

	init {
		val corners = Corners(topLeft = theme.borderRadius, topRight = theme.borderRadius, bottomLeft = 0.0, bottomRight = 0.0)
		notToggled = addChild(basicIconButtonSkin(texture = basicButtonSkin(theme, corners)))
		val borderThickness = Pad(left = theme.strokeThickness, top = theme.strokeThickness, right = theme.strokeThickness, bottom = 0.0)
		toggled = addChild(basicIconButtonSkin(texture = basicButtonSkin(theme, corners, borderThickness)))
	}

	override var buttonState: ButtonState by afterChangeWithInit(ButtonState.UP) { value ->
		toggled.buttonState = value
		notToggled.buttonState = value
		toggled.visible = value.isToggled
		notToggled.visible = !value.isToggled

		if (value.isToggled) {
			if (toggled.element !== element) {
				notToggled.element = null
				toggled.element = element
			}
		} else {
			if (notToggled.element !== element) {
				toggled.element = null
				notToggled.element = element
			}
		}
	}

	override var label: String by afterChangeWithInit("") { value ->
		toggled.label = value
		notToggled.label = value
	}

	private val activeSkin: IconButtonSkin
		get() = if (buttonState.isToggled) toggled else notToggled

	override fun onElementChanged(oldElement: UiComponent?, newElement: UiComponent?) {
		activeSkin.element = newElement
	}

	override fun updateLayout(explicitBounds: ExplicitBounds): Bounds {
		activeSkin.size(explicitWidth, explicitHeight)
		out.set(activeSkin.bounds)
	}
}

fun Context.basicTabSkin(theme: Theme): ButtonSkin = BasicTabSkin(this, theme)

class EmptyButtonSkin(owner: Context) : UiComponentImpl(owner), ButtonSkin {
	override var label: String = ""
	override var buttonState: ButtonState = ButtonState.UP
}

fun Context.emptyButtonSkin(): EmptyButtonSkin {
	return EmptyButtonSkin(this)
}
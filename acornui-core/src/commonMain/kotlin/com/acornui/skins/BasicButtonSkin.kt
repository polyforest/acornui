package com.acornui.skins

import com.acornui.component.*
import com.acornui.component.layout.VAlign
import com.acornui.component.layout.algorithm.CanvasLayoutContainer
import com.acornui.component.layout.algorithm.FlowHAlign
import com.acornui.component.layout.algorithm.HorizontalLayoutContainer
import com.acornui.component.text.TextField
import com.acornui.component.text.selectable
import com.acornui.component.text.text
import com.acornui.di.Owned
import com.acornui.graphic.Color
import com.acornui.math.*
import com.acornui.reflect.observableAndCall

private class BasicButtonSkin(
		owner: Owned,
		private val theme: Theme,
		borderRadius: CornersRo,
		borderThickness: PadRo
) : CanvasLayoutContainer(owner), ButtonSkin {

	constructor(owner: Owned, theme: Theme) : this(owner, theme, Corners(theme.borderRadius), Pad(theme.strokeThickness))

	override var label: String = ""

	override var buttonState: ButtonState by validationProp(ButtonState.UP, ValidationFlags.PROPERTIES)

	private val fill: Rect
	private val upShine: Rect
	private val downShine: Rect

	init {
		validation.addNode(ValidationFlags.PROPERTIES, 0, ::updateProperties)
		fill = +rect {
			style.apply {
				val bT = borderThickness.copy()
				this.borderThicknesses = bT
				this.borderRadii = borderRadius
			}
		} layout { fill() }

		upShine = +rect {
			snapToPixel = false
			style.apply {
				backgroundColor = Color.WHITE
				margin = Pad(top = borderThickness.top, right = borderThickness.right, bottom = 0f, left = borderThickness.left)
				this.borderRadii = Corners(
						topLeft = Vector2(borderRadius.topLeft.x - borderThickness.left, borderRadius.topLeft.y - borderThickness.top),
						topRight = Vector2(borderRadius.topRight.x - borderThickness.right, borderRadius.topRight.y - borderThickness.top),
						bottomLeft = Vector2(), bottomRight = Vector2()
				)
			}
		} layout {
			widthPercent = 1f
			heightPercent = 0.5f
		}

		downShine = +rect {
			snapToPixel = false
			style.apply {
				backgroundColor = Color.WHITE
				margin = Pad(top = 0f, right = borderThickness.right, bottom = borderThickness.bottom, left = borderThickness.left)
				this.borderRadii = Corners(
						topLeft = Vector2(), topRight = Vector2(),
						bottomLeft = Vector2(borderRadius.bottomLeft.x - borderThickness.left, borderRadius.bottomLeft.y - borderThickness.bottom),
						bottomRight = Vector2(borderRadius.bottomRight.x - borderThickness.right, borderRadius.bottomRight.y - borderThickness.bottom)
				)
			}
		} layout {
			widthPercent = 1f
			verticalCenter = 0f
			bottom = 0f
		}
	}

	private fun updateProperties() {
		val buttonState = buttonState
		fill.style.apply {
			backgroundColor = theme.getButtonFillColor(buttonState)
			borderColors = BorderColors(theme.getButtonStrokeColor(buttonState))
		}
		upShine.visible = !buttonState.isDown
		downShine.visible = buttonState.isDown
		val shineColor = if (buttonState.isToggled) theme.fillToggledShine else theme.fillShine
		upShine.colorTint = shineColor
		downShine.colorTint = shineColor
	}
}

fun Owned.basicButtonSkin(
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
		owner: Owned,
		private val texture: ButtonSkin,
		private val padding: PadRo
) : ElementContainerImpl<UiComponent>(owner), ButtonSkin {

	private val textField: TextField = text()

	init {
		+texture
		+textField

		textField.selectable = false
		textField.flowStyle.horizontalAlign = FlowHAlign.CENTER
	}

	override var buttonState: ButtonState by observableAndCall(ButtonState.UP) { value ->
		texture.buttonState = value
		textField.styleTags.clear()
		textField.styleTags.add(value.styleTag)
	}

	override var label: String by observableAndCall("") { value ->
		textField.label = value
		texture.label = value
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		textField.setSize(padding.reduceWidth(explicitWidth), null)
		var h = maxOf(minHeight ?: 0f, padding.expandHeight2(textField.height))
		val w = maxOf(h, explicitWidth ?: maxOf(minWidth ?: 0f, padding.expandWidth2(textField.width)))
		if (explicitHeight != null && explicitHeight > h) h = explicitHeight
		texture.setSize(w, h)
		textField.moveTo((padding.reduceWidth2(w) - textField.width) * 0.5f + padding.left, (padding.reduceHeight2(h) - textField.height) * 0.5f + padding.top)
		out.set(texture.width, texture.height, textField.baselineY)
	}
}

fun Owned.basicLabelButtonSkin(theme: Theme, texture: ButtonSkin = basicButtonSkin(theme), padding: PadRo = theme.buttonPad): ButtonSkin = BasicLabelButtonSkin(this, texture, padding)

/**
 * A typical implementation of a skin part for a labelable button state.
 */
private class BasicCheckboxSkin(
		owner: Owned,
		private val box: ButtonSkin
) : HorizontalLayoutContainer(owner), ButtonSkin {

	private val textField: TextField

	init {
		style.verticalAlign = VAlign.MIDDLE
		+box
		textField = +text("") layout {
			widthPercent = 1f
		}
	}

	override var buttonState: ButtonState by observableAndCall(ButtonState.UP) { value ->
		box.buttonState = value
		textField.styleTags.clear()
		textField.styleTags.add(value.styleTag)
	}

	override var label: String by observableAndCall("") { value ->
		textField.label = value
		textField.visible = value.isNotEmpty()
		box.label = value
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		super.updateLayout(explicitWidth, explicitHeight, out)
		// If the text field is visible, use it as a baseline.
		// The vertical alignment is MIDDLE,
		if (textField.visible) out.baseline = textField.baselineY
	}
}

private class BasicCheckboxBox(
		owner: Owned,
		theme: Theme,
		upRegion: String,
		toggledRegion: String,
		indeterminateRegion: String
) : ButtonSkin, StackLayoutContainer(owner) {

	private val indeterminateState = +iconAtlas(theme.atlasPath, indeterminateRegion)
	private val toggledState = +iconAtlas(theme.atlasPath, toggledRegion)
	private val upState = +iconAtlas(theme.atlasPath, upRegion)

	init {
		style.padding = Pad(-3f) // The icon is only 18px and has 3px of padding around it.
	}

	override var label: String = ""

	override var buttonState: ButtonState by observableAndCall(ButtonState.UP) { value ->
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

fun Owned.basicCheckboxSkin(theme: Theme): ButtonSkin {
	return BasicCheckboxSkin(this, BasicCheckboxBox(
			this,
			theme,
			upRegion = "ic_check_box_outline_blank_white_24dp",
			toggledRegion = "ic_check_box_white_24dp",
			indeterminateRegion = "ic_indeterminate_check_box_white_24dp"
	))
}

fun Owned.basicRadioButtonSkin(theme: Theme): ButtonSkin {
	return BasicCheckboxSkin(this, BasicCheckboxBox(
			this,
			theme,
			upRegion = "ic_radio_button_unchecked_white_24dp",
			toggledRegion = "ic_radio_button_checked_white_24dp",
			indeterminateRegion = "ic_indeterminate_check_box_white_24dp"
	))
}

fun Owned.collapseButtonSkin(theme: Theme): ButtonSkin {
	return BasicCheckboxSkin(this, BasicCheckboxBox(
			this,
			theme,
			upRegion = "ic_chevron_right_white_24dp",
			toggledRegion = "ic_expand_more_white_24dp",
			indeterminateRegion = "ic_indeterminate_check_box_white_24dp"
	))
}

private class BasicTabSkin(owner: Owned, val theme: Theme) : ContainerImpl(owner), ButtonSkin {

	private val notToggled: ButtonSkin
	private val toggled: ButtonSkin

	init {
		val corners = Corners(topLeft = theme.borderRadius, topRight = theme.borderRadius, bottomLeft = 0f, bottomRight = 0f)
		notToggled = addChild(basicLabelButtonSkin(theme, texture = basicButtonSkin(theme, corners)))
		val borderThickness = Pad(top = theme.strokeThickness, right = theme.strokeThickness, bottom = 0f, left = theme.strokeThickness)
		toggled = addChild(basicLabelButtonSkin(theme, texture = basicButtonSkin(theme, corners, borderThickness)))
	}

	override var buttonState: ButtonState by observableAndCall(ButtonState.UP) { value ->
		toggled.buttonState = value
		notToggled.buttonState = value
		toggled.visible = value.isToggled
		notToggled.visible = !value.isToggled
	}

	override var label: String by observableAndCall("") { value ->
		toggled.label = value
		notToggled.label = value
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		toggled.setSize(explicitWidth, explicitHeight)
		notToggled.setSize(explicitWidth, explicitHeight)
		out.set(toggled.bounds)
	}
}

fun Owned.basicTabSkin(theme: Theme): ButtonSkin = BasicTabSkin(this, theme)
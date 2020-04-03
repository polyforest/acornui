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
		defaultWidth = 100f
		defaultHeight = 50f
		validation.addNode(ValidationFlags.PROPERTIES, 0, ValidationFlags.STYLES, ::updateProperties)
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
						topLeft = vec2(borderRadius.topLeft.x - borderThickness.left, borderRadius.topLeft.y - borderThickness.top),
						topRight = vec2(borderRadius.topRight.x - borderThickness.right, borderRadius.topRight.y - borderThickness.top),
						bottomLeft = vec2(), bottomRight = vec2()
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
						topLeft = vec2(), topRight = vec2(),
						bottomLeft = vec2(borderRadius.bottomLeft.x - borderThickness.left, borderRadius.bottomLeft.y - borderThickness.bottom),
						bottomRight = vec2(borderRadius.bottomRight.x - borderThickness.right, borderRadius.bottomRight.y - borderThickness.bottom)
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
	}

	override var buttonState: ButtonState by afterChangeWithInit(ButtonState.UP) { value ->
		texture.buttonState = value
		refreshTextColor(value)
	}

	init {
		addChild(texture)
		addChild(textField)
		watch(charStyle) { refreshTextColor(buttonState) }
		textField.selectable = false
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

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		textField.setSize(padding.reduceWidth(explicitWidth), null)
		var h = maxOf(minHeight, padding.expandHeight(textField.height))
		val w = maxOf(h, explicitWidth ?: maxOf(minWidth, padding.expandWidth(textField.width)))
		if (explicitHeight != null && explicitHeight > h) h = explicitHeight
		texture.setSize(w, h)
		textField.moveTo((padding.reduceWidth(w) - textField.width) * 0.5f + padding.left, (padding.reduceHeight(h) - textField.height) * 0.5f + padding.top)
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
		style.verticalAlign = VAlign.MIDDLE
		+box
		textField = +text("") layout {
			widthPercent = 1f
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

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		super.updateLayout(explicitWidth, explicitHeight, out)
		// If the text field is visible, use it as a baseline.
		// The vertical alignment is MIDDLE,
		if (textField.visible) out.baseline = textField.baselineY
	}
}

private class BasicCheckboxBox(
		owner: Context,
		theme: Theme,
		upRegion: String,
		toggledRegion: String,
		indeterminateRegion: String
) : ButtonSkin, StackLayoutContainer<UiComponent>(owner) {

	private val indeterminateState = +iconAtlas(theme.atlasPath, indeterminateRegion)
	private val toggledState = +iconAtlas(theme.atlasPath, toggledRegion)
	private val upState = +iconAtlas(theme.atlasPath, upRegion)

	init {
		style.padding = Pad(-3f) // The icon is only 18px and has 3px of padding around it.
	}

	override var label: String = ""

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
		val corners = Corners(topLeft = theme.borderRadius, topRight = theme.borderRadius, bottomLeft = 0f, bottomRight = 0f)
		notToggled = addChild(basicIconButtonSkin(texture = basicButtonSkin(theme, corners)))
		val borderThickness = Pad(top = theme.strokeThickness, right = theme.strokeThickness, bottom = 0f, left = theme.strokeThickness)
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

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		activeSkin.setSize(explicitWidth, explicitHeight)
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
package com.acornui.skins

import com.acornui.component.*
import com.acornui.component.layout.SizeConstraints
import com.acornui.component.layout.VAlign
import com.acornui.component.text.TextField
import com.acornui.component.text.text
import com.acornui.di.Owned
import com.acornui.math.Bounds
import com.acornui.math.MathUtils
import com.acornui.math.Pad
import com.acornui.math.PadRo
import com.acornui.reflect.observableAndCall

interface IconButtonSkin : ButtonSkin, SingleElementContainer<UiComponent>

private class BasicIconButtonSkin(
		owner: Owned,
		private val texture: ButtonSkin,
		private val padding: PadRo,
		private val hGap: Float,

		/**
		 * The vertical alignment between the icon and the label.
		 */
		private val vAlign: VAlign,

		/**
		 * If false, the icon will be on the right instead of left.
		 */
		private val iconOnLeft: Boolean
) : SingleElementContainerImpl<UiComponent>(owner), IconButtonSkin {

	private val icon: Image
	private val textField: TextField

	init {
		addChild(texture)
		icon = addChild(image())
		textField = addChild(text {
			interactivityMode = InteractivityMode.NONE
		})
	}

	override var label: String = ""
		set(value) {
			field = value
			textField.label = value
			texture.label = value
		}

	override var buttonState: ButtonState by observableAndCall(ButtonState.UP) { value ->
		texture.buttonState = value
	}

	override fun onElementChanged(oldElement: UiComponent?, newElement: UiComponent?) {
		icon.element = newElement
	}

	override fun updateSizeConstraints(out: SizeConstraints) {
		out.width.min = icon.width + padding.left + padding.right
		out.height.min = icon.height + padding.top + padding.bottom
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val childAvailableWidth = padding.reduceWidth(explicitWidth)
		val childAvailableHeight = padding.reduceHeight(explicitHeight)
		val textWidth = if (childAvailableWidth == null) null else childAvailableWidth - icon.width - hGap
		textField.setSize(textWidth, childAvailableHeight)
		val contentWidth = MathUtils.roundToNearest(if (label == "") icon.width else icon.width + hGap + textField.width, 2f)
		val contentHeight = MathUtils.roundToNearest(if (label == "") icon.height else maxOf(textField.height, icon.height), 2f)
		val w = maxOf(padding.expandWidth(contentWidth), explicitWidth ?: 4f)
		val h = maxOf(padding.expandHeight(contentHeight), explicitHeight ?: 4f)

		texture.setSize(w, h)

		val iconX: Float
		val textFieldX: Float
		if (iconOnLeft) {
			iconX = if (childAvailableWidth != null) {
				(childAvailableWidth - contentWidth) * 0.5f + padding.left
			} else {
				padding.left
			}
			textFieldX = MathUtils.offsetRound(iconX + icon.width + hGap)
		} else {
			textFieldX = if (childAvailableWidth != null) {
				(childAvailableWidth - contentWidth) * 0.5f + padding.left
			} else {
				padding.left
			}
			iconX = textFieldX + textField.width + hGap
		}

		val yOffset = if (childAvailableHeight == null) padding.top else (childAvailableHeight - contentHeight) * 0.5f + padding.top

		val baseline = yOffset + textField.baseline
		val iconY: Float
		val textFieldY: Float
		when (vAlign) {
			VAlign.TOP -> {
				iconY = yOffset
				textFieldY = yOffset
			}
			VAlign.MIDDLE -> {
				iconY = yOffset + (contentHeight - icon.height) * 0.5f
				textFieldY = (yOffset + (contentHeight - textField.height) * 0.5f)
			}
			VAlign.BOTTOM -> {
				iconY = yOffset + (contentHeight - icon.height)
				textFieldY = yOffset + (contentHeight - textField.height)
			}
			VAlign.BASELINE -> {
				iconY = baseline - icon.baseline
				textFieldY = baseline - textField.baseline
			}
		}
		icon.moveTo(iconX, iconY)
		textField.moveTo(textFieldX, textFieldY)

		out.set(w, h, textField.baselineY)
	}
}

fun Owned.basicIconButtonSkin(texture: ButtonSkin,

							  /**
							   * The padding around the text and icon.
							   */
							  padding: PadRo = Pad(4f),

							  /**
							   * The horizontal gap between the icon and the textfield.
							   */
							  hGap: Float = 4f,

							  /**
							   * The vertical alignment between the icon and the label.
							   */
							  vAlign: VAlign = VAlign.MIDDLE,

							  /**
							   * If false, the icon will be on the right instead of left.
							   */
							  iconOnLeft: Boolean = true

): IconButtonSkin = BasicIconButtonSkin(this, texture, padding, hGap, vAlign, iconOnLeft)

fun Owned.basicIconButtonSkin(theme: Theme): IconButtonSkin = basicIconButtonSkin(basicButtonSkin(theme), theme.buttonPad, theme.iconButtonGap, VAlign.MIDDLE, iconOnLeft = true)
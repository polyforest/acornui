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

package com.acornui.skins

import com.acornui.component.*
import com.acornui.component.layout.algorithm.CanvasLayoutContainer
import com.acornui.component.layout.algorithm.canvas
import com.acornui.component.style.OptionalSkinPart
import com.acornui.core.di.Owned
import com.acornui.core.di.inject
import com.acornui.graphic.Color
import com.acornui.math.*

interface SkinPartProvider {

	/**
	 * Returns a factory to create an icon button skin part for the given button state.
	 */
	fun iconButtonSkin(buttonState: ButtonState, icon: String, padding: PadRo = Pad(5f), hGap: Float = 4f): OptionalSkinPart

	/**
	 * Returns a factory to create a label button skin part for the given button state.
	 */
	fun labelButtonSkin(theme: Theme, buttonState: ButtonState): OptionalSkinPart

	/**
	 * Returns a factory to create a tab button skin part for the given button state.
	 */
	fun tabButtonSkin(theme: Theme, buttonState: ButtonState): OptionalSkinPart

	/**
	 * A convenience function to create a button skin part.
	 */
	fun iconButtonSkin(theme: Theme, buttonState: ButtonState): OptionalSkinPart

	/**
	 * A checkbox skin part.
	 */
	fun checkboxSkin(theme: Theme, buttonState: ButtonState): OptionalSkinPart

	/**
	 * A checkbox skin part.
	 */
	fun collapseButtonSkin(theme: Theme, buttonState: ButtonState): OptionalSkinPart

	/**
	 * A convenience function to create a radio button skin part.
	 */
	fun radioButtonSkin(theme: Theme, buttonState: ButtonState): OptionalSkinPart

	/**
	 *
	 */
	fun Owned.buttonTexture(
			buttonState: ButtonState,
			borderRadius: CornersRo = Corners(inject(Theme).borderRadius),
			borderThickness: PadRo = Pad(inject(Theme).strokeThickness)
	): UiComponent
}

open class BasicSkinPartProvider : SkinPartProvider {

	override fun iconButtonSkin(buttonState: ButtonState, icon: String, padding: PadRo, hGap: Float): OptionalSkinPart = {
		if (buttonState.isIndeterminate) null
		else {
			val texture = buttonTexture(buttonState)
			val skinPart = IconButtonSkinPart(this, texture, padding, hGap)
			val theme = inject(Theme)
			skinPart.element = atlas(theme.atlasPath, icon)
			skinPart
		}
	}

	override fun labelButtonSkin(theme: Theme, buttonState: ButtonState): OptionalSkinPart = {
		if (buttonState.isIndeterminate) null
		else {
			val texture = buttonTexture(buttonState)
			LabelButtonSkinPart(this, texture, theme.buttonPad)
		}
	}

	override fun tabButtonSkin(theme: Theme, buttonState: ButtonState): OptionalSkinPart = {
		if (buttonState.isIndeterminate) null
		else {
			val borderThickness = Pad(theme.strokeThickness)
			if (buttonState.isToggled) borderThickness.bottom = 0f
			val texture = buttonTexture(
					buttonState,
					Corners(topLeft = theme.borderRadius, topRight = theme.borderRadius, bottomLeft = 0f, bottomRight = 0f),
					borderThickness
			)
			IconButtonSkinPart(this, texture, theme.buttonPad, theme.iconButtonGap)
		}
	}

	/**
	 * A convenience function to create a button skin part.
	 */
	override fun iconButtonSkin(theme: Theme, buttonState: ButtonState): OptionalSkinPart = {
		if (buttonState.isIndeterminate) null
		else {
			val texture = buttonTexture(buttonState)
			IconButtonSkinPart(this, texture, theme.buttonPad, theme.iconButtonGap)
		}
	}

	/**
	 * A checkbox skin part.
	 */
	override fun checkboxSkin(theme: Theme, buttonState: ButtonState): OptionalSkinPart = {
		val box = iconAtlas(theme.atlasPath, if (buttonState.isIndeterminate) "ic_indeterminate_check_box_white_24dp" else if (buttonState.isToggled) "ic_check_box_white_24dp" else "ic_check_box_outline_blank_white_24dp")
		CheckboxSkinPart(
				this,
				box
		)
	}

	/**
	 * A checkbox skin part.
	 */
	override fun collapseButtonSkin(theme: Theme, buttonState: ButtonState): OptionalSkinPart = {
		if (buttonState.isIndeterminate) null
		else {
			val box = iconAtlas(theme.atlasPath, if (buttonState.isToggled) "ic_expand_more_white_24dp" else "ic_chevron_right_white_24dp") {
				colorTint = theme.iconColor
			}
			CheckboxSkinPart(
					this,
					box
			)
		}
	}

	/**
	 * A convenience function to create a radio button skin part.
	 */
	override fun radioButtonSkin(theme: Theme, buttonState: ButtonState): Owned.() -> CheckboxSkinPart = {
		val radio = buttonTexture(buttonState, borderRadius = Corners(1000f), borderThickness = Pad(theme.strokeThickness))
		if (buttonState.isToggled) {
			val filledCircle = rect {
				style.margin = Pad(4f)
				style.borderRadii = Corners(1000f)
				style.backgroundColor = Color.DARK_GRAY.copy()
				layoutData = radio.createLayoutData().apply {
					fill()
				}
			}
			radio.addElement(filledCircle)
		}

		CheckboxSkinPart(
				this,
				radio
		).apply {
			radio layout {
				width = 18f
				height = 18f
			}
		}
	}

	override fun Owned.buttonTexture(buttonState: ButtonState, borderRadius: CornersRo, borderThickness: PadRo): CanvasLayoutContainer = canvas {
		val theme = inject(Theme)
		+rect {
			style.apply {
				backgroundColor = theme.getButtonFillColor(buttonState)
				borderColors = BorderColors(theme.getButtonStrokeColor(buttonState))
				val bT = borderThickness.copy()
				this.borderThicknesses = bT
				this.borderRadii = borderRadius
			}
		} layout { fill() }
		when (buttonState) {
			ButtonState.UP,
			ButtonState.OVER,
			ButtonState.TOGGLED_UP,
			ButtonState.TOGGLED_OVER -> {
				+rect {
					style.apply {
						margin = Pad(top = borderThickness.top, right = borderThickness.right, bottom = 0f, left = borderThickness.left)
						backgroundColor = if (buttonState.isToggled) theme.fillToggledShine else theme.fillShine
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
			}
			ButtonState.DISABLED -> {
			}
			else -> {
				+rect {
					style.apply {
						margin = Pad(top = 0f, right = borderThickness.right, bottom = borderThickness.bottom, left = borderThickness.left)
						backgroundColor = if (buttonState.isToggled) theme.fillToggledShine else theme.fillShine
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
		}
	}

}


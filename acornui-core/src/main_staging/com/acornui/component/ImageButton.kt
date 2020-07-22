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

package com.acornui.component

import com.acornui.collection.addAll
import com.acornui.component.style.*
import com.acornui.cursor.StandardCursor
import com.acornui.cursor.cursor
import com.acornui.di.Context

import com.acornui.focus.Focusable
import com.acornui.focus.mousePressOnKey
import com.acornui.gl.core.mulColorTransformation
import com.acornui.graphic.Color
import com.acornui.input.interaction.MouseOrTouchState
import com.acornui.math.Bounds
import com.acornui.math.colorTransformation
import com.acornui.math.grayscale
import com.acornui.properties.afterChange
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * A button that tints a single element (typically a white image).
 *
 * Note: ImageButton does not currently support indeterminate states.
 */
class ImageButton(
		owner: Context
) : SingleElementContainerImpl<UiComponent>(owner), Toggleable, Focusable {

	val style = bind(ImageButtonStyle())

	private val colorTransformation = colorTransformation()

	init {
		focusEnabled = true
		focusEnabledChildren = false
		addClassAll(Companion)

		cursor(StandardCursor.POINTER)

		validation.addNode(ValidationFlags.PROPERTIES, dependencies = ValidationFlags.STYLES, dependents = ValidationFlags.LAYOUT, onValidate = ::updateProperties)
		mousePressOnKey()
	}

	var disabled: Boolean by afterChange(false) {
		interactivityMode = if (it) InteractivityMode.NONE else InteractivityMode.ALL
		disabledTag = it
		invalidateProperties()
	}

	override var toggled: Boolean by validationProp(false, ValidationFlags.PROPERTIES)

	private fun updateProperties() {
		currentState = ButtonState.calculateButtonState(mouseOrTouchState.isOver, mouseOrTouchState.isDown, toggled, false, disabled)
		val colorTransform = when (currentState) {
			ButtonState.OVER -> style.overState
			ButtonState.DOWN -> style.downState
			ButtonState.DISABLED -> style.disabledState
			ButtonState.TOGGLED_UP -> style.toggledUpState
			ButtonState.TOGGLED_OVER -> style.toggledOverState
			ButtonState.TOGGLED_DOWN -> style.toggledDownState
			else -> style.upState
		}
		colorTransformation.set(colorTransform)
	}

	var currentState: ButtonState = ButtonState.UP
		get() {
			validate(ValidationFlags.PROPERTIES)
			return field
		}
		private set

	override fun updateLayout(explicitBounds: ExplicitBounds): Bounds {
		val element = element ?: return
		element.size(explicitWidth, explicitHeight)
		out.set(element.bounds)
	}

	private val mouseOrTouchState = own(MouseOrTouchState(this)).apply {
		isOverChanged.add { invalidateProperties() }
		isDownChanged.add { invalidateProperties() }
	}

	companion object : StyleTag {
		val ICON_IMAGE = styleTag()
	}

}

fun Context.imageButton(init: ImageButton.() -> Unit = {}): ImageButton {
	val i = ImageButton(this)
	i.init()
	return i
}

fun Context.iconImageButton(init: ImageButton.() -> Unit = {}): ImageButton =
		imageButton {
			addClass(ImageButton.ICON_IMAGE)
			init()
		}

fun Context.iconImageButton(atlasPath: String, atlasRegion: String, init: ImageButton.() -> Unit = {}): ImageButton =
		iconImageButton(mapOf(1.0 to atlasPath), atlasRegion, init)

fun Context.iconImageButton(atlasPaths: Map<Double, String>, atlasRegion: String, init: ImageButton.() -> Unit = {}): ImageButton =
		imageButton {
			addClass(ImageButton.ICON_IMAGE)
			element = atlas(atlasPaths, atlasRegion)
			init()
		}

class ImageButtonStyle : ObservableBase() {
	override val type = Companion

	var upState by prop(colorTransformation {})
	var overState by prop(colorTransformation { offset = Color(0.1, 0.1, 0.1, 0.0) })
	var downState by prop(colorTransformation { tint(0.9, 0.9, 0.9, 1.0); offset = Color(-0.1, -0.1, -0.1, 0.0) })

	var toggledUpState by prop(colorTransformation {})
	var toggledOverState by prop(colorTransformation { offset = Color(0.1, 0.1, 0.1, 0.0) })
	var toggledDownState by prop(colorTransformation { tint(0.9, 0.9, 0.9, 1.0); offset = Color(-0.1, -0.1, -0.1, 0.0) })

	var disabledState by prop(colorTransformation { tint(0.2, 0.2, 0.2, 0.5); grayscale(); offset = Color(-0.1, -0.1, -0.1, 0.0) })

	companion object : StyleType<ImageButtonStyle>
}

inline fun imageButtonStyle(init: ComponentInit<ImageButtonStyle> = {}): ImageButtonStyle {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return ImageButtonStyle().apply(init)
}

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
import com.acornui.cursor.StandardCursors
import com.acornui.cursor.cursor
import com.acornui.di.Owned
import com.acornui.di.own
import com.acornui.focus.Focusable
import com.acornui.gl.core.useColorTransformation
import com.acornui.graphic.Color
import com.acornui.input.interaction.MouseOrTouchState
import com.acornui.math.Bounds
import com.acornui.math.colorTransformation
import com.acornui.math.grayscale
import com.acornui.reflect.observable

/**
 * A button that tints a single element (typically a white image).
 *
 * Note: ImageButton does not currently support indeterminate states.
 */
class ImageButton(
		owner: Owned
) : SingleElementContainerImpl<UiComponent>(owner), Toggleable, Focusable {

	val style = bind(ImageButtonStyle())

	private val colorTransformation = colorTransformation()

	init {
		focusEnabled = true
		focusEnabledChildren = false
		styleTags.addAll(Companion)

		cursor(StandardCursors.HAND)

		validation.addNode(ValidationFlags.PROPERTIES, dependencies = ValidationFlags.STYLES, dependents = ValidationFlags.LAYOUT, onValidate = ::updateProperties)
	}

	var disabled: Boolean by observable(false) {
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

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val element = element ?: return
		element.setSize(explicitWidth, explicitHeight)
		out.set(element.bounds)
	}

	private val mouseOrTouchState = own(MouseOrTouchState(this)).apply {
		isOverChanged.add { invalidateProperties() }
		isDownChanged.add { invalidateProperties() }
	}

	override fun draw() {
		gl.uniforms.useColorTransformation(colorTransformation) {
			super.draw()
		}
	}

	companion object : StyleTag {
		val ICON_IMAGE = styleTag()
	}

}

fun Owned.imageButton(init: ImageButton.() -> Unit = {}): ImageButton {
	val i = ImageButton(this)
	i.init()
	return i
}

fun Owned.iconImageButton(init: ImageButton.() -> Unit = {}): ImageButton =
		imageButton {
			styleTags.add(ImageButton.ICON_IMAGE)
			init()
		}

fun Owned.iconImageButton(atlasPath: String, atlasRegion: String, init: ImageButton.() -> Unit = {}): ImageButton =
		imageButton {
			styleTags.add(ImageButton.ICON_IMAGE)
			element = atlas(atlasPath, atlasRegion)
			init()
		}

class ImageButtonStyle : StyleBase() {
	override val type = Companion

	var upState by prop(colorTransformation {})
	var overState by prop(colorTransformation { offset = Color(0.1f, 0.1f, 0.1f, 0.0f) })
	var downState by prop(colorTransformation { tint(0.9f, 0.9f, 0.9f, 1f); offset = Color(-0.1f, -0.1f, -0.1f, 0.0f) })

	var toggledUpState by prop(colorTransformation {})
	var toggledOverState by prop(colorTransformation { offset = Color(0.1f, 0.1f, 0.1f, 0.0f) })
	var toggledDownState by prop(colorTransformation { tint(0.9f, 0.9f, 0.9f, 1f); offset = Color(-0.1f, -0.1f, -0.1f, 0.0f) })

	var disabledState by prop(colorTransformation { tint(0.2f, 0.2f, 0.2f, 0.5f); grayscale(); offset = Color(-0.1f, -0.1f, -0.1f, 0.0f) })

	companion object : StyleType<ImageButtonStyle>
}

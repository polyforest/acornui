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
import com.acornui.component.layout.SizeConstraints
import com.acornui.component.style.*
import com.acornui.core.cursor.StandardCursors
import com.acornui.core.cursor.cursor
import com.acornui.core.di.Owned
import com.acornui.core.di.own
import com.acornui.core.focus.Focusable
import com.acornui.core.input.interaction.MouseOrTouchState
import com.acornui.gl.core.useColorTransformation
import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
import com.acornui.math.*
import com.acornui.reflect.observable

/**
 * A skinnable button with up, over, down, and disabled states.
 */
class ImageButton(
		owner: Owned
) : SingleElementContainerImpl<UiComponent>(owner), Focusable {

	val style = bind(ImageButtonStyle())

	private val colorTransformation = colorTransformation()

	init {
		focusEnabled = true
		focusEnabledChildren = false
		styleTags.addAll(Companion)

		cursor(StandardCursors.HAND)

		validation.addNode(ValidationFlags.PROPERTIES, dependencies = ValidationFlags.STYLES, dependents = ValidationFlags.SIZE_CONSTRAINTS, onValidate = ::updateProperties)
	}

	var disabled: Boolean by observable(false) {
		interactivityMode = if (it) InteractivityMode.NONE else InteractivityMode.ALL
		disabledTag = it
		invalidateProperties()
	}

	private fun updateProperties() {
		currentState = ButtonState.calculateButtonState(mouseOrTouchState.isOver, mouseOrTouchState.isDown, false, false, disabled)
		val colorTransform = when (currentState) {
			ButtonState.OVER -> style.overState
			ButtonState.DOWN -> style.downState
			ButtonState.DISABLED -> style.disabledState
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

	override fun updateSizeConstraints(out: SizeConstraints) {
		val element = element ?: return
		out.set(element.sizeConstraints)
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val element = element ?: return
		element.setSize(explicitWidth, explicitHeight)
		out.set(element.bounds)
	}

	private val mouseOrTouchState = own(MouseOrTouchState(this)).apply {
		isOverChanged.add { invalidateProperties() }
		isDownChanged.add { invalidateProperties() }
	}

	override fun draw(clip: MinMaxRo, transform: Matrix4Ro, tint: ColorRo) {
		glState.useColorTransformation(colorTransformation) {
			super.draw(clip, transform, tint)
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

	var disabledState by prop(colorTransformation { tint(0.2f, 0.2f, 0.2f, 0.5f); grayscale(); offset = Color(-0.1f, -0.1f, -0.1f, 0.0f) })

	companion object : StyleType<ImageButtonStyle>
}

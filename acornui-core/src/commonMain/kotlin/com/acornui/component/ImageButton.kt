/*
 * Copyright (c) 2019. Matrix Precise
 */

package com.acornui.component

import com.acornui.component.layout.SizeConstraints
import com.acornui.component.style.*
import com.acornui.core.cursor.StandardCursors
import com.acornui.core.cursor.cursor
import com.acornui.core.di.Owned
import com.acornui.core.di.own
import com.acornui.core.focus.Focusable
import com.acornui.core.input.interaction.MouseOrTouchState
import com.acornui.filter.colorTransformationFilter
import com.acornui.graphic.Color
import com.acornui.math.Bounds
import com.acornui.math.colorTransformation
import com.acornui.math.grayscale
import com.acornui.reflect.observable

/**
 * A skinnable button with up, over, down, and disabled states.
 */
class ImageButton(
		owner: Owned
) : SingleElementContainerImpl<UiComponent>(owner), Focusable {

	val style = bind(ImageButtonStyle())

	private val colorTransformationFilter = +colorTransformationFilter()

	init {
		focusEnabled = true
		focusEnabledChildren = false
		styleTags.add(Button)

		cursor(StandardCursors.HAND)

		validation.addNode(ValidationFlags.PROPERTIES, dependencies = ValidationFlags.STYLES, dependents = ValidationFlags.SIZE_CONSTRAINTS, onValidate = ::updateProperties)
	}

	var disabled: Boolean by observable(false) {
		interactivityMode = if (it) InteractivityMode.NONE else InteractivityMode.ALL
		disabledTag = it
		invalidateProperties()
	}

	private fun updateProperties() {
		currentState = ButtonState.calculateButtonState(mouseState.isOver, mouseState.isDown, false, false, disabled)
		val colorTransform = when (currentState) {
			ButtonState.OVER -> style.overState
			ButtonState.DOWN -> style.downState
			ButtonState.DISABLED -> style.disabledState
			else -> style.upState
		}
		colorTransformationFilter.colorTransformation.set(colorTransform)
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

	private val mouseState = own(MouseOrTouchState(this)).apply {
		isOverChanged.add { invalidateProperties() }
		isDownChanged.add { invalidateProperties() }
	}

	companion object : StyleTag {
		val ICON_IMAGE = styleTag()
	}

}

fun Owned.imageButton(atlasPath: String, region: String, init: ImageButton.() -> Unit = {}): ImageButton {
	val i = ImageButton(this)
	i.contentsAtlas(atlasPath, region)
	i.init()
	return i
}

fun Owned.imageButton(imagePath: String, init: ImageButton.() -> Unit = {}): ImageButton {
	val i = ImageButton(this)
	i.contentsImage(imagePath)
	i.init()
	return i
}

fun Owned.iconImageButton(atlasPath: String, region: String, init: ImageButton.() -> Unit = {}): ImageButton =
		imageButton(atlasPath, region) {
			styleTags.add(ImageButton.ICON_IMAGE)
			init()
		}

fun Owned.iconImageButton(imagePath: String, init: ImageButton.() -> Unit = {}): ImageButton = imageButton(imagePath) {
	styleTags.add(ImageButton.ICON_IMAGE)
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
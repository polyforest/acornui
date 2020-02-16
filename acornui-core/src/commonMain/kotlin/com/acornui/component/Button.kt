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

@file:Suppress("UNUSED_ANONYMOUS_PARAMETER")

package com.acornui.component

import com.acornui.component.style.StyleBase
import com.acornui.component.style.StyleTag
import com.acornui.component.style.StyleType
import com.acornui.component.style.disabledTag
import com.acornui.cursor.StandardCursors
import com.acornui.cursor.cursor
import com.acornui.di.Context
import com.acornui.di.own
import com.acornui.focus.Focusable
import com.acornui.input.interaction.MouseOrTouchState
import com.acornui.input.interaction.click
import com.acornui.math.Bounds
import com.acornui.properties.afterChange
import com.acornui.signal.Signal
import com.acornui.signal.Signal1
import com.acornui.skins.Theme
import com.acornui.skins.basicButtonSkin
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

interface ButtonRo : UiComponentRo, LabelableRo, ToggleableRo {

	val toggledChanged: Signal<(ButtonRo) -> Unit>

	val disabled: Boolean
}

interface Button : ButtonRo, UiComponent, Labelable, Toggleable {

	override var disabled: Boolean
}

/**
 * A skinnable button with up, over, down, and disabled states.
 */
open class ButtonImpl(
		owner: Context
) : ContainerImpl(owner), Button, Focusable {

	val style = bind(ButtonStyle())

	private val _toggledChanged = own(Signal1<ButtonRo>())

	/**
	 * Dispatched when the toggled flag has changed via user interaction. This will only be invoked if [toggleOnClick]
	 * is true, and the user clicks this button.
	 */
	override val toggledChanged = _toggledChanged.asRo()

	/**
	 * If true, when this button is pressed, the selected state will be toggled.
	 */
	var toggleOnClick = false

	protected var skin: ButtonSkin? = null

	init {
		focusEnabled = true
		focusEnabledChildren = false
		styleTags.add(ButtonImpl)

		click().add {
			if (toggleOnClick) {
				setUserToggled(!toggled)
			}
		}
		cursor(StandardCursors.HAND)

		watch(style) {
			skin?.dispose()
			skin = addChild(it.skin(this))
		}
		validation.addNode(ValidationFlags.PROPERTIES, dependencies = ValidationFlags.STYLES, dependents = ValidationFlags.LAYOUT, onValidate = ::updateProperties)
	}

	/**
	 * Sets the toggled value and dispatches a toggled changed signal.
	 */
	fun setUserToggled(value: Boolean) {
		toggled = value
		indeterminate = false
		_toggledChanged.dispatch(this)
	}

	override var disabled: Boolean by afterChange(false) {
		interactivityMode = if (it) InteractivityMode.NONE else InteractivityMode.ALL
		disabledTag = it
		invalidateProperties()
	}

	/**
	 * If true (and [indeterminate] is false), this button will be in the toggled state.
	 */
	override var toggled: Boolean by validationProp(false, ValidationFlags.PROPERTIES)

	/**
	 * If true, this button will be in the indeterminate state.
	 * If [toggleOnClick] is true, the next user click will set indeterminate to false and the [toggled]
	 * state will flip.
	 */
	var indeterminate: Boolean by validationProp(false, ValidationFlags.PROPERTIES)

	protected open fun updateProperties() {
		_currentState = ButtonState.calculateButtonState(mouseOrTouchState.isOver, mouseOrTouchState.isDown, toggled, indeterminate, disabled)
		skin?.buttonState = _currentState
		if (skin?.label != label)
			skin?.label = label
	}

	private var _currentState = ButtonState.UP
	val currentState: ButtonState
		get() {
			validate(ValidationFlags.PROPERTIES)
			return _currentState
		}

	/**
	 * Sets the label of this button. It is up to the skin to implement [Labelable] and use this label.
	 */
	override var label: String = ""
		set(value) {
			if (field != value) {
				field = value
				skin?.label = value
				invalidateLayout()
			}
		}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		if (skin != null) {
			skin!!.setSize(explicitWidth, explicitHeight)
			out.set(skin!!.bounds)
		}
	}

	private val mouseOrTouchState = own(MouseOrTouchState(this)).apply {
		isOverChanged.add { invalidateProperties() }
		isDownChanged.add { invalidateProperties() }
	}

	companion object : StyleTag

}

enum class ButtonState(
		val isUp: Boolean = false,
		val isOver: Boolean = false,
		val isDown: Boolean = false,
		val isToggled: Boolean = false,
		val isIndeterminate: Boolean = false
) {
	UP(isUp = true),
	OVER(isOver = true),
	DOWN(isOver = true, isDown = true),
	TOGGLED_UP(isUp = true, isToggled = true),
	TOGGLED_OVER(isOver = true, isToggled = true),
	TOGGLED_DOWN(isOver = true, isDown = true, isToggled = true),
	INDETERMINATE_UP(isUp = true, isIndeterminate = true),
	INDETERMINATE_OVER(isOver = true, isIndeterminate = true),
	INDETERMINATE_DOWN(isOver = true, isDown = true, isIndeterminate = true),
	DISABLED();

	val fallback: ButtonState?
		get() = when (this) {
			UP -> null
			OVER -> UP
			DOWN -> OVER
			TOGGLED_UP -> UP
			TOGGLED_OVER -> TOGGLED_UP
			TOGGLED_DOWN -> TOGGLED_UP
			INDETERMINATE_UP -> UP
			INDETERMINATE_OVER -> INDETERMINATE_UP
			INDETERMINATE_DOWN -> INDETERMINATE_UP
			DISABLED -> UP
		}

	companion object {
		fun calculateButtonState(isOver: Boolean = false, isDown: Boolean = false, toggled: Boolean = false, indeterminate: Boolean = false, disabled: Boolean = false): ButtonState {
			return if (disabled) {
				DISABLED
			} else {
				if (indeterminate) {
					when {
						isDown -> INDETERMINATE_DOWN
						isOver -> INDETERMINATE_OVER
						else -> INDETERMINATE_UP
					}
				} else {
					if (toggled) {
						when {
							isDown -> TOGGLED_DOWN
							isOver -> TOGGLED_OVER
							else -> TOGGLED_UP
						}
					} else {
						when {
							isDown -> DOWN
							isOver -> OVER
							else -> UP
						}
					}
				}
			}
		}
	}
}

/**
 * All button states except for [ButtonState.UP] have a fallback in case they aren't handled.
 * This method will find the first button state where [block] does not return null.
 * @return Returns the first state where [block] returned a non-null value, or null if no return value was non-null.
 */
fun <T : Any> ButtonState.fallbackWalk(block: (ButtonState) -> T?): T? {
	var curr: ButtonState? = this
	while (curr != null) {
		val result = block(curr)
		if (result != null) return result
		curr = curr.fallback
	}
	return null
}

open class ButtonStyle : StyleBase() {

	override val type: StyleType<ButtonStyle> = Companion

	var skin by prop<Context.() -> ButtonSkin> { basicButtonSkin(Theme()) }

	companion object : StyleType<ButtonStyle>
}

fun buttonStyle(skinProvider: Context.() -> ButtonSkin): ButtonStyle {
	return ButtonStyle().apply {
		skin = skinProvider
	}
}

interface LabelableRo : UiComponentRo {
	val label: String
}

/**
 * An interface for a skin part that can have a label assigned to it.
 */
interface Labelable : LabelableRo, UiComponent {
	override var label: String
}

interface ToggleableRo : UiComponentRo {

	val toggled: Boolean
}

interface Toggleable : ToggleableRo, UiComponent {
	override var toggled: Boolean
}

interface ButtonSkin : Labelable {

	var buttonState: ButtonState

}

inline fun Context.button(init: ComponentInit<ButtonImpl> = {}): ButtonImpl  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val b = ButtonImpl(this)
	b.init()
	return b
}

inline fun Context.button(label: String, init: ComponentInit<ButtonImpl> = {}): ButtonImpl  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val b = ButtonImpl(this)
	b.label = label
	b.init()
	return b
}

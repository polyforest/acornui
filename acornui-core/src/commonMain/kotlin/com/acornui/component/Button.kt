/*
 * Copyright 2015 Nicholas Bilyk
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

import com.acornui.component.layout.SizeConstraints
import com.acornui.component.style.*
import com.acornui.core.cursor.StandardCursors
import com.acornui.core.cursor.cursor
import com.acornui.core.di.Owned
import com.acornui.core.di.own
import com.acornui.core.focus.Focusable
import com.acornui.core.input.interaction.ClickInteractionRo
import com.acornui.core.input.interaction.MouseOrTouchState
import com.acornui.core.input.interaction.click
import com.acornui.factory.LazyInstance
import com.acornui.factory.disposeInstance
import com.acornui.math.Bounds
import com.acornui.reflect.observable
import com.acornui.signal.Signal1
import kotlin.collections.set


/**
 * A skinnable button with up, over, down, and disabled states.
 */
open class Button(
		owner: Owned
) : ContainerImpl(owner), Labelable, Toggleable, Focusable {

	val style = bind(ButtonStyle())

	private val _toggledChanged = own(Signal1<Button>())

	/**
	 * Dispatched when the toggled flag has changed via user interaction. This will only be invoked if [toggleOnClick]
	 * is true, and the user clicks this button.
	 */
	val toggledChanged = _toggledChanged.asRo()

	/**
	 * If true, when this button is pressed, the selected state will be toggled.
	 */
	var toggleOnClick = false

	private val _stateSkinMap = HashMap<ButtonState, LazyInstance<Owned, UiComponent?>>()

	init {
		focusEnabled = true
		focusEnabledChildren = false
		styleTags.add(Button)

		click().add {
			if (toggleOnClick) {
				setUserToggled(!toggled)
			}
		}
		cursor(StandardCursors.HAND)

		val oldInstances = ArrayList<LazyInstance<Owned, UiComponent?>>()
		watch(style) {
			oldInstances.addAll(_stateSkinMap.values)
			_stateSkinMap[ButtonState.UP] = LazyInstance(this, it.upState)
			_stateSkinMap[ButtonState.OVER] = LazyInstance(this, it.overState)
			_stateSkinMap[ButtonState.DOWN] = LazyInstance(this, it.downState)
			_stateSkinMap[ButtonState.TOGGLED_UP] = LazyInstance(this, it.toggledUpState)
			_stateSkinMap[ButtonState.TOGGLED_OVER] = LazyInstance(this, it.toggledOverState)
			_stateSkinMap[ButtonState.TOGGLED_DOWN] = LazyInstance(this, it.toggledDownState)
			_stateSkinMap[ButtonState.INDETERMINATE_UP] = LazyInstance(this, it.indeterminateUpState)
			_stateSkinMap[ButtonState.INDETERMINATE_OVER] = LazyInstance(this, it.indeterminateOverState)
			_stateSkinMap[ButtonState.INDETERMINATE_DOWN] = LazyInstance(this, it.indeterminateDownState)
			_stateSkinMap[ButtonState.DISABLED] = LazyInstance(this, it.disabledState)
			// Dispose the old state instances after we refresh state so that onCurrentStateChanged overrides have a
			// chance to transfer content children if necessary.
			for (i in 0..oldInstances.lastIndex) {
				oldInstances[i].disposeInstance()
			}
			oldInstances.clear()
		}

		validation.addNode(ValidationFlags.PROPERTIES, dependencies = ValidationFlags.STYLES, dependents = ValidationFlags.SIZE_CONSTRAINTS, onValidate = ::updateProperties)
	}

	/**
	 * Sets the toggled value and dispatches a toggled changed signal.
	 */
	fun setUserToggled(value: Boolean) {
		toggled = value
		indeterminate = false
		_toggledChanged.dispatch(this)
	}

	var disabled: Boolean by observable(false) {
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
		currentState(calculateButtonState())
	}

	protected open fun calculateButtonState(): ButtonState =
			ButtonState.calculateButtonState(mouseState.isOver, mouseState.isDown, toggled, indeterminate, disabled)

	protected var currentSkinPart: UiComponent? = null
		private set

	var currentState: ButtonState = ButtonState.UP
		get() {
			validate(ValidationFlags.PROPERTIES)
			return field
		}
		private set

	protected open fun currentState(newState: ButtonState) {
		if (isDisposed) return
		val previousState = currentState
		currentState = newState
		val newSkinPart = newState.backupWalk { state ->
			_stateSkinMap[state]?.instance
		}
		val previousSkinPart = currentSkinPart
		if (previousSkinPart == newSkinPart) return
		currentSkinPart = newSkinPart
		if (newSkinPart is Labelable) {
			newSkinPart.label = label
		}
		onCurrentStateChanged(previousState, newState, previousSkinPart, newSkinPart)
		if (newSkinPart != null) addChild(newSkinPart)
		removeChild(previousSkinPart)
	}

	protected open fun onCurrentStateChanged(previousState: ButtonState, newState: ButtonState, previousSkinPart: UiComponent?, newSkinPart: UiComponent?) {
	}

	/**
	 * Sets the label of this button. It is up to the skin to implement [Labelable] and use this label.
	 */
	override var label: String = ""
		set(value) {
			if (field != value) {
				field = value
				(currentSkinPart as? Labelable)?.label = (value)
				invalidateSize()
			}
		}

	override fun updateSizeConstraints(out: SizeConstraints) {
		if (currentSkinPart == null) return
		out.set(currentSkinPart!!.sizeConstraints)
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		if (currentSkinPart != null) {
			currentSkinPart!!.setSize(explicitWidth, explicitHeight)
			out.set(currentSkinPart!!.bounds)
		}
	}

	private val mouseState = own(MouseOrTouchState(this)).apply {
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
		val isIndeterminate: Boolean = false,
		val backup: ButtonState?
) {
	UP(isUp = true, backup = null),
	OVER(isOver = true, backup = UP),
	DOWN(isDown = true, backup = OVER),
	TOGGLED_UP(isUp = true, isToggled = true, backup = UP),
	TOGGLED_OVER(isOver = true, isToggled = true, backup = TOGGLED_UP),
	TOGGLED_DOWN(isDown = true, isToggled = true, backup = TOGGLED_UP),
	INDETERMINATE_UP(isUp = true, isIndeterminate = true, backup = UP),
	INDETERMINATE_OVER(isOver = true, isIndeterminate = true, backup = INDETERMINATE_UP),
	INDETERMINATE_DOWN(isDown = true, isIndeterminate = true, backup = INDETERMINATE_UP),
	DISABLED(backup = UP);

	companion object {
		fun calculateButtonState(isOver: Boolean = false, isDown: Boolean = false, toggled: Boolean = false, indeterminate: Boolean = false, disabled: Boolean = false): ButtonState {
			return if (disabled) {
				DISABLED
			} else {
				if (indeterminate) {
					if (isDown) {
						INDETERMINATE_DOWN
					} else if (isOver) {
						INDETERMINATE_OVER
					} else {
						INDETERMINATE_UP
					}
				} else {
					if (toggled) {
						if (isDown) {
							TOGGLED_DOWN
						} else if (isOver) {
							TOGGLED_OVER
						} else {
							TOGGLED_UP
						}
					} else {
						if (isDown) {
							DOWN
						} else if (isOver) {
							OVER
						} else {
							UP
						}
					}
				}
			}
		}
	}
}

fun <T : Any> ButtonState.backupWalk(block: (ButtonState) -> T?): T? {
	var curr: ButtonState? = this
	while (curr != null) {
		val result = block(curr)
		if (result != null) return result
		curr = curr.backup
	}
	return null
}

open class ButtonStyle : StyleBase() {

	override val type: StyleType<ButtonStyle> = ButtonStyle

	var upState by prop(noSkin)
	var overState by prop(noSkinOptional)
	var downState by prop(noSkinOptional)
	var toggledUpState by prop(noSkinOptional)
	var toggledOverState by prop(noSkinOptional)
	var toggledDownState by prop(noSkinOptional)
	var indeterminateUpState by prop(noSkinOptional)
	var indeterminateOverState by prop(noSkinOptional)
	var indeterminateDownState by prop(noSkinOptional)

	var disabledState by prop(noSkinOptional)

	companion object : StyleType<ButtonStyle>
}

fun ButtonStyle.set(skinPartFactory: (ButtonState) -> OptionalSkinPart): ButtonStyle {
	@Suppress("UNCHECKED_CAST")
	upState = skinPartFactory(ButtonState.UP) as SkinPart
	overState = skinPartFactory(ButtonState.OVER)
	downState = skinPartFactory(ButtonState.DOWN)
	toggledUpState = skinPartFactory(ButtonState.TOGGLED_UP)
	toggledOverState = skinPartFactory(ButtonState.TOGGLED_OVER)
	toggledDownState = skinPartFactory(ButtonState.TOGGLED_DOWN)
	indeterminateUpState = skinPartFactory(ButtonState.INDETERMINATE_UP)
	indeterminateOverState = skinPartFactory(ButtonState.INDETERMINATE_OVER)
	indeterminateDownState = skinPartFactory(ButtonState.INDETERMINATE_DOWN)
	disabledState = skinPartFactory(ButtonState.DISABLED)
	return this
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

fun Owned.button(init: ComponentInit<Button> = {}): Button {
	val b = Button(this)
	b.init()
	return b
}

fun Owned.button(label: String, init: ComponentInit<Button> = {}): Button {
	val b = Button(this)
	b.label = label
	b.init()
	return b
}
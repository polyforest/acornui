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
import com.acornui.core.input.*
import com.acornui.core.input.interaction.*
import com.acornui.core.userInfo
import com.acornui.factory.LazyInstance
import com.acornui.factory.disposeInstance
import com.acornui.math.Bounds
import com.acornui.reflect.observable
import com.acornui.signal.Signal
import com.acornui.signal.Signal1
import kotlin.collections.set


/**
 * A skinnable button with up, over, down, and disabled states.
 */
open class Button(
		owner: Owned
) : ElementContainerImpl<UiComponent>(owner), Labelable, Toggleable, Focusable {

	val style = bind(ButtonStyle())

	private val _toggledChanged = own(Signal1<Button>())

	/**
	 * Dispatched when the toggled flag has changed via user interaction. This will only be invoked if [toggleOnClick]
	 * is true, and the user clicks this button.
	 */
	val toggledChanged: Signal<(Button) -> Unit>
		get() = _toggledChanged

	/**
	 * If true, when this button is pressed, the selected state will be toggled.
	 */
	var toggleOnClick = false

	protected var _mouseIsOver = false
	protected var _mouseIsDown = false

	protected var _label: String = ""

	private var _currentState = ButtonState.UP
	private var _currentSkinPart: UiComponent? = null
	private val _stateSkinMap = HashMap<ButtonState, LazyInstance<Owned, UiComponent?>>()

	private val rollOverHandler = { event: MouseInteractionRo ->
		_mouseIsOver = true
		refreshState()
	}

	private val rollOutHandler = { event: MouseInteractionRo ->
		_mouseIsOver = false
		refreshState()
	}

	private val mouseDownHandler = { event: MouseInteractionRo ->
		if (!_mouseIsDown && event.button == WhichButton.LEFT) {
			_mouseIsDown = true
			stage.mouseUp().add(stageMouseUpHandler, true)
			refreshState()
		}
	}

	private val touchStartHandler = { event: TouchInteractionRo ->
		if (!_mouseIsDown) {
			_mouseIsDown = true
			stage.touchEnd().add(stageTouchEndHandler, true)
			refreshState()
		}
	}

	private val stageMouseUpHandler = { event: MouseInteractionRo ->
		if (event.button == WhichButton.LEFT) {
			_mouseIsDown = false
			refreshState()
		}
	}

	private val stageTouchEndHandler = { event: TouchInteractionRo ->
		_mouseIsDown = false
		refreshState()
	}

	private val clickHandler = { event: ClickInteractionRo ->
		if (toggleOnClick) {
			setUserToggled(!toggled)
		}
	}

	/**
	 * Sets the toggled value and dispatches a toggled changed signal.
	 */
	fun setUserToggled(value: Boolean) {
		toggled = value
		_toggledChanged.dispatch(this)

	}

	var disabled: Boolean by observable(false) {
		interactivityMode = if (it) InteractivityMode.NONE else InteractivityMode.ALL
		disabledTag = it
		refreshState()
	}

	override var toggled: Boolean by observable(false) { refreshState() }

	protected open fun refreshState() {
		currentState(calculateButtonState())
	}

	protected open fun calculateButtonState(): ButtonState {
		return if (disabled) {
			ButtonState.DISABLED
		} else {
			if (toggled) {
				if (_mouseIsDown) {
					ButtonState.TOGGLED_DOWN
				} else if (_mouseIsOver) {
					ButtonState.TOGGLED_OVER
				} else {
					ButtonState.TOGGLED_UP
				}
			} else {
				if (_mouseIsDown) {
					ButtonState.DOWN
				} else if (_mouseIsOver) {
					ButtonState.OVER
				} else {
					ButtonState.UP
				}
			}
		}
	}

	protected val currentSkinPart: UiComponent?
		get() = _currentSkinPart

	val currentState: ButtonState
		get() = _currentState

	protected open fun currentState(newState: ButtonState) {
		if (isDisposed) return
		val previousState = _currentState
		_currentState = newState
		val newSkinPart = newState.backupWalk { _stateSkinMap[newState]?.instance }
		val previousSkinPart = _currentSkinPart
		if (previousSkinPart == newSkinPart) return
		_currentSkinPart = newSkinPart
		if (newSkinPart is Labelable) {
			newSkinPart.label = _label
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
	override var label: String
		get() = _label
		set(value) {
			_label = value
			(_currentSkinPart as? Labelable)?.label = (value)
			invalidate(ValidationFlags.SIZE_CONSTRAINTS)
		}

	override fun updateSizeConstraints(out: SizeConstraints) {
		if (_currentSkinPart == null) return
		out.set(_currentSkinPart!!.sizeConstraints)
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		if (_currentSkinPart != null) {
			_currentSkinPart!!.setSize(explicitWidth, explicitHeight)
			out.set(_currentSkinPart!!.bounds)
		}
	}

	override fun dispose() {
		super.dispose()
		stage.mouseUp().remove(stageMouseUpHandler)
		stage.touchEnd().remove(stageTouchEndHandler)
	}

	init {
		focusEnabled = true
		focusEnabledChildren = false
		styleTags.add(Button)

		// Mouse over / out handlers cause problems on mobile.
		if (!userInfo.isTouchDevice) {
			rollOver().add(rollOverHandler)
			rollOut().add(rollOutHandler)
		}
		mouseDown().add(mouseDownHandler)
		touchStart().add(touchStartHandler)
		click().add(clickHandler)
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
			_stateSkinMap[ButtonState.DISABLED] = LazyInstance(this, it.disabledState)
			refreshState()
			// Dispose the old state instances after we refresh state so that onCurrentStateChanged overrides have a
			// chance to transfer content children if necessary.
			for (i in 0..oldInstances.lastIndex) {
				oldInstances[i].disposeInstance()
			}
			oldInstances.clear()
		}
	}

	companion object : StyleTag

}

enum class ButtonState(val toggled: Boolean, val backup: ButtonState?) {
	UP(false, null),
	OVER(false, UP),
	DOWN(false, OVER),
	TOGGLED_UP(true, UP),
	TOGGLED_OVER(true, TOGGLED_UP),
	TOGGLED_DOWN(true, TOGGLED_UP),
	DISABLED(false, UP);
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
	var disabledState by prop(noSkinOptional)

	companion object : StyleType<ButtonStyle>
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
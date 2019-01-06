package com.acornui.core.input.interaction

import com.acornui.component.UiComponentRo
import com.acornui.component.stage
import com.acornui.core.Disposable
import com.acornui.core.input.*
import com.acornui.core.userInfo
import com.acornui.signal.Signal
import com.acornui.signal.Signal0

/**
 * Provides information to buttons about the state of mouse and touch.
 * @param host The target to watch.
 */
class MouseOrTouchState(private val host: UiComponentRo) : Disposable {

	private val stage = host.stage

	private val _isOverChanged = Signal0()

	/**
	 * Dispatched when [isOver] has changed.
	 */
	val isOverChanged = _isOverChanged.asRo()

	private val _isDownChanged = Signal0()

	/**
	 * Dispatched when [isDown] has changed.
	 */
	val isDownChanged = _isDownChanged.asRo()

	/**
	 * True if either the touch or mouse is over the [host].
	 */
	var isOver: Boolean = false
		private set(value) {
			if (value == field) return
			field = value
			_isOverChanged.dispatch()
		}

	/**
	 * If the mouse or touch is pressed on the target, isDown becomes true, and when the mouse or touch is released
	 * (potentially outside of the target), isDown becomes false.
	 */
	var isDown: Boolean = false
		private set(value) {
			if (value == field) return
			field = value
			_isDownChanged.dispatch()
		}

	private val rollOverHandler = { event: MouseInteractionRo ->
		isOver = true
	}

	private val rollOutHandler = { event: MouseInteractionRo ->
		isOver = false
	}

	private val mouseDownHandler = { event: MouseInteractionRo ->
		if (!isDown && event.button == WhichButton.LEFT) {
			isDown = true
			stage.mouseUp().add(stageMouseUpHandler, true)
		}
	}

	private val touchStartHandler = { event: TouchInteractionRo ->
		if (!isDown) {
			isDown = true
			stage.touchEnd().add(stageTouchEndHandler, true)
		}
	}

	private val stageMouseUpHandler = { event: MouseInteractionRo ->
		if (event.button == WhichButton.LEFT) {
			isDown = false
		}
	}

	private val stageTouchEndHandler = { event: TouchInteractionRo ->
		isDown = false
	}

	override fun dispose() {
		host.rollOver().remove(rollOverHandler)
		host.rollOut().remove(rollOutHandler)
		host.mouseDown().remove(mouseDownHandler)
		host.touchStart().remove(touchStartHandler)
		stage.mouseUp().remove(stageMouseUpHandler)
		stage.touchEnd().remove(stageTouchEndHandler)
		_isOverChanged.dispose()
	}

	init {
		// Mouse over / out handlers cause problems on mobile.
		if (!userInfo.isTouchDevice) {
			host.rollOver().add(rollOverHandler)
			host.rollOut().add(rollOutHandler)
		}
		host.mouseDown().add(mouseDownHandler)
		host.touchStart().add(touchStartHandler)
	}
}
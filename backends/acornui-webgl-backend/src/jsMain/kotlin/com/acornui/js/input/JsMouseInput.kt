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

package com.acornui.js.input

import com.acornui.input.MouseInput
import com.acornui.input.WhichButton
import com.acornui.input.interaction.*
import com.acornui.js.html.TouchEvent
import com.acornui.signal.Signal0
import com.acornui.signal.Signal1
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.events.WheelEvent
import kotlin.browser.window
import kotlin.collections.HashMap
import kotlin.collections.firstOrNull
import kotlin.collections.lastIndex
import kotlin.collections.set

/**
 * @author nbilyk
 */
class JsMouseInput(private val canvas: HTMLElement) : MouseInput {

	private val _touchModeChange = Signal0()
	override val touchModeChanged = _touchModeChange.asRo()
	override var touchMode: Boolean = false
		private set(value) {
			if (value != field) {
				field = value
				_touchModeChange.dispatch()
			}
		}

	private val _touchStart = Signal1<TouchInteractionRo>()
	override val touchStart = _touchStart.asRo()
	private val _touchEnd = Signal1<TouchInteractionRo>()
	override val touchEnd = _touchEnd.asRo()
	private val _touchMove = Signal1<TouchInteractionRo>()
	override val touchMove = _touchMove.asRo()
	private val _touchCancel = Signal1<TouchInteractionRo>()
	override val touchCancel = _touchCancel.asRo()
	private val _mouseDown: Signal1<MouseInteraction> = Signal1()
	override val mouseDown = _mouseDown.asRo()
	private val _mouseUp = Signal1<MouseInteractionRo>()
	override val mouseUp = _mouseUp.asRo()
	private val _mouseMove = Signal1<MouseInteractionRo>()
	override val mouseMove = _mouseMove.asRo()
	private val _mouseWheel = Signal1<WheelInteractionRo>()
	override val mouseWheel = _mouseWheel.asRo()
	private val _overCanvasChanged = Signal1<Boolean>()
	override val overCanvasChanged = _overCanvasChanged.asRo()

	private val touchEvent = TouchInteraction()
	private val mouseEvent = MouseInteraction()
	private val wheelEvent = WheelInteraction()

	private var _overCanvas: Boolean = false
	private var _canvasX: Float = 0f
	private var _canvasY: Float = 0f

	var linePixelSize = 24f
	var pagePixelSize = 24f * 200f

	private val downMap = HashMap<WhichButton, Boolean>()

	override val canvasX: Float
		get() = _canvasX

	override val canvasY: Float
		get() = _canvasY

	override val touches: List<TouchRo>
		get() = touchEvent.touches

	private val mouseEnterHandler = { jsEvent: Event ->
		overCanvas(true)
	}

	private val mouseLeaveHandler = { jsEvent: Event ->
		overCanvas(false)
	}

	override val overCanvas: Boolean
		get() = _overCanvas

	private fun overCanvas(value: Boolean) {
		if (_overCanvas == value) return
		_overCanvas = value
		_overCanvasChanged.dispatch(value)
	}

	private val touchStartHandler = { jsEvent: Event ->
		populateTouchEvent(jsEvent as TouchEvent)
		_touchStart.dispatch(touchEvent)
		if (jsEvent.cancelable && touchEvent.defaultPrevented())
			jsEvent.preventDefault()
		else touchMode = true
	}

	private val touchMoveHandler = { jsEvent: Event ->
		populateTouchEvent(jsEvent as TouchEvent)
		_touchMove.dispatch(touchEvent)
		if (jsEvent.cancelable && touchEvent.defaultPrevented())
			jsEvent.preventDefault()
	}

	private val touchEndHandler = { jsEvent: Event ->
		populateTouchEvent(jsEvent as TouchEvent)
		_touchEnd.dispatch(touchEvent)
		if (jsEvent.cancelable && touchEvent.defaultPrevented())
			jsEvent.preventDefault()
	}

	private val touchCancelHandler = { jsEvent: Event ->
		populateTouchEvent(jsEvent as TouchEvent)
		_touchCancel.dispatch(touchEvent)

		if (jsEvent.cancelable && touchEvent.defaultPrevented())
			jsEvent.preventDefault()
	}

	private val mouseMoveHandler = { jsEvent: Event ->
		populateMouseEvent(jsEvent as MouseEvent)
		mouseEvent.button = WhichButton.UNKNOWN
		_mouseMove.dispatch(mouseEvent)
		if (jsEvent.cancelable && mouseEvent.defaultPrevented())
			jsEvent.preventDefault()
	}

	private val mouseDownHandler = { jsEvent: Event ->
		populateMouseEvent(jsEvent as MouseEvent)
		downMap[mouseEvent.button] = true
		_mouseDown.dispatch(mouseEvent)
		if (jsEvent.cancelable && mouseEvent.defaultPrevented())
			jsEvent.preventDefault()
		else touchMode = false
	}

	private val mouseUpHandler = { jsEvent: Event ->
		populateMouseEvent(jsEvent as MouseEvent)
		downMap[mouseEvent.button] = false
		_mouseUp.dispatch(mouseEvent)
		if (jsEvent.cancelable && mouseEvent.defaultPrevented())
			jsEvent.preventDefault()
	}

	private val mouseWheelHandler = { jsEvent: Event ->
		if (jsEvent is WheelEvent) {
			wheelEvent.clear()
			wheelEvent.timestamp = jsEvent.timeStamp.toLong()
			// TODO: This probably doesn't work if the root canvas is nested.
			wheelEvent.canvasX = jsEvent.pageX.toFloat() - canvas.offsetLeft.toFloat()
			wheelEvent.canvasY = jsEvent.pageY.toFloat() - canvas.offsetTop.toFloat()
			wheelEvent.button = getWhichButton(jsEvent.button.toInt())
			_canvasX = wheelEvent.canvasX
			_canvasY = wheelEvent.canvasY

			val m = if (jsEvent.deltaMode == WheelEvent.DOM_DELTA_PAGE) pagePixelSize else if (jsEvent.deltaMode == WheelEvent.DOM_DELTA_LINE) linePixelSize else 1f
			wheelEvent.deltaX = m * jsEvent.deltaX.toFloat()
			wheelEvent.deltaY = m * jsEvent.deltaY.toFloat()
			wheelEvent.deltaZ = m * jsEvent.deltaZ.toFloat()
			_mouseWheel.dispatch(wheelEvent)
		}
	}

	init {
		// Touch
		window.addEventListener("touchstart", touchStartHandler, true)
		window.addEventListener("touchend", touchEndHandler, true)
		window.addEventListener("touchmove", touchMoveHandler, true)
		window.addEventListener("touchcancel", touchCancelHandler, true)
		canvas.addEventListener("touchleave", mouseLeaveHandler, true)
		window.addEventListener("touchleave", mouseLeaveHandler, true)

		// Mouse
		canvas.addEventListener("mouseenter", mouseEnterHandler, true)
		window.addEventListener("mouseleave", mouseLeaveHandler, true)
		canvas.addEventListener("mouseleave", mouseLeaveHandler, true)
		window.addEventListener("mousemove", mouseMoveHandler, true)
		window.addEventListener("mousedown", mouseDownHandler, true)
		window.addEventListener("mouseup", mouseUpHandler, true)
		canvas.addEventListener("wheel", mouseWheelHandler, true)
	}

	private fun populateMouseEvent(jsEvent: MouseEvent) {
		mouseEvent.clear()
		mouseEvent.timestamp = jsEvent.timeStamp.toLong()
		mouseEvent.canvasX = jsEvent.clientX.toFloat() - canvas.offsetLeft.toFloat()
		mouseEvent.canvasY = jsEvent.clientY.toFloat() - canvas.offsetTop.toFloat()
		mouseEvent.button = getWhichButton(jsEvent.button.toInt())
		_canvasX = mouseEvent.canvasX
		_canvasY = mouseEvent.canvasY
	}

	private fun populateTouchEvent(jsEvent: TouchEvent) {
		touchEvent.clear()
		touchEvent.set(jsEvent)
		val firstTouch = touchEvent.touches.firstOrNull() ?: return
		_canvasX = firstTouch.canvasX
		_canvasY = firstTouch.canvasY
	}

	private fun TouchInteraction.set(jsEvent: TouchEvent) {
		timestamp = jsEvent.timeStamp.toLong()
		clearTouches()
		for (i in 0..jsEvent.changedTouches.lastIndex) {
			val changedTouch = jsEvent.changedTouches[i]
			val t = Touch.obtain()
			t.set(changedTouch)
			changedTouches.add(t)
		}
		for (i in 0..jsEvent.touches.lastIndex) {
			val touch = jsEvent.touches[i]
			val t = Touch.obtain()
			t.set(touch)
			touches.add(t)
		}
	}

	private fun Touch.set(jsTouch: com.acornui.js.html.Touch) {
		canvasX = jsTouch.clientX.toFloat() - canvas.offsetLeft.toFloat()
		canvasY = jsTouch.clientY.toFloat() - canvas.offsetTop.toFloat()
		identifier = jsTouch.identifier
	}

	override fun dispose() {
		_touchStart.dispose()
		_touchEnd.dispose()
		_touchMove.dispose()
		_touchCancel.dispose()

		_mouseDown.dispose()
		_mouseUp.dispose()
		_mouseMove.dispose()
		_mouseWheel.dispose()
		_overCanvasChanged.dispose()

		window.removeEventListener("touchstart", touchStartHandler, true)
		window.removeEventListener("touchend", touchEndHandler, true)
		window.removeEventListener("touchmove", touchMoveHandler, true)
		canvas.removeEventListener("touchleave", mouseLeaveHandler, true)
		window.removeEventListener("touchleave", mouseLeaveHandler, true)

		canvas.removeEventListener("mouseenter", mouseEnterHandler, true)
		canvas.removeEventListener("mouseleave", mouseLeaveHandler, true)
		window.removeEventListener("mousemove", mouseMoveHandler, true)
		window.removeEventListener("mousedown", mouseDownHandler, true)
		window.removeEventListener("mouseup", mouseUpHandler, true)
		canvas.removeEventListener("wheel", mouseWheelHandler, true)
	}

	override fun mouseIsDown(button: WhichButton): Boolean {
		return downMap[button] == true
	}

	companion object {
		fun getWhichButton(i: Int): WhichButton {
			return when (i) {
				-1 -> WhichButton.UNKNOWN
				0 -> WhichButton.LEFT
				1 -> WhichButton.MIDDLE
				2 -> WhichButton.RIGHT
				3 -> WhichButton.BACK
				4 -> WhichButton.FORWARD
				else -> WhichButton.UNKNOWN
			}
		}
	}
}

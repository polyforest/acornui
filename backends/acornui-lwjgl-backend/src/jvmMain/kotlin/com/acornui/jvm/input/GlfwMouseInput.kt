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

package com.acornui.jvm.input

import com.acornui.core.input.MouseInput
import com.acornui.core.input.WhichButton
import com.acornui.core.input.interaction.*
import com.acornui.core.time.time
import com.acornui.signal.Signal1
import org.lwjgl.glfw.*

/**
 * @author nbilyk
 */
class GlfwMouseInput(private val window: Long) : MouseInput {

	// TODO: Touch input for lwjgl?
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

	private val mouseEvent = MouseInteraction()
	private val wheelEvent = WheelInteraction()
	private var _canvasX: Float = 0f
	private var _canvasY: Float = 0f
	private var _overCanvas: Boolean = false

	private val downMap = HashMap<WhichButton, Boolean>()

	val scrollSpeed = 24f

	override val canvasX: Float
		get() {
			return _canvasX
		}

	override val canvasY: Float
		get() {
			return _canvasY
		}

	override val overCanvas: Boolean
		get() {
			return _overCanvas
		}

	private val mouseButtonCallback: GLFWMouseButtonCallback = object : GLFWMouseButtonCallback() {
		override fun invoke(window: Long, button: Int, action: Int, mods: Int) {
			mouseEvent.clear()
			mouseEvent.canvasX = _canvasX
			mouseEvent.canvasY = _canvasY
			mouseEvent.button = getWhichButton(button)
			mouseEvent.timestamp = time.nowMs()
			if (mouseEvent.button != WhichButton.UNKNOWN) {
				when (action) {
					GLFW.GLFW_PRESS -> {
						downMap[mouseEvent.button] = true
						_mouseDown.dispatch(mouseEvent)
					}
					GLFW.GLFW_RELEASE -> {
						downMap[mouseEvent.button] = false
						_mouseUp.dispatch(mouseEvent)
					}
				}
			}
		}
	}

	private val cursorPosCallback: GLFWCursorPosCallback = object : GLFWCursorPosCallback() {
		override fun invoke(window: Long, xpos: Double, ypos: Double) {
			if (mouseMove.isDispatching) return
			_canvasX = xpos.toFloat()
			_canvasY = ypos.toFloat()

			mouseEvent.clear()
			mouseEvent.canvasX = _canvasX
			mouseEvent.canvasY = _canvasY
			mouseEvent.button = WhichButton.UNKNOWN
			mouseEvent.timestamp = time.nowMs()
			_mouseMove.dispatch(mouseEvent)
		}
	}

	private val cursorEnterCallback: GLFWCursorEnterCallback = object : GLFWCursorEnterCallback() {
		override fun invoke(window: Long, entered: Boolean) {
			if (overCanvasChanged.isDispatching) return
			_overCanvas = entered
			_overCanvasChanged.dispatch(_overCanvas)
		}
	}

	private val mouseWheelCallback: GLFWScrollCallback = object : GLFWScrollCallback() {
		override fun invoke(window: Long, xoffset: Double, yoffset: Double) {
			if (mouseWheel.isDispatching) return
			wheelEvent.clear()
			wheelEvent.canvasX = _canvasX
			wheelEvent.canvasY = _canvasY
			wheelEvent.button = WhichButton.UNKNOWN
			wheelEvent.timestamp = time.nowMs()
			wheelEvent.deltaX = scrollSpeed * -xoffset.toFloat()
			wheelEvent.deltaY = scrollSpeed * -yoffset.toFloat()
			_mouseWheel.dispatch(wheelEvent)
		}
	}

	init {
		GLFW.glfwSetMouseButtonCallback(window, mouseButtonCallback)
		GLFW.glfwSetCursorPosCallback(window, cursorPosCallback)
		GLFW.glfwSetCursorEnterCallback(window, cursorEnterCallback)
		GLFW.glfwSetScrollCallback(window, mouseWheelCallback)
	}

	override fun mouseIsDown(button: WhichButton): Boolean {
		return downMap[button] == true
	}

	override fun dispose() {
		GLFW.glfwSetMouseButtonCallback(window, null)
		GLFW.glfwSetCursorPosCallback(window, null)
		GLFW.glfwSetCursorEnterCallback(window, null)
		GLFW.glfwSetScrollCallback(window, null)

		_touchStart.dispose()
		_touchEnd.dispose()
		_touchMove.dispose()
		_touchCancel.dispose()

		_mouseDown.dispose()
		_mouseUp.dispose()
		_mouseMove.dispose()
		_mouseWheel.dispose()
		_overCanvasChanged.dispose()
	}

	companion object {
		fun getWhichButton(button: Int): WhichButton {
			when (button) {
				0 -> return WhichButton.LEFT
				1 -> return WhichButton.RIGHT
				2 -> return WhichButton.MIDDLE
				3 -> return WhichButton.BACK
				4 -> return WhichButton.FORWARD
				else -> return WhichButton.UNKNOWN
			}
		}
	}
}

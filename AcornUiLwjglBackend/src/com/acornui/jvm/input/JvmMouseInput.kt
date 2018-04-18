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

package com.acornui.jvm.input

import com.acornui.core.input.MouseInput
import com.acornui.core.input.WhichButton
import com.acornui.core.input.interaction.MouseInteraction
import com.acornui.core.input.interaction.TouchInteraction
import com.acornui.core.input.interaction.WheelInteraction
import com.acornui.core.time.time
import com.acornui.signal.Signal1
import org.lwjgl.glfw.*

/**
 * @author nbilyk
 */
class JvmMouseInput(private val window: Long) : MouseInput {

	// TODO: Touch input for lwjgl?
	override val touchStart: Signal1<TouchInteraction> = Signal1()
	override val touchEnd: Signal1<TouchInteraction> = Signal1()
	override val touchMove: Signal1<TouchInteraction> = Signal1()
	override val touchCancel: Signal1<TouchInteraction> = Signal1()

	override val mouseDown: Signal1<MouseInteraction> = Signal1()
	override val mouseUp: Signal1<MouseInteraction> = Signal1()
	override val mouseMove: Signal1<MouseInteraction> = Signal1()
	override val mouseWheel: Signal1<WheelInteraction> = Signal1()
	override val overCanvasChanged: Signal1<Boolean> = Signal1()

	private val mouseEvent = MouseInteraction()
	private val wheelEvent = WheelInteraction()
	private var _canvasX: Float = 0f
	private var _canvasY: Float = 0f
	private var _overCanvas: Boolean = false

	private val downMap = HashMap<WhichButton, Boolean>()

	val scrollSpeed = 24f

	override fun canvasX(): Float {
		return _canvasX
	}

	override fun canvasY(): Float {
		return _canvasY
	}

	override fun overCanvas(): Boolean {
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
						mouseDown.dispatch(mouseEvent)
					}
					GLFW.GLFW_RELEASE -> {
						downMap[mouseEvent.button] = false
						mouseUp.dispatch(mouseEvent)
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
			mouseMove.dispatch(mouseEvent)
		}
	}

	private val cursorEnterCallback: GLFWCursorEnterCallback = object : GLFWCursorEnterCallback() {
		override fun invoke(window: Long, entered: Boolean) {
			if (overCanvasChanged.isDispatching) return
			_overCanvas = entered
			overCanvasChanged.dispatch(_overCanvas)
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
			mouseWheel.dispatch(wheelEvent)
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

		touchStart.dispose()
		touchEnd.dispose()
		touchMove.dispose()
		touchCancel.dispose()

		mouseDown.dispose()
		mouseUp.dispose()
		mouseMove.dispose()
		mouseWheel.dispose()
		overCanvasChanged.dispose()
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
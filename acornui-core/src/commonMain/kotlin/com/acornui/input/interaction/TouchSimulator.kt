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

package com.acornui.input.interaction

import com.acornui.component.InteractivityMode
import com.acornui.component.Stage
import com.acornui.component.atlas
import com.acornui.component.layout.moveTo
import com.acornui.Disposable
import com.acornui.component.mousePosition
import com.acornui.di.Injector
import com.acornui.di.Scoped
import com.acornui.di.inject
import com.acornui.input.*
import com.acornui.graphic.Color
import com.acornui.math.Vector2
import com.acornui.reflect.afterChange

class TouchSimulator(override val injector: Injector) : Scoped, Disposable {

	private val stage = inject(Stage)
	private val handle = stage.atlas("assets/uiskin/uiskin.json", "Picker") {
		setOrigin(5f, 5f)
		includeInLayout = false
		interactivityMode = InteractivityMode.NONE
		colorTint = Color.GREEN
	}

	private val startPosition = Vector2()
	private val position = Vector2()

	private var enterFrameRef: Disposable? = null
	private val fakeTouchEvent = TouchInteraction()
	private val interactivity by InteractivityManager
	private val mouseState by MouseState
	private val keyState by KeyState
	private var mouseIsDown = false

	init {
		stage.keyDown().add(::keyDownHandler)
		stage.keyUp().add(::keyUpHandler)
	}

	private fun keyDownHandler(event: KeyInteractionRo) {
		if (!event.isRepeat && event.keyCode == Ascii.ALT && !event.handled) {
			event.handled = true
			isSimulating = true
		}
	}

	private fun keyUpHandler(event: KeyInteractionRo) {
		if (event.keyCode == Ascii.ALT) {
			event.handled = true
			isSimulating = false
		}
	}

	private var isSimulating: Boolean by afterChange(false) { new ->
		if (new) {
			startPosition.set(mouseState.mouseX, mouseState.mouseY)

			handle.moveTo(stage.mousePosition(startPosition))
			stage.addElement(handle)

			fakeTouchEvent.clear()
			fakeTouchEvent.type = TouchInteractionRo.TOUCH_START
			populateTouches()
			interactivity.dispatch(startPosition.x, startPosition.y, fakeTouchEvent)

			stage.mouseDown(true).add(::mouseDownHandler)
			stage.mouseUp(true).add(::mouseUpHandler)
			stage.mouseMove(true).add(::mouseMoveHandler)
		} else {
			// Remove any currently active touches.
			position.set(mouseState.mouseX, mouseState.mouseY)
			fakeTouchEvent.clear()
			fakeTouchEvent.type = TouchInteractionRo.TOUCH_END
			populateTouches()
			interactivity.dispatch(position.x, position.y, fakeTouchEvent)

			stage.removeElement(handle)
			enterFrameRef?.dispose()
			enterFrameRef = null

			stage.mouseDown(true).remove(::mouseDownHandler)
			stage.mouseUp(true).remove(::mouseUpHandler)
			stage.mouseMove(true).remove(::mouseMoveHandler)
		}
	}

	private fun populateTouches() {
		if (isSimulating) {
			fakeTouchEvent.touches.add(Touch.obtain().apply {
				canvasX = startPosition.x
				canvasY = startPosition.y
				identifier = 1
			})
		}
		if (mouseIsDown) {
			fakeTouchEvent.touches.add(Touch.obtain().apply {
				canvasX = position.x
				canvasY = position.y
				identifier = 2
			})
		}
		fakeTouchEvent.changedTouches.add(Touch.obtain().apply {
			canvasX = startPosition.x
			canvasY = startPosition.y
			identifier = 1
		})
		fakeTouchEvent.changedTouches.add(Touch.obtain().apply {
			canvasX = position.x
			canvasY = position.y
			identifier = 2
		})
	}

	private fun mouseDownHandler(event: MouseInteractionRo) {
		if (event.button != WhichButton.LEFT) return
		event.preventDefault()
		event.propagation.stopImmediatePropagation()
		mouseIsDown = true
		position.set(event.canvasX, event.canvasY)

		fakeTouchEvent.clear()
		fakeTouchEvent.type = TouchInteractionRo.TOUCH_START
		populateTouches()
		interactivity.dispatch(position.x, position.y, fakeTouchEvent)
	}

	private fun mouseMoveHandler(event: MouseInteractionRo) {
		event.preventDefault()
		event.propagation.stopImmediatePropagation()
		position.set(event.canvasX, event.canvasY)
		if (!keyState.keyIsDown(Ascii.CONTROL))
			startPosition.set(position)

		fakeTouchEvent.clear()
		fakeTouchEvent.type = TouchInteractionRo.TOUCH_MOVE
		populateTouches()
		interactivity.dispatch(position.x, position.y, fakeTouchEvent)
	}

	private fun mouseUpHandler(event: MouseInteractionRo) {
		if (event.button != WhichButton.LEFT) return
		event.preventDefault()
		event.propagation.stopImmediatePropagation()
		mouseIsDown = false

		position.set(event.canvasX, event.canvasY)
		if (!keyState.keyIsDown(Ascii.CONTROL))
			startPosition.set(position)

		fakeTouchEvent.clear()
		fakeTouchEvent.type = TouchInteractionRo.TOUCH_END
		populateTouches()
		interactivity.dispatch(position.x, position.y, fakeTouchEvent)
	}

	override fun dispose() {
		isSimulating = false
		handle.dispose()
		stage.keyDown().remove(::keyDownHandler)
		stage.keyUp().remove(::keyUpHandler)
	}
}

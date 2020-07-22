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

import com.acornui.Disposable
import com.acornui.component.*
import com.acornui.component.layout.moveTo
import com.acornui.di.Context
import com.acornui.di.ContextImpl
import com.acornui.graphic.Color
import com.acornui.input.*
import com.acornui.math.vec2
import com.acornui.properties.afterChange

class TouchSimulator(owner: Context) : ContextImpl(owner), Disposable {

	private val stage = inject(Stage)
	private val handle = stage.atlas("assets/uiskin/uiskin_{0}x.json".toDpis(1.0, 2.0), "Picker") {
		setOrigin(5.0, 5.0)
		includeInLayout = false
		interactivityMode = InteractivityMode.NONE
		colorTint = Color.GREEN
	}

	private val startPosition = vec2()
	private val position = vec2()

	private var enterFrameRef: Disposable? = null
	private val fakeTouchEvent = TouchEvent()
	private val interactivity by InteractivityManager
	private val mouseState by MouseState
	private val keyState by KeyState
	private var mouseIsDown = false

	init {
		stage.keyDown().add(::keyDownHandler)
		stage.keyUp().add(::keyUpHandler)
	}

	private fun keyDownHandler(event: KeyEventRo) {
		if (!event.isRepeat && event.keyCode == Ascii.ALT && !event.handled) {
			event.handled = true
			isSimulating = true
		}
	}

	private fun keyUpHandler(event: KeyEventRo) {
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
			fakeTouchEvent.type = TouchEventRo.TOUCH_START
			populateTouches()
			interactivity.dispatch(startPosition.x, startPosition.y, fakeTouchEvent)

			stage.mouseDown(true).add(::mouseDownHandler)
			stage.mouseUp(true).add(::mouseUpHandler)
			stage.mouseMove(true).add(::mouseMoveHandler)
		} else {
			// Remove any currently active touches.
			position.set(mouseState.mouseX, mouseState.mouseY)
			fakeTouchEvent.clear()
			fakeTouchEvent.type = TouchEventRo.TOUCH_END
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

	private fun mouseDownHandler(event: MouseEventRo) {
		if (event.button != WhichButton.LEFT) return
		event.preventDefault()
		event.propagation.stopImmediatePropagation()
		mouseIsDown = true
		position.set(event.canvasX, event.canvasY)

		fakeTouchEvent.clear()
		fakeTouchEvent.type = TouchEventRo.TOUCH_START
		populateTouches()
		interactivity.dispatch(position.x, position.y, fakeTouchEvent)
	}

	private fun mouseMoveHandler(event: MouseEventRo) {
		event.preventDefault()
		event.propagation.stopImmediatePropagation()
		position.set(event.canvasX, event.canvasY)
		if (!keyState.keyIsDown(Ascii.CONTROL))
			startPosition.set(position)

		fakeTouchEvent.clear()
		fakeTouchEvent.type = TouchEventRo.TOUCH_MOVE
		populateTouches()
		interactivity.dispatch(position.x, position.y, fakeTouchEvent)
	}

	private fun mouseUpHandler(event: MouseEventRo) {
		if (event.button != WhichButton.LEFT) return
		event.preventDefault()
		event.propagation.stopImmediatePropagation()
		mouseIsDown = false

		position.set(event.canvasX, event.canvasY)
		if (!keyState.keyIsDown(Ascii.CONTROL))
			startPosition.set(position)

		fakeTouchEvent.clear()
		fakeTouchEvent.type = TouchEventRo.TOUCH_END
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

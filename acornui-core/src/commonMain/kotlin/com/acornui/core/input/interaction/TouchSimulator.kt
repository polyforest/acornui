/*
 * Copyright 2018 Nicholas Bilyk
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

package com.acornui.core.input.interaction

import com.acornui.component.InteractivityMode
import com.acornui.component.Stage
import com.acornui.component.atlas
import com.acornui.component.layout.moveTo
import com.acornui.core.Disposable
import com.acornui.core.di.Injector
import com.acornui.core.di.Scoped
import com.acornui.core.di.inject
import com.acornui.core.input.*
import com.acornui.graphic.Color
import com.acornui.math.Vector2
import kotlin.properties.Delegates

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
	private val interactivity = inject(InteractivityManager)
	private val mouseState = inject(MouseState)
	private var mouseIsDown = false

	init {
		stage.keyDown().add(::keyDownHandler)
	}

	private fun keyDownHandler(event: KeyInteractionRo) {
		if (!event.isRepeat && event.ctrlKey && event.metaKey && !event.handled) {
			event.handled = true
			isSimulating = true
		}
	}

	private fun keyUpHandler(event: KeyInteractionRo) {
		if (!event.ctrlKey || !event.metaKey) {
			event.handled = true
			isSimulating = false
		}
	}

	private var isSimulating: Boolean by Delegates.observable(false) { _, old, new ->
		if (old != new && new) {
			startPosition.set(mouseState.canvasX, mouseState.canvasY)

			handle.moveTo(stage.mousePosition(startPosition))
			stage.addElement(handle)
			stage.keyUp().add(::keyUpHandler)
			stage.mouseDown(true).add(::mouseDownHandler)
			stage.mouseUp(true).add(::mouseUpHandler)

			fakeTouchEvent.clear()
			fakeTouchEvent.type = TouchInteractionRo.TOUCH_START
			populateTouches()
			interactivity.dispatch(startPosition.x, startPosition.y, fakeTouchEvent)

		} else {
			// Remove any currently active touches.
			fakeTouchEvent.clear()
			fakeTouchEvent.type = TouchInteractionRo.TOUCH_END
			populateTouches()
			interactivity.dispatch(position.x, position.y, fakeTouchEvent)

			stage.removeElement(handle)
			enterFrameRef?.dispose()
			enterFrameRef = null

			stage.keyUp().remove(::keyDownHandler)
			stage.mouseDown(true).remove(::mouseDownHandler)
		}
	}

	private var wasSimulating = false
	private var mouseWasDown = false

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
		wasSimulating = isSimulating
		mouseWasDown = mouseIsDown
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
		stage.mouseMove(true).add(::mouseMoveHandler)
		stage.mouseUp(true).add(::mouseUpHandler)
	}

	private fun mouseMoveHandler(event: MouseInteractionRo) {
		event.preventDefault()
		event.propagation.stopImmediatePropagation()
		position.set(event.canvasX, event.canvasY)

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

		fakeTouchEvent.clear()
		fakeTouchEvent.type = TouchInteractionRo.TOUCH_END
		populateTouches()
		interactivity.dispatch(position.x, position.y, fakeTouchEvent)
		stage.mouseMove(true).remove(::mouseMoveHandler)
		stage.mouseUp(true).remove(::mouseUpHandler)
	}

	override fun dispose() {
		isSimulating = false
		stage.keyDown().remove(::keyDownHandler)
		handle.dispose()
	}
}
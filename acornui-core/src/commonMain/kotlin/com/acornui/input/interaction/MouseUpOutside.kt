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

import com.acornui.component.UiComponentRo
import com.acornui.component.createOrReuse
import com.acornui.component.stage
import com.acornui.input.EventType
import com.acornui.input.WhichButton
import com.acornui.input.mouseDown
import com.acornui.input.mouseUp
import com.acornui.signal.StoppableSignal
import com.acornui.signal.StoppableSignalImpl

/**
 * An interaction where the user touches down on a target, then releases outside of that target. (Including outside
 * the canvas)
 */
class MouseUpOutside(private val target: UiComponentRo) : StoppableSignalImpl<MouseEvent>() {

	private val stage = target.stage

	private var downButtons = BooleanArray(WhichButton.values().size)
	private var downCount = 0

	private val mouseDownHandler = {
		event: MouseEventRo ->
		if (downCount == 0) {
			// When the target interactive element has been given a touch down, listen for the next touch up,
			// no matter where it is.
			stage.mouseUp().add(stageMouseUpHandler)
		}
		if (!downButtons[event.button.ordinal]) {
			downButtons[event.button.ordinal] = true
			downCount++
		}

	}

	// TODO: Workaround for https://youtrack.jetbrains.com/issue/KT-10350
	private val stageMouseUpHandler = {
		event: MouseEventRo ->
		_rawTouchUpHandler(event)
	}

	private fun _rawTouchUpHandler(event: MouseEventRo) {
		if (downButtons[event.button.ordinal]) {
			downButtons[event.button.ordinal] = false
			downCount--
			if (downCount == 0) {
				stage.mouseUp().remove(stageMouseUpHandler)
			}
			if (isNotEmpty()) {
				if (!target.containsCanvasPoint(event.canvasX, event.canvasY)) {
					mouseUpOutsideEvent.set(event)
					dispatch(mouseUpOutsideEvent)
				}
			}
		}
	}

	init {
		target.mouseDown().add(mouseDownHandler)
	}

	override fun dispose() {
		super.dispose()
		target.mouseDown().remove(mouseDownHandler)
		if (downCount > 0) {
			stage.mouseUp().remove(stageMouseUpHandler)
		}
	}

	companion object {
		val MOUSE_UP_OUTSIDE = EventType<MouseEvent>("mouseUpOutside")
		private val mouseUpOutsideEvent: MouseEvent = MouseEvent()

		init {
			mouseUpOutsideEvent.type = MOUSE_UP_OUTSIDE
		}
	}
}

/**
 * Dispatched when a touch down event has happened on this interactive element, and the next touch up event is not on
 * on a different element. This does not have capture or bubble phases; a touchUpOutside event for a child does
 * not necessarily mean a touchUpOutside event for its parent.
 */
fun UiComponentRo.mouseUpOutside(): StoppableSignal<MouseEvent> {
	return createOrReuse(MouseUpOutside.MOUSE_UP_OUTSIDE, false)
}

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

package com.acornui.core.input.interaction

import com.acornui.component.UiComponentRo
import com.acornui.component.createOrReuseAttachment
import com.acornui.component.isDescendantOf
import com.acornui.core.Disposable
import com.acornui.core.input.mouseOut
import com.acornui.core.input.mouseOver
import com.acornui.signal.StoppableSignal
import com.acornui.signal.StoppableSignalImpl

/**
 * A class that dispatches an event when the roll over status on a target object has changed.
 * A roll over is different from a mouse over in that a mouse over is dispatched on each child of a container,
 * where a roll over will only dispatch for the specific target.
 * @author nbilyk
 */
private class MouseOverChanged(
		private val interactiveElement: UiComponentRo,
		private val isCapture: Boolean) : Disposable {

	val over = StoppableSignalImpl<MouseInteractionRo>()
	val out = StoppableSignalImpl<MouseInteractionRo>()

	private val mouseOverHandler = {
		event: MouseInteractionRo ->
		if (filter(event))
			over.dispatch(event)
	}

	private val mouseOutHandler = {
		event: MouseInteractionRo ->
		if (filter(event))
			out.dispatch(event)
	}

	private fun filter(event: MouseInteractionRo): Boolean {
		return event.relatedTarget?.isDescendantOf(interactiveElement) != true
	}

	init {
		interactiveElement.mouseOver(isCapture).add(mouseOverHandler)
		interactiveElement.mouseOut(isCapture).add(mouseOutHandler)
	}

	override fun dispose() {
		interactiveElement.mouseOver(isCapture).remove(mouseOverHandler)
		interactiveElement.mouseOut(isCapture).remove(mouseOutHandler)
	}
}

/**
 * An interaction signal dispatched when this element has had the mouse move over the element, but unlike touchOver,
 * this will not bubble, and therefore will not be fired if a child element has had a touchOver event.
 */
fun UiComponentRo.rollOver(isCapture: Boolean = false): StoppableSignal<MouseInteractionRo> {
	return createOrReuseAttachment("MouseOverChanged_" + isCapture, {
		MouseOverChanged(this, isCapture = isCapture)
	}).over
}

fun UiComponentRo.rollOut(isCapture: Boolean = false): StoppableSignal<MouseInteractionRo> {
	return createOrReuseAttachment("MouseOverChanged_" + isCapture, {
		MouseOverChanged(this, isCapture = isCapture)
	}).out
}
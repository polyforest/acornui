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
private class MouseOverChangedAttachment(
		private val target: UiComponentRo,
		private val isCapture: Boolean) : Disposable {

	private val _over = StoppableSignalImpl<MouseInteractionRo>()
	val over = _over.asRo()

	private val _out = StoppableSignalImpl<MouseInteractionRo>()
	val out = _out.asRo()

	private var isOver = false

	init {
		target.mouseOver(isCapture).add(::mouseOverHandler)
		target.mouseOut(isCapture).add(::mouseOutHandler)
	}

	private fun mouseOverHandler(event: MouseInteractionRo) {
		if (!isOver) {
			isOver = true
			_over.dispatch(event)
		}
	}

	private fun mouseOutHandler(event: MouseInteractionRo) {
		if (isOver && event.relatedTarget?.isDescendantOf(target) != true) {
			isOver = false
			_out.dispatch(event)
		}
	}

	override fun dispose() {
		target.mouseOver(isCapture).remove(::mouseOverHandler)
		target.mouseOut(isCapture).remove(::mouseOutHandler)
		_over.dispose()
		_out.dispose()
	}
}

/**
 * An interaction signal dispatched when this element has had the mouse move over the element, but unlike touchOver,
 * this will not bubble, and therefore will not be fired if a child element has had a touchOver event.
 */
fun UiComponentRo.rollOver(isCapture: Boolean = false): StoppableSignal<MouseInteractionRo> {
	return createOrReuseAttachment("MouseOverChanged_$isCapture") {
		MouseOverChangedAttachment(this, isCapture = isCapture)
	}.over
}

fun UiComponentRo.rollOut(isCapture: Boolean = false): StoppableSignal<MouseInteractionRo> {
	return createOrReuseAttachment("MouseOverChanged_$isCapture") {
		MouseOverChangedAttachment(this, isCapture = isCapture)
	}.out
}

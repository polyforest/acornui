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
import com.acornui.component.createOrReuse
import com.acornui.core.di.inject
import com.acornui.core.input.InteractionType
import com.acornui.core.input.InteractivityManager
import com.acornui.core.input.WhichButton
import com.acornui.core.time.callLater
import com.acornui.core.time.time
import com.acornui.signal.StoppableSignal

interface ClickInteractionRo : MouseInteractionRo {

	val count: Int

	companion object {
		val LEFT_CLICK = InteractionType<ClickInteractionRo>("leftClick")
		val RIGHT_CLICK = InteractionType<ClickInteractionRo>("rightClick")
		val BACK_CLICK = InteractionType<ClickInteractionRo>("backClick")
		val FORWARD_CLICK = InteractionType<ClickInteractionRo>("forwardClick")
		val MIDDLE_CLICK = InteractionType<ClickInteractionRo>("middleClick")
	}
}

/**
 * @author nbilyk
 */
open class ClickInteraction : ClickInteractionRo, MouseInteraction() {

	/**
	 * In a standard click event, this is always 1. When used in a multi click event, count is the number of
	 * consecutive clicks, each click within [ClickDispatcher.multiClickSpeed] milliseconds of the next.
	 */
	override var count: Int = 0

	override fun clear() {
		super.clear()
		count = 0
	}
}

/**
 * A click interaction is where there is a touch down event, then a touch up event on that same target.
 */
fun UiComponentRo.click(isCapture: Boolean = false): StoppableSignal<ClickInteractionRo> {
	return createOrReuse(ClickInteractionRo.LEFT_CLICK, isCapture)
}

fun UiComponentRo.rightClick(isCapture: Boolean = false): StoppableSignal<ClickInteractionRo> {
	return createOrReuse(ClickInteractionRo.RIGHT_CLICK, isCapture)
}

fun UiComponentRo.middleClick(isCapture: Boolean = false): StoppableSignal<ClickInteractionRo> {
	return createOrReuse(ClickInteractionRo.MIDDLE_CLICK, isCapture)
}

fun UiComponentRo.backClick(isCapture: Boolean = false): StoppableSignal<ClickInteractionRo> {
	return createOrReuse(ClickInteractionRo.BACK_CLICK, isCapture)
}

fun UiComponentRo.forwardClick(isCapture: Boolean = false): StoppableSignal<ClickInteractionRo> {
	return createOrReuse(ClickInteractionRo.FORWARD_CLICK, isCapture)
}

private val fakeClickEvent = ClickInteraction()

fun UiComponentRo.dispatchClick() {
	fakeClickEvent.clear()
	fakeClickEvent.type = ClickInteractionRo.LEFT_CLICK
	fakeClickEvent.target = this
	fakeClickEvent.button = WhichButton.LEFT
	fakeClickEvent.timestamp = time.nowMs()
	fakeClickEvent.count = 1
	inject(InteractivityManager).dispatch(this, fakeClickEvent)
}

private val clickHandler = { event: ClickInteractionRo ->
	event.handled = true
	event.preventDefault()
}

fun UiComponentRo.clickHandledForAFrame() {
	click().add(clickHandler)
	callLater { click().remove(clickHandler) }
}
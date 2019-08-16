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
import com.acornui.component.UiComponent
import com.acornui.component.UiComponentRo
import com.acornui.component.createOrReuseAttachment
import com.acornui.component.stage
import com.acornui.component.style.StyleBase
import com.acornui.component.style.StyleType
import com.acornui.di.inject
import com.acornui.input.*
import com.acornui.time.nowMs
import com.acornui.time.timer

class DownRepeat(
		private val target: UiComponentRo) : Disposable {

	private val mouseState = target.inject(MouseState)
	private val stage = target.stage
	private val interactivity = target.inject(InteractivityManager)

	// TODO: This style won't inherit
	val style = DownRepeatStyle()

	private var repeatTimer: Disposable? = null

	init {
	}

	private val repeatWaitHandler = {
		val m = mouseState
		MOUSE_DOWN_REPEAT.clear()
		MOUSE_DOWN_REPEAT.type = MouseInteractionRo.MOUSE_DOWN
		MOUSE_DOWN_REPEAT.canvasX = m.canvasX
		MOUSE_DOWN_REPEAT.canvasY = m.canvasY
		MOUSE_DOWN_REPEAT.button = WhichButton.LEFT
		MOUSE_DOWN_REPEAT.timestamp = nowMs()
		MOUSE_DOWN_REPEAT.localize(target)
		interactivity.dispatch(target, MOUSE_DOWN_REPEAT, useCapture = false, useBubble = false)
	}

	private val mouseDownHandler = {
		event: MouseInteractionRo ->
		if (event !== MOUSE_DOWN_REPEAT) {
			repeatTimer?.dispose()
			repeatTimer = timer(style.repeatInterval, -1, style.repeatDelay, repeatWaitHandler)
			stage.mouseUp().add(rawMouseUpHandler, true)
		}
	}

	private val rawMouseUpHandler = {
		event: MouseInteractionRo ->
		if (event.button == WhichButton.LEFT) {
			repeatTimer?.dispose()
			repeatTimer = null
		}
	}

	init {
		target.mouseDown().add(mouseDownHandler)
	}

	override fun dispose() {
		style.dispose()
		target.mouseDown().remove(mouseDownHandler)
		stage.mouseUp().remove(rawMouseUpHandler)
		repeatTimer?.dispose()
		repeatTimer = null
	}

	companion object {
		private val MOUSE_DOWN_REPEAT = MouseInteraction()
	}
}

/**
 * Returns true if the down repeat interaction is enabled on this [UiComponent].
 */
fun UiComponentRo.downRepeatEnabled(): Boolean {
	return getAttachment<DownRepeat>(DownRepeat) != null
}

fun UiComponentRo.enableDownRepeat(): DownRepeat {
	return createOrReuseAttachment(DownRepeat) { DownRepeat(this) }
}

/**
 * Sets this component to dispatch a mouse down event repeatedly after holding down on the target.
 * @param repeatDelay The number of seconds after holding down the target to start repeat dispatching.
 * @param repeatInterval Once the repeat dispatching begins, subsequent events are dispatched at this interval (in
 * seconds).
 */
fun UiComponentRo.enableDownRepeat(repeatDelay: Float, repeatInterval: Float): DownRepeat {
	return createOrReuseAttachment(DownRepeat) {
		val dR = DownRepeat(this)
		dR.style.repeatDelay = repeatDelay
		dR.style.repeatInterval = repeatInterval
		dR
	}
}

fun UiComponentRo.disableDownRepeat() {
	removeAttachment<DownRepeat>(DownRepeat)?.dispose()
}

class DownRepeatStyle : StyleBase() {
	override val type = Companion

	var repeatDelay by prop(0.24f)
	var repeatInterval by prop(0.02f)

	companion object : StyleType<DownRepeatStyle>
}

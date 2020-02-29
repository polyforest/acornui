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
import com.acornui.component.UiComponentRo
import com.acornui.component.createOrReuseAttachment
import com.acornui.di.ContextImpl
import com.acornui.function.as1
import com.acornui.input.interaction.LongPressAttachment.Companion.DEFAULT_LONG_PRESS_TIME
import com.acornui.input.touchEnd
import com.acornui.input.touchMove
import com.acornui.input.touchStart
import com.acornui.math.Vector2
import com.acornui.signal.Signal
import com.acornui.signal.Signal0
import com.acornui.time.timer
import kotlin.time.Duration
import kotlin.time.seconds

/**
 *
 */
private class LongPressAttachment(
		private val target: UiComponentRo,
		private val isCapture: Boolean,
		private val longPressTime: Duration = DEFAULT_LONG_PRESS_TIME,
		private val affordance: Float = DragAttachment.DEFAULT_AFFORDANCE
) : ContextImpl(target) {

	private val _longPress = Signal0()
	val longPress = _longPress.asRo()

	private var timerRef: Disposable? = null
	private var startCanvasX: Float = -9999f
	private var startCanvasY: Float = -9999f
	private var preventTouchEnd = false

	init {
		target.touchStart(isCapture).add(::touchStartHandler)
		target.touchEnd(isCapture).add(::touchEndHandler)
		target.touchMove(isCapture).add(::touchMoveHandler)
		target.rightClick(isCapture).add(::rightClickHandler)
	}

	private fun touchStartHandler(event: TouchInteractionRo) {
		stopTimer()
		if (event.touches.size != 1) return
		val t = event.touches.firstOrNull() ?: return
		startCanvasX = t.canvasX
		startCanvasY = t.canvasY
		timerRef = timer(longPressTime, callback = ::timerHandler.as1)
	}

	private fun touchMoveHandler(event: TouchInteractionRo) {
		val t = event.touches.firstOrNull() ?: return

		if (Vector2.manhattanDst(t.canvasX, t.canvasY, startCanvasX, startCanvasY) > affordance) {
			stopTimer()
		}
	}

	private fun timerHandler() {
		preventTouchEnd = true
		_longPress.dispatch()
	}

	private fun touchEndHandler(event: TouchInteractionRo) {
		if (preventTouchEnd) {
			event.preventDefault()
			preventTouchEnd = false
		}
		stopTimer()
	}

	private fun stopTimer() {
		timerRef?.dispose()
		timerRef = null
	}

	private fun rightClickHandler(event: ClickInteractionRo) {
		// Prevent the webgl context menu:
		if (_longPress.isNotEmpty())
			event.preventDefault()
	}

	override fun dispose() {
		super.dispose()
		stopTimer()
		target.touchStart(isCapture).remove(::touchStartHandler)
		target.touchEnd(isCapture).remove(::touchEndHandler)
		target.rightClick(isCapture).remove(::rightClickHandler)
		_longPress.dispose()
	}

	companion object {
		val DEFAULT_LONG_PRESS_TIME = 0.45.seconds
	}
}


/**
 *
 */
fun UiComponentRo.longPress(
		isCapture: Boolean = false,
		longPressTime: Duration = DEFAULT_LONG_PRESS_TIME,
		affordance: Float = DragAttachment.DEFAULT_AFFORDANCE
): Signal<() -> Unit> {
	return createOrReuseAttachment("LongPress_$isCapture") {
		LongPressAttachment(this, isCapture, longPressTime, affordance)
	}.longPress
}
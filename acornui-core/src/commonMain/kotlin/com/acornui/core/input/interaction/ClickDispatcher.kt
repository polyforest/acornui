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

import com.acornui.collection.arrayListObtain
import com.acornui.collection.arrayListPool
import com.acornui.component.Stage
import com.acornui.component.UiComponentRo
import com.acornui.component.ancestry
import com.acornui.component.getChildUnderPoint
import com.acornui.core.Disposable
import com.acornui.core.di.Injector
import com.acornui.core.di.Scoped
import com.acornui.core.di.inject
import com.acornui.core.input.*
import com.acornui.core.time.TimeDriver
import com.acornui.core.time.timer


/**
 * Watches the stage for mouse and touch events that constitute a 'click', then dispatches the click event on the
 * interactivity manager.
 *
 * This will respond to mouse and touch, but not both.
 */
abstract class ClickDispatcher(
		override val injector: Injector
) : Scoped, Disposable {

	protected val stage = inject(Stage)
	private val timeDriver = inject(TimeDriver)
	private val interactivityManager = inject(InteractivityManager)

	var multiClickSpeed: Int = 400

	private val downButtons: Array<MutableList<UiComponentRo>?> = Array(WhichButton.values().size) { null }
	protected val clickEvent = ClickInteraction()

	private var lastTarget: UiComponentRo? = null
	private var currentCount = 0
	private var previousTimestamp = 0L

	private var preventMouse = false

	private var pendingClick = false

	private var preventMouseTimer: Disposable? = null

	init {
		stage.mouseDown(isCapture = true).add(::rootMouseDownHandler)
		stage.touchStart(isCapture = true).add(::rootTouchStartHandler)
		stage.mouseUp().add(::rootMouseUpHandler)
		stage.touchEnd().add(::rootTouchEndHandler)
		stage.touchCancel(isCapture = true).add(::rootTouchCancelHandler)
	}

	private fun rootMouseDownHandler(event: MouseInteractionRo) {
		if (!preventMouse) {
			if (event.defaultPrevented()) {
				clearDownButton(event.button)
				return
			}

			val downElement = stage.getChildUnderPoint(event.canvasX, event.canvasY, onlyInteractive = true)
			if (downElement != null) {
				setDownElement(downElement, event.button)
			}
		}
	}

	private fun rootTouchStartHandler(event: TouchInteractionRo) {
		val first = event.changedTouches.first()
		val downElement = stage.getChildUnderPoint(first.canvasX, first.canvasY, onlyInteractive = true)
		if (downElement != null) {
			setDownElement(downElement, WhichButton.LEFT)
		}
	}

	private fun setDownElement(downElement: UiComponentRo, button: WhichButton) {
		if (lastTarget != downElement) {
			lastTarget = downElement
			currentCount = 1
		}
		@Suppress("UNCHECKED_CAST")
		val ancestry = arrayListObtain<UiComponentRo>()
		downElement.ancestry(ancestry)
		downButtons[button.ordinal] = ancestry
	}

	private fun rootMouseUpHandler(event: MouseInteractionRo) {
		if (!preventMouse) {
			release(event.button, event.canvasX, event.canvasY, event.timestamp, false)
		}
	}

	private fun rootTouchEndHandler(event: TouchInteractionRo) {
		if (event.defaultPrevented()) {
			clearDownButton(WhichButton.LEFT)
			return
		}
		val first = event.changedTouches.first()
		release(WhichButton.LEFT, first.canvasX, first.canvasY, event.timestamp, true)
		preventMouse = true
		preventMouseTimer?.dispose()
		preventMouseTimer = timer(timeDriver, 2.5f, 1) {
			preventMouse = false
			preventMouseTimer = null
		}
	}

	private fun rootTouchCancelHandler(event: TouchInteractionRo) {
		clearDownButton(WhichButton.LEFT)
	}

	private fun clearDownButton(button: WhichButton) {
		val downElements = downButtons[button.ordinal]
		if (downElements != null) {
			downButtons[button.ordinal] = null
			arrayListPool.free(downElements)
		}
	}

	protected fun release(button: WhichButton, canvasX: Float, canvasY: Float, timestamp: Long, fromTouch: Boolean) {
		val downElements = downButtons[button.ordinal]
		if (downElements != null) {
			downButtons[button.ordinal] = null
			for (i in 0..downElements.lastIndex) {
				val downElement = downElements[i]
				if (downElement.containsCanvasPoint(canvasX, canvasY)) {
					if (timestamp - previousTimestamp <= multiClickSpeed) {
						currentCount++
					} else {
						// Not fast enough to be considered a multi-click
						currentCount = 1
					}
					previousTimestamp = timestamp
					clickEvent.clear()
					clickEvent.type = getClickType(button)
					clickEvent.target = downElement
					clickEvent.button = button
					clickEvent.timestamp = timestamp
					clickEvent.count = currentCount
					clickEvent.canvasX = canvasX
					clickEvent.canvasY = canvasY
					clickEvent.fromTouch = fromTouch
					pendingClick = true
					break
				}
			}
			arrayListPool.free(downElements)
		}
	}

	private fun getClickType(button: WhichButton): InteractionType<ClickInteractionRo> {
		return when (button) {
			WhichButton.LEFT -> ClickInteractionRo.LEFT_CLICK
			WhichButton.RIGHT -> ClickInteractionRo.RIGHT_CLICK
			WhichButton.MIDDLE -> ClickInteractionRo.MIDDLE_CLICK
			WhichButton.BACK -> ClickInteractionRo.BACK_CLICK
			WhichButton.FORWARD -> ClickInteractionRo.FORWARD_CLICK
			else -> throw Exception("Unknown click type.")
		}
	}

	protected fun fireHandler(e: InteractionEventRo) {
		if (!e.defaultPrevented())
			fireClickEvent()
	}

	protected fun fireClickEvent(): Boolean {
		if (pendingClick) {
			pendingClick = false
			interactivityManager.dispatch(clickEvent.target, clickEvent)
			return true
		}
		return false
	}

	override fun dispose() {
		stage.mouseDown(isCapture = true).remove(::rootMouseDownHandler)
		stage.touchStart(isCapture = true).remove(::rootTouchStartHandler)
		stage.mouseUp().remove(::rootMouseUpHandler)
		stage.touchEnd().remove(::rootTouchEndHandler)
		stage.touchCancel(isCapture = true).remove(::rootTouchCancelHandler)
		for (i in 0..downButtons.lastIndex) {
			val list = downButtons[i]
			if (list != null) {
				list.clear()
				arrayListPool.free(list)
			}
		}
	}
}

class JvmClickDispatcher(injector: Injector) : ClickDispatcher(injector) {

	init {
		stage.mouseUp().add(::mouseUpHandler)
		stage.touchEnd().add(::touchEndHandler)
	}

	private fun mouseUpHandler(e: MouseInteractionRo) {
		if (!e.isFabricated && !e.defaultPrevented())
			fireClickEvent()
	}

	private fun touchEndHandler(e: TouchInteractionRo) {
		if (!e.isFabricated && !e.defaultPrevented())
			fireClickEvent()
	}

	override fun dispose() {
		super.dispose()
		stage.mouseUp().remove(::mouseUpHandler)
		stage.touchEnd().remove(::touchEndHandler)
	}
}

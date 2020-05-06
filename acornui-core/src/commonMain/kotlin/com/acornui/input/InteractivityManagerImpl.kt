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

@file:Suppress("UNUSED_PARAMETER")

package com.acornui.input

import com.acornui.Disposable
import com.acornui.collection.arrayListObtain
import com.acornui.collection.arrayListPool
import com.acornui.component.UiComponentRo
import com.acornui.component.ancestry
import com.acornui.component.getChildUnderPoint
import com.acornui.input.interaction.*
import com.acornui.recycle.ClearableObjectPool
import com.acornui.signal.StoppableSignal
import com.acornui.signal.StoppableSignalImpl
import com.acornui.time.nowMs

// TODO: possibly add a re-validation when the HIERARCHY has been invalidated. (use case: when an element has moved out from underneath the mouse)
/**
 * An implementation of InteractivityManager
 *
 * Must set the root ui component to use when propagating input.
 * @author nbilyk
 */
open class InteractivityManagerImpl(
		private val mouseInput: MouseInput,
		private val keyInput: KeyInput) : InteractivityManager, Disposable {

	private var _root: UiComponentRo? = null
	private val root: UiComponentRo
		get() = _root!!

	private var _activeElement: UiComponentRo? = null
	override val activeElement: UiComponentRo
		get() = _activeElement ?: root

	override fun activeElement(value: UiComponentRo?) {
		_activeElement = value
	}

	private val mousePool = ClearableObjectPool { MouseInteraction() }
	private val touchPool = ClearableObjectPool { TouchInteraction() }
	private val wheelPool = ClearableObjectPool { WheelInteraction() }
	private val keyPool = ClearableObjectPool { KeyInteraction() }
	private val charPool = ClearableObjectPool { CharInteraction() }

	private val overTargets = ArrayList<UiComponentRo>()

	private fun overCanvasChangedHandler(overCanvas: Boolean) {
		if (!overCanvas)
			mouseOverTarget(null)
	}

	private fun rawTouchStartHandler(event: TouchInteractionRo) {
		touchHandler(TouchInteractionRo.TOUCH_START, event)
	}

	private fun rawTouchEndHandler(event: TouchInteractionRo) {
		touchHandler(TouchInteractionRo.TOUCH_END, event)
	}

	private fun rawTouchMoveHandler(event: TouchInteractionRo) {
		touchHandler(TouchInteractionRo.TOUCH_MOVE, event)
	}

	private fun rawMouseDownHandler(event: MouseInteractionRo) {
		mouseHandler(MouseInteractionRo.MOUSE_DOWN, event)
	}

	private fun rawMouseUpHandler(event: MouseInteractionRo) {
		mouseHandler(MouseInteractionRo.MOUSE_UP, event)
	}

	private fun rawMouseMoveHandler(event: MouseInteractionRo) {
		mouseOverTarget(mouseHandler(MouseInteractionRo.MOUSE_MOVE, event))
	}

	private fun rawWheelHandler(event: WheelInteractionRo) {
		val wheel = wheelPool.obtain()
		wheel.set(event)
		wheel.type = WheelInteractionRo.MOUSE_WHEEL
		dispatch(event.canvasX + 0.5f, event.canvasY + 0.5f, wheel)
		if (wheel.defaultPrevented())
			event.preventDefault()
		wheelPool.free(wheel)
	}

	/**
	 * Dispatches a mouse event of the given type with the target being the element under the mouse position or the
	 * [root].
	 * @return Returns the component the mouse is over, or null.
	 */
	private fun <T : MouseInteractionRo> mouseHandler(type: InteractionType<T>, event: MouseInteractionRo): UiComponentRo? {
		val mouse = mousePool.obtain()
		mouse.set(event)
		mouse.type = type
		val ele = root.getChildUnderPoint(mouse.canvasX + 0.5f, mouse.canvasY + 0.5f, onlyInteractive = true) ?: root
		dispatch(mouse, ele, true, true)
		if (mouse.defaultPrevented())
			event.preventDefault()
		mousePool.free(mouse)
		return ele
	}

	/**
	 * Dispatches a touch event of the given type with the target being the element under the first touch point or the
	 * [root].
	 * @return Returns the component the first touch is over, or null.
	 */
	private fun touchHandler(type: InteractionType<TouchInteractionRo>, event: TouchInteractionRo): UiComponentRo? {
		val touch = touchPool.obtain()
		touch.set(event)
		touch.type = type
		val first = event.changedTouches.first()
		val ele = root.getChildUnderPoint(first.canvasX + 0.5f, first.canvasY + 0.5f, onlyInteractive = true) ?: root
		dispatch(touch, ele, useCapture = true, useBubble = true)
		if (touch.defaultPrevented())
			event.preventDefault()
		touchPool.free(touch)
		return ele
	}

	private fun keyDownHandler(event: KeyInteractionRo) {
		keyHandler(KeyInteractionRo.KEY_DOWN, event)
	}

	private fun keyUpHandler(event: KeyInteractionRo) {
		keyHandler(KeyInteractionRo.KEY_UP, event)
	}

	private fun charHandler(event: CharInteractionRo) {
		charHandler(CharInteractionRo.CHAR, event)
	}

	private fun <T : KeyInteractionRo> keyHandler(type: InteractionType<T>, event: KeyInteractionRo) {
		val f = activeElement
		val key = keyPool.obtain()
		key.type = type
		key.set(event)
		dispatch(key, f)
		if (key.defaultPrevented()) event.preventDefault()
		keyPool.free(key)
	}

	private fun <T : CharInteractionRo> charHandler(type: InteractionType<T>, event: CharInteractionRo) {
		val f = activeElement
		val char = charPool.obtain()
		char.type = CharInteractionRo.CHAR
		char.set(event)
		dispatch(char, f)
		if (char.defaultPrevented())
			event.preventDefault()
		charPool.free(char)
	}

	override fun init(root: UiComponentRo) {
		check(_root == null) {
			"Already initialized"
		}
		_root = root
		mouseInput.overCanvasChanged.add(::overCanvasChangedHandler)
		mouseInput.mouseDown.add(::rawMouseDownHandler)
		mouseInput.mouseUp.add(::rawMouseUpHandler)
		mouseInput.mouseMove.add(::rawMouseMoveHandler)
		mouseInput.mouseWheel.add(::rawWheelHandler)

		mouseInput.touchStart.add(::rawTouchStartHandler)
		mouseInput.touchEnd.add(::rawTouchEndHandler)
		mouseInput.touchMove.add(::rawTouchMoveHandler)

		keyInput.keyDown.add(::keyDownHandler)
		keyInput.keyUp.add(::keyUpHandler)
		keyInput.char.add(::charHandler)
	}

	private fun mouseOverTarget(target: UiComponentRo?) {
		val previousOverTarget = overTargets.firstOrNull()
		if (target == previousOverTarget) return
		val mouse = mousePool.obtain()
		mouse.canvasX = mouseInput.mouseX
		mouse.canvasY = mouseInput.mouseY
		mouse.button = WhichButton.UNKNOWN
		mouse.timestamp = nowMs()

		if (previousOverTarget != null) {
			mouse.relatedTarget = target
			mouse.target = previousOverTarget
			mouse.type = MouseInteractionRo.MOUSE_OUT
			dispatch(overTargets, mouse)
		}
		if (target != null) {
			target.ancestry(overTargets)
			mouse.relatedTarget = previousOverTarget
			mouse.target = target
			mouse.type = MouseInteractionRo.MOUSE_OVER
			dispatch(overTargets, mouse)
		} else {
			overTargets.clear()
		}
		mousePool.free(mouse)
	}

	override fun <T : InteractionEventRo> getSignal(host: UiComponentRo, type: InteractionType<T>, isCapture: Boolean): StoppableSignal<T> {
		return StoppableSignalImpl()
	}

	/**
	 * Dispatches an interaction for the layout element at the given stage position.
	 * @param canvasX The x coordinate relative to the canvas.
	 * @param canvasY The y coordinate relative to the canvas.
	 * @param event The interaction event to dispatch.
	 */
	override fun dispatch(canvasX: Float, canvasY: Float, event: InteractionEvent, useCapture: Boolean, useBubble: Boolean) {
		val ele = root.getChildUnderPoint(canvasX, canvasY, onlyInteractive = true) ?: root
		dispatch(event, ele, useCapture, useBubble)
	}

	/**
	 * Dispatches an interaction for a single interactive element.
	 * This will first dispatch a capture event from the stage down to the given target, and then
	 * a bubbling event up to the stage.
	 */
	override fun dispatch(event: InteractionEvent, target: UiComponentRo, useCapture: Boolean, useBubble: Boolean) {
		event.target = target
		if (!useCapture && !useBubble) {
			// Dispatch only for current target.
			dispatchForCurrentTarget(target, event, isCapture = false)
		} else {
			val rawAncestry = arrayListObtain<UiComponentRo>()
			target.ancestry(rawAncestry)
			dispatch(rawAncestry, event, useCapture, useBubble)
			arrayListPool.free(rawAncestry)
		}
	}

	private fun dispatch(rawAncestry: List<UiComponentRo>, event: InteractionEvent, useCapture: Boolean = true, useBubble: Boolean = true) {
		// Capture phase
		if (useCapture) {
			for (i in rawAncestry.lastIndex downTo 0) {
				if (event.propagation.propagationStopped) break
				dispatchForCurrentTarget(rawAncestry[i], event, isCapture = true)
			}
		}
		// Bubble phase
		if (useBubble) {
			for (i in 0..rawAncestry.lastIndex) {
				if (event.propagation.propagationStopped) break
				dispatchForCurrentTarget(rawAncestry[i], event, isCapture = false)
			}
		}
	}

	private fun dispatchForCurrentTarget(currentTarget: UiComponentRo, event: InteractionEvent, isCapture: Boolean) {
		val signal = currentTarget.getInteractionSignal(event.type, isCapture) as StoppableSignalImpl?
		if (signal != null && signal.isNotEmpty()) {
			event.currentTarget = currentTarget
			signal.dispatch(event)
		}
	}

	override fun dispose() {
		_activeElement = null
		mouseOverTarget(null)

		val mouse = mouseInput
		mouse.mouseDown.remove(::rawMouseDownHandler)
		mouse.mouseUp.remove(::rawMouseUpHandler)
		mouse.mouseMove.remove(::rawMouseMoveHandler)
		mouse.mouseWheel.remove(::rawWheelHandler)

		mouseInput.touchStart.remove(::rawTouchStartHandler)
		mouseInput.touchEnd.remove(::rawTouchEndHandler)
		mouseInput.touchMove.remove(::rawTouchMoveHandler)

		val key = keyInput
		key.keyDown.remove(::keyDownHandler)
		key.keyUp.remove(::keyUpHandler)
		key.char.remove(::charHandler)
	}
}

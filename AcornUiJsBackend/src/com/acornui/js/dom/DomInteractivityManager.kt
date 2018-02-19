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

package com.acornui.js.dom

import com.acornui._assert
import com.acornui.collection.arrayListObtain
import com.acornui.collection.arrayListPool
import com.acornui.component.UiComponentRo
import com.acornui.component.ancestry
import com.acornui.component.getChildUnderPoint
import com.acornui.core.input.*
import com.acornui.core.input.interaction.*
import com.acornui.core.input.interaction.Touch
import com.acornui.js.dom.component.DomComponent
import com.acornui.js.html.*
import com.acornui.logging.Log
import com.acornui.signal.StoppableSignalImpl
import org.w3c.dom.HTMLElement
import org.w3c.dom.Node
import org.w3c.dom.events.*
import kotlin.browser.window

/**
 * An implementation of InteractivityManager that uses native browser events.
 *
 * @author nbilyk
 */
open class DomInteractivityManager : InteractivityManager {

	var scrollSpeed = 24f

	private var _root: UiComponentRo? = null
	private val root: UiComponentRo
		get() = _root!!
	private var _rootElement: HTMLElement? = null
	private val rootElement: HTMLElement
		get() = _rootElement!!

	private val keyEvent = KeyInteraction()
	private val mouseEvent = MouseInteraction()
	private val clickEvent = ClickInteraction()
	private val touchEvent = TouchInteraction()
	private val wheelEvent = WheelInteraction()
	private val clipboardEvent = ClipboardInteraction()

	private val resetKeyEventHandler = {
		e: Event ->
		keyEvent.clear()
		Unit
	}

	private val resetMouseEventHandler = {
		e: Event ->
		mouseEvent.clear()
		Unit
	}

	private val resetClickEventHandler = {
		e: Event ->
		clickEvent.clear()
		Unit
	}

	private val resetTouchEventHandler = {
		e: Event ->
		touchEvent.clear()
		Unit
	}

	private val resetWheelEventHandler = {
		e: Event ->
		wheelEvent.clear()
		Unit
	}

	private val resetClipboardEventHandler = {
		e: Event ->
		clipboardEvent.clear()
		Unit
	}

	override fun init(root: UiComponentRo) {
		_assert(_root == null, "Already initialized.")
		_root = root
		_rootElement = (root.native as DomComponent).element

		val rootElement = rootElement
		rootElement.addEventListeners(arrayOf("keydown", "keyup", "keypress"), resetKeyEventHandler)
		rootElement.addEventListeners(arrayOf("mouseover", "mouseout", "mousemove", "mousedown", "mouseup"), resetMouseEventHandler)
		rootElement.addEventListener("click", resetClickEventHandler, true)
		rootElement.addEventListeners(arrayOf("touchstart", "touchend", "touchmove", "touchcancel", "touchenter", "touchleave"), resetTouchEventHandler)
		rootElement.addEventListener("wheel", resetWheelEventHandler, true)
		rootElement.addEventListeners(arrayOf("copy", "cut", "paste"), resetClipboardEventHandler)

		mouseOutWorkaround()
	}

	//----------------------------------------------------------
	// MouseOutWorkaround
	//----------------------------------------------------------

	private val overTargets = ArrayList<EventTarget>()

	private val rootMouseOverHandler = {
		e: Event ->
		val target = e.target
		if (target != null) {
			if (!overTargets.contains(target)) {
				overTargets.add(target)
			}
		}
	}

	private val rootMouseOutHandler = {
		e: Event ->
		val target = e.target
		if (target != null) {
			overTargets.remove(target)
		}
	}

	private val rootMouseMoveHandler = {
		e: Event ->
		e as MouseEvent
		val target = e.target as Node?
		// Check if we need to fake a mouseout for any of the targets we think we're over.
		var i = 0
		while (i < overTargets.size) {
			val overTarget = overTargets[i] as HTMLElement
			if (target == null || !overTarget.owns(target)) {
				val targetComponent = findComponentFromDom(overTarget, root)
				if (targetComponent != null) {
					mouseEvent.relatedTarget = findComponentFromDom(e.relatedTarget, root)
					mouseEvent.timestamp = e.timeStamp.toLong()
					// TODO: this probably doesn't work if the root canvas is nested.
					mouseEvent.canvasX = e.pageX.toFloat() - rootElement.offsetLeft.toFloat()
					mouseEvent.canvasY = e.pageY.toFloat() - rootElement.offsetTop.toFloat()
					mouseEvent.button = getWhichButton(e.button.toInt())

					dispatch(targetComponent, mouseEvent)
				}
				overTargets.removeAt(i)
				i--
			}
			i++
		}
		Unit
	}

	/**
	 * JS does not dispatch a mouseout event when an element is moved out from underneath the mouse (as opposed to
	 * the mouse moving). This is a workaround to that.
	 */
	private fun mouseOutWorkaround() {
		rootElement.addEventListener("mousemove", rootMouseMoveHandler)
		rootElement.addEventListener("mouseover", rootMouseOverHandler)
		rootElement.addEventListener("mouseout", rootMouseOutHandler)
	}

	//----------------------------------------------------------

	private val nativeKeyHandler: (Event) -> dynamic = {
		keyEvent.set(it as KeyboardEvent)
		Unit
	}

	private val nativeMouseHandler: (Event) -> dynamic = {
		mouseEvent.set(it as MouseEvent)
		Unit
	}

	private val nativeWheelHandler: (Event) -> dynamic = {
		it as WheelEvent
		wheelEvent.set(it)
		wheelEvent.deltaX = scrollSpeed * if (it.deltaX > 0f) 1f else -1f
		wheelEvent.deltaY = scrollSpeed * if (it.deltaY > 0f) 1f else -1f
		wheelEvent.deltaZ = scrollSpeed * if (it.deltaZ > 0f) 1f else -1f
		Unit
	}

	private val nativeClickHandler: (Event) -> dynamic = {
		clickEvent.set(it as MouseEvent)
		clickEvent.count = it.detail
		Unit
	}

	private val nativeTouchHandler: (Event) -> dynamic = {
		touchEvent.set(it as TouchEvent)
		Unit
	}

	private val nativeClipboardHandler: (Event) -> dynamic = {
		clipboardEvent.set(it as ClipboardEvent)
		Unit
	}


	override fun <T : InteractionEventRo> getSignal(host: UiComponentRo, type: InteractionType<T>, isCapture: Boolean): StoppableSignalImpl<T> {
		return when (type) {
			KeyInteraction.KEY_DOWN -> {
				NativeSignal(host, "keydown", isCapture, type, keyEvent, nativeKeyHandler)
			}
			KeyInteraction.KEY_UP -> {
				NativeSignal(host, "keyup", isCapture, type, keyEvent, nativeKeyHandler)
			}
			MouseInteraction.MOUSE_OVER -> {
				NativeSignal(host, "mouseover", isCapture, type, mouseEvent, nativeMouseHandler)
			}
			MouseInteraction.MOUSE_OUT -> {
				NativeSignal(host, "mouseout", isCapture, type, mouseEvent, nativeMouseHandler)
			}
			MouseInteraction.MOUSE_MOVE -> {
				NativeSignal(host, "mousemove", isCapture, type, mouseEvent, nativeMouseHandler)
			}
			MouseInteraction.MOUSE_UP -> {
				NativeSignal(host, "mouseup", isCapture, type, mouseEvent, nativeMouseHandler)
			}
			MouseInteraction.MOUSE_DOWN -> {
				NativeSignal(host, "mousedown", isCapture, type, mouseEvent, nativeMouseHandler)
			}
			WheelInteraction.MOUSE_WHEEL -> {
				NativeSignal(host, "wheel", isCapture, type, wheelEvent, nativeWheelHandler)
			}
			ClickInteraction.LEFT_CLICK -> {
				NativeSignal(host, "click", isCapture, type, clickEvent, nativeClickHandler)
			}
			TouchInteraction.TOUCH_START -> {
				NativeSignal(host, "touchstart", isCapture, type, touchEvent, nativeTouchHandler)
			}
			TouchInteraction.TOUCH_END -> {
				NativeSignal(host, "touchend", isCapture, type, touchEvent, nativeTouchHandler)
			}
			TouchInteraction.TOUCH_MOVE -> {
				NativeSignal(host, "touchmove", isCapture, type, touchEvent, nativeTouchHandler)
			}
			TouchInteraction.TOUCH_CANCEL -> {
				NativeSignal(host, "touchcancel", isCapture, type, touchEvent, nativeTouchHandler)
			}
			TouchInteraction.TOUCH_ENTER -> {
				NativeSignal(host, "touchenter", isCapture, type, touchEvent, nativeTouchHandler)
			}
			TouchInteraction.TOUCH_LEAVE -> {
				NativeSignal(host, "touchleave", isCapture, type, touchEvent, nativeTouchHandler)
			}
			ClipboardInteraction.COPY -> {
				NativeSignal(host, "copy", isCapture, type, clipboardEvent, nativeClipboardHandler)
			}
			ClipboardInteraction.CUT -> {
				NativeSignal(host, "cut", isCapture, type, clipboardEvent, nativeClipboardHandler)
			}
			ClipboardInteraction.PASTE -> {
				NativeSignal(host, "paste", isCapture, type, clipboardEvent, nativeClipboardHandler)
			}
			else -> {
				Log.warn("Could not find a signal for the type $type")
				StoppableSignalImpl()
			}
		}
	}

	private fun KeyInteraction.set(jsEvent: KeyboardEvent) {
		timestamp = jsEvent.timeStamp.toLong()
		location = keyLocationFromInt(jsEvent.location)
		keyCode = jsEvent.keyCode
		altKey = jsEvent.altKey
		ctrlKey = jsEvent.ctrlKey
		metaKey = jsEvent.metaKey
		shiftKey = jsEvent.shiftKey
		isRepeat = jsEvent.repeat
	}

	fun keyLocationFromInt(location: Int): KeyLocation {
		when (location) {
			0 -> return KeyLocation.STANDARD
			1 -> return KeyLocation.LEFT
			2 -> return KeyLocation.RIGHT
			3 -> return KeyLocation.NUMBER_PAD
			else -> return KeyLocation.UNKNOWN
		}
	}

	private fun MouseInteraction.set(jsEvent: MouseEvent) {
		relatedTarget = findComponentFromDom(jsEvent.relatedTarget, root)
		timestamp = jsEvent.timeStamp.toLong()
		// TODO: this probably doesn't work if the root canvas is nested.
		canvasX = jsEvent.pageX.toFloat() - rootElement.offsetLeft.toFloat()
		canvasY = jsEvent.pageY.toFloat() - rootElement.offsetTop.toFloat()
		button = getWhichButton(jsEvent.button.toInt())
	}

	private fun TouchInteraction.set(jsEvent: TouchEvent) {
		timestamp = jsEvent.timeStamp.toLong()
		clearTouches()
		for (targetTouch in jsEvent.targetTouches) {
			val t = Touch.obtain()
			t.set(targetTouch)
			targetTouches.add(t)
		}
		for (changedTouch in jsEvent.changedTouches) {
			val t = Touch.obtain()
			t.set(changedTouch)
			changedTouches.add(t)
		}
		for (touch in jsEvent.touches) {
			val t = Touch.obtain()
			t.set(touch)
			touches.add(t)
		}
	}

	private fun ClipboardInteraction.set(jsEvent: ClipboardEvent) {
		target = findComponentFromDom(jsEvent.target, root)
		dataTransfer = DomDataTransfer(jsEvent.clipboardData ?: window.clipboardData)
	}

	private fun Touch.set(jsTouch: com.acornui.js.html.Touch) {
		target = findComponentFromDom(jsTouch.target, root)
		canvasX = jsTouch.clientX.toFloat() - rootElement.offsetLeft.toFloat()
		canvasY = jsTouch.clientY.toFloat() - rootElement.offsetTop.toFloat()
	}

	private fun getWhichButton(i: Int): WhichButton {
		when (i) {
			-1 -> return WhichButton.UNKNOWN
			0 -> return WhichButton.LEFT
			1 -> return WhichButton.MIDDLE
			2 -> return WhichButton.RIGHT
			3 -> return WhichButton.BACK
			4 -> return WhichButton.FORWARD
			else -> return WhichButton.UNKNOWN
		}
	}

	/**
	 * Dispatches an interaction for the layout element at the given stage position.
	 * @param canvasX The x coordinate relative to the canvas.
	 * @param canvasY The y coordinate relative to the canvas.
	 * @param event The interaction event to dispatch.
	 */
	override fun dispatch(canvasX: Float, canvasY: Float, event: InteractionEvent, useCapture: Boolean, useBubble: Boolean) {
		val ele = root.getChildUnderPoint(canvasX, canvasY, onlyInteractive = true) ?: root
		dispatch(ele, event, useCapture, useBubble)
	}

	/**
	 * Dispatches an interaction for a single interactive element.
	 * This will first dispatch a capture event from the stage down to the given target, and then
	 * a bubbling event up to the stage.
	 */
	override fun dispatch(target: UiComponentRo, event: InteractionEvent, useCapture: Boolean, useBubble: Boolean) {
		val rawAncestry = arrayListObtain<UiComponentRo>()
		event.target = target
		if (!useCapture && !useBubble) {
			// Dispatch only for current target.
			dispatchForCurrentTarget(target, event, isCapture = false)
		} else {
			target.ancestry(rawAncestry)
			dispatch(rawAncestry, event, useCapture, useBubble)
		}
		arrayListPool.free(rawAncestry)
	}

	private fun dispatch(rawAncestry: List<UiComponentRo>, event: InteractionEvent, useCapture: Boolean, useBubble: Boolean) {
		// Capture phase
		if (useCapture) {
			for (i in rawAncestry.lastIndex downTo 0) {
				if (event.propagation.propagationStopped()) break
				dispatchForCurrentTarget(rawAncestry[i], event, isCapture = true)
			}
		}
		// Bubble phase
		if (useBubble) {
			for (i in 0..rawAncestry.lastIndex) {
				if (event.propagation.propagationStopped()) break
				dispatchForCurrentTarget(rawAncestry[i], event, isCapture = false)
			}
		}
	}

	private fun dispatchForCurrentTarget(currentTarget: UiComponentRo, event: InteractionEvent, isCapture: Boolean) {
		val signal = currentTarget.getInteractionSignal(event.type, isCapture) as StoppableSignalImpl?
		if (signal != null && signal.isNotEmpty()) {
			event.localize(currentTarget)
			signal.dispatch(event)
		}
	}

	override fun dispose() {
		val rootElement = _rootElement ?: return
		rootElement.removeEventListeners(arrayOf("keydown", "keyup", "keypress"), resetKeyEventHandler)
		rootElement.removeEventListeners(arrayOf("mouseover", "mouseout", "mousemove", "mousedown", "mouseup"), resetMouseEventHandler)
		rootElement.removeEventListener("click", resetClickEventHandler, true)
		rootElement.removeEventListeners(arrayOf("touchstart", "touchend", "touchmove", "touchcancel", "touchenter", "touchleave"), resetTouchEventHandler)
		rootElement.removeEventListener("wheel", resetWheelEventHandler, true)
		rootElement.removeEventListeners(arrayOf("copy", "cut", "paste"), resetClipboardEventHandler)
	}

	private fun EventTarget.addEventListeners(list: Array<String>, handler: (Event) -> dynamic) {
		for (s in list) {
			addEventListener(s, handler, true)
		}
	}

	private fun EventTarget.removeEventListeners(list: Array<String>, handler: (Event) -> dynamic) {
		for (s in list) {
			removeEventListener(s, handler, true)
		}
	}
}
/*
 * Copyright 2020 Poly Forest, LLC
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

@file:Suppress("unused")

package com.acornui.input

import com.acornui.Disposable
import com.acornui.and
import com.acornui.collection.find
import com.acornui.collection.first
import com.acornui.component.UiComponent
import com.acornui.di.ContextImpl
import com.acornui.dom.clientToLocal
import com.acornui.dom.handle
import com.acornui.dom.isFabricated
import com.acornui.frame
import com.acornui.math.Vector2
import com.acornui.math.vec2
import com.acornui.own
import com.acornui.properties.afterChange
import com.acornui.signal.*
import kotlinx.browser.window
import org.w3c.dom.TouchEvent
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.events.Event as DomEvent

/**
 * Drag behavior for a target component.
 * Supports mobile, drag affordance, cancellation, and click prevention.
 */
open class Drag(
	val target: UiComponent
) : ContextImpl(target) {

	/**
	 * Dispatched when the drag is about to begin, and may be prevented.
	 * This will only dispatch after the drag has passed [affordance] pixels.
	 *
	 * If [affordance] is zero, this event will be dispatched within the user event handler.
	 */
	val dragStarting = signal<DragEvent>()

	/**
	 * Dispatched when the drag has begun.
	 * This will only dispatch after the drag has passed [affordance] pixels.
	 *
	 * If [affordance] is zero, this event will be dispatched within the user event handler.
	 */
	val dragStarted = signal<DragEvent>()

	/**
	 * Dispatched on each frame during a drag.
	 */
	val dragged = signal<DragEvent>()

	/**
	 * Dispatched when the drag has completed.
	 * This may either be from the mouse/touch ending or by cancelling via ESCAPE.
	 * If the drag is cancelled via ESCAPE, [DragEvent.defaultPrevented] will be true.
	 */
	val dragEnded = signal<DragEvent>()

	/**
	 * The distance the user must drag before the drag action has started.
	 */
	var affordance = DEFAULT_AFFORDANCE

	private var clickHandle: Disposable? = null

	/**
	 * The user is currently dragging and past the [affordance] distance.
	 */
	var isDragging: Boolean = false
		private set

	/**
	 * If true, the touchMove event will have default prevented.
	 * This will prevent drag scrolling on mobile.
	 */
	var preventDefaultOnTouch = true

	/**
	 * If true, the mouseMove event will have default prevented.
	 */
	var preventDefaultOnMouse = true

	/**
	 * Returns true if the user is currently interacting.
	 * This is similar to [isDragging] except will return true if the user has not passed [affordance].
	 */
	val userIsActive: Boolean
		get() = watchHandle != null

	private var watchHandle: Disposable? = null

	private val win = window.asWithEventTarget()

	private var maxDragDistance = 0.0
	private var startPositionClient = Vector2.ZERO
	private var previousPositionClient = Vector2.ZERO
	private var previousPositionLocal = Vector2.ZERO
	private var positionClient = Vector2.ZERO
	private var positionLocal = Vector2.ZERO
	private var startPositionLocal = Vector2.ZERO
	private var ctrlKey: Boolean = false
	private var shiftKey: Boolean = false
	private var altKey: Boolean = false
	private var metaKey: Boolean = false

	private var touchId: Int = -1

	private val fromTouch: Boolean
		get() = touchId != -1

	/**
	 * True if the drag was cancelled by hitting ESCAPE.
	 */
	private var cancelled = false

	private fun initStartVariables(positionClient: Vector2, touchId: Int = -1) {
		this.startPositionClient = positionClient
		this.previousPositionClient = positionClient
		this.positionClient = positionClient
		this.positionLocal = target.clientToLocal(positionClient)
		this.previousPositionLocal = positionLocal
		this.touchId = touchId
		startPositionLocal = target.clientToLocal(startPositionClient)
		cancelled = false
	}

	private fun move(positionClient: Vector2) {
		this.previousPositionClient = this.positionClient
		this.previousPositionLocal = this.positionLocal
		this.positionClient = positionClient
		this.positionLocal = target.clientToLocal(positionClient)
	}

	private fun windowKeyDownHandler(event: KeyboardEvent) {
		if (event.keyCode == Ascii.ESCAPE) {
			event.handle()
			cancelled = true
			stop()
		}
	}

	//--------------------------------------------------------------
	// Mouse UX
	//--------------------------------------------------------------

	private fun windowMouseMoveHandler(event: MouseEvent) {
		event.handle()
		setModifiers(event)
		move(vec2(event.clientX, event.clientY))
		if (preventDefaultOnMouse)
			event.preventDefault()
	}

	private fun mousePressedHandler(event: MouseEvent) {
		if (userIsActive || !allowMouseStart(event)) return
		initStartVariables(vec2(event.clientX, event.clientY))
		event.handle()
		setModifiers(event)
		watchMouse()
		frameHandler(event.isTrusted)
		if (preventDefaultOnMouse && isDragging)
			event.preventDefault()
	}

	private fun windowMouseReleasedHandler(event: MouseEvent) {
		if (!allowMouseEnd(event)) return
		event.handle()
		move(vec2(event.clientX, event.clientY))
		stop()
	}

	private fun watchMouse() {
		watchHandle = frame.listen { frameHandler(false) } and
				win.mouseMoved.listen(
					eventOptions = EventOptions(isPassive = false),
					handler = ::windowMouseMoveHandler
				) and
				win.mouseReleased.listen(handler = ::windowMouseReleasedHandler) and
				win.keyPressed.listen(handler = ::windowKeyDownHandler)
	}

	/**
	 * Return true if the drag should start watching movement.
	 * This does not determine if a drag start may begin.
	 */
	protected open fun allowMouseStart(event: MouseEvent): Boolean =
		isEnabled && !event.isFabricated && event.button == WhichButton.LEFT

	protected open fun allowMouseEnd(event: MouseEvent): Boolean =
		!event.isFabricated && event.button == WhichButton.LEFT

	//--------------------------------------------------------------
	// Touch UX
	//--------------------------------------------------------------

	private fun touchStartHandler(event: TouchEvent) {
		if (userIsActive || !allowTouchStart(event)) return
		val t = event.touches.first()
		initStartVariables(vec2(t.clientX, t.clientY), t.identifier)
		event.handle()
		setModifiers(event)
		watchTouch()
		frameHandler(event.isTrusted)
		if (preventDefaultOnTouch && isDragging)
			event.preventDefault()
	}

	private fun watchTouch() {
		watchHandle = frame.listen { frameHandler(false) } and
				win.touchMoved.listen(EventOptions(isPassive = false), ::windowTouchMoveHandler) and
				win.touchEnded.listen(::windowTouchEndHandler) and
				win.keyPressed.listen(handler = ::windowKeyDownHandler)
	}

	/**
	 * Return true if the drag should start watching movement.
	 * This does not determine if a drag start may begin.
	 */
	protected open fun allowTouchStart(event: TouchEvent): Boolean =
		isEnabled && !event.isFabricated

	protected open fun allowTouchEnd(event: TouchEvent): Boolean =
		event.touches.find { it?.identifier == touchId } == null

	private fun windowTouchMoveHandler(event: TouchEvent) {
		event.handle()
		val touch = event.touches.find { it?.identifier == touchId }
		if (touch != null)
			move(vec2(touch.clientX, touch.clientY))
		setModifiers(event)
		if (preventDefaultOnTouch)
			event.preventDefault()
	}

	private fun windowTouchEndHandler(event: TouchEvent) {
		if (!allowTouchEnd(event)) return
		event.handle()
		stop()
	}

	//--------------------------------------------------------------
	// Drag
	//--------------------------------------------------------------

	private fun frameHandler(isTrusted: Boolean) {
		if (!isDragging) {
			if (positionClient.dst(startPositionClient) >= affordance) {
				val e = dragEvent(isTrusted, cancellable = true)
				dragStarting.dispatch(e)

				if (e.defaultPrevented) {
					stop()
				} else {
					isDragging = true
					dragStarted.dispatch(e.copy(cancellable = false))
				}
			}
		}
		if (isDragging && target.isConnected)
			dragged.dispatch(dragEvent(isTrusted))
	}

	private fun dragEvent(isTrusted: Boolean, cancellable: Boolean = false): DragEvent = DragEvent(
		cancellable = cancellable,
		startPositionClient = startPositionClient,
		startPositionLocal = startPositionLocal,
		previousPositionClient = previousPositionClient,
		previousPositionLocal = previousPositionLocal,
		positionClient = positionClient,
		positionLocal = positionLocal,
		fromTouch = fromTouch,
		touchId = touchId,
		target = target,
		isTrusted = isTrusted,
		ctrlKey = ctrlKey,
		shiftKey = shiftKey,
		altKey = altKey,
		metaKey = metaKey
	)

	private fun setModifiers(event: TouchEvent) {
		ctrlKey = event.ctrlKey
		shiftKey = event.shiftKey
		altKey = event.altKey
		metaKey = event.metaKey
	}

	private fun setModifiers(event: MouseEvent) {
		ctrlKey = event.ctrlKey
		shiftKey = event.shiftKey
		altKey = event.altKey
		metaKey = event.metaKey
	}

	/**
	 * If true, drag operations are enabled.
	 */
	var isEnabled: Boolean by afterChange(true) {
		if (!it) stop()
	}

	fun stop() = stop(false)
	fun stop(event: DomEvent) = stop(event.isTrusted)

	private fun stop(isTrusted: Boolean) {
		watchHandle?.dispose()
		watchHandle = null
		if (!isDragging) return
		frameHandler(isTrusted)
		isDragging = false
		val e = dragEvent(isTrusted)
		if (cancelled) e.preventDefault()
		dragEnded.dispatch(e)

		// Set the next click to be marked as handled:
		clickHandle = win.clicked.listen(
			EventOptions(isCapture = true, isPassive = false)
		) { event ->
			event.handle()
			event.preventDefault() // Prevent focus change
		}
		frame.once { clickHandle?.dispose(); clickHandle = null }
	}

	/**
	 * Forces the drag operation to begin.
	 * This can be a way to transfer a drag operation from one component to another.
	 */
	fun start(event: DragEvent) {
		stop()
		startPositionClient = event.startPositionClient
		previousPositionClient = event.previousPositionClient
		positionClient = event.positionClient
		touchId = event.touchId

		if (fromTouch) watchTouch()
		else watchMouse()
		frameHandler(event.isTrusted)
	}

	init {
		own(target.mousePressed.listen(EventOptions(isPassive = false), ::mousePressedHandler))
		own(target.touchStarted.listen(EventOptions(isPassive = false), ::touchStartHandler))
	}

	override fun dispose() {
		stop()
		super.dispose()
	}

	companion object {
		const val DEFAULT_AFFORDANCE = 5.0
	}
}

data class DragEvent(

	/**
	 * True if the default may be prevented.
	 */
	val cancellable: Boolean,

	/**
	 * The starting position (in page coordinates) for the drag.
	 */
	val startPositionClient: Vector2,

	/**
	 * The starting position relative to the target element's bounding rectangle.
	 */
	val startPositionLocal: Vector2,

	/**
	 * The position of the last event (in page coordinates).
	 */
	val previousPositionClient: Vector2,

	/**
	 * The previous position relative to the target element's bounding rectangle.
	 */
	val previousPositionLocal: Vector2,

	/**
	 * The current position (in client coordinates).
	 */
	val positionClient: Vector2,

	/**
	 * The position relative to the target element's bounding rectangle.
	 */
	val positionLocal: Vector2,

	/**
	 * True if initialized from a touch interaction, false if mouse.
	 */
	val fromTouch: Boolean,

	/**
	 * If [fromTouch] is true, this is the touch id from which the drag started.
	 */
	val touchId: Int,

	/**
	 * The target dispatching the drag event.
	 */
	val target: UiComponent,

	/**
	 * True if the JS Event is trusted.
	 * @see org.w3c.dom.events.Event.isTrusted
	 */
	val isTrusted: Boolean,

	/**
	 * True if the control key is pressed.
	 */
	val ctrlKey: Boolean,

	/**
	 * True if the shift key is pressed.
	 */
	val shiftKey: Boolean,

	/**
	 * True if the alt key is pressed.
	 */
	val altKey: Boolean,

	/**
	 * True if the windows or command key is pressed.
	 */
	val metaKey: Boolean,

) : Event()

/**
 * The difference between [DragEvent.positionClient] and [DragEvent.previousPositionClient]
 */
val DragEvent.positionClientDelta: Vector2
	get() = positionClient - previousPositionClient

/**
 * The difference between [DragEvent.positionLocal] and [DragEvent.previousPositionLocal]
 */
val DragEvent.positionLocalDelta: Vector2
	get() = positionLocal - previousPositionClient
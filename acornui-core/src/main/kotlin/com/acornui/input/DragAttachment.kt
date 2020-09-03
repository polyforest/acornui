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

@file:Suppress("unused", "UNUSED_PARAMETER")

package com.acornui.input

import com.acornui.*
import com.acornui.collection.find
import com.acornui.collection.first
import com.acornui.component.UiComponent
import com.acornui.component.createOrReuseAttachment
import com.acornui.component.disposeAttachment
import com.acornui.di.ContextImpl
import com.acornui.dom.clientToLocal
import com.acornui.dom.handle
import com.acornui.dom.isFabricated
import com.acornui.dom.isHandled
import com.acornui.function.as2
import com.acornui.math.Vector2
import com.acornui.math.vec2
import com.acornui.signal.*
import com.acornui.time.tick
import org.w3c.dom.TouchEvent
import org.w3c.dom.events.MouseEvent
import kotlinx.browser.window
import org.w3c.dom.events.KeyboardEvent

/**
 * A behavior for a touch down, touch move, then touch up on a target UiComponent.
 */
class DragAttachment(
	val target: UiComponent
) : ContextImpl(target) {

	/**
	 * Dispatched when the drag has begun.
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

	private var clickHandle: Disposable? = null

	private var isWatchingMouse = false
		set(value) {
			if (field == value) return
			field = value
			watchHandle = if (value) {
				tick(-1, callback = ::enterFrameHandler.as2) and
						win.mouseMoved.listen(eventOptions = EventOptions(isPassive = false), handler = ::windowMouseMoveHandler) and
						win.mouseReleased.listen(handler = ::windowMouseUpHandler) and
						win.keyPressed.listen(handler = ::windowKeyDownHandler)
			} else {
				watchHandle?.dispose()
				null
			}
		}

	private var isWatchingTouch = false
		set(value) {
			if (field == value) return
			field = value
			watchHandle = if (value) {
				tick(-1, callback = ::enterFrameHandler.as2) and
						win.touchMoved.listen(EventOptions(isPassive = false), ::windowTouchMoveHandler) and
						win.touchEnded.listen(::windowTouchEndHandler) and
						win.keyPressed.listen(handler = ::windowKeyDownHandler)
			} else {
				watchHandle?.dispose()
				null
			}
		}

	/**
	 * The user is currently dragging.
	 */
	var isDragging: Boolean = false
		private set(value) {
			if (field == value) return
			field = value
			if (value) {
				dispatch(dragStarted)
				if (isDragging) {
					// Set the next click to be marked as isHandled:
					clickHandle = win.clicked.listen(
						EventOptions(isCapture = true, isPassive = false),
						handler = ::clickBlocker
					)
					dispatch(dragged)
				}
			} else {
				if (target.dom.isConnected)
					dispatch(dragged)
				dispatch(dragEnded)

				frame.once { clickHandle?.dispose(); clickHandle = null }
			}
		}

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
	 * @see isDragging
	 */
	val userIsActive: Boolean
		get() = isWatchingMouse || isWatchingTouch

	private var watchHandle: Disposable? = null

	private val win = window.asWithEventTarget()

	private var maxDragDistance = 0.0
	private var startPositionClient = Vector2.ZERO
	private var previousPositionClient = Vector2.ZERO
	private var positionClient = Vector2.ZERO

	private var fromTouch = false
	private var touchId: Int = -1

	/**
	 * True if the drag was cancelled by hitting ESCAPE.
	 */
	private var cancelled = false

	private fun startMouse(positionClient: Vector2, positionLocal: Vector2) {
		start(positionClient)
		fromTouch = false
		touchId = -1
	}

	private fun startTouch(positionClient: Vector2, positionLocal: Vector2, touchId: Int) {
		start(positionClient)
		fromTouch = true
		this.touchId = touchId
	}

	private fun start(positionClient: Vector2) {
		this.startPositionClient = positionClient
		this.previousPositionClient = positionClient
		this.positionClient = positionClient
		cancelled = false
	}

	private fun move(positionClient: Vector2) {
		this.previousPositionClient = this.positionClient
		this.positionClient = positionClient
	}

	private fun windowKeyDownHandler(event: KeyboardEvent) {
		if (event.keyCode == Ascii.ESCAPE) {
			event.handle()
			cancelled = true
			isWatchingMouse = false
			isDragging = false
		}
	}

	//--------------------------------------------------------------
	// Mouse UX
	//--------------------------------------------------------------

	private fun windowMouseMoveHandler(event: MouseEvent) {
		event.handle()
		positionClient = vec2(event.clientX, event.clientY)
		if (preventDefaultOnMouse)
			event.preventDefault()
	}

	private fun mouseDownHandler(event: MouseEvent) {
		if (!isWatchingMouse && !isWatchingTouch && allowMouseStart(event)) {
			startMouse(
				positionClient = vec2(event.clientX, event.clientY),
				positionLocal = vec2(event.clientX, event.clientY)
			)
			isWatchingMouse = true
			event.handle()
			if (preventDefaultOnMouse)
				event.preventDefault()
			isDragging = true
		}
	}

	private fun windowMouseUpHandler(event: MouseEvent) {
		event.handle()
		move(vec2(event.clientX, event.clientY))
		isWatchingMouse = false
		isDragging = false
	}

	/**
	 * Return true if the drag should start watching movement.
	 * This does not determine if a drag start may begin.
	 */
	private fun allowMouseStart(event: MouseEvent): Boolean {
		return isEnabled && !event.isFabricated && event.button == WhichButton.LEFT && !event.isHandled
	}

	//--------------------------------------------------------------
	// Touch UX
	//--------------------------------------------------------------

	private fun move(event: TouchEvent) {
		val touch = event.touches.find { it?.identifier == touchId }
		if (touch != null)
			move(vec2(touch.clientX, touch.clientY))
	}

	private fun touchStartHandler(event: TouchEvent) {
		if (!isWatchingMouse && !isWatchingTouch && allowTouchStart(event)) {
			val t = event.touches.first()
			startTouch(
				positionClient = vec2(t.clientX, t.clientY),
				positionLocal = vec2(t.clientX, t.clientY),
				touchId = t.identifier
			)
			setIsWatchingTouch(true)
			event.handle()
			if (preventDefaultOnTouch)
				event.preventDefault()
			isDragging = true
		}
	}

	/**
	 * Return true if the drag should start watching movement.
	 * This does not determine if a drag start may begin.
	 */
	private fun allowTouchStart(event: TouchEvent): Boolean {
		return isEnabled && !event.isFabricated && !event.isHandled
	}

	private fun allowTouchEnd(event: TouchEvent): Boolean {
		return event.touches.find { it?.identifier == touchId } == null
	}

	private fun setIsWatchingTouch(value: Boolean) {
		if (isWatchingTouch == value) return
		isWatchingTouch = value
	}

	private fun windowTouchMoveHandler(event: TouchEvent) {
		event.handle()
		move(event)
		if (preventDefaultOnTouch)
			event.preventDefault()
	}

	private fun windowTouchEndHandler(event: TouchEvent) {
		if (allowTouchEnd(event)) {
			event.handle()
			isWatchingTouch = false
			isDragging = false
		}
	}

	//--------------------------------------------------------------
	// Drag
	//--------------------------------------------------------------

	private fun enterFrameHandler() {
		if (isDragging) {
			dispatch(dragged)
		} else {
			isDragging = true
		}
	}

	private fun dispatch(signal: MutableSignal<DragEvent>) {
		val e = dragEvent()
		if (cancelled)
			e.preventDefault()
		signal.dispatch(e)
		if (e.defaultPrevented)
			isDragging = false
	}

	private fun dragEvent(): DragEvent = DragEvent(
		startPositionClient,
		previousPositionClient,
		positionClient,
		fromTouch,
		touchId,
		target
	)

	private fun clickBlocker(event: MouseEvent) {
		event.handle()
		event.preventDefault() // Prevent focus change
	}

	/**
	 * If true, drag operations are enabled.
	 */
	var isEnabled: Boolean = true
		set(value) {
			if (field == value) return
			field = value
			if (!value) stop()
		}

	fun stop() {
		isWatchingMouse = false
		isWatchingTouch = false
		isDragging = false
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
		fromTouch = event.fromTouch
		touchId = event.touchId

		if (fromTouch) setIsWatchingTouch(true)
		else isWatchingMouse = true
		isDragging = (true)
	}

	init {
		own(target.mousePressed.listen(EventOptions(isPassive = false), ::mouseDownHandler))
		own(target.touchStarted.listen(EventOptions(isPassive = false), ::touchStartHandler))
	}

	override fun dispose() {
		super.dispose()
		stop()
	}

	companion object
}

class DragEvent(

	/**
	 * The starting position (in page coordinates) for the drag.
	 */
	val startPositionClient: Vector2,

	/**
	 * The position of the last event (in page coordinates).
	 */
	val previousPositionClient: Vector2,

	/**
	 * The current position (in client coordinates).
	 */
	val positionClient: Vector2,

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
	val target: UiComponent

) : Event() {

	/**
	 * The starting position relative to the target element's bounding rectangle.
	 */
	val startPositionLocal: Vector2 by lazy {
		target.clientToLocal(startPositionClient)
	}

	/**
	 * The position relative to the target element's bounding rectangle.
	 */
	val positionLocal: Vector2 by lazy {
		target.clientToLocal(positionClient)
	}

	override fun toString(): String {
		return "DragEvent(startPosition=$startPositionClient, position=$positionClient, isTouch=$fromTouch, touchId=$touchId)"
	}

}

/**
 * Disposes and removes the drag attachment.
 */
fun UiComponent.disposeDragAttachment() {
	disposeAttachment<DragAttachment>(DragAttachment)
}

/**
 * Creates or reuses a drag attachment without changing the affordance.
 */
fun UiComponent.dragAttachment(): DragAttachment = createOrReuseAttachment(DragAttachment) {
	DragAttachment(this)
}

/**
 * @see DragAttachment.dragStarted
 */
val UiComponent.dragStarted: Signal<DragEvent>
	get() = dragAttachment().dragStarted

/**
 * @see DragAttachment.dragged
 */
val UiComponent.dragged: Signal<DragEvent>
	get() = dragAttachment().dragged

/**
 * @see DragAttachment.isDragging
 */
val UiComponent.isDragging: Boolean
	get() = getAttachment<DragAttachment>(DragAttachment)?.isDragging ?: false

/**
 * @see DragAttachment.dragEnded
 */
val UiComponent.dragEnded: Signal<DragEvent>
	get() = dragAttachment().dragEnded

/**
 * A filter for drag events to only fire after they have been dragged a certain distance.
 *
 * @param target The target to watch for acorn drag events.
 * @param affordance The distance, in dips, the drag must surpass before this object's drag signals will fire.
 */
class DragWithAffordance(
	private val target: UiComponent,
	val affordance: Double
) : DisposableBase(target), ManagedDisposable {

	/**
	 * Dispatched when the drag has surpassed the [affordance] distance.
	 */
	val dragStarted = signal<DragEvent>()

	/**
	 * Dispatched when the drag has moved and the drag at some point has surpassed the [affordance] distance.
	 */
	val dragged = signal<DragEvent>()

	/**
	 * Dispatched when the drag has completed.
	 * This may either be from the mouse/touch ending.
	 */
	val dragEnded = signal<DragEvent>()

	private var hasPassedAffordance = false

	init {
		own(target.dragStarted.listen(::dragStartHandler))
		own(target.dragged.listen(::dragHandler))
		own(target.dragEnded.listen(::dragEndHandler))
	}

	private fun dragStartHandler(event: DragEvent) {
		hasPassedAffordance = false
	}

	private fun dragHandler(event: DragEvent) {
		if (!hasPassedAffordance) {
			if (event.positionClient.dst(event.startPositionClient) >= affordance) {
				hasPassedAffordance = true
				dragStarted.dispatch(event)
			}
		}
		if (hasPassedAffordance)
			dragged.dispatch(event)
	}

	private fun dragEndHandler(event: DragEvent) {
		if (hasPassedAffordance)
			dragEnded.dispatch(event)
	}

	companion object {
		const val DEFAULT_AFFORDANCE = 5.0
	}
}

fun UiComponent.dragWithAffordance(affordance: Double = DragWithAffordance.DEFAULT_AFFORDANCE) =
	DragWithAffordance(this, affordance)
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

@file:Suppress("unused")

package com.acornui.input.interaction

import com.acornui.Disposable
import com.acornui.component.*
import com.acornui.di.ContextImpl
import com.acornui.function.as1
import com.acornui.function.as2
import com.acornui.input.*
import com.acornui.math.Vector2
import com.acornui.math.Vector2Ro
import com.acornui.math.vec2
import com.acornui.signal.Signal
import com.acornui.signal.Signal1
import com.acornui.time.callLater
import com.acornui.time.tick

/**
 * A behavior for a touch down, touch move, then touch up on a target UiComponent.
 */
class DragAttachment(
		val target: UiComponentRo,

		/**
		 * The manhattan distance between the start drag position and the current position before dragging will begin.
		 */
		var affordance: Float = DEFAULT_AFFORDANCE
) : ContextImpl(target) {

	private val stage = target.stage
	private val mouse = inject(MouseState)

	private var watchingMouse = false
	private var watchingTouch = false
	private var touchId = -1

	/**
	 * The movement has passed the affordance, and is currently dragging.
	 */
	var isDragging: Boolean = false
		private set

	/**
	 * If true, the touchMove event will have default prevented.
	 * This will prevent drag scrolling on mobile.
	 */
	var preventDefaultOnTouchMove = true

	/**
	 * Returns true if the user is currently interacting.
	 * Note: This will be true even before the movement has passed the [affordance] threshold.
	 * @see isDragging
	 */
	val userIsActive: Boolean
		get() = watchingMouse || watchingTouch

	private val dragEvent: DragEvent = DragEvent()

	private val _dragStart = Signal1<DragEventRo>()

	/**
	 * Dispatched when the drag has passed the [affordance] distance.
	 */
	val dragStart = _dragStart.asRo()

	private val _drag = Signal1<DragEventRo>()

	/**
	 * Dispatched on each frame during a drag.
	 */
	val drag = _drag.asRo()

	private val _dragEnd = Signal1<DragEventRo>()

	/**
	 * Dispatched when the drag has completed.
	 * This may either be from the mouse/touch ending, or the target deactivating.
	 */
	val dragEnd = _dragEnd.asRo()

	private val previousPosition = vec2()
	private val position = vec2()
	private val startPosition = vec2()
	private val startPositionLocal = vec2()
	private var isTouch: Boolean = false
	private var enterFrameHandle: Disposable? = null

	private fun targetDeactivatedHandler() {
		stop()
	}

	private fun setIsWatchingMouse(value: Boolean) {
		if (watchingMouse == value) return
		watchingMouse = value
		if (value) {
			enterFrameHandle = tick(-1, callback = ::enterFrameHandler.as2)
			stage.mouseMove().add(::stageMouseMoveHandler)
			stage.mouseUp().add(::stageMouseUpHandler)
		} else {
			enterFrameHandle?.dispose()
			enterFrameHandle = null
			stage.mouseMove().remove(::stageMouseMoveHandler)
			stage.mouseUp().remove(::stageMouseUpHandler)
		}
	}

	private fun stageMouseMoveHandler(event: MouseEventRo) {
		event.handled = true
	}

	//--------------------------------------------------------------
	// Mouse UX
	//--------------------------------------------------------------

	private fun mouseDownHandler(event: MouseEventRo) {
		if (!watchingMouse && !watchingTouch && allowMouseStart(event)) {
			isTouch = false
			touchId = -1
			setIsWatchingMouse(true)
			event.handled = true
			startPosition.set(event.canvasX, event.canvasY)
			previousPosition.set(startPosition)
			position.set(startPosition)
			startPositionLocal.set(event.localX, event.localY)
			if (allowMouseDragStart()) {
				setIsDragging(true)
			}
		}
	}

	private fun stageMouseUpHandler(event: MouseEventRo) {
		event.handled = true
		position.set(event.canvasX, event.canvasY)
		setIsWatchingMouse(false)
		setIsDragging(false)
	}

	/**
	 * Return true if the drag should start watching movement.
	 * This does not determine if a drag start may begin.
	 * @see allowMouseDragStart
	 */
	private fun allowMouseStart(event: MouseEventRo): Boolean {
		return enabled && !event.isFabricated && event.button == WhichButton.LEFT && !event.handled
	}

	private fun allowMouseDragStart(): Boolean {
		return position.manhattanDst(startPosition) >= affordance
	}

	//--------------------------------------------------------------
	// Touch UX
	//--------------------------------------------------------------

	private fun touchStartHandler(event: TouchEventRo) {
		if (!watchingMouse && !watchingTouch && allowTouchStart(event)) {
			isTouch = true
			setIsWatchingTouch(true)
			event.handled = true
			val t = event.touches.first()
			touchId = t.identifier
			startPosition.set(t.canvasX, t.canvasY)
			position.set(startPosition)
			previousPosition.set(startPosition)
			startPositionLocal.set(t.localX, t.localY)
			if (allowTouchDragStart()) {
				setIsDragging(true)
			}
		}
	}

	/**
	 * Return true if the drag should start watching movement.
	 * This does not determine if a drag start may begin.
	 * @see allowTouchDragStart
	 */
	private fun allowTouchStart(event: TouchEventRo): Boolean {
		return enabled && !event.handled
	}

	private fun allowTouchDragStart(): Boolean {
		return position.manhattanDst(startPosition) >= affordance
	}

	private fun allowTouchEnd(event: TouchEventRo): Boolean {
		return event.touches.find { it.identifier == touchId } == null
	}

	private fun setIsWatchingTouch(value: Boolean) {
		if (watchingTouch == value) return
		watchingTouch = value
		if (value) {
			enterFrameHandle = tick(-1, callback = ::enterFrameHandler.as2)
			stage.touchMove().add(::stageTouchMoveHandler)
			stage.touchEnd().add(::stageTouchEndHandler)
		} else {
			enterFrameHandle?.dispose()
			enterFrameHandle = null
			stage.touchMove().remove(::stageTouchMoveHandler)
			stage.touchEnd().remove(::stageTouchEndHandler)
		}
	}

	private fun stageTouchMoveHandler(event: TouchEventRo) {
		event.handled = true
		if (preventDefaultOnTouchMove)
			event.preventDefault()
	}

	private fun stageTouchEndHandler(event: TouchEventRo) {
		if (allowTouchEnd(event)) {
			touchId = -1
			event.handled = true
			val firstTouch = event.touches.firstOrNull()
			if (firstTouch != null)
				position.set(firstTouch.canvasX, firstTouch.canvasY)
			setIsWatchingTouch(false)
			setIsDragging(false)
		}
	}

	//--------------------------------------------------------------
	// Drag
	//--------------------------------------------------------------

	private fun enterFrameHandler() {
		if (watchingTouch)
			position.set(mouse.touchX, mouse.touchY)
		else if (watchingMouse)
			position.set(mouse.mouseX, mouse.mouseY)
		if (isDragging) {
			dispatchDragEvent(DragEvent.DRAG, _drag)
		} else {
			if (!isDragging && allowMouseDragStart()) {
				setIsDragging(true)
			}
		}
	}

	private fun setIsDragging(value: Boolean) {
		if (isDragging == value) return
		isDragging = value
		if (value) {
			dispatchDragEvent(DragEvent.DRAG_START, _dragStart)
			if (dragEvent.defaultPrevented()) {
				isDragging = false
			} else {
				stage.click(isCapture = true).add(::clickBlocker, true) // Set the next click to be marked as handled.
				dispatchDragEvent(DragEvent.DRAG, _drag)
			}
		} else {
			if (target.isActive) {
				dispatchDragEvent(DragEvent.DRAG, _drag)
			}
			dispatchDragEvent(DragEvent.DRAG_END, _dragEnd)

			callLater { stage.click(isCapture = true).remove(::clickBlocker) }
		}
	}

	private fun clickBlocker(event: ClickEventRo) {
		event.handled = true
	}

	private fun dispatchDragEvent(type: EventType<DragEventRo>, signal: Signal1<DragEventRo>) {
		dragEvent.clear()
		dragEvent.target = target
		dragEvent.currentTarget = target
		dragEvent.type = type
		dragEvent.startPosition.set(startPosition)
		dragEvent.startPositionLocal.set(startPositionLocal)
		dragEvent.position.set(position)
		dragEvent.previousPosition.set(previousPosition)
		dragEvent.fromTouch = isTouch
		dragEvent.touchId = touchId
		previousPosition.set(position)
		signal.dispatch(dragEvent)
	}

	private var _enabled = true

	/**
	 * If true, drag operations are enabled.
	 */
	var enabled: Boolean
		get() = _enabled
		set(value) {
			if (_enabled == value) return
			_enabled = value
			if (!value) stop()
		}

	fun stop() {
		setIsWatchingMouse(false)
		setIsWatchingTouch(false)
		setIsDragging(false)
	}

	/**
	 * Forces the drag operation to begin.
	 * This can be a way to transfer a drag operation from one component to another.
	 */
	fun start(event: DragEventRo) {
		stop()
		startPosition.set(event.startPosition)
		startPositionLocal.set(event.startPositionLocal)
		previousPosition.set(event.previousPosition)
		position.set(event.position)
		isTouch = event.fromTouch
		touchId = event.touchId
		if (isTouch) setIsWatchingTouch(true)
		else setIsWatchingMouse(true)
		setIsDragging(true)
	}

	init {
		target.deactivated.add(::targetDeactivatedHandler.as1)
		target.mouseDown().add(::mouseDownHandler)
		target.touchStart().add(::touchStartHandler)
	}

	override fun dispose() {
		super.dispose()
		stop()
		_dragStart.dispose()
		_drag.dispose()
		_dragEnd.dispose()

		target.deactivated.remove(::targetDeactivatedHandler.as1)
		target.mouseDown().remove(::mouseDownHandler)
		target.touchStart().remove(::touchStartHandler)
	}

	companion object {

		/**
		 * The manhattan distance the target must be dragged before the dragStart and drag events begin.
		 */
		const val DEFAULT_AFFORDANCE: Float = 5f

	}
}

interface DragEventRo : EventRo {

	/**
	 * The starting position (in canvas coordinates) for the drag.
	 */
	val startPosition: Vector2Ro

	/**
	 * The starting position relative to the [target] element.
	 */
	val startPositionLocal: Vector2Ro

	/**
	 * The position of the last event (in canvas coordinates).
	 */
	val previousPosition: Vector2Ro

	/**
	 * The current position (in canvas coordinates).
	 */
	val position: Vector2Ro

	/**
	 * The current position, relative to the [target] element.
	 */
	val positionLocal: Vector2Ro

	/**
	 * The position change from the last event, relative to the [target] element.
	 */
	val positionLocalDelta: Vector2Ro

	/**
	 * True if initialized from a touch interaction, false if mouse.
	 */
	val fromTouch: Boolean

	/**
	 * If [fromTouch] is true, this is the touch id the drag started from.
	 */
	val touchId: Int
}

class DragEvent : EventBase(), DragEventRo {

	override val startPosition: Vector2 = vec2()

	override val startPositionLocal: Vector2 = vec2()

	override val previousPosition: Vector2 = vec2()
	override val position: Vector2 = vec2()

	private val _previousPositionLocal = vec2()
	private val _positionLocal = vec2()
	override val positionLocal: Vector2
		get() {
			validate()
			return _positionLocal
		}

	private val _positionLocalDelta = vec2()
	override val positionLocalDelta: Vector2Ro
		get() {
			validate()
			return _positionLocalDelta
		}

	override var fromTouch: Boolean = false
	override var touchId: Int = -1

	private var isValid = false

	override fun clear() {
		super.clear()
		startPosition.clear()
		previousPosition.clear()
		position.clear()
		fromTouch = false
		touchId = -1
		isValid = false
	}

	fun validate() {
		if (isValid) return
		isValid = true
		currentTarget.canvasToLocal(_positionLocal.set(position))
		currentTarget.canvasToLocal(_previousPositionLocal.set(previousPosition))
		_positionLocalDelta.set(_positionLocal).sub(_previousPositionLocal)
	}

	override fun toString(): String {
		return "DragInteraction(startPosition=$startPosition, position=$position, isTouch=$fromTouch, touchId=$touchId)"
	}


	companion object {
		val DRAG_START = EventType<DragEventRo>("dragStart")
		val DRAG = EventType<DragEventRo>("drag")
		val DRAG_END = EventType<DragEventRo>("dragEnd")
	}

}

/**
 * Disposes and removes the drag attachment with the given affordance.
 */
@Deprecated("Use disposeAttachment", ReplaceWith("disposeAttachment<DragAttachment>(key)"))
fun UiComponentRo.clearDragAttachment(key: Any = DragAttachment) {
	disposeAttachment<DragAttachment>(key)
}

/**
 * Creates or reuses a drag attachment, setting its affordance.
 * @param affordance The distance the mouse or touch must move before being considered a drag.
 * @param key The key for the attachment. If two drag behaviors with different affordances are desired, they must have
 * separate keys.
 * @see DragAttachment.affordance
 */
fun UiComponentRo.dragAttachment(affordance: Float, key: Any = DragAttachment): DragAttachment {
	return createOrReuseAttachment(key) { DragAttachment(this, affordance) }.also { it.affordance = affordance }
}

/**
 * Creates or reuses a drag attachment without changing the affordance.
 */
fun UiComponentRo.dragAttachment(key: Any = DragAttachment): DragAttachment {
	return createOrReuseAttachment(key) { DragAttachment(this, DragAttachment.DEFAULT_AFFORDANCE) }
}

/**
 * @see DragAttachment.dragStart
 */
fun UiComponentRo.dragStart(): Signal<(DragEventRo) -> Unit> {
	return dragAttachment().dragStart
}

/**
 * @see DragAttachment.drag
 */
fun UiComponentRo.drag(): Signal<(DragEventRo) -> Unit> {
	return dragAttachment().drag
}

/**
 * @see DragAttachment.dragEnd
 */
fun UiComponentRo.dragEnd(): Signal<(DragEventRo) -> Unit> {
	return dragAttachment().dragEnd
}

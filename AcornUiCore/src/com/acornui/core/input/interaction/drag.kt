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

import com.acornui.component.InteractiveElementRo
import com.acornui.component.UiComponentRo
import com.acornui.component.createOrReuseAttachment
import com.acornui.component.stage
import com.acornui.core.Disposable
import com.acornui.core.LifecycleRo
import com.acornui.core.di.inject
import com.acornui.core.input.*
import com.acornui.core.time.TimeDriver
import com.acornui.core.time.callLater
import com.acornui.core.time.enterFrame
import com.acornui.math.Vector2
import com.acornui.math.Vector2Ro
import com.acornui.signal.Signal
import com.acornui.signal.Signal1

/**
 * A behavior for a touch down, touch move, then touch up on a target UiComponent.
 */
class DragAttachment(
		val target: UiComponentRo,

		/**
		 * The manhattan distance between the start drag position and the current position before dragging will begin.
		 */
		var affordance: Float = DEFAULT_AFFORDANCE
) : Disposable {

	private val stage = target.stage
	private val mouse = target.inject(MouseState)
	private val timeDriver = target.inject(TimeDriver)

	private var watchingMouse = false
	private var watchingTouch = false
	private var touchId = -1

	private var _isDragging = false

	/**
	 * The movement has passed the affordance, and is currently dragging.
	 */
	val isDragging: Boolean
		get() = _isDragging

	private val dragEvent: DragInteraction = DragInteraction()

	private val _dragStart = Signal1<DragInteraction>()

	/**
	 * Dispatched when the drag has passed the [affordance] distance.
	 */
	val dragStart: Signal<(DragInteractionRo) -> Unit>
		get() = _dragStart

	private val _drag = Signal1<DragInteraction>()

	/**
	 * Dispatched on each move during a drag.
	 * This will not be dispatched if the target is not on the stage.
	 */
	val drag: Signal<(DragInteractionRo) -> Unit>
		get() = _drag

	private val _dragEnd = Signal1<DragInteraction>()

	/**
	 * Dispatched when the drag has completed.
	 */
	val dragEnd: Signal<(DragInteractionRo) -> Unit>
		get() = _dragEnd

	private val position = Vector2()
	private val startPosition = Vector2()
	private val startPositionLocal = Vector2()
	private var startElement: InteractiveElementRo? = null
	private var isTouch: Boolean = false
	private var _enterFrame: Disposable? = null

	private fun targetDeactivatedHandler(c: LifecycleRo) {
		stop()
	}

	private val clickBlocker = { event: ClickInteractionRo ->
		event.handled = true
		event.preventDefault()
	}

	private fun setIsWatchingMouse(value: Boolean) {
		if (watchingMouse == value) return
		watchingMouse = value
		if (value) {
			_enterFrame = enterFrame(timeDriver, -1, this::enterFrameHandler)
			stage.mouseMove().add(this::stageMouseMoveHandler)
			stage.mouseUp().add(this::stageMouseUpHandler)
		} else {
			_enterFrame?.dispose()
			_enterFrame = null
			stage.mouseMove().remove(this::stageMouseMoveHandler)
			stage.mouseUp().remove(this::stageMouseUpHandler)
		}
	}

	private fun stageMouseMoveHandler(event: MouseInteractionRo) {
		event.handled = true
		event.preventDefault()
	}

	//--------------------------------------------------------------
	// Mouse UX
	//--------------------------------------------------------------

	private fun mouseDownHandler(event: MouseInteractionRo) {
		if (!watchingMouse && !watchingTouch && allowMouseStart(event)) {
			isTouch = false
			touchId = -1
			setIsWatchingMouse(true)
			event.handled = true
			startElement = event.target
			startPosition.set(event.canvasX, event.canvasY)
			position.set(startPosition)
			startPositionLocal.set(event.localX, event.localY)
			if (!_isDragging && allowMouseDragStart()) {
				setIsDragging(true)
			}
		}
	}

	private fun stageMouseUpHandler(event: MouseInteractionRo) {
		event.handled = true
		setIsWatchingMouse(false)
		setIsDragging(false)
	}

	/**
	 * Return true if the drag should start watching movement.
	 * This does not determine if a drag start may begin.
	 * @see allowMouseDragStart
	 */
	private fun allowMouseStart(event: MouseInteractionRo): Boolean {
		return enabled && !event.isFabricated && event.button == WhichButton.LEFT && !event.handled
	}

	private fun allowMouseDragStart(): Boolean {
		return position.manhattanDst(startPosition) >= affordance
	}

	//--------------------------------------------------------------
	// Touch UX
	//--------------------------------------------------------------

	private fun touchStartHandler(event: TouchInteractionRo) {
		if (!watchingMouse && !watchingTouch && allowTouchStart(event)) {
			isTouch = true
			setIsWatchingTouch(true)
			event.handled = true
			startElement = event.target
			val t = event.touches.first()
			touchId = t.identifier
			startPosition.set(t.canvasX, t.canvasY)
			position.set(startPosition)
			startPositionLocal.set(t.localX, t.localY)
			if (!_isDragging && allowTouchDragStart(event)) {
				setIsDragging(true)
			}
		}
	}

	/**
	 * Return true if the drag should start watching movement.
	 * This does not determine if a drag start may begin.
	 * @see allowTouchDragStart
	 */
	private fun allowTouchStart(event: TouchInteractionRo): Boolean {
		return enabled && !event.handled
	}

	private fun allowTouchDragStart(event: TouchInteractionRo): Boolean {
		return position.manhattanDst(startPosition) >= affordance
	}

	private fun allowTouchEnd(event: TouchInteractionRo): Boolean {
		return event.touches.find { it.identifier == touchId } == null
	}

	private fun setIsWatchingTouch(value: Boolean) {
		if (watchingTouch == value) return
		watchingTouch = value
		if (value) {
			_enterFrame = enterFrame(timeDriver, -1, this::enterFrameHandler)
			stage.touchMove().add(this::stageTouchMoveHandler)
			stage.touchEnd().add(this::stageTouchEndHandler)
		} else {
			_enterFrame?.dispose()
			_enterFrame = null
			stage.touchMove().remove(this::stageTouchMoveHandler)
			stage.touchEnd().remove(this::stageTouchEndHandler)
		}
	}

	private fun stageTouchMoveHandler(event: TouchInteractionRo) {
		event.handled = true
		event.preventDefault()
	}

	private fun stageTouchEndHandler(event: TouchInteractionRo) {
		if (allowTouchEnd(event)) {
			touchId = -1
			event.handled = true
			setIsWatchingTouch(false)
			setIsDragging(false)
		}
		Unit
	}

	//--------------------------------------------------------------
	// Drag
	//--------------------------------------------------------------

	private fun enterFrameHandler() {
		mouse.mousePosition(position)
		if (_isDragging) {
			dispatchDragEvent(DragInteraction.DRAG, _drag)
		} else {
			if (!_isDragging && allowMouseDragStart()) {
				setIsDragging(true)
			}
		}
	}

	private fun setIsDragging(value: Boolean) {
		if (_isDragging == value) return
		_isDragging = value
		if (value) {
			dispatchDragEvent(DragInteraction.DRAG_START, _dragStart)
			if (dragEvent.defaultPrevented()) {
				_isDragging = false
			} else {
				stage.click(isCapture = true).add(clickBlocker, true) // Set the next click to be marked as handled.
				dispatchDragEvent(DragInteraction.DRAG, _drag)
			}
		} else {
			if (target.isActive) {
				dispatchDragEvent(DragInteraction.DRAG, _drag)
			}
			dispatchDragEvent(DragInteraction.DRAG_END, _dragEnd)
			startElement = null

			target.callLater { stage.click(isCapture = true).remove(clickBlocker) }
		}
	}

	private fun dispatchDragEvent(type: InteractionType<DragInteractionRo>, signal: Signal1<DragInteraction>) {
		dragEvent.clear()
		dragEvent.target = target
		dragEvent.currentTarget = target
		dragEvent.type = type
		dragEvent.startElement = startElement
		dragEvent.startPosition.set(startPosition)
		dragEvent.startPositionLocal.set(startPositionLocal)
		dragEvent.position.set(position)
		dragEvent.isTouch = isTouch
		dragEvent.touchId = touchId
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
	fun start(event: DragInteractionRo) {
		stop()
		startElement = event.startElement
		startPosition.set(event.startPosition)
		startPositionLocal.set(event.startPositionLocal)
		position.set(event.position)
		isTouch = event.isTouch
		touchId = event.touchId
		if (isTouch) setIsWatchingTouch(true)
		else setIsWatchingMouse(true)
		setIsDragging(true)
	}

	init {
		target.deactivated.add(this::targetDeactivatedHandler)
		target.mouseDown().add(this::mouseDownHandler)
		target.touchStart().add(this::touchStartHandler)
	}

	override fun dispose() {
		stop()
		_dragStart.dispose()
		_drag.dispose()
		_dragEnd.dispose()

		target.deactivated.remove(this::targetDeactivatedHandler)
		target.mouseDown().remove(this::mouseDownHandler)
		target.touchStart().remove(this::touchStartHandler)
	}

	companion object {

		/**
		 * The manhattan distance the target must be dragged before the dragStart and drag events begin.
		 */
		val DEFAULT_AFFORDANCE: Float = 5f

	}
}

interface DragInteractionRo : InteractionEventRo {

	val startElement: InteractiveElementRo?

	/**
	 * The starting position (in canvas coordinates) for the drag.
	 */
	val startPosition: Vector2Ro

	/**
	 * The starting position relative to the startElement for the drag.
	 */
	val startPositionLocal: Vector2Ro

	/**
	 * The current position (in canvas coordinates).
	 */
	val position: Vector2Ro

	/**
	 * The current position, local to the target element.
	 * Note that this value is calculated, and not cached.
	 */
	val positionLocal: Vector2Ro

	/**
	 * True if initialized from a touch interaction, false if mouse.
	 */
	val isTouch: Boolean

	/**
	 * If [isTouch] is true, this is the touch id the drag started from.
	 */
	val touchId: Int
}

class DragInteraction : InteractionEventBase(), DragInteractionRo {

	override var startElement: InteractiveElementRo? = null

	override val startPosition: Vector2 = Vector2()

	override val startPositionLocal: Vector2 = Vector2()

	override val position: Vector2 = Vector2()

	private val _positionLocal = Vector2()

	override val positionLocal: Vector2
		get() {
			return currentTarget.canvasToLocal(_positionLocal.set(position))
		}

	override var isTouch: Boolean = false
	override var touchId: Int = -1

	override fun clear() {
		super.clear()
		startPosition.clear()
		position.clear()
		startElement = null
		isTouch = false
		touchId = -1
	}

	companion object {
		val DRAG_START = InteractionType<DragInteractionRo>("dragStart")
		val DRAG = InteractionType<DragInteractionRo>("drag")
		val DRAG_END = InteractionType<DragInteractionRo>("dragEnd")
	}

}

fun UiComponentRo.dragAttachment(affordance: Float = DragAttachment.DEFAULT_AFFORDANCE): DragAttachment {
	return createOrReuseAttachment(DragAttachment) { DragAttachment(this, affordance) }
}

/**
 * @see DragAttachment.dragStart
 */
fun UiComponentRo.dragStart(): Signal<(DragInteractionRo) -> Unit> {
	return dragAttachment().dragStart
}

/**
 * @see DragAttachment.drag
 */
fun UiComponentRo.drag(): Signal<(DragInteractionRo) -> Unit> {
	return dragAttachment().drag
}

/**
 * @see DragAttachment.dragEnd
 */
fun UiComponentRo.dragEnd(): Signal<(DragInteractionRo) -> Unit> {
	return dragAttachment().dragEnd
}
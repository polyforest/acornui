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
import com.acornui.component.UiComponent
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
import com.acornui.signal.Signal
import com.acornui.signal.Signal1

/**
 * A behavior for a touch down, touch move, then touch up on a target UiComponent.
 */
class DragAttachment(
		val target: UiComponent,

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
	val dragStart: Signal<(DragInteraction) -> Unit>
		get() = _dragStart

	private val _drag = Signal1<DragInteraction>()

	/**
	 * Dispatched on each move during a drag.
	 * This will not be dispatched if the target is not on the stage.
	 */
	val drag: Signal<(DragInteraction) -> Unit>
		get() = _drag

	private val _dragEnd = Signal1<DragInteraction>()

	/**
	 * Dispatched when the drag has completed.
	 */
	val dragEnd: Signal<(DragInteraction) -> Unit>
		get() = _dragEnd

	private val position = Vector2()
	private val startPosition = Vector2()
	private val startPositionLocal = Vector2()
	private var startElement: InteractiveElementRo? = null
	private var _enterFrame: Disposable? = null

	private fun targetDeactivatedHandler(c: LifecycleRo) {
		stop()
	}

	private val clickBlocker: (ClickInteraction)->Unit = {
		event ->
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

	private fun stageMouseMoveHandler(event: MouseInteraction) {
		event.handled = true
		event.preventDefault()
	}

	//--------------------------------------------------------------
	// Mouse UX
	//--------------------------------------------------------------

	private fun mouseDownHandler(event: MouseInteraction) {
		if (!watchingMouse && !watchingTouch && allowMouseStart(event)) {
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

	private fun stageMouseUpHandler(event: MouseInteraction) {
		event.handled = true
		setIsWatchingMouse(false)
		setIsDragging(false)
		Unit
	}

	/**
	 * Return true if the drag should start watching movement.
	 * This does not determine if a drag start may begin.
	 * @see allowMouseDragStart
	 */
	private fun allowMouseStart(event: MouseInteraction): Boolean {
		return enabled && !event.isFabricated && event.button == WhichButton.LEFT && !event.handled
	}

	private fun allowMouseDragStart(): Boolean {
		return position.manhattanDst(startPosition) >= affordance
	}

	//--------------------------------------------------------------
	// Touch UX
	//--------------------------------------------------------------

	private fun touchStartHandler(event: TouchInteraction) {
		if (!watchingMouse && !watchingTouch && allowTouchStart(event)) {
			setWatchingTouch(true)
			event.handled = true
			startElement = event.target
			val t = event.touches.first()
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
	private fun allowTouchStart(event: TouchInteraction): Boolean {
		return enabled && !event.handled
	}

	private fun allowTouchDragStart(event: TouchInteraction): Boolean {
		return position.manhattanDst(startPosition) >= affordance
	}

	private fun allowTouchEnd(event: TouchInteraction): Boolean {
		return event.touches.isEmpty()
	}

	private fun setWatchingTouch(value: Boolean) {
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

	private fun stageTouchMoveHandler(event: TouchInteraction) {
		event.handled = true
		event.preventDefault()
	}

	private fun stageTouchEndHandler(event: TouchInteraction) {
		if (allowTouchEnd(event)) {
			event.handled = true
			setWatchingTouch(false)
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

	private fun dispatchDragEvent(type: InteractionType<DragInteraction>, signal: Signal1<DragInteraction>) {
		dragEvent.clear()
		dragEvent.target = target
		dragEvent.currentTarget = target
		dragEvent.type = type
		dragEvent.startElement = startElement
		dragEvent.startPosition.set(startPosition)
		dragEvent.startPositionLocal.set(startPositionLocal)
		dragEvent.position.set(position)
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
		setWatchingTouch(false)
		setIsDragging(false)
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

class DragInteraction : InteractionEventBase() {

	var startElement: InteractiveElementRo? = null

	/**
	 * The starting position (in window coordinates) for the drag.
	 */
	val startPosition: Vector2 = Vector2()

	/**
	 * The starting position relative to the startElement for the drag.
	 */
	val startPositionLocal: Vector2 = Vector2()

	/**
	 * The current position (in window coordinates).
	 */
	val position: Vector2 = Vector2()

	private val _positionLocal = Vector2()

	/**
	 * The current position, local to the target element.
	 * Note that this value is calculated, and not cached.
	 */
	val positionLocal: Vector2
		get() {
			return currentTarget!!.windowToLocal(_positionLocal.set(position))
		}

	override fun clear() {
		super.clear()
		startPosition.clear()
		position.clear()
		startElement = null
	}

	companion object {
		val DRAG_START = InteractionType<DragInteraction>("dragStart")
		val DRAG = InteractionType<DragInteraction>("drag")
		val DRAG_END = InteractionType<DragInteraction>("dragEnd")
	}

}

fun UiComponent.dragAttachment(affordance: Float = DragAttachment.DEFAULT_AFFORDANCE): DragAttachment {
	return createOrReuseAttachment(DragAttachment, { DragAttachment(this, affordance) })
}

/**
 * @see DragAttachment.dragStart
 */
fun UiComponent.dragStart(): Signal<(DragInteraction) -> Unit> {
	return dragAttachment().dragStart
}

/**
 * @see DragAttachment.drag
 */
fun UiComponent.drag(): Signal<(DragInteraction) -> Unit> {
	return dragAttachment().drag
}

/**
 * @see DragAttachment.dragEnd
 */
fun UiComponent.dragEnd(): Signal<(DragInteraction) -> Unit> {
	return dragAttachment().dragEnd
}
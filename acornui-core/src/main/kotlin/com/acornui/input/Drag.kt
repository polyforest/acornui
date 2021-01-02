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
import com.acornui.component.UiComponent
import com.acornui.di.ContextImpl
import com.acornui.dom.handle
import com.acornui.dom.isHandled
import com.acornui.frame
import com.acornui.own
import com.acornui.properties.afterChange
import com.acornui.signal.EventOptions
import com.acornui.signal.asWithEventTarget
import com.acornui.signal.once
import com.acornui.signal.signal
import kotlinx.browser.window
import org.w3c.dom.CustomEvent
import org.w3c.dom.events.KeyboardEvent

/**
 * Drag behavior for a target component.
 * Supports mobile, drag affordance, cancellation, and click prevention.
 *
 * Each instance of `Drag` will respond to one drag sequence at a time. That is, if a drag operation started via
 * left mouse button, and then a touch interaction begins before the mouse ends, the touch drag sequence will be
 * ignored. This means that after a [dragStarted] event, a [dragEnded] event is guaranteed to be called before the next
 * [dragStarted].
 *
 * This watches and decorates drag events dispatched by the [DragManager]. This is unrelated to the native dom
 * [org.w3c.dom.DragEvent].
 */
open class Drag(
	val target: UiComponent
) : ContextImpl(target) {

	/**
	 * Dispatched when the drag is about to begin, and may be prevented.
	 * Calling [Event.preventDefault] on this drag event will prevent this drag behavior from continuing.
	 * It will not, however, prevent the drag event from [DragManager].
	 *
	 * This will only dispatch after the drag has passed [affordance] pixels.
	 */
	val dragStarting = signal<DragEvent>()

	/**
	 * Dispatched when the drag has begun.
	 * This will only dispatch after the drag has passed [affordance] pixels.
	 * A [dragged] event will be dispatched immediately after drag started.
	 */
	val dragStarted = signal<DragEvent>()

	/**
	 * Dispatched during a drag.
	 * This will be dispatched on a [DragData.DRAG] event.
	 */
	val dragged = signal<DragEvent>()

	/**
	 * Dispatched when the drag has completed.
	 * This may either be from the mouse/touch ending or by cancelling via ESCAPE.
	 *
	 * If the drag is cancelled via ESCAPE, [DragEvent.defaultPrevented] will be true.
	 */
	val dragEnded = signal<DragEvent>()

	/**
	 * The distance the user must drag before the drag action has started.
	 */
	var affordance = DEFAULT_AFFORDANCE

	private var clickHandle: Disposable? = null

	private var lastDragEvent: CustomEvent? = null

	private var pendingDragId: String? = null

	/**
	 * This Drag instance tracks one drag operation at a time.
	 */
	private val dragId: String?
		get() = lastDragEvent?.dragId

	/**
	 * The user is currently dragging and past the [affordance] distance.
	 */
	var isDragging: Boolean = false
		private set

	/**
	 * Returns true if the user is currently interacting.
	 * This is similar to [isDragging] except will return true if the user has not passed [affordance].
	 */
	val userIsActive: Boolean
		get() = dragId != null

	/**
	 * True if the drag was cancelled by hitting ESCAPE.
	 */
	private var userCancelled = false

	/**
	 * A filter that returns true if the [com.acornui.input.dragStarting] event from the [DragManager] should initiate
	 * this drag behavior.
	 *
	 * Drag events passing this filter will be marked as handled, thus indicating to the [DragManager] to begin
	 * dispatching drag events for this touch or mouse button.
	 *
	 * This by default only allows drag operations from events originating from left click or a touches that haven't
	 * been handled by a child element.
	 */
	var startFilter: (event: CustomEvent, dragData: DragData) -> Boolean = { event, dragData ->
		!event.isHandled && dragData.button == WhichButton.LEFT || dragData.button == WhichButton.UNKNOWN
	}

	private val win = window.asWithEventTarget()

	private fun windowKeyDownHandler(event: KeyboardEvent) {
		if (event.keyCode == Ascii.ESCAPE) {
			event.handle()
			userCancelled = true
			stop()
		}
	}


	//--------------------------------------------------------------
	// Drag
	//--------------------------------------------------------------

	/**
	 * If true, drag operations are enabled.
	 */
	var isEnabled: Boolean by afterChange(true) {
		if (!it) stop()
	}

	init {
		target.inject(DragManager) // Ensure the drag manager is initialized.
		own(target.dragStarting.listen(::dragStartingHandler))
		own(target.dragStarted.listen(::dragStartedHandler))
		own(target.dragged.listen(::draggedHandler))
		own(target.dragEnded.listen(::dragEndedHandler))
	}

	private fun dragStartingHandler(event: CustomEvent) {
		if (!isEnabled || lastDragEvent != null) return
		if (startFilter(event, event.dragData)) {
			event.acceptDrag()
			pendingDragId = event.dragId
		}
	}

	private fun dragStartedHandler(event: CustomEvent) {
		if (lastDragEvent != null || event.dragId != pendingDragId) return
		lastDragEvent = event
		userCancelled = false
	}

	private fun draggedHandler(event: CustomEvent) {
		if (event.dragId != dragId) return
		lastDragEvent = event
		if (!isDragging) {
			// Check if we've passed the affordance.
			if (event.dragData.maxDistance >= affordance) {
				val startingEvent = dragEvent(event, cancellable = true)
				dragStarting.dispatch(startingEvent)
				if (startingEvent.defaultPrevented) {
					lastDragEvent = null
				} else {
					isDragging = true
					dragStarted.dispatch(dragEvent(event))
					onDrag()
				}
			}
		} else {
			onDrag()
		}
	}

	private fun onDrag() {
		val lastDragEvent = lastDragEvent ?: return
		if (!target.isConnected || !isDragging) return
		dragged.dispatch(dragEvent(lastDragEvent))
	}

	private fun dragEndedHandler(event: CustomEvent) {
		if (event.dragId != dragId) return
		stop()
	}

	/**
	 * Stops the current drag operation if there is one.
	 * If a drag has started [isDragging], a [dragEnded] event will be dispatched.
	 */
	fun stop(cancelled: Boolean = false) {
		val lastDragEvent = lastDragEvent ?: return
		this.lastDragEvent = null
		if (!isDragging) return
		isDragging = false
		val e = dragEvent(lastDragEvent, cancellable = true)
		if (cancelled || userCancelled)
			e.preventDefault()
		dragEnded.dispatch(e)

		// Set any clicks within the next frame to be marked as handled and cancelled:
		clickHandle = win.clicked.listen(
			EventOptions(isCapture = true, isPassive = false)
		) { event ->
			event.handle()
			event.preventDefault() // Prevent focus change
		}
		frame.once { clickHandle?.dispose(); clickHandle = null }
	}

	override fun dispose() {
		stop()
		super.dispose()
	}

	private val CustomEvent.dragData: DragData
		get() = detail.unsafeCast<DragData>()

	private val CustomEvent.dragId: String
		get() = detail.unsafeCast<DragData>().dragId

	private fun dragEvent(e: CustomEvent, cancellable: Boolean = false): DragEvent {
		val dragData = e.dragData
		return DragEvent(
			cancellable = cancellable,
			dragData = dragData
		)
	}

	companion object {
		const val DEFAULT_AFFORDANCE = 5.0
	}
}

class DragEvent(

	/**
	 * True if the default may be prevented.
	 */
	cancellable: Boolean,

	val dragData: DragData

	) : Event(cancellable)

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

package com.acornui.input

import com.acornui.Disposable
import com.acornui.UidUtil
import com.acornui.collection.forEach
import com.acornui.di.Context
import com.acornui.di.ContextImpl
import com.acornui.di.dependencyFactory
import com.acornui.dom.clientToLocal
import com.acornui.dom.handle
import com.acornui.dom.isHandled
import com.acornui.frame
import com.acornui.function.as1
import com.acornui.input.DragData.Companion.DRAG
import com.acornui.input.DragData.Companion.DRAG_END
import com.acornui.input.DragData.Companion.DRAG_ENTER
import com.acornui.input.DragData.Companion.DRAG_EXIT
import com.acornui.input.DragData.Companion.DRAG_OVER
import com.acornui.input.DragData.Companion.DRAG_START
import com.acornui.input.DragData.Companion.DRAG_STARTING
import com.acornui.input.DragData.Companion.DROP
import com.acornui.math.Vector2
import com.acornui.math.vec2
import com.acornui.own
import com.acornui.signal.EventOptions
import com.acornui.signal.WithEventTarget
import com.acornui.signal.asWithEventTarget
import com.acornui.signal.event
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.*
import org.w3c.dom.events.EventTarget
import org.w3c.dom.events.MouseEvent
import kotlin.collections.set
import org.w3c.dom.Node as DomNode
import org.w3c.dom.events.Event as DomEvent

/**
 * The drag manager is responsible for dispatching custom drag events on the display graph.
 *
 * - [dragStarting]
 * - [dragStarted]
 * - [dragged]
 * - [dragEnded]
 *
 * All of these events will have [CustomEvent.detail] set to be a [DragData] object.
 *
 * Use the [Drag] behavior for listening to a specific target, which will provide extra user friendly options such as
 * tracking a single drag sequence, affordance checking, click prevention, mobile scrolling prevention, and more.
 *
 * Note that this is unrelated to the native dom [org.w3c.dom.DragEvent], which only works on desktop, but provides
 * different features such as [org.w3c.dom.DataTransfer].
 */
interface DragManager {

	/**
	 * The current number of drag operations in effect.
	 */
	val dragCount: Int

	companion object : Context.Key<DragManager> {
		override val factory = dependencyFactory { DragManagerImpl(it) }
	}
}

val Context.dragManager: DragManager
	get() = inject(DragManager)

class DragManagerImpl(owner: Context) : ContextImpl(owner), DragManager {

	private val doc = document.asWithEventTarget()
	private val win = window.asWithEventTarget()

	private val drags = HashMap<Int, DragData>()

	override val dragCount: Int
		get() = drags.size

	private var frameHandle: Disposable? = null

	init {
		// Listen to the document for starting a drag, listen to the window for updating and ending a drag.
		own(doc.mousePressed.listen(EventOptions(isPassive = false), ::mousePressedHandler))
		own(win.mouseMoved.listen(::mouseMovedHandler))
		own(win.mouseReleased.listen(::mouseReleasedHandler))
		own(doc.touchStarted.listen(EventOptions(isPassive = false), ::touchStartHandler))
		own(win.touchMoved.listen(::touchMovedHandler))
		own(win.touchEnded.listen(::touchEndedHandler))
	}

	private fun mousePressedHandler(event: MouseEvent) {
		val id = event.identifier
		val target = event.target.unsafeCast<DomNode?>() ?: return
		val dragData = DragData(
			startPositionClient = Vector2(event.clientX, event.clientY),
			button = event.button,
			ctrlKey = event.ctrlKey,
			shiftKey = event.shiftKey,
			altKey = event.altKey,
			metaKey = event.metaKey,
		)
		val dragStartingEvent = target.dispatchDrag(DRAG_STARTING, dragData, cancellable = true)
		val draggedNode = dragStartingEvent.draggedNode
		if (dragStartingEvent.defaultPrevented || draggedNode == null) return
		event.preventDefault() // Prevent native drag
		dragStart(id, dragData.copy(draggedNode = draggedNode, overTarget = getOverTarget(draggedNode, dragData.positionClient)))
	}

	private fun mouseMovedHandler(event: MouseEvent) {
		val id = event.identifier
		val dragData = drags[id] ?: return
		drag(id, dragData.next(event))
	}

	private fun mouseReleasedHandler(event: MouseEvent) {
		val id = event.identifier
		val dragData = drags[id] ?: return
		val newDragData = dragData.next(event)
		dragEnd(id, newDragData)
	}

	private fun touchStartHandler(event: TouchEvent) {
		event.changedTouches.forEach { touch ->
			val target = touch.target.unsafeCast<DomNode?>()
			if (target != null) {
				val dragData = DragData(
					startPositionClient = Vector2(touch.clientX, touch.clientY),
					touchId = touch.identifier,
					ctrlKey = event.ctrlKey,
					shiftKey = event.shiftKey,
					altKey = event.altKey,
					metaKey = event.metaKey,
				)
				val dragStartingEvent = target.dispatchDrag(DRAG_STARTING, dragData, cancellable = true)
				val draggedNode = dragStartingEvent.draggedNode
				if (!dragStartingEvent.defaultPrevented && draggedNode != null) {
					event.preventDefault() // Prevent native drag
					dragStart(touch.identifier, dragData.copy(draggedNode = draggedNode, overTarget = getOverTarget(draggedNode, dragData.positionClient)))
				}
			}
		}
	}

	/**
	 * Dispatches [DRAG_START], [DRAG], and begins a frame watch if needed.
	 */
	private fun dragStart(id: Int, dragData: DragData) {
		dragData.draggedNode!!
		drags[id] = dragData
		dragData.draggedNode.dispatchDrag(DRAG_START, dragData)
		dragData.overTarget?.dispatchDrag(DRAG_ENTER, dragData)
		dragData.draggedNode.dispatchDrag(DRAG, dragData)

		if (frameHandle == null) {
			frameHandle = frame.listen(::frameHandler.as1)
		}
	}

	private fun touchMovedHandler(event: TouchEvent) {
		event.changedTouches.forEach { touch ->
			drags[touch.identifier]?.let { dragData ->
				drag(touch.identifier, dragData.next(event, touch))
			}
		}
	}

	private fun touchEndedHandler(event: TouchEvent) {
		event.changedTouches.forEach { touch ->
			val dragData = drags[touch.identifier]
			if (dragData != null) {
				val newDragData = dragData.next(event, touch)
				dragEnd(touch.identifier, newDragData)
			}
		}
	}

	private fun frameHandler() {
		drags.values.forEach { dragData ->
			dragData.draggedNode!!.dispatchDrag(DRAG, dragData)
			dragData.overTarget?.dispatchDrag(DRAG_OVER, dragData)
		}
	}

	private fun drag(id: Int, dragData: DragData) {
		drags[id] = dragData
		if (dragData.previousOverTarget != dragData.overTarget) {
			dragData.previousOverTarget?.dispatchDrag(DRAG_EXIT, dragData)
			dragData.overTarget?.dispatchDrag(DRAG_ENTER, dragData)
		}
	}

	private fun dragEnd(id: Int, dragData: DragData) {
		drag(id, dragData)
		drags.remove(id)
		if (drags.isEmpty()) {
			frameHandle?.dispose()
			frameHandle = null
		}
		dragData.overTarget?.dispatchDrag(DRAG_EXIT,
			// In order to prevent the user from needing to check for both drag exit and drag end in order to know when
			// to reset drop zone indicators, drag exit is dispatched as if we've left the over target.
			dragData.copy(previousOverTarget = dragData.overTarget, overTarget = null)
		)
		dragData.overTarget?.dispatchDrag(DROP, dragData)
		dragData.draggedNode!!.dispatchDrag(DRAG_END, dragData)
	}

	private fun getOverTarget(draggedNode: DomNode, clientPosition: Vector2): Element? {
		val overElements = document.elementsFromPoint(clientPosition.x, clientPosition.y)
		return overElements.firstOrNull { draggedNode != it && !draggedNode.contains(it) }
	}

	private val MouseEvent.identifier: Int
		get() = -button.toInt() - 1

	/**
	 * Constructs the next drag data object from a mouse event.
	 */
	private fun DragData.next(event: MouseEvent): DragData {
		return next(
			positionClient = Vector2(event.clientX, event.clientY),
			ctrlKey = event.ctrlKey,
			shiftKey = event.shiftKey,
			altKey = event.altKey,
			metaKey = event.metaKey,
		)
	}

	/**
	 * Constructs the next drag data object from a touch event.
	 */
	private fun DragData.next(event: TouchEvent, touch: Touch): DragData {
		return next(
			positionClient = Vector2(touch.clientX, touch.clientY),
			ctrlKey = event.ctrlKey,
			shiftKey = event.shiftKey,
			altKey = event.altKey,
			metaKey = event.metaKey,
		)
	}

	private fun EventTarget.dispatchDrag(type: String, dragData: DragData, cancellable: Boolean = false): CustomEvent {
		val e = CustomEvent(
			type, CustomEventInit(
				dragData,
				bubbles = true,
				cancelable = cancellable
			)
		)
		dispatchEvent(e)
		return e
	}

	/**
	 * Updates relevant properties that change during a drag, and sets the new drag data's tracked previous values.
	 */
	private fun DragData.next(
		positionClient: Vector2,
		ctrlKey: Boolean,
		shiftKey: Boolean,
		altKey: Boolean,
		metaKey: Boolean
	): DragData {
		val newDistance = maxOf(maxDistance, startPositionClient.dst(positionClient))
		val newOverTarget = getOverTarget(draggedNode!!, positionClient)
		return copy(
			previousOverTarget = overTarget,
			overTarget = newOverTarget,
			maxDistance = newDistance,
			previousPositionClient = this.positionClient,
			positionClient = positionClient,
			ctrlKey = ctrlKey,
			shiftKey = shiftKey,
			altKey = altKey,
			metaKey = metaKey
		)
	}

	override fun dispose() {
		if (frameHandle != null) {
			frameHandle?.dispose()
			frameHandle = null
			drags.entries.forEach { dragEntry ->
				dragEnd(dragEntry.key, dragEntry.value)
			}
		}
		super.dispose()
	}
}

data class DragData(

	/**
	 * The dom node that accepted this drag via [CustomEvent.acceptDrag].
	 * This will be null only for [DRAG_STARTING] events.
	 */
	val draggedNode: DomNode? = null,

	/**
	 * An identifier that is consistent through the drag starting, drag started, drag, and drag ended events.
	 */
	val dragId: String = UidUtil.createUid(),

	/**
	 * The maximum distance the drag has travelled from [startPositionClient].
	 * This is used for determining whether or not a drag has passed an affordance distance.
	 */
	val maxDistance: Double = 0.0,

	/**
	 * The starting position (in page coordinates) for the drag.
	 */
	val startPositionClient: Vector2,

	/**
	 * The position of the last event (in page coordinates).
	 */
	val previousPositionClient: Vector2 = startPositionClient,

	/**
	 * The current position (in client coordinates).
	 */
	val positionClient: Vector2 = startPositionClient,

	/**
	 * This is the touch id from which the drag started, or -1 if the drag originated from a mouse.
	 */
	val touchId: Int = -1,

	/**
	 * The mouse button that initiated the drag.
	 *
	 * @see WhichButton
	 * [WhichButton.UNKNOWN]: A touch event initiated the drag.
	 */
	val button: Short = WhichButton.UNKNOWN,

	/**
	 * The dom node this drag operation is currently over, if any.
	 * This will never be an element contained by the [draggedNode].
	 *
	 * On a [dragExited] event, this will always be null, even if the pointer is currently over a target.
	 */
	val overTarget: DomNode? = null,

	/**
	 * The previous [overTarget]. This is most useful during a [dragExited] event.
	 *
	 * On a [dragEntered] event, this will always be null.
	 */
	val previousOverTarget: DomNode? = null,

	/**
	 * A data map that will persist across a drag set.
	 * For example if a [dragStarted] event handler sets data["foo"] = 3
	 * then that data is available within a [dropped] event handler with the same [dragId].
	 */
	val data: MutableMap<String, Any?> = HashMap(),

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
	val metaKey: Boolean

) {

	/**
	 * True if initialized from a touch interaction, false if mouse.
	 */
	val fromTouch: Boolean
		get() = touchId != -1

	private val r = draggedNode?.parentElement?.getBoundingClientRect() ?: DOMRect()
	private val scrollLeft = draggedNode?.parentElement?.scrollLeft ?: 0.0
	private val scrollTop = draggedNode?.parentElement?.scrollTop ?: 0.0

	private fun toLocal(clientPosition: Vector2): Vector2 {
		return vec2(clientPosition.x - r.left + scrollLeft, clientPosition.y - r.top + scrollTop)
	}

	/**
	 * The starting position, relative to the dragged node's parent.
	 */
	val startPositionLocal: Vector2 = toLocal(startPositionClient)

	/**
	 * The current position, relative to the dragged node's parent.
	 */
	val positionLocal: Vector2 = toLocal(positionClient)

	/**
	 * The previous position, relative to the dragged node's parent.
	 */
	val previousPositionLocal: Vector2 = toLocal(previousPositionClient)

	companion object {

		const val DRAG_STARTING = "customDragStarting"
		const val DRAG_START = "customDragStart"
		const val DRAG = "customDrag"
		const val DRAG_END = "customDragEnd"
		const val DRAG_ENTER = "customDragEnter"
		const val DRAG_OVER = "customDragOver"
		const val DROP = "customDrop"

		/**
		 * Named exit as opposed to leave to be consistent with native drag and drop, despite this being inconsistent
		 * with mouse leave.
		 * Signals for both drag and mouse are named 'exited'.
		 */
		const val DRAG_EXIT = "customDragExit"
	}
}

/**
 * The difference between [DragData.positionClient] and [DragData.previousPositionClient]
 */
val DragData.positionClientDelta: Vector2
	get() = positionClient - previousPositionClient

/**
 * The difference between [positionLocal] and [previousPositionLocal]
 */
val DragData.positionLocalDelta: Vector2
	get() = positionLocal - previousPositionLocal

/**
 * Dispatched in response to a document mouse pressed or touch started.
 * This will be dispatched before drag start, and may be cancelled. In order for a drag operation to begin,
 * this event must be marked as handled via [DomEvent.handle].
 */
val WithEventTarget.dragStarting
	get() = event<CustomEvent>(DRAG_STARTING)

/**
 * Dispatched after drag starting if the [dragStarting] event was marked as handled
 * via [CustomEvent.acceptDrag] and not default prevented via [DomEvent.preventDefault].
 * The drag started event is immediately followed by a [dragged] event.
 */
val WithEventTarget.dragStarted
	get() = event<CustomEvent>(DRAG_START)

private var CustomEvent.draggedNode: DomNode?
	get() = asDynamic().dragTarget
	set(value) {
		asDynamic().dragTarget = value
	}

/**
 * Indicates to a [dragStarting] event that a drag operation may begin, and the [DragData.draggedNode] should be this
 * event's [DomEvent.currentTarget].
 */
fun CustomEvent.acceptDrag() {
	require(type == DRAG_STARTING) { "acceptDrag is only for DragManager.DRAG_STARTING events." }
	handle()
	asDynamic().dragTarget = currentTarget
}

/**
 * During a drag operation, dragged will be dispatched every animation frame.
 * This will additionally fire once immediately after [dragStarted] and once immediately before [dragEnded].
 *
 * NB: A drag operation will not begin unless the [dragStarting] event is marked as accepted via
 * [CustomEvent.acceptDrag].
 */
val WithEventTarget.dragged
	get() = event<CustomEvent>(DRAG)

/**
 * Dispatched when a drag operation has ended from the mouse or touch being released.
 * Immediately before drag ended is dispatched, a [dragged] and potentially a [dragExited] event is dispatched.
 *
 * NB: A drag operation will not begin unless the [dragStarting] event is marked as accepted via
 * [CustomEvent.acceptDrag].
 */
val WithEventTarget.dragEnded
	get() = event<CustomEvent>(DRAG_END)

/**
 * Dispatched when the drag point has entered a new dom element.
 * Drag entered is always paired with a [dragExited].
 *
 * NB: A drag operation will not begin unless the [dragStarting] event is marked as accepted via
 * [CustomEvent.acceptDrag].
 */
val WithEventTarget.dragEntered
	get() = event<CustomEvent>(DRAG_ENTER)

/**
 * Dispatched when the drag point has exited a dom element or immediately before a [dropped] event.
 * Drag entered is always paired with a [dragExited].
 *
 * NB: A drag operation will not begin unless the [dragStarting] event is marked as accepted via
 * [CustomEvent.acceptDrag].
 */
val WithEventTarget.dragExited
	get() = event<CustomEvent>(DRAG_EXIT)

/**
 * Dispatched every animation frame on the dom element the drag point is over.
 * This is dispatched immediately after [dragged]. The drag data will be identical, but the event target will be not
 * the element being dragged, but the element the drag is over.
 *
 * NB: A drag operation will not begin unless the [dragStarting] event is marked as accepted via
 * [CustomEvent.acceptDrag].
 */
val WithEventTarget.draggedOver
	get() = event<CustomEvent>(DRAG_OVER)

/**
 * If [DragData.overTarget] is not null, this is dispatched immediately before [dragEnded] and after [dragExited] on
 * that over target.
 */
val WithEventTarget.dropped
	get() = event<CustomEvent>(DROP)
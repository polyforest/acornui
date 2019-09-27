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

@file:Suppress("UNUSED_ANONYMOUS_PARAMETER", "unused")

package com.acornui.component.scroll

import com.acornui.Disposable
import com.acornui.collection.poll
import com.acornui.component.UiComponent
import com.acornui.component.createOrReuseAttachment
import com.acornui.config
import com.acornui.input.InteractionType
import com.acornui.input.interaction.*
import com.acornui.math.Vector2
import com.acornui.math.Vector2Ro
import com.acornui.signal.StoppableSignal
import com.acornui.signal.StoppableSignalImpl
import com.acornui.time.nowMs
import com.acornui.time.tick

/**
 * A toss scroller lets you grab a target component, and update [ScrollModelRo] objects by dragging it.
 */
class TossScroller(
		val target: UiComponent,

		/**
		 * Dampening affects how quickly the toss velocity will slow to a stop.
		 * Make this number 0 &lt; dampening &lt; 1.  Where 1 will go forever, and 0 will prevent any momentum.
		 */
		var dampening: Float = DEFAULT_DAMPENING,

		private val dragAttachment: DragAttachment = target.dragAttachment(minTossDistance)
) : Disposable {

	private val tickTime = target.config.frameTime

	private val _tossStart = StoppableSignalImpl<DragInteraction>()

	/**
	 *  Dispatched when the toss has begun.
	 */
	val tossStart: StoppableSignal<DragInteractionRo>
		get() = _tossStart

	private val _toss = StoppableSignalImpl<DragInteraction>()

	/**
	 *  Dispatched every frame the toss is being updated.
	 */
	val toss: StoppableSignal<DragInteractionRo>
		get() = _toss

	private val _tossEnd = StoppableSignalImpl<DragInteraction>()

	/**
	 * Dispatched when the toss has ended.
	 */
	val tossEnd: StoppableSignal<DragInteractionRo>
		get() = _tossEnd

	private val _velocity = Vector2()

	/**
	 * The current velocity of the toss.
	 */
	val velocity: Vector2Ro = _velocity

	/**
	 * Returns true if the user is currently interacting or if there is still momentum.
	 * @see isDragging
	 * @see hasVelocity
	 */
	val userIsActive: Boolean
		get() = dragAttachment.userIsActive

	/**
	 * Returns true if the user is currently dragging (the drag has surpassed the drag affordance,
	 * and the user has not yet released.)
	 */
	val isDragging: Boolean
		get() = dragAttachment.isDragging

	/**
	 * Returns true if the toss scrolling currently has momentum.
	 * @see stop
	 */
	val hasVelocity: Boolean
		get() = _timer != null

	private val event = DragInteraction()
	private val startPosition: Vector2 = Vector2()
	private val position: Vector2 = Vector2()

	private val historyPoints = ArrayList<Vector2>()
	private val historyTimes = ArrayList<Long>()

	private var _timer: Disposable? = null
	private var clickPreventer = 0

	private val diff = Vector2()

	private val dragStartHandler = { event: DragInteractionRo ->
		stop()
		startPosition.set(event.startPosition)
		position.set(event.position)

		clickPreventer = 5
		clearHistory()
		pushHistory()
		startEnterFrame()
		dispatchDragEvent(TOSS_START, _tossStart)
		Unit
	}

	private fun pushHistory() {
		historyPoints.add(Vector2.obtain().set(position.x, position.y))
		historyTimes.add(nowMs())
		if (historyPoints.size > MAX_HISTORY) {
			Vector2.free(historyPoints.poll())
			historyTimes.poll()
		}
	}

	private fun startEnterFrame() {
		if (_timer == null) {
			_timer = tick {
				if (dragAttachment.isDragging) {
					// History is also added in an enter frame instead of the drag handler so that if the user stops dragging, the history reflects that.
					pushHistory()
				} else {
					if (clickPreventer > 0) clickPreventer--
					_velocity.scl(dampening)
					position.add(_velocity)
					dispatchDragEvent(TOSS, _toss)
					if (_velocity.isZero(0.1f)) {
						stop()
					}
				}
			}
		}
	}

	private val dragHandler = { event: DragInteractionRo ->
		position.set(event.position)
		dispatchDragEvent(TOSS, _toss)
		Unit
	}

	private fun dispatchDragEvent(type: InteractionType<DragInteraction>, signal: StoppableSignalImpl<DragInteraction>) {
		event.clear()
		event.target = target
		event.currentTarget = target
		event.type = type
		event.startPosition.set(startPosition)
		event.position.set(position)
		signal.dispatch(event)
	}

	private val dragEndHandler = { event: DragInteractionRo ->
		pushHistory()
		// Calculate the velocity.
		if (historyPoints.size >= 2) {
			diff.set(historyPoints.last()).sub(historyPoints.first())
			val time = (historyTimes.last() - historyTimes.first()) * 0.001f
			_velocity.set(diff.x / time, diff.y / time).scl(tickTime)
		}
		clearHistory()
	}

	private fun clearHistory() {
		for (i in 0..historyPoints.lastIndex) {
			Vector2.free(historyPoints[i])
		}
		historyPoints.clear()
		historyTimes.clear()
	}

	private val clickHandler = { event: ClickInteractionRo ->
		if (clickPreventer > 0) {
			event.propagation.stopImmediatePropagation()
		}
	}

	init {
		dragAttachment.dragStart.add(dragStartHandler)
		dragAttachment.drag.add(dragHandler)
		dragAttachment.dragEnd.add(dragEndHandler)
		target.click(isCapture = true).add(clickHandler)
	}

	/**
	 * Enables or disables this toss scroller. If this is set to false in the middle of a toss, the toss will
	 * still finish as if the drag were immediately released. Use [stop] to halt the current velocity.
	 */
	var enabled: Boolean
		get() = dragAttachment.enabled
		set(value) {
			dragAttachment.enabled = value
		}

	fun stop() {
		if (_timer != null) {
			dispatchDragEvent(TOSS_END, _tossEnd)
			clickPreventer = 0
			_velocity.clear()
			_timer?.dispose()
			_timer = null
			event.clear()
		}
	}

	override fun dispose() {
		stop()
		_tossStart.dispose()
		_toss.dispose()
		_tossEnd.dispose()
		dragAttachment.dragStart.remove(dragStartHandler)
		dragAttachment.drag.remove(dragHandler)
		dragAttachment.dragEnd.remove(dragEndHandler)
		target.click(isCapture = true).remove(clickHandler)
	}

	companion object {

		val TOSS_START = InteractionType<DragInteraction>("tossStart")
		val TOSS = InteractionType<DragInteraction>("toss")
		val TOSS_END = InteractionType<DragInteraction>("tossEnd")

		const val DEFAULT_DAMPENING: Float = 0.9f
		private const val MAX_HISTORY = 10

		var minTossDistance: Float = 7f
	}
}

/**
 * Converts the current toss difference in points to whatever unit the scroll models are using.
 */
typealias PointsToModel = (modelStart: Vector2Ro, diffPoints: Vector2Ro, out: Vector2) -> Unit

open class TossScrollModelBinding(
		private val tossScroller: TossScroller,
		private val hScrollModel: ScrollModel,
		private val vScrollModel: ScrollModel
) : Disposable {

	private val lastPositionLocal = Vector2()
	private val diff = Vector2()

	init {
		tossScroller.tossStart.add(::tossStartHandler)
		tossScroller.toss.add(::tossHandler)
	}

	private fun tossStartHandler(event: DragInteractionRo) {
		lastPositionLocal.set(event.positionLocal)
	}

	private fun tossHandler(event: DragInteractionRo) {
		diff.set(event.positionLocal).sub(lastPositionLocal)
		globalToModel(diff)
		hScrollModel.value -= diff.x
		vScrollModel.value -= diff.y
		lastPositionLocal.set(event.positionLocal)
	}

	/**
	 * Converts points, in global coordinate space, to model.
	 */
	protected open fun globalToModel(diffPoints: Vector2) {
	}


	override fun dispose() {
		tossScroller.tossStart.remove(::tossStartHandler)
		tossScroller.toss.remove(::tossHandler)
	}
}

fun UiComponent.enableTossScrolling(dampening: Float = TossScroller.DEFAULT_DAMPENING): TossScroller {
	return createOrReuseAttachment(TossScroller) { TossScroller(this, dampening) }
}

fun UiComponent.disableTossScrolling() {
	removeAttachment<TossScroller>(TossScroller)?.dispose()
}

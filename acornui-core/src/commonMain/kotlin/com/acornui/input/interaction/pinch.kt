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

import com.acornui.ExperimentalAcorn
import com.acornui.component.UiComponent
import com.acornui.component.createOrReuseAttachment
import com.acornui.component.stage
import com.acornui.di.ContextImpl
import com.acornui.function.as1
import com.acornui.input.*
import com.acornui.math.Vector2
import com.acornui.math.Vector2Ro
import com.acornui.math.vec2
import com.acornui.recycle.Clearable
import com.acornui.signal.Signal
import com.acornui.signal.Signal1
import com.acornui.time.callLater

interface PinchPointsRo {
	val first: Vector2Ro
	val second: Vector2Ro
}

data class PinchPoints(override val first: Vector2 = vec2(), override val second: Vector2 = vec2()) : PinchPointsRo, Clearable {

	fun set(value: PinchPointsRo): PinchPoints {
		first.set(value.first)
		second.set(value.second)
		return this
	}

	override fun clear() {
		first.clear()
		second.clear()
	}
}

interface PinchEventRo : EventRo {

	/**
	 * The two touch points that initiated the pinch, in canvas coordinates.
	 */
	val startPoints: PinchPointsRo

	/**
	 * The distance of [startPoints], in canvas coordinates.
	 */
	val startDistance: Float
		get() = startPoints.first.dst(startPoints.second)

	/**
	 * The current manhattan distance of [startPoints], in canvas coordinates.
	 */
	val startManhattanDst: Float
		get() = startPoints.first.manhattanDst(startPoints.second)

	/**
	 * The midpoint of the two starting touch points, in canvas coordinates.
	 */
	val startMidpoint: Vector2Ro
		get() = (startPoints.second + startPoints.first) * 0.5f

	/**
	 * The starting angle of the pinch, in canvas coordinates.
	 */
	val startRotation: Float
		get() = (startPoints.second - startPoints.first).angle

	/**
	 * The two current touch points, in canvas coordinates.
	 */
	val points: PinchPointsRo

	/**
	 * The current distance of [points], in canvas coordinates.
	 */
	val distance: Float
		get() = points.first.dst(points.second)

	/**
	 * The current manhattan distance of [points], in canvas coordinates.
	 */
	val manhattanDst: Float
		get() {
			return points.first.manhattanDst(points.second)
		}

	/**
	 * The midpoint of the two touch points, in canvas coordinates.
	 */
	val midpoint: Vector2Ro
		get() = (points.second + points.first) * 0.5f
	/**
	 * The current angle of the pinch, in canvas coordinates.
	 */
	val rotation: Float
		get() = (points.second - points.first).angle

	val distanceDelta: Float
		get() = distance - startDistance
}

class PinchEvent : PinchEventRo, EventBase() {

	override val startPoints = PinchPoints()
	override val points = PinchPoints()

	override fun clear() {
		super.clear()
		startPoints.clear()
		points.clear()
	}

	companion object {
		val PINCH_START = EventType<PinchEventRo>("pinchStart")
		val PINCH = EventType<PinchEventRo>("pinch")
		val PINCH_END = EventType<PinchEventRo>("pinchEnd")
	}
}

@ExperimentalAcorn
class PinchAttachment(
		val target: UiComponent,

		/**
		 * The manhattan distance delta the pinch must change before the pinch starts.
		 */
		var affordance: Float
) : ContextImpl(target) {

	private val stage = target.stage

	private var watchingTouch = false

	/**
	 * The movement has passed the affordance, and is currently pinching.
	 */
	private var _isPinching = false

	/**
	 * True if the user is currently pinching.
	 */
	val isPinching: Boolean
		get() = _isPinching

	private val pinchEvent: PinchEvent = PinchEvent()

	private val _pinchStart = Signal1<PinchEventRo>()

	/**
	 * Dispatched when the pinch has begun. This will be after the pinch has passed the affordance value.
	 */
	val pinchStart = _pinchStart.asRo()

	private val _pinch = Signal1<PinchEventRo>()

	/**
	 * Dispatched on each frame during a pinch.
	 */
	val pinch = _pinch.asRo()

	private val _pinchEnd = Signal1<PinchEventRo>()

	/**
	 * Dispatched when the pinch has completed.
	 * This may either be by stopping a touch point, or the target deactivating.
	 */
	val pinchEnd = _pinchEnd.asRo()

	private val startPoints = PinchPoints()
	private val points = PinchPoints()

	private fun targetDeactivatedHandler() {
		stop()
	}

	private fun clickBlocker(event: ClickEventRo) {
		event.handled = true
		event.preventDefault()
	}

	//--------------------------------------------------------------
	// Touch UX
	//--------------------------------------------------------------

	private fun touchStartHandler(event: TouchEventRo) {
		if (!watchingTouch && allowTouchStart(event)) {
			setWatchingTouch(true)
			event.handled = true
			val firstT = event.touches[0]
			startPoints.first.set(firstT.canvasX, firstT.canvasY)
			val secondT = event.touches[1]
			startPoints.second.set(secondT.canvasX, secondT.canvasY)
			points.set(startPoints)
			if (allowTouchPinchStart(event)) {
				setIsPinching(true)
			}
		}
	}

	/**
	 * Return true if the pinch should start watching movement.
	 * This does not determine if a pinch start may begin.
	 * @see allowTouchPinchStart
	 */
	private fun allowTouchStart(event: TouchEventRo): Boolean {
		return enabled && event.touches.size == 2
	}

	private fun allowTouchPinchStart(event: TouchEventRo): Boolean {
		return Vector2.manhattanDst(event.touches[0].canvasX, event.touches[0].canvasY, event.touches[1].canvasX, event.touches[1].canvasY) >= affordance
	}

	private fun allowTouchEnd(event: TouchEventRo): Boolean {
		return event.touches.size < 2
	}


	private fun setWatchingTouch(value: Boolean) {
		if (watchingTouch == value) return
		watchingTouch = value
		if (value) {
			stage.touchMove().add(::stageTouchMoveHandler)
			stage.touchEnd().add(::stageTouchEndHandler)
		} else {
			stage.touchMove().remove(::stageTouchMoveHandler)
			stage.touchEnd().remove(::stageTouchEndHandler)
		}
	}

	private fun stageTouchMoveHandler(event: TouchEventRo) {
		if (event.touches.size < 2) return
		val firstT = event.touches[0]
		points.first.set(firstT.canvasX, firstT.canvasY)
		val secondT = event.touches[1]
		points.second.set(secondT.canvasX, secondT.canvasY)

		if (_isPinching) {
			event.handled = true
			event.preventDefault()
			dispatchPinchEvent(PinchEvent.PINCH, _pinch)
		} else {
			if (!_isPinching && allowTouchPinchStart(event)) {
				setIsPinching(true)
			}
		}
	}

	private fun stageTouchEndHandler(event: TouchEventRo) {
		if (allowTouchEnd(event)) {
			event.handled = true
			setWatchingTouch(false)
			setIsPinching(false)
		}
	}

	//--------------------------------------------------------------
	// Pinch
	//--------------------------------------------------------------

	private fun setIsPinching(value: Boolean) {
		if (_isPinching == value) return
		_isPinching = value
		if (value) {
			dispatchPinchEvent(PinchEvent.PINCH_START, _pinchStart)
			if (pinchEvent.defaultPrevented()) {
				_isPinching = false
			} else {
				stage.click(isCapture = true).add(::clickBlocker, true) // Set the next click to be marked as handled.
				dispatchPinchEvent(PinchEvent.PINCH, _pinch)
			}
		} else {
			if (target.isActive) {
				dispatchPinchEvent(PinchEvent.PINCH, _pinch)
			}
			dispatchPinchEvent(PinchEvent.PINCH_END, _pinchEnd)

			callLater { stage.click(isCapture = true).remove(::clickBlocker) }
		}
	}

	private fun dispatchPinchEvent(type: EventType<PinchEventRo>, signal: Signal1<PinchEventRo>) {
		pinchEvent.clear()
		pinchEvent.target = target
		pinchEvent.currentTarget = target
		pinchEvent.type = type
		pinchEvent.startPoints.set(startPoints)
		pinchEvent.points.set(points)
		signal.dispatch(pinchEvent)
	}

	private var _enabled = true

	/**
	 * If true, pinch operations are enabled.
	 */
	var enabled: Boolean
		get() = _enabled
		set(value) {
			if (_enabled == value) return
			_enabled = value
			if (!value) stop()
		}

	fun stop() {
		setWatchingTouch(false)
		setIsPinching(false)
	}

	init {
		target.deactivated.add(::targetDeactivatedHandler.as1)
		target.touchStart().add(::touchStartHandler)
	}

	override fun dispose() {
		super.dispose()
		stop()
		_pinchStart.dispose()
		_pinch.dispose()
		_pinchEnd.dispose()

		target.deactivated.remove(::targetDeactivatedHandler.as1)
		target.touchStart().remove(::touchStartHandler)
	}

	companion object {

		/**
		 * The manhattan distance the target must be pinched before the pinchStart and pinch events begin.
		 */
		const val DEFAULT_AFFORDANCE: Float = 5f
	}
}

@ExperimentalAcorn
fun UiComponent.pinchAttachment(affordance: Float = PinchAttachment.DEFAULT_AFFORDANCE): PinchAttachment {
	return createOrReuseAttachment(PinchAttachment) { PinchAttachment(this, affordance) }
}

/**
 * @see PinchAttachment.pinchStart
 */
@ExperimentalAcorn
fun UiComponent.pinchStart(): Signal<(PinchEventRo) -> Unit> {
	return pinchAttachment().pinchStart
}

/**
 * @see PinchAttachment.pinch
 */
@ExperimentalAcorn
fun UiComponent.pinch(): Signal<(PinchEventRo) -> Unit> {
	return pinchAttachment().pinch
}

/**
 * @see PinchAttachment.pinchEnd
 */
@ExperimentalAcorn
fun UiComponent.pinchEnd(): Signal<(PinchEventRo) -> Unit> {
	return pinchAttachment().pinchEnd
}

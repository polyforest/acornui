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

package com.acornui.core.input.interaction

import com.acornui.recycle.Clearable
import com.acornui.component.UiComponent
import com.acornui.component.createOrReuseAttachment
import com.acornui.component.stage
import com.acornui.core.Disposable
import com.acornui.core.Lifecycle
import com.acornui.core.input.*
import com.acornui.core.time.callLater
import com.acornui.math.Vector2
import com.acornui.math.Vector2.Companion.manhattanDst
import com.acornui.math.Vector2Ro
import com.acornui.signal.Signal
import com.acornui.signal.Signal1

interface PinchPointsRo {
	val first: Vector2Ro
	val second: Vector2Ro
}

data class PinchPoints(override val first: Vector2 = Vector2(), override val second: Vector2 = Vector2()) : PinchPointsRo, Clearable {

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

interface PinchInteractionRo : InteractionEventRo {

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

class PinchInteraction : PinchInteractionRo, InteractionEventBase() {

	override val startPoints = PinchPoints()
	override val points = PinchPoints()

	override fun clear() {
		super.clear()
		startPoints.clear()
		points.clear()
	}

	companion object {
		val PINCH_START = InteractionType<PinchInteractionRo>("pinchStart")
		val PINCH = InteractionType<PinchInteractionRo>("pinch")
		val PINCH_END = InteractionType<PinchInteractionRo>("pinchEnd")
	}
}

class PinchAttachment(
		val target: UiComponent,

		/**
		 * The manhattan distance delta the pinch must change before the pinch starts.
		 */
		var affordance: Float
) : Disposable {

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

	private val pinchEvent: PinchInteraction = PinchInteraction()

	private val _pinchStart = Signal1<PinchInteractionRo>()

	/**
	 * Dispatched when the pinch has begun. This will be after the pinch has passed the affordance value.
	 */
	val pinchStart = _pinchStart.asRo()

	private val _pinch = Signal1<PinchInteractionRo>()

	/**
	 * Dispatched on each frame during a pinch.
	 */
	val pinch = _pinch.asRo()

	private val _pinchEnd = Signal1<PinchInteractionRo>()

	/**
	 * Dispatched when the pinch has completed.
	 * This may either be by stopping a touch point, or the target deactivating.
	 */
	val pinchEnd = _pinchEnd.asRo()

	private val startPoints = PinchPoints()
	private val points = PinchPoints()

	private fun targetDeactivatedHandler(c: Lifecycle) {
		stop()
	}

	private fun clickBlocker(event: ClickInteractionRo) {
		event.handled = true
		event.preventDefault()
	}

	//--------------------------------------------------------------
	// Touch UX
	//--------------------------------------------------------------

	private fun touchStartHandler(event: TouchInteractionRo) {
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
	private fun allowTouchStart(event: TouchInteractionRo): Boolean {
		return enabled && event.touches.size == 2
	}

	private fun allowTouchPinchStart(event: TouchInteractionRo): Boolean {
		return manhattanDst(event.touches[0].canvasX, event.touches[0].canvasY, event.touches[1].canvasX, event.touches[1].canvasY) >= affordance
	}

	private fun allowTouchEnd(event: TouchInteractionRo): Boolean {
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

	private fun stageTouchMoveHandler(event: TouchInteractionRo) {
		if (event.touches.size < 2) return
		val firstT = event.touches[0]
		points.first.set(firstT.canvasX, firstT.canvasY)
		val secondT = event.touches[1]
		points.second.set(secondT.canvasX, secondT.canvasY)

		if (_isPinching) {
			event.handled = true
			event.preventDefault()
			dispatchPinchEvent(PinchInteraction.PINCH, _pinch)
		} else {
			if (!_isPinching && allowTouchPinchStart(event)) {
				setIsPinching(true)
			}
		}
	}

	private fun stageTouchEndHandler(event: TouchInteractionRo) {
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
			dispatchPinchEvent(PinchInteraction.PINCH_START, _pinchStart)
			if (pinchEvent.defaultPrevented()) {
				_isPinching = false
			} else {
				stage.click(isCapture = true).add(::clickBlocker, true) // Set the next click to be marked as handled.
				dispatchPinchEvent(PinchInteraction.PINCH, _pinch)
			}
		} else {
			if (target.isActive) {
				dispatchPinchEvent(PinchInteraction.PINCH, _pinch)
			}
			dispatchPinchEvent(PinchInteraction.PINCH_END, _pinchEnd)

			target.callLater { stage.click(isCapture = true).remove(::clickBlocker) }
		}
	}

	private fun dispatchPinchEvent(type: InteractionType<PinchInteractionRo>, signal: Signal1<PinchInteractionRo>) {
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
		target.deactivated.add(::targetDeactivatedHandler)
		target.touchStart().add(::touchStartHandler)
	}

	override fun dispose() {
		stop()
		_pinchStart.dispose()
		_pinch.dispose()
		_pinchEnd.dispose()

		target.deactivated.remove(::targetDeactivatedHandler)
		target.touchStart().remove(::touchStartHandler)
	}

	companion object {

		/**
		 * The manhattan distance the target must be pinched before the pinchStart and pinch events begin.
		 */
		const val DEFAULT_AFFORDANCE: Float = 5f
	}
}


fun UiComponent.pinchAttachment(affordance: Float = PinchAttachment.DEFAULT_AFFORDANCE): PinchAttachment {
	return createOrReuseAttachment(PinchAttachment) { PinchAttachment(this, affordance) }
}

/**
 * @see PinchAttachment.pinchStart
 */
fun UiComponent.pinchStart(): Signal<(PinchInteractionRo) -> Unit> {
	return pinchAttachment().pinchStart
}

/**
 * @see PinchAttachment.pinch
 */
fun UiComponent.pinch(): Signal<(PinchInteractionRo) -> Unit> {
	return pinchAttachment().pinch
}

/**
 * @see PinchAttachment.pinchEnd
 */
fun UiComponent.pinchEnd(): Signal<(PinchInteractionRo) -> Unit> {
	return pinchAttachment().pinchEnd
}

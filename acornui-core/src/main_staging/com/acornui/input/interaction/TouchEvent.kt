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

package com.acornui.input.interaction

import com.acornui.component.UiComponent
import com.acornui.component.canvasToLocal
import com.acornui.input.EventBase
import com.acornui.input.EventRo
import com.acornui.input.EventType
import com.acornui.math.Vector2
import com.acornui.math.Vector2Ro
import com.acornui.math.vec2
import com.acornui.recycle.Clearable
import com.acornui.recycle.ClearableObjectPool
import kotlin.math.sqrt

interface TouchEventRo : EventRo {

	/**
	 * The number of milliseconds from the Unix epoch.
	 */
	val timestamp: Long

	/**
	 * A list of all the Touch objects representing individual points of contact whose states changed between the
	 * previous touch event and this one.
	 */
	val changedTouches: List<TouchRo>

	/**
	 * A list of all the Touch objects representing all current points of contact with the surface, regardless of
	 * target or changed status.
	 */
	val touches: List<TouchRo>

	/**
	 * True if this event was not created from the raw touch input.
	 */
	val isFabricated: Boolean

	companion object {

		val TOUCH_START = EventType<TouchEventRo>("touchStart")
		val TOUCH_MOVE = EventType<TouchEventRo>("touchMove")
		val TOUCH_END = EventType<TouchEventRo>("touchEnd")
		val TOUCH_CANCEL = EventType<TouchEventRo>("touchCancel")

	}

}

class TouchEvent : TouchEventRo, EventBase() {

	/**
	 * The number of milliseconds from the Unix epoch.
	 */
	override var timestamp: Long = 0

	/**
	 * A list of all the Touch objects representing individual points of contact whose states changed between the
	 * previous touch event and this one.
	 */
	override val changedTouches: MutableList<Touch> = ArrayList()

	/**
	 * A list of all the Touch objects representing all current points of contact with the surface, regardless of
	 * target or changed status.
	 */
	override val touches: MutableList<Touch> = ArrayList()

	override var isFabricated = false

	override var currentTarget: UiComponent
		get() = super.currentTarget
		set(value) {
			super.currentTarget = value
			for (i in 0..changedTouches.lastIndex) {
				changedTouches[i].currentTarget = value
			}
			for (i in 0..touches.lastIndex) {
				touches[i].currentTarget = value
			}
		}

	fun set(event: TouchEventRo) {
		type = event.type
		clearTouches()
		changedTouches.addTouches(event.changedTouches)
		touches.addTouches(event.touches)

		timestamp = event.timestamp
	}

	fun clearTouches() {
		clearTouches(changedTouches)
		clearTouches(touches)
	}

	private fun clearTouches(touches: MutableList<Touch>) {
		for (i in 0..touches.lastIndex) {
			Touch.free(touches[i])
		}
		touches.clear()
	}

	override fun clear() {
		super.clear()
		changedTouches.clear()
		timestamp = 0L
		isFabricated = false
		clearTouches()
	}

	companion object {

		/**
		 * Fills the [toList] with clones of the touches in [fromList]
		 */
		private fun MutableList<Touch>.addTouches(fromList: List<TouchRo>) {
			for (i in 0..fromList.lastIndex) {
				val fromTouch = fromList[i]
				val newTouch = Touch.obtain()
				newTouch.set(fromTouch)
				add(newTouch)
			}
		}
	}

}

interface TouchRo {

	/**
	 * The x position of the mouse event relative to the root canvas.
	 */
	val canvasX: Double

	/**
	 * The y position of the mouse event relative to the root canvas.
	 */
	val canvasY: Double

	val currentTarget: UiComponent?

	val identifier: Int

	/**
	 * The x position of the mouse event relative to the [currentTarget].
	 */
	val localX: Double

	/**
	 * The y position of the mouse event relative to the [currentTarget].
	 */
	val localY: Double

	/**
	 * The distance of this touch point to the other touch point (in canvas coordinates)
	 */
	fun dst(other: TouchRo): Double {
		val xD = other.canvasX - canvasX
		val yD = other.canvasY - canvasY
		return sqrt((xD * xD + yD * yD))
	}
}

class Touch private constructor(): TouchRo, Clearable {

	/**
	 * The x position of the mouse event relative to the root canvas.
	 */
	override var canvasX: Double = 0.0

	/**
	 * The y position of the mouse event relative to the root canvas.
	 */
	override var canvasY: Double = 0.0

	override var currentTarget: UiComponent? = null
		set(value) {
			if (field == value) return
			field = value
			_localPositionIsValid = false
		}

	override var identifier: Int = -1

	private var _localPositionIsValid = false
	private val _localPosition: Vector2 = vec2()

	/**
	 * The position of the mouse event relative to the [currentTarget].
	 */
	private fun localPosition(): Vector2Ro {
		if (!_localPositionIsValid) {
			_localPositionIsValid = true
			currentTarget!!.canvasToLocal(_localPosition.set(canvasX, canvasY))
		}
		return _localPosition
	}

	/**
	 * The x position of the mouse event relative to the [currentTarget].
	 */
	override val localX: Double
		get() = localPosition().x

	/**
	 * The y position of the mouse event relative to the [currentTarget].
	 */
	override val localY: Double
		get() = localPosition().y

	override fun clear() {
		canvasX = 0.0
		canvasY = 0.0
		currentTarget = null
		identifier = -1
		_localPositionIsValid = false
	}

	fun set(otherTouch: TouchRo) {
		canvasX = otherTouch.canvasX
		canvasY = otherTouch.canvasY
		currentTarget = otherTouch.currentTarget
		identifier = otherTouch.identifier
		_localPositionIsValid = false
	}

	companion object {

		private val pool = ClearableObjectPool { Touch() }
		fun obtain(): Touch = pool.obtain()
		fun free(touch: Touch) = pool.free(touch)
	}
}

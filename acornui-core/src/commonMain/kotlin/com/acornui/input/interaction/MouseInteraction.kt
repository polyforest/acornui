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

import com.acornui.component.InteractiveElement
import com.acornui.component.UiComponentRo
import com.acornui.component.canvasToLocal
import com.acornui.input.InteractionEventBase
import com.acornui.input.InteractionEventRo
import com.acornui.input.InteractionType
import com.acornui.input.WhichButton
import com.acornui.math.Vector2
import com.acornui.math.vec2
import kotlin.math.sqrt

interface MouseInteractionRo : InteractionEventRo {

	/**
	 * The x position of the mouse event relative to the root canvas.
	 */
	val canvasX: Float

	/**
	 * The y position of the mouse event relative to the root canvas.
	 */
	val canvasY: Float

	/**
	 * The x position of the mouse event relative to the [currentTarget].
	 */
	val localX: Float

	/**
	 * The y position of the mouse event relative to the [currentTarget].
	 */
	val localY: Float

	/**
	 * On a mouse out interaction, the relatedTarget will be the new over target (or null if there isn't one)
	 * On a mouse over interaction, the relatedTarget will be the previous over target (or null if there wasn't one)
	 */
	val relatedTarget: UiComponentRo?

	val button: WhichButton

	/**
	 * The number of milliseconds from the Unix epoch.
	 */
	val timestamp: Long

	/**
	 * If true, this interaction was triggered from code, not real user input.
	 */
	val isFabricated: Boolean

	/**
	 * Calculates the average velocity in pixels per millisecond of this touch event compared to a previous touch event.
	 */
	fun velocity(previous: MouseInteractionRo): Float {
		val xDiff = previous.canvasX - canvasX
		val yDiff = previous.canvasY - canvasY
		val distance = sqrt((xDiff * xDiff + yDiff * yDiff).toDouble()).toFloat()
		val time = timestamp - previous.timestamp
		return distance / time
	}

	companion object {

		/**
		 * Dispatched when a mouse button has been pressed down.
		 */
		val MOUSE_DOWN = InteractionType<MouseInteractionRo>("mouseDown")

		/**
		 * Dispatched when a mouse button has been released.
		 */
		val MOUSE_UP = InteractionType<MouseInteractionRo>("mouseUp")

		/**
		 * Dispatched when the mouse has moved.
		 */
		val MOUSE_MOVE = InteractionType<MouseInteractionRo>("mouseMove")

		val MOUSE_OVER = InteractionType<MouseInteractionRo>("mouseOver")
		val MOUSE_OUT = InteractionType<MouseInteractionRo>("mouseOut")
	}
}

/**
 * Representing a mouse event, as provided to [InteractiveElement] objects by the [InteractivityManager]
 * @author nbilyk
 */
open class MouseInteraction : InteractionEventBase(), MouseInteractionRo {

	/**
	 * The x position of the mouse event relative to the root canvas.
	 */
	override var canvasX: Float = 0f

	/**
	 * The y position of the mouse event relative to the root canvas.
	 */
	override var canvasY: Float = 0f

	private var _localPositionIsValid = false
	private val _localPosition: Vector2 = vec2()

	/**
	 * The position of the mouse event relative to the [currentTarget].
	 * This is calculated the first time it's requested.
	 */
	private fun localPosition(): Vector2 {
		if (!_localPositionIsValid) {
			_localPositionIsValid = true
			_localPosition.set(canvasX, canvasY)
			currentTarget.canvasToLocal(_localPosition)
		}
		return _localPosition
	}

	/**
	 * The x position of the mouse event relative to the [currentTarget].
	 */
	override val localX: Float
		get() = localPosition().x

	/**
	 * The y position of the mouse event relative to the [currentTarget].
	 */
	override val localY: Float
		get() = localPosition().y

	/**
	 * On a mouse out interaction, the relatedTarget will be the new over target (or null if there isn't one)
	 * On a mouse over interaction, the relatedTarget will be the previous over target (or null if there wasn't one)
	 */
	override var relatedTarget: UiComponentRo? = null

	override var button: WhichButton = WhichButton.UNKNOWN

	/**
	 * The number of milliseconds from the Unix epoch.
	 */
	override var timestamp: Long = 0

	/**
	 * If true, this interaction was triggered from code, not real user input.
	 */
	override var isFabricated: Boolean = false

	override var currentTarget: UiComponentRo
		get() = super.currentTarget
		set(value) {
			super.currentTarget = value
			_localPositionIsValid = false
		}

	open fun set(event: MouseInteractionRo) {
		type = event.type
		canvasX = event.canvasX
		canvasY = event.canvasY
		button = event.button
		timestamp = event.timestamp
	}

	override fun clear() {
		super.clear()
		canvasX = 0f
		canvasY = 0f
		_localPositionIsValid = false
		_localPosition.clear()
		relatedTarget = null
		button = WhichButton.UNKNOWN
		timestamp = 0
		isFabricated = false
	}
}

interface WheelInteractionRo : MouseInteractionRo {

	val deltaX: Float
	val deltaY: Float
	val deltaZ: Float

	companion object {
		val MOUSE_WHEEL = InteractionType<WheelInteractionRo>("mouseWheel")
	}
}

class WheelInteraction : MouseInteraction(), WheelInteractionRo {

	override var deltaX: Float = 0f
	override var deltaY: Float = 0f
	override var deltaZ: Float = 0f

	fun set(event: WheelInteractionRo) {
		super.set(event)
		deltaX = event.deltaX
		deltaY = event.deltaY
		deltaZ = event.deltaZ
	}

	override fun clear() {
		super.clear()
		deltaX = 0f
		deltaY = 0f
		deltaZ = 0f
	}

}

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

package com.acornui.core.input

import com.acornui.core.Disposable
import com.acornui.core.di.DKey
import com.acornui.core.input.interaction.MouseInteraction
import com.acornui.core.input.interaction.TouchInteraction
import com.acornui.core.input.interaction.WheelInteraction
import com.acornui.math.Vector2
import com.acornui.signal.Signal1

/**
 * Tracks the mouse position and whether or not the mouse is over the canvas.
 *
 * @author nbilyk
 */
interface MouseState : Disposable {

	/**
	 * Dispatched when the mouse has entered or left the canvas.
	 * Note: This is not 100% reliable in most browsers.
	 */
	val overCanvasChanged: Signal1<Boolean>

	/**
	 * True if the mouse is over the canvas.
	 */
	val overCanvas: Boolean

	/**
	 * The mouse x position relative to the canvas.
	 */
	val canvasX: Float

	/**
	 * The mouse y position relative to the canvas.
	 */
	val canvasY: Float

	/**
	 * Sets the [out] vector to the current canvas position.
	 * @return Returns the [out] vector.
	 */
	fun mousePosition(out: Vector2): Vector2 {
		out.set(canvasX, canvasY)
		return out
	}

	fun mouseIsDown(button: WhichButton): Boolean

	companion object : DKey<MouseState>

}

/**
 * Dispatches touch and mouse events for the canvas.
 */
interface MouseInput : MouseState {

	val touchStart: Signal1<TouchInteraction>
	val touchEnd: Signal1<TouchInteraction>
	val touchMove: Signal1<TouchInteraction>
	val touchCancel: Signal1<TouchInteraction>

	/**
	 * Dispatched when the user has pressed down (either via mouse or touch)
	 * Do not keep a reference to this event, it will be recycled.
	 */
	val mouseDown: Signal1<MouseInteraction>

	/**
	 * Dispatched when the user has released from a touch down (either via mouse or touch)
	 * Do not keep a reference to this event, it will be recycled.
	 */
	val mouseUp: Signal1<MouseInteraction>

	/**
	 * Dispatched when the user has moved their mouse, or pressed down on a touch surface and moved.
	 */
	val mouseMove: Signal1<MouseInteraction>

	/**
	 * Dispatched when the user has used the mouse wheel.
	 */
	val mouseWheel: Signal1<WheelInteraction>

	companion object : DKey<MouseInput> {
		override val extends: DKey<*>? = MouseState
	}
}


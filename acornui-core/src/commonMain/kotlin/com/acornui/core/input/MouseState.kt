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
import com.acornui.core.input.interaction.MouseInteractionRo
import com.acornui.core.input.interaction.TouchInteractionRo
import com.acornui.core.input.interaction.WheelInteractionRo
import com.acornui.math.Vector2
import com.acornui.signal.Signal

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
	val overCanvasChanged: Signal<(Boolean) -> Unit>

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

	/**
	 * Dispatched when the user has touched down on a touch device.
	 */
	val touchStart: Signal<(TouchInteractionRo) -> Unit>

	/**
	 * Dispatched when the user has released a touch on a touch device.
	 */
	val touchEnd: Signal<(TouchInteractionRo) -> Unit>

	/**
	 * Dispatched when one of the touch points has moved.
	 */
	val touchMove: Signal<(TouchInteractionRo) -> Unit>

	/**
	 * Dispatched when one or more touch points have been disrupted in an implementation-specific manner (for example,
	 * too many touch points are created).
	 */
	val touchCancel: Signal<(TouchInteractionRo) -> Unit>

	/**
	 * Dispatched when the user has pressed down.
	 * Do not keep a reference to this event, it will be recycled.
	 */
	val mouseDown: Signal<(MouseInteractionRo) -> Unit>

	/**
	 * Dispatched when the user has released from a touch down (either via mouse or touch)
	 * Do not keep a reference to this event, it will be recycled.
	 */
	val mouseUp: Signal<(MouseInteractionRo) -> Unit>

	/**
	 * Dispatched when the user has moved their mouse, or pressed down on a touch surface and moved.
	 */
	val mouseMove: Signal<(MouseInteractionRo) -> Unit>

	/**
	 * Dispatched when the user has used the mouse wheel.
	 */
	val mouseWheel: Signal<(WheelInteractionRo) -> Unit>

	companion object : DKey<MouseInput> {
		override val extends: DKey<*>? = MouseState
	}
}


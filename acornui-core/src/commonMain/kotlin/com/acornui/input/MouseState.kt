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

package com.acornui.input

import com.acornui.Disposable
import com.acornui.di.Context
import com.acornui.input.interaction.MouseInteractionRo
import com.acornui.input.interaction.TouchInteractionRo
import com.acornui.input.interaction.TouchRo
import com.acornui.input.interaction.WheelInteractionRo
import com.acornui.signal.Signal

/**
 * Tracks the mouse position and whether or not the mouse is over the canvas.
 *
 * @author nbilyk
 */
interface MouseState : Disposable {

	/**
	 * Dispatched when [touchMode] has changed.
	 */
	val touchModeChanged: Signal<() -> Unit>

	/**
	 * True if the last interaction was via touch, and not mouse.
	 */
	val touchMode: Boolean

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
	@Deprecated("Use mouseX", ReplaceWith("mouseX"))
	val canvasX: Float
		get() = mouseX

	/**
	 * The mouse or first touch y position relative to the canvas.
	 */
	@Deprecated("Use mouseY", ReplaceWith("mouseY"))
	val canvasY: Float
		get() = mouseY

	/**
	 * The first touch x position relative to the canvas.
	 */
	val touchX: Float

	/**
	 * The first touch y position relative to the canvas.
	 */
	val touchY: Float

	/**
	 * The mouse x position relative to the canvas.
	 */
	val mouseX: Float

	/**
	 * The mouse y position relative to the canvas.
	 */
	val mouseY: Float

	/**
	 * The current list of touch points.
	 */
	val touches: List<TouchRo>

	fun mouseIsDown(button: WhichButton): Boolean

	companion object : Context.Key<MouseState>

}

/**
 * The raw touch and mouse input on the canvas. This will only be key input from the system, and never fabricated events.
 * Components shouldn't use this directly, but instead use the the events from the [InteractivityManager].
 *
 * @see com.acornui.input.mouseDown
 * @see com.acornui.input.mouseUp
 * @see com.acornui.input.touchStart
 * @see com.acornui.input.touchEnd
 * @see com.acornui.input.mouseMove
 * @see com.acornui.input.touchMove
 * @see com.acornui.input.touchCancel
 */
interface MouseInput : MouseState {

	/**
	 * Dispatched when the user has touched down on a touch device.
	 *
	 * Both a touch start and a mouse down event will be dispatched, if only the touch start event is desired, invoke
	 * [TouchInteractionRo.preventDefault] on the touch start event.
	 */
	val touchStart: Signal<(TouchInteractionRo) -> Unit>

	/**
	 * Dispatched when the user has released a touch on a touch device.
	 *
	 * Both a touch end and a mouse up event will be dispatched, if only the touch end event is desired, invoke
	 * [TouchInteractionRo.preventDefault] on the touch end event.
	 */
	val touchEnd: Signal<(TouchInteractionRo) -> Unit>

	/**
	 * Dispatched when one of the touch points has moved.
	 *
	 * Both a touch move and a mouse move event will be dispatched, if only the touch move event is desired, invoke
	 * [TouchInteractionRo.preventDefault] on the touch move event.
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

	companion object : Context.Key<MouseInput> {
		override val extends: Context.Key<*>? = MouseState
	}
}


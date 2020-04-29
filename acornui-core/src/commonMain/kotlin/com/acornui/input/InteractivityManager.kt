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
import com.acornui.component.StageRo
import com.acornui.component.UiComponentRo
import com.acornui.di.Context
import com.acornui.recycle.Clearable
import com.acornui.signal.Stoppable
import com.acornui.signal.StoppableSignal

/**
 * A manager that feeds user input as events to layout elements.
 * @author nbilyk
 */
interface InteractivityManager : Disposable {

	/**
	 * Initializes the interactivity manager with the root element for dispatching.
	 */
	fun init(root: StageRo)

	/**
	 * Produces a new Signal for the specified interaction type.
	 */
	fun <T : InteractionEventRo> getSignal(host: UiComponentRo, type: InteractionType<T>, isCapture: Boolean): StoppableSignal<T>

	/**
	 * Dispatches an interaction for the layout element at the given stage position.
	 * @param canvasX The x coordinate relative to the canvas.
	 * @param canvasY The y coordinate relative to the canvas.
	 * @param event The interaction event to dispatch.
	 */
	fun dispatch(canvasX: Float, canvasY: Float, event: InteractionEvent, useCapture: Boolean = true, useBubble: Boolean = true)

	/**
	 * Dispatches an interaction for a single interactive element.
	 * This will first dispatch a capture event from the stage down to the given target, and then
	 * a bubbling event up to the stage.
	 * @param target The target for the event.
	 * @param event The event to dispatch on each ancestor.
	 * @param useCapture If true, there will be a capture phase. That is, starting from the highest ancestor
	 * (the Stage if the target is active) the event will be dispatched on each ancestor down to (and including)
	 * the target.
	 * @param useBubble If true, there will be a bubble phase. That is, starting from the target, each ancestor
	 * will have the event dispatched.
	 * If both [useCapture] and [useBubble] are false, only the target will have the event dispatched. (Using the
	 * bubble-phase signal)
	 */
	fun dispatch(target: UiComponentRo, event: InteractionEvent, useCapture: Boolean = true, useBubble: Boolean = true)

	companion object : Context.Key<InteractivityManager>
}

@Suppress("unused")
data class InteractionType<out T : InteractionEventRo>(val displayName: String) {

	override fun toString(): String {
		return "InteractionType($displayName)"
	}

	companion object
}

interface InteractionEventRo : Stoppable {

	val type: InteractionType<InteractionEventRo>

	/**
	 * The object dispatching the event. Unlike [currentTarget] this will not change during the bubble or capture
	 * phases.
	 * This will throw a null reference error if not accessed from the interactivity manager.
	 */
	val target: UiComponentRo

	/**
	 * The current target. This is the element currently walked in the capture and bubble phases.
	 * This will throw a null reference error if not accessed from the interactivity manager.
	 */
	val currentTarget: UiComponentRo

	/**
	 * True if this event was used in an interaction.
	 */
	var handled: Boolean

	val propagation: PropagationRo

	fun preventDefault()

	fun defaultPrevented(): Boolean

	companion object {
		val UNKNOWN = InteractionType<InteractionEventRo>("unknown")
	}
}

interface InteractionEvent : InteractionEventRo, Clearable {

	override var type: InteractionType<InteractionEventRo>
	override var target: UiComponentRo
	override var currentTarget: UiComponentRo

	override val propagation: Propagation

	override fun isStopped(): Boolean {
		return propagation.immediatePropagationStopped
	}

}

/**
 * A convenient base class for [InteractionEvent] implementations.
 */
abstract class InteractionEventBase : InteractionEvent {

	override var type: InteractionType<InteractionEventRo> = InteractionEventRo.UNKNOWN

	override var handled: Boolean = false

	private var _target: UiComponentRo? = null
	override var target: UiComponentRo
		get() = _target!!
		set(value) {
			_target = value
		}

	private var _currentTarget: UiComponentRo? = null
	override var currentTarget: UiComponentRo
		get() = _currentTarget!!
		set(value) {
			_currentTarget = value
		}

	override val propagation: Propagation = Propagation()

	private var _defaultPrevented: Boolean = false

	override fun defaultPrevented(): Boolean = _defaultPrevented

	/**
	 * Flags this event to indicate that default behavior should be ignored.
	 */
	override fun preventDefault() {
		_defaultPrevented = true
	}

	override fun clear() {
		type = InteractionEventRo.UNKNOWN
		propagation.clear()
		handled = false
		_target = null
		_currentTarget = null
		_defaultPrevented = false
	}
}

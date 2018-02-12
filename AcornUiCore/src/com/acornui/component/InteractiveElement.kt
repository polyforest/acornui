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

package com.acornui.component

import com.acornui.component.layout.LayoutElement
import com.acornui.component.layout.LayoutElementRo
import com.acornui.core.ChildRo
import com.acornui.core.di.Owned
import com.acornui.core.di.inject
import com.acornui.core.input.InteractionEvent
import com.acornui.core.input.InteractionType
import com.acornui.core.input.InteractivityManager
import com.acornui.math.Vector2
import com.acornui.signal.StoppableSignal

interface InteractiveElementRo : LayoutElementRo, CameraElementRo, AttachmentHolder, ChildRo, Owned {

	val native: NativeComponent

	/**
	 * If false, interaction will be blocked on this element.
	 * This value is calculated based on the [inheritedInteractivityMode] property.
	 */
	val interactivityEnabled: Boolean

	/**
	 * The inherited interactivity mode.
	 */
	val inheritedInteractivityMode: InteractivityMode

	/**
	 * Determines how this element will block or accept interaction events.
	 */
	val interactivityMode: InteractivityMode

	fun <T: InteractionEvent> handlesInteraction(type: InteractionType<T>): Boolean
	fun <T: InteractionEvent> handlesInteraction(type: InteractionType<T>, isCapture: Boolean): Boolean

	fun hasInteraction(): Boolean

	fun <T: InteractionEvent> hasInteraction(type: InteractionType<T>): Boolean
	fun <T: InteractionEvent> hasInteraction(type: InteractionType<T>, isCapture: Boolean): Boolean

	fun <T: InteractionEvent> getInteractionSignal(type: InteractionType<T>): StoppableSignal<T>?
	fun <T: InteractionEvent> getInteractionSignal(type: InteractionType<T>, isCapture: Boolean): StoppableSignal<T>?

	/**
	 * Sets the [out] vector to the local mouse coordinates.
	 * @return Returns the [out] vector.
	 */
	fun mousePosition(out: Vector2): Vector2

	/**
	 * Returns true if the mouse is currently over this element.
	 */
	fun mouseIsOver(): Boolean

	fun <T: InteractionEvent> addInteractionSignal(type: InteractionType<T>, signal: StoppableSignal<T>)
	fun <T: InteractionEvent> addInteractionSignal(type: InteractionType<T>, signal: StoppableSignal<T>, isCapture: Boolean)

	fun <T: InteractionEvent> removeInteractionSignal(type: InteractionType<T>)
	fun <T: InteractionEvent> removeInteractionSignal(type: InteractionType<T>, isCapture: Boolean)
}

/**
 * InteractiveElement provides a way to add and use signals for interaction events.
 * To use interaction signals, use their respective extension function.
 * See commonInteractions.kt
 */
interface InteractiveElement : InteractiveElementRo, LayoutElement, CameraElement {

	/**
	 * Determines how this element will block or accept interaction events.
	 */
	override var interactivityMode: InteractivityMode

}

enum class InteractivityMode {

	/**
	 * This InteractiveElement and its children will not be interactive.
	 */
	NONE,

	/**
	 * This InteractiveElement and its children will be interactive.
	 */
	ALL,

	/**
	 * This InteractiveElement will NOT be interactive, but its children will be.
	 */
	CHILDREN,

	/**
	 * This InteractiveElement will always pass boundary testing.
	 */
	ALWAYS
}


/**
 * Creates or reuses a stoppable signal for the specified interaction type.
 * This should be used in the same style you see in CommonInteractions.kt
 */
fun <T : InteractionEvent> InteractiveElementRo.createOrReuse(type: InteractionType<T>, isCapture: Boolean): StoppableSignal<T> {
	val existing = getInteractionSignal(type, isCapture)
	if (existing != null) {
		return existing
	} else {
		val interactivityManager = inject(InteractivityManager)
		val newHandler = interactivityManager.getSignal(this, type, isCapture)
		addInteractionSignal(type, newHandler, isCapture)
		return newHandler
	}
}
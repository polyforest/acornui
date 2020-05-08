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

package com.acornui.component

import com.acornui.component.layout.LayoutElement
import com.acornui.component.layout.LayoutElementRo
import com.acornui.di.Context
import com.acornui.input.EventRo
import com.acornui.input.EventType
import com.acornui.input.InteractivityManager
import com.acornui.math.RayRo
import com.acornui.math.Vector3
import com.acornui.math.vec3
import com.acornui.signal.StoppableSignal

interface InteractiveElementRo : LayoutElementRo, CanvasTransformableRo, AttachmentHolder, Context {

	/**
	 * If false, interaction will be blocked on this element.
	 * This value is calculated based on the [interactivityModeInherited] property.
	 */
	val interactivityEnabled: Boolean

	/**
	 * The inherited interactivity mode.
	 * E.g. if an ancestor has [interactivityEnabled] == [InteractivityMode.NONE], this component's
	 * [interactivityModeInherited] will be [InteractivityMode.NONE].
	 */
	val interactivityModeInherited: InteractivityMode

	/**
	 * Determines how this element will block or accept interaction events.
	 */
	val interactivityMode: InteractivityMode

	/**
	 * Given a canvas position, casts a ray in the direction of the camera, and returns true if that ray intersects
	 * with this component. This will always return false if this element is not active (on the stage)
	 * @param canvasX
	 * @param canvasY
	 */
	fun containsCanvasPoint(canvasX: Float, canvasY: Float): Boolean

	/**
	 * Returns true if this primitive intersects with the provided ray (in world coordinates)
	 * If there was an intersection, the intersection vector will be set to the intersection point.
	 *
	 * @param globalRay The ray (in world coordinates) to cast.
	 *
	 * @return Returns true if the ray intersects with the bounding box of this layout element.
	 */
	fun intersectsGlobalRay(globalRay: RayRo, intersection: Vector3): Boolean

	fun <T: EventRo> handlesInteraction(type: EventType<T>): Boolean
	fun <T: EventRo> handlesInteraction(type: EventType<T>, isCapture: Boolean): Boolean

	fun hasInteraction(): Boolean

	fun <T: EventRo> hasInteraction(type: EventType<T>, isCapture: Boolean = false): Boolean

	fun <T: EventRo> getInteractionSignal(type: EventType<T>, isCapture: Boolean = false): StoppableSignal<T>?

	fun <T: EventRo> addInteractionSignal(type: EventType<T>, signal: StoppableSignal<T>, isCapture: Boolean = false)

	fun <T: EventRo> removeInteractionSignal(type: EventType<T>, isCapture: Boolean = false)
}

private val tmpVec = vec3()

/**
 * Returns true if this primitive intersects with the provided ray (in world coordinates)
 *
 * @return Returns true if the ray intersects with the bounding box of this layout element.
 */
fun InteractiveElementRo.intersectsGlobalRay(globalRay: RayRo): Boolean = intersectsGlobalRay(globalRay, tmpVec)


/**
 * InteractiveElement provides a way to add and use signals for interaction events.
 * To use interaction signals, use their respective extension function.
 * See commonInteractions.kt
 */
interface InteractiveElement : InteractiveElementRo, LayoutElement {

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
fun <T : EventRo> UiComponentRo.createOrReuse(type: EventType<T>, isCapture: Boolean): StoppableSignal<T> {
	val existing = getInteractionSignal(type, isCapture)
	return if (existing != null) {
		existing
	} else {
		val interactivityManager = inject(InteractivityManager)
		val newHandler = interactivityManager.getSignal(this, type, isCapture)
		addInteractionSignal(type, newHandler, isCapture)
		newHandler
	}
}

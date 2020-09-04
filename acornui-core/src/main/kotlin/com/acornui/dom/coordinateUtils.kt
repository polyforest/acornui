/*
 * Copyright 2020 Poly Forest, LLC
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

package com.acornui.dom

import com.acornui.component.UiComponent
import com.acornui.math.Vector2
import com.acornui.math.vec2
import org.w3c.dom.Element
import org.w3c.dom.events.MouseEvent

/**
 * Given a client-relative coordinate, converts the coordinate to a space relative to this element's bounding rectangle.
 */
fun Element.clientToLocal(clientX: Double, clientY: Double): Vector2 {
	val r = getBoundingClientRect()
	return vec2(clientX - r.left + scrollLeft, clientY - r.top + scrollTop)
}

/**
 * Given a client-relative coordinate, converts the coordinate to a space relative to this element's bounding rectangle.
 */
fun Element.clientToLocal(clientCoord: Vector2): Vector2 = clientToLocal(clientCoord.x, clientCoord.y)

fun UiComponent.clientToLocal(clientCoord: Vector2): Vector2 = dom.clientToLocal(clientCoord.x, clientCoord.y)

/**
 * Given a coordinate relative to this element's bounding rectangle, converts the coordinate to a space relative to the
 * client.
 */
fun Element.localToClient(localX: Double, localY: Double): Vector2 {
	val r = getBoundingClientRect()
	return vec2(localX + r.left - scrollLeft, localY - r.top - scrollTop)
}

/**
 * Given a coordinate relative to this element's bounding rectangle, converts the coordinate to a space relative to the
 * client.
 */
fun Element.localToClient(localCoord: Vector2): Vector2 = localToClient(localCoord.x, localCoord.y)

fun UiComponent.localToClient(localCoord: Vector2): Vector2 = dom.localToClient(localCoord.x, localCoord.y)

/**
 * Converts the client position to a local position relative to the bounding box of the target component.
 */
fun MouseEvent.toLocal(target: UiComponent) = target.dom.clientToLocal(clientX.toDouble(), clientY.toDouble())


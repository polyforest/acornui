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

package com.acornui.input.draganddrop

import com.acornui.component.UiComponent
import com.acornui.signal.WithEventTarget
import com.acornui.signal.event
import org.w3c.dom.DragEvent

var UiComponent.draggable: Boolean
	get() = dom.draggable
	set(value) {
		dom.draggable = value
	}

/**
 * The dragstart event is fired when the user starts dragging an element or text selection.
 */
val WithEventTarget.dragStarted
	get() = event<DragEvent>("dragstart")

/**
 * The drag event is fired every few hundred milliseconds as an element or text selection is being dragged by the user.
 */
val WithEventTarget.dragged
	get() = event<DragEvent>("drag")

/**
 * The dragend event is fired when a drag operation is being ended (by releasing a mouse button or hitting the escape
 * key).
 */
val WithEventTarget.dragEnded
	get() = event<DragEvent>("dragend")

/**
 * The dragenter event is fired when a dragged element or text selection enters a valid drop target.
 *
 * The target object is the immediate user selection (the element directly indicated by the user as the drop target), or
 * the <body> element.
 */
val WithEventTarget.dragEntered
	get() = event<DragEvent>("dragentered")

/**
 * The dragexit event is fired when an element is no longer the drag operation's immediate selection target.
 */
val WithEventTarget.dragExited
	get() = event<DragEvent>("dragexit")

/**
 * The drop event is fired when an element or text selection is dropped on a valid drop target.
 */
val WithEventTarget.dropped
	get() = event<DragEvent>("drop")
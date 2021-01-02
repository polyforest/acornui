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

import org.w3c.dom.events.Event

/**
 * An extension for whether an event has been marked that it's been acted upon.
 * This allows handlers to change behavior if a higher priority handler has already handled this event.
 */
val Event.isHandled: Boolean
	get() = asDynamic().isHandled ?: false

/**
 * Marks that an event has been acted upon.
 * Sets [isHandled] to true.
 */
fun Event.handle() {
	asDynamic().isHandled = true
}

var Event.isFabricated: Boolean
	get() = asDynamic().isFabricated ?: false
	set(value) {
		asDynamic().isFabricated = value
	}
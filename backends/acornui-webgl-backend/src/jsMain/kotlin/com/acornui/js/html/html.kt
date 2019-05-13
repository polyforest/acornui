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

package com.acornui.js.html

import org.w3c.dom.*
import org.w3c.dom.events.Event

/**
 * cut/copy/paste events are type ClipboardEvent, except in IE, which is a DragEvent
 */
external class ClipboardEvent(type: String, eventInitDict: EventInit) : Event {
	val clipboardData: DataTransfer?
}

fun Node.owns(element: Node): Boolean {
	var p: Node? = element
	while (p != null) {
		if (p == this) return true
		p = p.parentNode
	}
	return false
}

fun HTMLElement.hide() {
	style.apply {
		// Necessary to hide on iOS.
		width = "0px"
		height = "0px"
		overflowX = "hidden"
		overflowY = "hidden"
		visibility = "hidden"
	}
}

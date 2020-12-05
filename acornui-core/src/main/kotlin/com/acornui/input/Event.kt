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

package com.acornui.input

/**
 * An event object not native to the DOM.
 * These events are meant to be dispatched by a signal. They do not bubble or capture on the display graph.
 */
open class Event {

	/**
	 * Returns true if [preventDefault] was called during this dispatch.
	 */
	var defaultPrevented: Boolean = false
		private set

	/**
	 * Sets [defaultPrevented] to true. For certain events, this will cancel default functionality.
	 */
	fun preventDefault() {
		defaultPrevented = true
	}

	/**
	 * True if [handle] has been called on this dispatch.
	 */
	var isHandled: Boolean = false
		private set

	/**
	 * Marks an event as having been handled.
	 */
	fun handle() {
		isHandled = true
	}
}
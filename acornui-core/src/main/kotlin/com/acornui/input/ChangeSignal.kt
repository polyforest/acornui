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

import com.acornui.component.UiComponent
import com.acornui.signal.Signal
import com.acornui.signal.SignalSubscription
import com.acornui.signal.event
import org.w3c.dom.events.Event

/**
 * A delegate to the change event.
 */
class ChangeSignal<T : UiComponent>(private val host: T) : Signal<T> {

	private val e = host.event<Event>("change")

	override fun listen(isOnce: Boolean, handler: (T) -> Unit): SignalSubscription {
		return e.listen(isOnce) {
			handler.invoke(host)
		}
	}
}
/*
 * Copyright 2017 Nicholas Bilyk
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

package com.acornui.signal

import com.acornui.collection.Clearable
import com.acornui.core.Disposable

/**
 * A Signal binding is a delegate to a [Signal] object, and allows the handlers to be removed as a set.
 */
class SignalHandlerSet<in T : Any>(private val signal: Signal<T>) : Clearable, Disposable {

	private val handlers = ArrayList<T>()

	fun add(handler: T) {
		if (handlers.contains(handler)) return
		handlers.add(handler)
		signal.add(handler)
	}

	override fun clear() {
		for (i in 0..handlers.lastIndex) {
			signal.remove(handlers[i])
		}
		handlers.clear()
	}

	override fun dispose() {
		clear()
	}
}
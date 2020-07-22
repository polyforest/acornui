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

@file:Suppress("UNUSED_PARAMETER")

package com.acornui.signal

/**
 * A Signal placeholder that contains no handlers and cannot be dispatched.
 */
internal object EmptySignal : Signal<Any> {

	override fun listen(isOnce: Boolean, handler: (Any) -> Unit): SignalSubscription =
		EmptySignalSubscription

}

internal object EmptySignalSubscription : SignalSubscription {

	override val isOnce: Boolean = false

	override var isPaused: Boolean
			get() = false
			set(value) {}

	override fun dispose() {
	}
}

fun <T> emptySignal(): Signal<T> = EmptySignal.unsafeCast<Signal<T>>()

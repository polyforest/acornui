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

package com.acornui.signal

import com.acornui.ManagedDisposable
import com.acornui.Owner
import com.acornui.ownUnsafe

/**
 * A signal that can only be dispatched once.
 *
 * - When this signal has dispatched, all handlers will be removed.
 * - If a handler is added after this signal has been dispatched, it will be disregarded.
 * - Subsequent dispatch calls will result in an error.
 * - All handlers will be coerced to once handlers.
 */
open class OnceSignal<T> : SignalImpl<T>() {

	var hasDispatched = false
		private set

	/**
	 * Adds a handler to this signal.
	 *
	 * @param isOnce In a OnceSignal, this argument is disregarded and always considered true.
	 * All handlers are removed after dispatch.
	 *
	 * @return Returns a signal subscription that may be paused or disposed.
	 * [SignalSubscription.isOnce] will always be true.
	 */
	override fun listen(isOnce: Boolean, handler: (T) -> Unit): SignalSubscription {
		if (hasDispatched) return EmptyOnceSignalSubscription
		return super.listen(true, handler)
	}

	/**
	 * Adds a handler to this signal.
	 * When this signal is dispatched, the handler will be removed.
	 *
	 * @param handler The callback that will be invoked when the signal is dispatched. The handler will be given
	 * a single argument; the value provided on the dispatch.
	 *
	 * @return Returns a signal subscription that may be paused or disposed.
	 */
	override fun listen(handler: (T) -> Unit): SignalSubscription = listen(true, handler)

	override fun dispatch(value: T) {
		if (hasDispatched) error("OnceSignal may only be dispatched once.")
		hasDispatched = true
		super.dispatch(value)
	}
}

internal object EmptyOnceSignalSubscription : SignalSubscription {

	override val isOnce: Boolean = true

	@Suppress("UNUSED_PARAMETER")
	override var isPaused: Boolean
		get() = false
		set(value) {}

	override fun dispose() {
	}
}

class ManagedOnceSignal<T> : OnceSignal<T>(), ManagedDisposable

/**
 * Creates an owned once signal that will be disposed automatically when the owner is disposed.
 */
fun <T> Owner.onceSignal(): OnceSignal<T> =
	ownUnsafe(ManagedOnceSignal())
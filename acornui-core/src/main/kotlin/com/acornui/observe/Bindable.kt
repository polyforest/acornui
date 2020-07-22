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

package com.acornui.observe

import com.acornui.Disposable
import com.acornui.toDisposable
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Bindable is a generic interface that represents the ability to add callbacks to the asynchronous event(s)
 * this bindable object represents.
 * The callbacks are always no arguments in order to combine multiple bindable objects.
 * @see or
 */
interface Bindable {

	/**
	 * Adds a callback.
	 * Does not invoke the callback immediately.
	 * @return Returns a disposable reference to remove the binding.
	 */
	fun addBinding(callback: () -> Unit): Disposable
}

/**
 * Adds a callback and invokes it immediately.
 * @return Returns a [Disposable] object that will remove the callback on [Disposable.dispose]
 */
@Deprecated("Use Context.bind(bindable, callback)", ReplaceWith("bind(this, callback)"))
fun Bindable.bind(callback: () -> Unit): Disposable {
	contract { callsInPlace(callback, InvocationKind.AT_LEAST_ONCE) }
	val binding = addBinding(callback)
	callback()
	return binding
}

private class MergedBinding(private val bindableA: Bindable, private val bindableB: Bindable) : Bindable {

	override fun addBinding(callback: () -> Unit): Disposable {
		val a = bindableA.addBinding(callback)
		val b = bindableB.addBinding(callback)
		return {
			a.dispose()
			b.dispose()
		}.toDisposable()
	}
}

infix fun Bindable.or(other: Bindable): Bindable = MergedBinding(this, other)

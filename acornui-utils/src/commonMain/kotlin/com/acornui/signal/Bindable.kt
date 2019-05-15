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

package com.acornui.signal

import com.acornui.core.Disposable

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
	 */
	fun addBinding(callback: () -> Unit)

	/**
	 * Removes a callback.
	 */
	fun removeBinding(callback: () -> Unit)
}

/**
 * Adds a callback and invokes it immediately.
 * @return Returns a [Disposable] object that will remove the callback on [Disposable.dispose]
 */
fun Bindable.bind(callback: () -> Unit): Disposable {
	addBinding(callback)
	callback()
	return object : Disposable {
		override fun dispose() = removeBinding(callback)
	}
}

private class MergedBinding(private val bindableA: Bindable, private val bindableB: Bindable) : Bindable {

	override fun addBinding(callback: () -> Unit) {
		bindableA.addBinding(callback)
		bindableB.addBinding(callback)
	}

	override fun removeBinding(callback: () -> Unit) {
		bindableA.removeBinding(callback)
		bindableB.removeBinding(callback)
	}
}

infix fun Bindable.or(other: Bindable): Bindable = MergedBinding(this, other)

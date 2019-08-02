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

package com.acornui.observe

import com.acornui.Disposable
import com.acornui.signal.Signal
import com.acornui.signal.Signal2

interface DataBindingRo<out T> {

	/**
	 * Dispatched when the data binding [value] has changed.
	 * The handler signature will be (oldValue, newValue)->Unit
	 */
	val changed: Signal<(T, T) -> Unit>

	val value: T

	/**
	 * Immediately, and when the data has changed, the callback will be invoked.
	 *
	 * If you need to know the old value as well, use the [changed] signal.
	 */
	fun bind(callback: (T) -> Unit): Disposable {
		val handler: DataChangeHandler<T> = { _, new: T ->
			callback(new)
		}
		changed.add(handler)
		callback(value)

		return object : Disposable {
			override fun dispose() {
				changed.remove(handler)
			}
		}
	}
}

interface DataBinding<T> : DataBindingRo<T> {

	/**
	 * This data binding's value.
	 * This will do nothing if the [changed] signal is currently dispatching.
	 */
	override var value: T

	/**
	 * Changes this data binding's value.
	 * @param callback The callback is given the old value, and should return the new value.
	 * @return Returns false if the data has not changed or if the [changed] signal is currently dispatching.
	 */
	fun change(callback: (T) -> T): Boolean

	fun asRo(): DataBindingRo<T> = this
}

class DataBindingImpl<T>(initialValue: T) : DataBinding<T>, Disposable {

	private val _changed = Signal2<T, T>()
	override val changed = _changed.asRo()

	private var _value: T = initialValue

	override var value: T
		get() = _value
		set(value) {
			if (_changed.isDispatching) return
			val old = _value
			if (old == value) return
			if (_changed.isDispatching) return
			_value = value
			_changed.dispatch(old, value)
		}

	override fun change(callback: (T) -> T): Boolean {
		if (_changed.isDispatching) return false
		val old = _value
		val newValue = callback(value)
		if (old == newValue) return false
		_value = newValue
		_changed.dispatch(old, newValue)
		return true
	}

	override fun dispose() {
		_changed.dispose()
	}
}

/**
 * A handler for when a data binding has changed.
 * oldValue, newValue
 */
typealias DataChangeHandler<T> = (T, T) -> Unit

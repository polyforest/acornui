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

package com.acornui.core.observe

import com.acornui.core.Disposable
import com.acornui.core.di.Owned
import com.acornui.core.di.own
import com.acornui.signal.*

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

interface DataBinding<T> : DataBindingRo<T>, Disposable {

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

class DataBindingImpl<T>(initialValue: T) : DataBinding<T> {

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

infix fun <S, T> DataBindingRo<S>.or(other: DataBindingRo<T>): Bindable {
	return changed or other.changed
}

infix fun <T> DataBindingRo<T>.or(other: Bindable): Bindable {
	return changed or other
}

infix fun <T> Bindable.or(other: DataBindingRo<T>): Bindable {
	return this or other.changed
}

/**
 * Mirrors changes from two data binding objects. If one changes, the other will be set.
 * @param other The receiver and other will be bound to each other. other will be initially set to the value of the
 * receiver.
 */
fun <T> DataBinding<T>.mirror(other: DataBinding<T>): Disposable {
	if (this === other) throw IllegalArgumentException("Cannot mirror to self")
	val a = bind {
		other.value = it
	}
	val b = other.bind {
		value = it
	}
	return object : Disposable {
		override fun dispose() {
			a.dispose()
			b.dispose()
		}
	}
}

/**
 * A handler for when a data binding has changed.
 * oldValue, newValue
 */
typealias DataChangeHandler<T> = (T, T) -> Unit

fun <T> Owned.dataBinding(initialValue: T): DataBindingImpl<T> {
	return own(DataBindingImpl(initialValue))
}

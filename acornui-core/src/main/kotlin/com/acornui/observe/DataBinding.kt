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
import com.acornui.signal.SignalImpl
import com.acornui.signal.unmanagedSignal
import kotlin.properties.ObservableProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class ChangeEvent<out T>(val oldData: T, val newData: T)

fun <T> observable(initialValue: T, signal: SignalImpl<ChangeEvent<T>>): ReadWriteProperty<Any?, T> =
	object : ObservableProperty<T>(initialValue) {

		override fun beforeChange(property: KProperty<*>, oldValue: T, newValue: T): Boolean =
			!signal.isDispatching

		override fun afterChange(property: KProperty<*>, oldValue: T, newValue: T) {
			if (oldValue != newValue)
				signal.dispatch(ChangeEvent(oldValue, newValue))
		}
	}

interface DataBindingRo<out T> {

	/**
	 * Dispatched when the data binding [value] has changed.
	 * The handler signature will be (oldValue, newValue)->Unit
	 */
	val changed: Signal<ChangeEvent<T>>

	val value: T

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

	override val changed = unmanagedSignal<ChangeEvent<T>>()

	private var _value: T = initialValue

	/**
	 * When the data is set, the validator will be given the new value. If the data is invalid, this method should
	 * throw an exception.
	 */
	var validator: (T) -> Unit = {}

	override var value: T
		get() = _value
		set(value) {
			setValueInternal(value)
		}

	override fun change(callback: (T) -> T): Boolean {
		if (changed.isDispatching) return false
		val old = _value
		val newValue = callback(value)
		validator(newValue)
		if (old == newValue) return false
		_value = newValue
		changed.dispatch(ChangeEvent(old, newValue))
		return true
	}

	private fun setValueInternal(value: T) {
		if (changed.isDispatching) return
		validator(value)
		val old = _value
		if (old == value) return
		if (changed.isDispatching) return
		_value = value
		changed.dispatch(ChangeEvent(old, value))
	}

	override fun dispose() {
		changed.dispose()
	}
}

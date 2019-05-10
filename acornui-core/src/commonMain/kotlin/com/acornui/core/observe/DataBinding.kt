package com.acornui.core.observe

import com.acornui.core.Disposable
import com.acornui.core.di.Owned
import com.acornui.core.di.own
import com.acornui.signal.*

interface DataBindingRo<T> {

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
	fun bind(callback: (T) -> Unit): Disposable

	/**
	 * Removes the callback handler that was added via [bind].
	 */
	fun remove(callback: (T) -> Unit)
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
}

class DataBindingImpl<T>(initialValue: T) : DataBinding<T> {

	private val _changed = Signal2<T, T>()
	override val changed = _changed.asRo()

	private val _wrapped: MutableMap<(T) -> Unit, (T, T) -> Unit> = HashMap()

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
		val old = _value
		if (_changed.isDispatching) return false
		val newValue = callback(value)
		if (old == newValue) return false
		_value = newValue
		_changed.dispatch(old, newValue)
		return true
	}

	override fun bind(callback: (T) -> Unit): Disposable {
		val handler: DataChangeHandler<T> = { _, new: T ->
			callback(new)
		}
		_wrapped[callback] = handler
		_changed.add(handler)
		callback(_value)

		return object : Disposable {
			override fun dispose() {
				remove(callback)
			}
		}
	}

	override fun remove(callback: (T) -> Unit) {
		val handler = _wrapped[callback]
		if (handler != null) {
			_wrapped.remove(callback)
			_changed.remove(handler)
		}
	}

	override fun dispose() {
		_changed.dispose()
		_wrapped.clear()
	}

	fun asRo(): DataBindingRo<T> = this
}

infix fun <S, T> DataBinding<S>.or(other: DataBinding<T>): Bindable {
	return changed or other.changed
}

infix fun <T> DataBindingRo<T>.or(other: Bindable): Bindable {
	return changed or other
}

infix fun <T> Bindable.or(other: DataBinding<T>): Bindable {
	return this or other.changed
}

/**
 * Mirrors changes from two data binding objects. If one changes, the other will be set.
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
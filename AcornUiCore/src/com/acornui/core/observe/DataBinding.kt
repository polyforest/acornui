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

class DataBinding<T>(initialValue: T) : DataBindingRo<T>, Disposable {

	private val _changed = Signal2<T, T>()

	override val changed: Signal<(T, T) -> Unit>
		get() = _changed

	private val _wrapped = HashMap<(T) -> Unit, DataChangeHandler<T>>()

	private var _value: T = initialValue

	override var value: T
		get() = _value
		set(value) {
			if (_changed.isDispatching) return
			val old = _value
			if (old == value) return
			_value = value
			_changed.dispatch(old, value)
		}

	fun change(callback: (T) -> T) {
		if (_changed.isDispatching) return
		value = callback(value)
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
}

infix fun <S, T> DataBinding<S>.or(other: DataBinding<T>): Bindable {
	return changed or other.changed
}

infix fun <T> DataBinding<T>.or(other: Bindable): Bindable {
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

fun <T> Owned.dataBinding(initialValue: T): DataBinding<T> {
	return own(DataBinding(initialValue))
}
package com.acornui.core.immutable

import com.acornui.core.Disposable
import com.acornui.core.di.Owned
import com.acornui.core.di.own
import com.acornui.signal.Signal3

class DataBinding<T> : Disposable {

	private val _changed = Signal3<Boolean, T?, T>()
	private val _wrapped = HashMap<(T) -> Unit, DataChangeHandler<T>>()

	private var _data: T? = null

	private var _isSet: Boolean = false

	/**
	 * Returns true if this data binding has ever been provided a value via [set]
	 */
	val isSet: Boolean
		get() = _isSet

	/**
	 * Sets the current data.
	 */
	fun set(data: T) {
		if (_changed.isDispatching) return
		val wasSet = _isSet
		_isSet = true
		val old = _data
		if (old == data) return
		_data = data
		_changed.dispatch(!wasSet, old, data)
	}

	/**
	 * If [isSet] is true, the current value is returned, otherwise null.
	 */
	fun get(): T? {
		if (!_isSet) return null
		return _data
	}

	@Deprecated("Use change instead", ReplaceWith("change(callback)"))
	operator fun invoke(callback: (T) -> T) = change(callback)

	/**
	 * Call to modify the current value.
	 * @param callback The current value is passed to the callback, and the callback should return the new modified
	 * value.
	 */
	fun change(callback: (T) -> T) {
		if (_changed.isDispatching) return
		if (_isSet) {
			@Suppress("UNCHECKED_CAST")
			val newModel = callback(_data as T)
			set(newModel)
		}
	}

	/**
	 * When the data has changed, the callback will be invoked.
	 * If the data has been set, the callback will be invoked immediately.
	 * If you need to know the old value as well, use [bind2].
	 */
	fun bind(callback: (T) -> Unit) {
		val handler: DataChangeHandler<T> = { _, _, new: T ->
			callback(new)
		}
		_wrapped[callback] = handler
		bind2(handler)
	}

	/**
	 * When the data has changed, the callback will be invoked.
	 * If the data has been set, the callback will be invoked immediately.
	 */
	fun bind2(callback: DataChangeHandler<T>) {
		_changed.add(callback)
		@Suppress("UNCHECKED_CAST")
		if (_isSet)
			callback(true, null, _data as T)
	}

	/**
	 * Removes the callback handler that was added via [bind2].
	 */
	fun remove(callback: (T) -> Unit) {
		val handler = _wrapped[callback]
		if (handler != null) {
			_wrapped.remove(callback)
			remove2(handler)
		}
	}

	/**
	 * Removes the callback handler that was added via [bind2].
	 */
	fun remove2(callback: DataChangeHandler<T>) {
		_changed.remove(callback)
	}

	override fun dispose() {
		_changed.dispose()
		_wrapped.clear()
	}
}

/**
 * Mirrors changes from two data binding objects. If one changes, the other will be set.
 */
fun <T> DataBinding<T>.mirror(other: DataBinding<T>) {
	bind {
		other.set(it)
	}
	other.bind {
		set(it)
	}
}

/**
 * A handler for when a data binding has changed.
 * isFirst, oldValue (null if isFirst is true), newValue
 */
typealias DataChangeHandler<T> = (Boolean, T?, T) -> Unit

fun <T> Owned.dataBinding(initialValue: T): DataBinding<T> {
	return dataBinding<T>().apply {
		set(initialValue)
	}
}

fun <T> Owned.dataBinding(): DataBinding<T> {
	return own(DataBinding())
}
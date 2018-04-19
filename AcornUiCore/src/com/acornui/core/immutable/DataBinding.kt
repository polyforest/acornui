package com.acornui.core.immutable

import com.acornui.core.Disposable
import com.acornui.signal.Signal1

class DataBinding<T> : Disposable {

//	var group: CommandGroup? = null

	private val _changed = Signal1<T>()

	private var _data: T? = null

	private var isSet: Boolean = false

	/**
	 * Sets the current data.
	 */
	fun set(data: T) {
		if (_changed.isDispatching) return
		isSet = true
		if (_data === data) return
		_data = data
		_changed.dispatch(data)
	}

	fun get(): T? {
		if (!isSet) return null
		return _data
	}

	operator fun invoke(callback: (T) -> T) {
		if (_changed.isDispatching) return
		@Suppress("UNCHECKED_CAST")
		if (isSet) {
			val newModel = callback(_data as T)
			set(newModel)
		}
	}

	/**
	 * When the data has changed, the callback will be invoked.
	 */
	fun bind(callback: (T) -> Unit) {
		_changed.add(callback)
		@Suppress("UNCHECKED_CAST")
		if (isSet)
			callback(_data as T)
	}

	fun remove(callback: (T) -> Unit) {
		_changed.remove(callback)
	}

	override fun dispose() {
		_changed.dispose()
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
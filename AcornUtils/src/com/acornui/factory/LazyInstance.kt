package com.acornui.factory

import com.acornui.core.Disposable

class LazyInstance<out R, out T>(
		private val receiver: R,
		private val factory: R.() -> T
) {

	private var _created = false
	private var _instance: T? = null

	val created: Boolean
		get() = _created

	val instance: T
		get() {
			if (!_created) {
				_instance = receiver.factory()
				_created = true
			}
			return _instance!!
		}

	fun clear() {
		_instance = null
		_created = false
	}

}

fun <R, T : Disposable?> LazyInstance<R, T>.disposeInstance() {
	if (created) {
		instance?.dispose()
		clear()
	}
}
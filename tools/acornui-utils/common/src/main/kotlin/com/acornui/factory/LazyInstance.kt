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

/**
 * Creates a LazyInstance object that isn't lazy. Can be used to coerce an instance in places that expect laziness.
 */
fun <R, T> R.lazyInstance(instance: T): LazyInstance<R, T> {
	val notReallyLazyInstance= LazyInstance(this) { instance }
	notReallyLazyInstance.instance // Mark as created
	return notReallyLazyInstance
}

fun <R, T> R.lazyInstance(factory: R.() -> T): LazyInstance<R, T> {
	val notReallyLazyInstance= LazyInstance(this, factory)
	notReallyLazyInstance.instance // Mark as created
	return notReallyLazyInstance
}

fun <R, T : Disposable?> LazyInstance<R, T>.disposeInstance() {
	if (created) {
		instance?.dispose()
		clear()
	}
}
package com.acornui.observe

import com.acornui.ManagedDisposable
import com.acornui.di.Context
import com.acornui.di.onDisposed
import com.acornui.di.own
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

fun <T> Context.dataBinding(initialValue: T): DataBindingImpl<T> {
	return own(DataBindingImpl(initialValue))
}


/**
 * Immediately, and when the data has changed, the callback will be invoked.
 * @return Returns a handle to dispose of the binding. This will be disposed automatically if this context is disposed.
 */
fun <T> Context.bind(dataBinding: DataBindingRo<T>, callback: (T) -> Unit): ManagedDisposable {
	contract { callsInPlace(callback, InvocationKind.AT_LEAST_ONCE) }
	val handler = { _: T, new: T -> callback(new) }
	dataBinding.changed.add(handler)
	callback(dataBinding.value)
	return onDisposed {
		dataBinding.changed.remove(handler)
	}
}

/**
 * Similar to [bind] except the callback will be given the previous value as well.
 * Immediately, and when the data has changed, the callback will be invoked.
 * The first time the callback is invoked, the `old` parameter will be null.
 */
fun <T> Context.bind2(dataBinding: DataBindingRo<T>, callback: (old: T?, new: T) -> Unit): ManagedDisposable {
	contract { callsInPlace(callback, InvocationKind.AT_LEAST_ONCE) }
	dataBinding.changed.add(callback)
	callback(null, dataBinding.value)
	return onDisposed {
		dataBinding.changed.remove(callback)
	}
}
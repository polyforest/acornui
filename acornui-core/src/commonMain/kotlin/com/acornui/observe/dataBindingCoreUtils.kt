package com.acornui.observe

import com.acornui.Disposable
import com.acornui.di.Context
import com.acornui.di.own
import com.acornui.signal.addWithHandle
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

fun <T> Context.dataBinding(initialValue: T): DataBindingImpl<T> {
	return own(DataBindingImpl(initialValue))
}


/**
 * Immediately, and when the data has changed, the callback will be invoked.
 */
fun <T> Context.bind(dataBinding: DataBindingRo<T>, callback: (T) -> Unit): Disposable {
	contract { callsInPlace(callback, InvocationKind.AT_LEAST_ONCE) }
	val handler = { _: T, new: T -> callback(new) }
	val handle = own(dataBinding.changed.addWithHandle(handler))
	callback(dataBinding.value)
	return handle
}

/**
 * Similar to [bind] except the callback will be given the previous value as well.
 * Immediately, and when the data has changed, the callback will be invoked.
 * The first time the callback is invoked, the `old` parameter will be null.
 */
fun <T> Context.bind2(dataBinding: DataBindingRo<T>, callback: (old: T?, new: T) -> Unit): Disposable {
	contract { callsInPlace(callback, InvocationKind.AT_LEAST_ONCE) }
	val handle = own(dataBinding.changed.addWithHandle(callback))
	callback(null, dataBinding.value)
	return handle
}
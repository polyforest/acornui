package com.acornui.observe

import com.acornui.ManagedDisposable
import com.acornui.Owner
import com.acornui.onDisposed
import com.acornui.own
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

fun <T> Owner.dataBinding(initialValue: T): DataBindingImpl<T> {
	return own(DataBindingImpl(initialValue))
}

/**
 * Immediately, and when the data has changed, the callback will be invoked.
 * @return Returns a handle to dispose of the binding. This will be disposed automatically if this context is disposed.
 */
fun <T> Owner.bind(dataBinding: DataBindingRo<T>, callback: (T) -> Unit): ManagedDisposable {
	contract { callsInPlace(callback, InvocationKind.AT_LEAST_ONCE) }
	val handler = { e: ChangeEvent<T> -> callback(e.newData) }
	val listener = dataBinding.changed.listen(handler)
	callback(dataBinding.value)
	return onDisposed(listener::dispose)
}

/**
 * Similar to [bind] except the callback will be given the previous value as well.
 * Immediately, and when the data has changed, the callback will be invoked.
 * The first time the callback is invoked, the `old` parameter will be null.
 */
fun <T> Owner.bind2(dataBinding: DataBindingRo<T>, callback: (old: T?, new: T) -> Unit): ManagedDisposable {
	contract { callsInPlace(callback, InvocationKind.AT_LEAST_ONCE) }
	val handler = { e: ChangeEvent<T> -> callback(e.oldData, e.newData) }
	val listener = dataBinding.changed.listen(handler)
	callback(null, dataBinding.value)
	return onDisposed(listener::dispose)
}


/**
 * Mirrors changes from two data binding objects. If one changes, the other will be set.
 * @param x
 * @param y [x] and [y] will be bound to each other. [y] will be initially set to the value of the
 * [x].
 */
fun <T> Owner.mirror(x: DataBinding<T>, y: DataBinding<T>): ManagedDisposable {
	require(this !== y) { "Cannot mirror to self" }
	val thisChanged = { e: ChangeEvent<T> ->
		y.value = e.newData
	}
	val otherChanged = { e: ChangeEvent<T> ->
		x.value = e.newData
	}
	val subA = x.changed.listen(thisChanged)
	val subB = y.changed.listen(otherChanged)
	y.value = x.value

	return onDisposed {
		subA.dispose()
		subB.dispose()
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
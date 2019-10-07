@file:Suppress("unused")

package com.acornui.observe

import com.acornui.Disposable
import com.acornui.toDisposable
import com.acornui.signal.Bindable
import com.acornui.signal.bind
import com.acornui.signal.or
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Mirrors changes from two data binding objects. If one changes, the other will be set.
 * @param other The receiver and other will be bound to each other. other will be initially set to the value of the
 * receiver.
 */
fun <T> DataBinding<T>.mirror(other: DataBinding<T>): Disposable {
	require(this !== other) { "Cannot mirror to self" }
	val a = bind {
		other.value = it
	}
	val b = other.bind {
		value = it
	}
	return {
		a.dispose()
		b.dispose()
	}.toDisposable()
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

/**
 * Immediately, and when the data has changed, the callback will be invoked.
 */
inline fun <T> DataBindingRo<T>.bind(crossinline callback: (T) -> Unit): Disposable {
	contract { callsInPlace(callback, InvocationKind.AT_LEAST_ONCE) }
	val handler = { _: T, new: T -> callback(new) }
	changed.add(handler)
	callback(value)
	return {
		changed.remove(handler)
	}.toDisposable()
}

/**
 * Similar to [bind] except the callback will be given the previous value as well.
 * Immediately, and when the data has changed, the callback will be invoked.
 * The first time the callback is invoked, the `old` parameter will be null.
 */
@Suppress("NOTHING_TO_INLINE")
inline fun <T> DataBindingRo<T>.bind2(noinline callback: (old: T?, new: T) -> Unit): Disposable {
	contract { callsInPlace(callback, InvocationKind.AT_LEAST_ONCE) }
	changed.add(callback)
	callback(null, value)
	return {
		changed.remove(callback)
	}.toDisposable()
}
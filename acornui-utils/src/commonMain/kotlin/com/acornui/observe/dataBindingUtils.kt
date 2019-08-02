package com.acornui.observe

import com.acornui.Disposable
import com.acornui.signal.Bindable
import com.acornui.signal.or

/**
 * Mirrors changes from two data binding objects. If one changes, the other will be set.
 * @param other The receiver and other will be bound to each other. other will be initially set to the value of the
 * receiver.
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


infix fun <S, T> DataBindingRo<S>.or(other: DataBindingRo<T>): Bindable {
	return changed or other.changed
}

infix fun <T> DataBindingRo<T>.or(other: Bindable): Bindable {
	return changed or other
}

infix fun <T> Bindable.or(other: DataBindingRo<T>): Bindable {
	return this or other.changed
}
/*
 * Copyright 2019 Poly Forest, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("UNUSED_PARAMETER")

package com.acornui

import com.acornui.di.Context
import com.acornui.function.as1
import com.acornui.signal.Signal
import com.acornui.signal.once
import com.acornui.signal.unmanagedSignal
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job

fun interface Disposable {

	/**
	 * Prepares this object for garbage collection.
	 * A disposed object should no longer be used or referenced.
	 */
	fun dispose()
}

infix fun Disposable.and(other: Disposable): Disposable = Disposable {
	this.dispose()
	other.dispose()
}

/**
 * An [Owner] notifies when it has been disposed, and
 */
interface Owner {

	/**
	 * Dispatched when this object has been disposed.
	 */
	val disposed: Signal<Disposable>

	/**
	 * Returns true if this object has been disposed.
	 */
	val isDisposed: Boolean

}

/**
 * Managed disposables are expected to already be owned.
 */
@Deprecated("Cannot own a managed disposable.", ReplaceWith("value"), level = DeprecationLevel.ERROR)
fun Owner.own(value: ManagedDisposable) : Nothing = error("Cannot own a managed disposable")

/**
 * When creating a [ManagedDisposable], it can be owned with this.
 */
fun <T : ManagedDisposable> Owner.ownUnsafe(value: T) : T  {
	if (isDisposed) throw DisposedException()
	disposed.listen(value::dispose.as1)
	return value
}

/**
 * When this object is disposed, also dispose the given [Disposable] value.
 * If the provided [value] is disposed first, the handle will be released.
 */
fun <T> Owner.own(value: T) : T where T : Disposable?, T : Owner {
	if (isDisposed) throw DisposedException()
	if (value == null) return value
	val handle = disposed.listen(value::dispose.as1)
	value.disposed.listen(handle::dispose.as1)
	return value
}

/**
 * When this object is disposed, invokes the given callback.
 * @return Returns a [ManagedDisposable] where if disposed, invokes the callback and removes
 * management.
 */
fun Owner.own(value: ()->Unit) : ManagedDisposable {
	if (isDisposed) throw DisposedException()
	val handle = disposed.listen(value.as1)
	return ManagedDisposable {
		handle.dispose()
		value()
	}
}

/**
 * When this object is disposed, also dispose the given [Disposable] value.
 */
fun <T : Disposable?> Owner.own(value: T) : T {
	if (isDisposed) throw DisposedException()
	if (value == null) return value
	disposed.listen(value::dispose.as1)
	return value
}

/**
 * When this object is disposed, the job will be cancelled.
 */
fun <T : Job?> Owner.own(value: T) : T {
	if (isDisposed) throw DisposedException()
	if (value == null) return value
	val disposedHandle = disposed.once {
		value.cancel(CancellationException("Owner disposed"))
	}
	value.invokeOnCompletion {
		disposedHandle.dispose()
	}
	return value
}


/**
 * A base class that will dispose a list of disposables upon its own [dispose].
 */
abstract class DisposableBase() : Disposable, Owner {

	/**
	 * If an owner is provided, when the owner is disposed, this will be disposed.
	 */
	constructor(owner: Owner?) : this() {
		owner?.ownThis()
	}

	final override val disposed = unmanagedSignal<Disposable>()

	final override var isDisposed = false
		private set

	/**
	 * Watches the given owner for disposal, disposing this object in turn.
	 * If this object is disposed before the owner, the watch will end.
	 */
	protected fun Owner.ownThis() {
		val t = this@DisposableBase
		require(t is ManagedDisposable) { "this must implement ManagedDisposable" }
		t.own(disposed.listen(t::dispose.as1))
	}

	/**
	 * Throws a [DisposedException] if this object is disposed.
	 */
	protected fun checkDisposed() {
		if (isDisposed)
			throw DisposedException()
	}

	/**
	 * Disposes this object and all objects registered via [own].
	 */
	override fun dispose() {
		checkDisposed()
		isDisposed = true
		disposed.dispatch(this)
		disposed.dispose()
	}
}

/**
 * An object that may be disposed, and will automatically be disposed by its creator.
 */
fun interface ManagedDisposable : Disposable

class DisposedException : IllegalStateException("This component has been disposed")

fun <A : Comparable<A>> A?.compareTo(other: A?): Int {
	if (this == null && other == null) return 0
	if (this == null) return -1
	if (other == null) return 1
	return compareTo(other)
}

typealias EqualityCheck<E> = (a: E, b: E) -> Boolean

/**
 * Converts a lambda to a [ManagedDisposable] object, where the lambda is called on [Disposable.dispose].
 */
fun (()->Any?).toManagedDisposable() = ManagedDisposable { this@toManagedDisposable() }

fun (()->Any?).toDisposable() = Disposable { this@toDisposable() }

/**
 * Used to mark parts of the Acorn API as not being ready for public.
 */
@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
annotation class ExperimentalAcorn

/**
 * Invokes the given callback when this context is disposed.
 *
 * @param callback The callback to invoke on [Context.disposed].
 *
 * @return Returns a [ManagedDisposable] reference where, if disposed, will invoke [callback] and remove the disposed
 * handling.
 */
fun Owner.onDisposed(callback: () -> Unit): ManagedDisposable {
	return object : ManagedDisposable {
		private val handle = disposed.listen(::dispose.as1)
		override fun dispose() {
			callback()
			handle.dispose()
		}
	}
}

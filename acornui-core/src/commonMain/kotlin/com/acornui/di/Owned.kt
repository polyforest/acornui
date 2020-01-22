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

package com.acornui.di

import com.acornui.Disposable
import com.acornui.DisposedException
import com.acornui.Lifecycle
import com.acornui.component.ComponentInit
import com.acornui.function.as1
import com.acornui.signal.Signal
import com.acornui.signal.Signal1
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * When an [Owned] object is created, the creator is its owner.
 * This is used for dependency injection, styling, and organization.
 */
interface Owned : Scoped {

	/**
	 * Returns true if this [Owned] object has been disposed.
	 */
	val isDisposed: Boolean

	/**
	 * Dispatched then this object has been disposed.
	 */
	val disposed: Signal<(Owned) -> Unit>

	/**
	 * The creator of this instance.
	 */
	val owner: Owned?

}

/**
 * Wraps the callback in an `if (!isDisposed)` block.
 */
@Deprecated("launch and async will now cancel on disposal.")
inline fun Owned.notDisposed(crossinline callback: () -> Unit): () -> Unit {
	return { if (!isDisposed) callback() }
}

/**
 * Wraps the callback in an `if (!isDisposed)` block.
 */
@Deprecated("launch and async will now cancel on disposal.")
inline fun <P1> Owned.notDisposed(crossinline callback: (P1) -> Unit): (P1) -> Unit {
	return { p1 -> if (!isDisposed) callback(p1) }
}

/**
 * Wraps the callback in an `if (!isDisposed)` block.
 */
@Deprecated("launch and async will now cancel on disposal.")
inline fun <P1, P2> Owned.notDisposed(crossinline callback: (P1, P2) -> Unit): (P1, P2) -> Unit {
	return { p1, p2 -> if (!isDisposed) callback(p1, p2) }
}

/**
 * Wraps the callback in an `if (!isDisposed)` block.
 */
@Deprecated("launch and async will now cancel on disposal.")
inline fun <P1, P2, P3> Owned.notDisposed(crossinline callback: (P1, P2, P3) -> Unit): (P1, P2, P3) -> Unit {
	return { p1, p2, p3 -> if (!isDisposed) callback(p1, p2, p3) }
}

/**
 * Wraps the callback in an `if (!isDisposed)` block.
 */
@Deprecated("launch and async will now cancel on disposal.")
inline fun <P1, P2, P3, P4> Owned.notDisposed(crossinline callback: (P1, P2, P3, P4) -> Unit): (P1, P2, P3, P4) -> Unit {
	return { p1, p2, p3, p4 -> if (!isDisposed) callback(p1, p2, p3, p4) }
}

inline fun Owned.createScope(vararg dependenciesList: DependencyPair<*>, init: ComponentInit<Owned> = {}): Owned  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return createScope(dependenciesList.toList(), init)
}

inline fun Owned.createScope(dependenciesList: List<DependencyPair<*>>, init: ComponentInit<Owned> = {}): Owned  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val owner = this
	val o = object : Owned {
		override val isDisposed: Boolean = false
		override val disposed: Signal<(Owned) -> Unit> = owner.disposed
		override val owner: Owned? = owner
		override val injector: Injector = owner.injector + dependenciesList
	}
	o.init()
	return o
}

fun Owned.owns(other: Owned?): Boolean {
	var p: Owned? = other
	while (p != null) {
		if (p == this) return true
		p = p.owner
	}
	return false
}

/**
 * When this object is disposed, the target will also be disposed.
 */
fun <T : Disposable> Owned.own(target: T): T {
	val disposer: (Owned) -> Unit = { target.dispose() }
	disposed.add(disposer)
	if (target is Lifecycle) {
		target.disposed.add {
			disposed.remove(disposer)
		}
	}
	return target
}

/**
 * Factory methods for components typically don't have separated [owner] and [injector] parameters. This
 * implementation can be used to have a different dependency injector than what the owner uses.
 */
open class OwnedImpl(
		final override val owner: Owned?,
		final override val injector: Injector
) : Owned, Disposable {

	/**
	 * Constructs this OwnedImpl with no owner and the provided injector.
	 */
	constructor(injector: Injector) : this(null, injector)

	/**
	 * Constructs this OwnedImpl with the same injector as the owner.
	 */
	constructor(owner: Owned) : this(owner, owner.injector)

	private var _isDisposed = false
	override val isDisposed: Boolean
		get() = _isDisposed

	private val _disposed = Signal1<Owned>()
	override val disposed = _disposed.asRo()

	init {
		owner?.disposed?.add(::dispose.as1)
	}

	override fun dispose() {
		if (_isDisposed) throw DisposedException()
		_isDisposed = true
		owner?.disposed?.remove(::dispose.as1)
		_disposed.dispatch(this)
		_disposed.dispose()
	}
}

/**
 * Traverses this Owned object's ownership lineage, invoking a callback on each owner up the chain.
 * (including this object)
 * @param callback The callback to invoke on each owner ancestor. If this callback returns true, iteration will
 * continue, if it returns false, iteration will be halted.
 * @return If [callback] returned false, this method returns the element on which the iteration halted.
 */
inline fun Owned.ownerWalk(callback: (Owned) -> Boolean): Owned? {
	var p: Owned? = this
	while (p != null) {
		val shouldContinue = callback(p)
		if (!shouldContinue) return p
		p = p.owner
	}
	return null
}

package com.acornui.core.di

import com.acornui.async.Deferred
import com.acornui.async.Work
import com.acornui.component.ComponentInit
import com.acornui.core.Disposable
import com.acornui.core.DisposedException
import com.acornui.core.Lifecycle
import com.acornui.signal.Signal
import com.acornui.signal.Signal1

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

	//----------------------------------------------
	// Async utility
	//----------------------------------------------

	/**
	 * Wraps [com.acornui.async.async] with a check that will fail the task if this object is disposed.
	 *
	 * Example usage:
	 * ```
	 * async {
	 *   delay(3f)
	 * } then {
	 *   println("I'm done!")
	 * } catch {
	 *   println("I failed.")
	 * }
	 * ```
	 */
	fun <T> async(work: Work<T>): Deferred<T> = com.acornui.async.async {
		val result = work()
		if (isDisposed) {
			throw DisposedException()
		}
		result
	}

}

fun Owned.createScope(vararg dependenciesList: DependencyPair<*>, init: ComponentInit<Owned> = {}): Owned {
	return createScope(dependenciesList.toList(), init)
}

fun Owned.createScope(dependenciesList: List<DependencyPair<*>>, init: ComponentInit<Owned> = {}): Owned {
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
	override val disposed: Signal<(Owned) -> Unit>
		get() = _disposed

	private val ownerDisposedHandler = {
		owner: Owned ->
		dispose()
	}

	init {
		owner?.disposed?.add(ownerDisposedHandler)
	}

	override fun dispose() {
		if (_isDisposed) throw DisposedException()
		_isDisposed = true
		owner?.disposed?.remove(ownerDisposedHandler)
		_disposed.dispatch(this)
		_disposed.dispose()
	}
}
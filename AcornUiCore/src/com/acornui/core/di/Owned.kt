package com.acornui.core.di

import com.acornui.core.Disposable
import com.acornui.core.Lifecycle
import com.acornui.signal.Signal
import com.acornui.signal.Signal1

/**
 * When an [Owned] object is created, the creator is its owner.
 * This is used for dependency injection, styling, and organization.
 */
interface Owned : Scoped {

	/**
	 * Dispatched then this object has been disposed.
	 */
	val disposed: Signal<(Owned) -> Unit>

	/**
	 * The creator of this instance.
	 */
	val owner: Owned?

}

fun Owned.createScope(vararg dependenciesList: DependencyPair<*>): Owned {
	return createScope(dependenciesList.toList())
}

fun Owned.createScope(dependenciesList: List<DependencyPair<*>>): Owned {
	val r = this
	return object : Owned {
		override val disposed: Signal<(Owned) -> Unit>
			get() = r.disposed
		override val owner: Owned?
			get() = r
		override val injector: Injector = r.injector + dependenciesList
	}
}

fun Owned.owns(other: Owned): Boolean {
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
class OwnedImpl(
		override val injector: Injector,
		override val owner: Owned? = null
) : Owned, Disposable {

	private val _disposed = Signal1<Owned>()
	override val disposed: Signal<(Owned) -> Unit>
		get() = _disposed

	override fun dispose() {
		_disposed.dispatch(this)
		_disposed.dispose()
	}
}
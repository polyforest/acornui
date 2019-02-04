package com.acornui.core.di

import com.acornui.async.LateValue
import com.acornui.async.awaitAll
import com.acornui.async.launch
import com.acornui.core.Disposable

class Bootstrap : Disposable {

	private val dependenciesList = ArrayList<DependencyPair<*>>()

	/**
	 * Creates a new injector with the dependencies set on this bootstrap.
	 */
	suspend fun createInjector(parentInjector: Injector? = null): Injector {
		awaitAll()

		return InjectorImpl(parentInjector, dependenciesList)
	}

	private val _map = HashMap<DKey<*>, LateValue<Any>>()

	suspend fun <T : Any> get(key: DKey<T>): T {
		val late = _map.getOrPut(key) { LateValue() }
		@Suppress("UNCHECKED_CAST")
		return late.await() as T
	}

	fun <T : Any> set(key: DKey<T>, value: T) {
		dependenciesList.add(key to value)

		var p: DKey<*>? = key
		while (p != null) {
			val late = _map.getOrPut(p) { LateValue() }
			if (!late.isPending)
				throw Exception("value already set for key $p")
			late.setValue(value)
			p = p.extends
		}
	}

	suspend fun awaitAll() {
		_map.awaitAll()
	}

	override fun dispose() {
		launch {
			// Waits for all of the dependencies to be calculated before attempting to dispose.
			awaitAll()
			// Dispose the dependencies in the reverse order they were added:
			for (i in dependenciesList.lastIndex downTo 0) {
				(dependenciesList[i].value as? Disposable)?.dispose()
			}
		}
	}
}
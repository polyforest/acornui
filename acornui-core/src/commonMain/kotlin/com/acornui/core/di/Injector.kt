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

package com.acornui.core.di

import com.acornui.async.PendingDisposablesRegistry
import com.acornui.core.Disposable


/**
 * A scoped object has a dependency injector.
 */
interface Scoped {

	/**
	 * The dependency injector for this scope.
	 * Implementations should be immutable.
	 */
	val injector: Injector
}

fun <T : Any> Scoped.injectOptional(key: DKey<T>): T? = injector.injectOptional(key)
fun <T : Any> Scoped.inject(key: DKey<T>): T = injector.inject(key)

interface Injector {

	/**
	 * Returns true if this injector contains a dependency with the given key.
	 */
	fun containsKey(key: DKey<*>): Boolean

	fun <T : Any> injectOptional(key: DKey<T>): T?

	fun <T : Any> inject(key: DKey<T>): T {
		@Suppress("UNCHECKED_CAST")
		return injectOptional(key) ?: throw Exception("Dependency not found for key: $key")
	}
}

operator fun Injector.plus(dependenciesList: List<DependencyPair<*>>): Injector {
	return InjectorImpl(this, dependenciesList)
}

class InjectorImpl(
		private val parent: Injector?,
		dependenciesList: List<DependencyPair<*>>
) : Injector {

	private val dependencies = HashMap<DKey<*>, Any>()

	constructor(dependenciesList: List<DependencyPair<*>>) : this(null, dependenciesList)

	init {
		for ((key, value) in dependenciesList) {
			_set(key, value)
		}
	}

	override fun containsKey(key: DKey<*>): Boolean {
		return dependencies.containsKey(key)
	}

	override fun <T : Any> injectOptional(key: DKey<T>): T? {
		@Suppress("UNCHECKED_CAST")
		var d = dependencies[key] as T?
		if (d == null) {
			d = parent?.injectOptional(key)
			if (d == null && parent == null) {
				d = key.factory(this)
				if (d != null) {
					// If the dependency key's factory method produces an instance, set it on the root injector.
					if (d is Disposable)
						PendingDisposablesRegistry.register(d)
					_set(key, d)
				}
			}
		}
		return d
	}

	private fun _set(key: DKey<out Any>, value: Any) {
		var p: DKey<*>? = key
		while (p != null) {
			if (dependencies.containsKey(p))
				throw Exception("Injector already contains dependency $p")
			dependencies[p] = value
			p = p.extends
		}
	}
}

/**
 * A DependencyKey is a marker interface indicating an object representing a key of a specific dependency type.
 */
@Suppress("AddVarianceModifier")
interface DKey<T : Any> {

	val extends: DKey<*>?
		get() = null

	/**
	 * A dependency key has an optional factory method, where if it provides a non-null value, the
	 * dependency doesn't need to be set before use, and the factory method will produce the default implementation.
	 */
	fun factory(injector: Injector): T? = null

	infix fun to(value: T): DependencyPair<T> = DependencyPair(this, value)
}


data class DependencyPair<T : Any>(val key: DKey<T>, val value: T)

@Deprecated("Use dKey", ReplaceWith("dKey()"))
open class DependencyKeyImpl<T : Any> : DKey<T>

/**
 * Creates a dependency key with the given factory function.
 */
fun <T : Any> dKey(): DKey<T> {
	return object : DKey<T> {}
}

fun <T : SuperKey, SuperKey : Any> dKey(extends: DKey<SuperKey>): DKey<T> {
	return object : DKey<T> {
		override val extends: DKey<SuperKey>? = extends
	}
}

/*
 * Copyright 2020 Poly Forest, LLC
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
import com.acornui.Idempotent
import com.acornui.Lifecycle
import com.acornui.collection.copy
import com.acornui.function.as1
import com.acornui.signal.Signal
import com.acornui.signal.Signal1
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext
import kotlin.jvm.JvmName
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * An Acorn Context
 */
interface Context : CoroutineScope {

	/**
	 * The context that constructed this context.
	 */
	val owner: Context?

	/**
	 * The dependencies to use when constructing new objects.
	 */
	var childDependencies: DependencyMap

	/**
	 * Returns true if this object has been disposed.
	 */
	val isDisposed: Boolean

	/**
	 * Dispatched when this object has been disposed.
	 */
	val disposed: Signal<(Context) -> Unit>

	/**
	 * Returns true if this context contains a dependency with the given key.
	 */
	fun containsKey(key: DKey<*>): Boolean

	fun <T : Any> injectOptional(key: DKey<T>): T?

	fun <T : Any> inject(key: DKey<T>): T

	/**
	 * Returns an iterator for iterating over all dependencies.
	 */
	fun dependenciesIterator(): Iterator<DependencyPair<*>>
}

/**
 * Constructs a new context with the receiver as the owner and [Context.childDependencies] as the new dependencies.
 */
fun Context.childContext(): Context = ContextImpl(this, childDependencies)

/**
 * A basic [Context] implementation. 
 */
open class ContextImpl(
		final override val owner: Context? = null,
		private val dependencies: DependencyMap = DependencyMap(),
		override val coroutineContext: CoroutineContext = (owner?.coroutineContext ?: GlobalScope.coroutineContext) + Job(owner?.coroutineContext?.get(Job))
) : Context, Disposable {

	constructor(owner: Context?, dependencies: List<DependencyPair<*>>) : this(owner, DependencyMap(dependencies))
	constructor(dependencies: DependencyMap) : this(null, dependencies)
	constructor(owner: Context) : this(owner, owner.childDependencies)
	constructor(dependencies: List<DependencyPair<*>>) : this(null, dependencies)

	private val _disposed = Signal1<Context>()
	override val disposed = _disposed.asRo()

	final override var childDependencies: DependencyMap = dependencies

	final override fun containsKey(key: DKey<*>): Boolean {
		return dependencies.containsKey(key)
	}

	final override fun <T : Any> injectOptional(key: DKey<T>): T? {
		return dependencies[key] ?: run {
			// Dependency not found
			@Suppress("UNCHECKED_CAST")
			val defaultDependencies = defaultDependencies as MutableMap<DKey<T>, T?>
			if (defaultDependencies.containsKey(key)) {
				defaultDependencies[key]
			} else {
				if (constructing.contains(key))
					throw CyclicDependencyException("Cyclic dependency detected: ${constructing.joinToString(" -> ")} -> $key")
				constructing.add(key)
				var root: Context = this
				while (root.owner != null) root = root.owner!!
				val fromFactory = key.factory(root)
				constructing.remove(key)
				defaultDependencies[key] = fromFactory
				fromFactory
			}
		}
	}

	final override fun <T : Any> inject(key: DKey<T>): T {
		@Suppress("UNCHECKED_CAST")
		return injectOptional(key) ?: error("Dependency not found for key: $key")
	}

	final override var isDisposed: Boolean = false
		private set

	init {
		owner?.disposed?.add(::dispose.as1)
	}

	final override fun dependenciesIterator(): Iterator<DependencyPair<Any>> = dependencies.iterator()

	override fun dispose() {
		checkDisposed()
		isDisposed = true
		coroutineContext[Job]?.cancel()
		owner?.disposed?.remove(::dispose.as1)
		_disposed.dispatch(this)
		_disposed.dispose()
	}

	/**
	 * Throws a [DisposedException] if this component is disposed.
	 */
	fun checkDisposed() {
		if (isDisposed)
			throw DisposedException()
	}

	companion object {
		private val defaultDependencies = HashMap<DKey<*>, Any?>()
		private val constructing = ArrayList<DKey<*>>()
	}
}

@Deprecated("Context objects are owned implicitly", ReplaceWith("target"), level = DeprecationLevel.ERROR)
@JvmName("improperOwn")
@Suppress("unused", "UNUSED_PARAMETER")
fun <T : Context> Context.own(target: T): T {
	error("Context objects are owned implicitly")
}

/**
 * When this object is disposed, the target will also be disposed.
 */
fun <T : Disposable> Context.own(target: T): T {
	val callback = target::dispose.as1
	require(!disposed.contains(callback)) { "target already owned." }
	disposed.add(callback)
	if (target is Lifecycle) {
		target.disposed.add {
			// If the target is disposed before this context is, remove the handler.
			disposed.remove(callback)
		}
	}
	return target
}

fun Context?.owns(other: Context?): Boolean {
	if (this == null) return false
	var p: Context? = other
	while (p != null) {
		if (p == this) return true
		p = p.owner
	}
	return false
}

/**
 * Traverses this Owned object's ownership lineage, invoking a callback on each owner up the chain.
 * (including this object)
 * @param callback The callback to invoke on each owner ancestor. If this callback returns true, iteration will
 * continue, if it returns false, iteration will be halted.
 * @return If [callback] returned false, this method returns the element on which the iteration halted.
 */
fun Context.ownerWalk(callback: (Context) -> Boolean): Context? {
	var p: Context? = this
	while (p != null) {
		val shouldContinue = callback(p)
		if (!shouldContinue) return p
		p = p.owner
	}
	return null
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
	@Idempotent
	fun factory(context: Context): T? = null

	operator fun provideDelegate(
			thisRef: Context,
			prop: KProperty<*>
	): ReadOnlyProperty<Context, T> = LazyDependency(this)

	infix fun to(value: T): DependencyPair<T> = DependencyPair(this, value)
}

data class DependencyPair<T : Any>(val key: DKey<T>, val value: T)



/**
 * A dependency map is a mutable map that enforces that the dependency key is the right type for the value.
 */
class DependencyMap private constructor(private val inner: Map<DKey<*>, Any>) : Iterable<DependencyPair<Any>> {

	constructor(dependencies: Iterable<DependencyPair<*>>) : this(dependencies.toDependencyMap())
	
	constructor() : this(emptyMap())

	/**
	 * Returns the number of key/value pairs in the map.
	 */
	val size: Int = inner.size

	/**
	 * Returns `true` if the map is empty (contains no elements), `false` otherwise.
	 */
	fun isEmpty(): Boolean = inner.isEmpty()

	/**
	 * Returns `true` if this map is not empty.
	 */
	fun isNotEmpty(): Boolean = inner.isNotEmpty()

	/**
	 * Returns `true` if the map contains the specified [key].
	 */
	fun containsKey(key: DKey<*>): Boolean = inner.containsKey(key)

	operator fun <T : Any> get(key: DKey<T>): T? {
		@Suppress("UNCHECKED_CAST")
		return inner[key] as T?
	}
	
	/**
	 * Creates a new read-only dependency map by replacing or adding entries to this map from another.
	 */
	operator fun plus(other: DependencyMap): DependencyMap {
		if (other.isEmpty()) return this
		if (isEmpty()) return other
		return DependencyMap(inner + other.inner)
	}
	
	/**
	 * Creates a new read-only dependency map by replacing or adding entries to this map from another.
	 */
	operator fun plus(dependencies: List<DependencyPair<*>>): DependencyMap {
		if (dependencies.isEmpty()) return this
		if (isEmpty()) return DependencyMap(dependencies)
		return DependencyMap(inner + dependencies.toDependencyMap())
	}

	/**
	 * Creates a new dependency map with the new dependency added or replacing the existing dependency of that key.
	 */
	operator fun plus(dependency: DependencyPair<*>): DependencyMap {
		val new = inner.copy()
		new[dependency.key] = dependency.value
		return DependencyMap(new)
	}

	override fun iterator(): Iterator<DependencyPair<Any>> {
		return object : Iterator<DependencyPair<Any>> {
			private val innerIt = inner.iterator()

			override fun hasNext(): Boolean = innerIt.hasNext()

			override fun next(): DependencyPair<Any> {
				val n = innerIt.next()
				@Suppress("UNCHECKED_CAST")
				return n.key as DKey<Any> to n.value
			}
		}
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other == null || this::class != other::class) return false
		other as DependencyMap
		if (inner != other.inner) return false
		return true
	}

	override fun hashCode(): Int {
		return inner.hashCode()
	}
}

private fun Iterable<DependencyPair<*>>.toDependencyMap(): Map<DKey<*>, Any> {
	val map = HashMap<DKey<*>, Any>()
	for ((key, value) in this) {
		var p: DKey<*>? = key
		while (p != null) {
			map[p] = value
			p = p.extends
		}
	}
	return map
}

fun dependencyMapOf(vararg dependencies: DependencyPair<*>) = DependencyMap(dependencies.toList())

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

private class LazyDependency<T : Any>(private val key: DKey<T>) : ReadOnlyProperty<Context, T> {

	private var value: T? = null

	override fun getValue(thisRef: Context, property: KProperty<*>): T {
		if (value == null)
			value = thisRef.inject(key)
		return value as T
	}
}

class CyclicDependencyException(message: String) : Exception(message)
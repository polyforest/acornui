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
import com.acornui.Lifecycle
import com.acornui.ManagedDisposable
import com.acornui.collection.copy
import com.acornui.component.ComponentInit
import com.acornui.function.as1
import com.acornui.signal.Signal
import com.acornui.signal.Signal1
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.CoroutineContext
import kotlin.jvm.JvmName
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * An Acorn Context is the base object for providing:
 *  - Ownership hierarchy
 *  - Dependency injection
 *  - Coroutine scoping
 *  - Disposable cleanup
 */
interface Context : CoroutineScope {

	/**
	 * The context that constructed this context.
	 */
	val owner: Context?

	/**
	 * If this context represents a special scope (e.g. [ContextMarker.MAIN] or [ContextMarker.APPLICATION]),
	 * this will be set.
	 */
	val marker: ContextMarker?

	/**
	 * The map of dependencies to use for injection.
	 * Don't retrieve dependencies from this map, use [inject] which will make use of factories and key inheritance.
	 *
	 * @see inject
	 */
	val dependencies: DependencyMap

	/**
	 * Returns true if this object has been disposed.
	 */
	val isDisposed: Boolean

	/**
	 * Dispatched when this object has been disposed.
	 */
	val disposed: Signal<(self: Context) -> Unit>

	/**
	 * Returns true if this context contains a dependency with the given key.
	 */
	fun containsKey(key: Key<*>): Boolean

	fun <T : Any> injectOptional(key: Key<T>): T?

	fun <T : Any> inject(key: Key<T>): T

	/**
	 * A context key is used for getting and setting dependencies on a [Context].
	 */
	interface Key<T : Any> {

		/**
		 * This key's supertype.
		 * If not null, when the context installs a dependency with this key, it will also install that dependency for
		 * this supertype key.
		 *
		 * The parameterized type of this key should be Key<S> where S : T.
		 */
		val extends: Key<*>?
			get() = null

		/**
		 * When a dependency is requested that isn't found, this factory (if not null) will be used to construct a new
		 * instance.
		 */
		val factory: DependencyFactory<T>?
			get() = null

		operator fun provideDelegate(
				thisRef: Context,
				prop: KProperty<*>
		): ReadOnlyProperty<Context, T> = LazyDependency(this)

		infix fun to(value: T): DependencyPair<T> = DependencyPair(this, value)
	}

	/**
	 * A factory for providing dependency instances.
	 */
	interface DependencyFactory<out T : Any> {

		/**
		 * Constructs the new dependency using the context found where [Context.marker] == [installTo].
		 * The new dependency will be installed on that matching context and will be reused on the next injection
		 * from that context and its children.
		 *
		 * @param context The context found where [Context.marker] == [installTo]
		 */
		operator fun invoke(context: Context): T

		/**
		 * Returns the scope for which the new dependency should be installed.
		 * If the scope isn't found, a [ContextMarkerNotFoundException] exception will be thrown.
		 */
		val installTo: ContextMarker
			get() = ContextMarker.APPLICATION
	}

	class ContextMarkerNotFoundException(val key: Key<*>) : IllegalStateException("Scope \"${key.factory!!.installTo.value}\" not found for Context.Key $key")
}

/**
 * Creates a new dependency factory for the given scope and constructor.
 */
fun <T : Any> dependencyFactory(scope: ContextMarker = ContextMarker.APPLICATION, create: (context: Context) -> T): Context.DependencyFactory<T> {
	return object : Context.DependencyFactory<T> {
		override val installTo: ContextMarker = scope
		override fun invoke(context: Context): T = create(context)
	}
}

/**
 * Constructs a new context with the receiver as the owner and [Context.childDependencies] as the new dependencies.
 */
fun Context.childContext(): Context = ContextImpl(this, dependencies)

/**
 * A basic [Context] implementation.
 */
open class ContextImpl(
		final override val owner: Context? = null,
		final override var dependencies: DependencyMap = DependencyMap(),
		final override val coroutineContext: CoroutineContext = (owner?.coroutineContext
				?: GlobalScope.coroutineContext) + Job(owner?.coroutineContext?.get(Job)),
		override val marker: ContextMarker? = null
) : Context, ManagedDisposable {

	constructor(owner: Context?, dependencies: List<DependencyPair<*>>) : this(owner, DependencyMap(dependencies))
	constructor(dependencies: DependencyMap) : this(null, dependencies)
	constructor(owner: Context) : this(owner, owner.dependencies)
	constructor(dependencies: List<DependencyPair<*>>) : this(null, dependencies)

	private val constructing = ArrayList<Context.Key<*>>()

	private val _disposed = Signal1<Context>()
	override val disposed = _disposed.asRo()

	@Deprecated("Child dependencies no longer separated.", ReplaceWith("dependencies"), DeprecationLevel.ERROR)
	var childDependencies: DependencyMap = dependencies

	final override fun containsKey(key: Context.Key<*>): Boolean {
		return dependencies.containsKey(key)
	}

	final override fun <T : Any> injectOptional(key: Context.Key<T>): T? {
		return dependencies[key] ?: run {
			val factory = key.factory ?: return null
			val factoryScope = factory.installTo
			val contextWithScope = findOwner { it.marker === factoryScope }
					?: throw Context.ContextMarkerNotFoundException(key)
			if (contextWithScope != this)
				return install(key, contextWithScope.injectOptional(key))
			if (constructing.contains(key))
				throw CyclicDependencyException("Cyclic dependency detected: ${constructing.joinToString(" -> ")} -> $key")
			constructing.add(key)
			val newInstance: T = factory(this)
			install(key, newInstance)
			constructing.remove(key)
			return newInstance
		}
	}

	@JvmName("installNullable")
	private fun <T : Any> install(key: Context.Key<T>, instance: T?): T? {
		return if (instance == null) null else install(key, instance)
	}

	private fun <T : Any> install(key: Context.Key<T>, instance: T): T {
		val newDependency = key to instance
		dependencies += newDependency
		return instance
	}

	final override fun <T : Any> inject(key: Context.Key<T>): T {
		@Suppress("UNCHECKED_CAST")
		return injectOptional(key) ?: error("Dependency not found for key: $key")
	}

	final override var isDisposed: Boolean = false
		private set

	init {
		owner?.disposed?.add(::dispose.as1)
	}

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
}

/**
 * A `ContextMarker` marks special [Context] objects.
 */
class ContextMarker(val value: String) {

	companion object {

		/**
		 * The context created from `runMain`. This will typically be the most global scope.
		 */
		val MAIN = ContextMarker("Main")

		/**
		 * The context created from an application. There may be multiple applications created from a single `runMain`.
		 */
		val APPLICATION = ContextMarker("Application")
	}
}

/**
 * When this object is disposed, the target will also be disposed.
 */
fun <T : Disposable?> Context.own(target: T): T {
	if (target == null) return target
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

@Deprecated("Context objects are owned implicitly", ReplaceWith("target"), level = DeprecationLevel.ERROR)
@JvmName("improperOwn")
@Suppress("unused")
fun Context.own(target: ManagedDisposable): Context {
	error("Context objects are owned implicitly")
}

/**
 * Invokes the given callback when this context is disposed.
 *
 * @param callback The callback to invoke on [Context.disposed].
 *
 * @return Returns a [ManagedDisposable] reference where, if disposed, will invoke [callback] and remove the disposed
 * handling.
 */
fun Context.onDisposed(callback: () -> Unit): ManagedDisposable {
	return object : ManagedDisposable {
		init {
			disposed.add(::dispose.as1)
		}

		override fun dispose() {
			callback()
			disposed.remove(::dispose.as1)
		}
	}
}

inline fun Context.context(init: ComponentInit<ContextImpl>): ContextImpl {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return ContextImpl(this).apply(init)
}

/**
 * Returns true if `this` is an ancestor in [other]'s ownership chain.
 * Ownership chains are based on construction, not based on display hierarchy.
 */
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
 * Returns a new context where the new context's owner is `this` and the [Context.coroutineContext] contains elements
 * of both this [Context.coroutineContext] and [context].
 *
 * @see [CoroutineContext.plus]
 */
operator fun Context.plus(context: CoroutineContext): Context {
	return ContextImpl(owner = this, dependencies = dependencies, coroutineContext = coroutineContext + context)
}

/**
 * Traverses this Owned object's ownership lineage (starting with `this`), invoking a callback on each owner up the
 * chain.
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
 * Traverses this Owned object's ownership lineage (starting with `this`), returning the first [Context] where
 * [callback] returns true.
 *
 * @param callback Invoked on each ancestor until true is returned.
 * @return Returns the first ancestor for which [callback] returned true.
 */
inline fun Context.findOwner(crossinline callback: (Context) -> Boolean): Context? = ownerWalk { !callback(it) }

/**
 * A DependencyKey is a marker interface indicating an object representing a key of a specific dependency type.
 */
@Deprecated("Use Context.Key", ReplaceWith("Context.Key<T>"))
typealias DKey<T> = Context.Key<T>

data class DependencyPair<T : Any>(val key: Context.Key<T>, val value: T)

/**
 * A dependency map is a mutable map that enforces that the dependency key is the right type for the value.
 */
class DependencyMap private constructor(private val inner: Map<Context.Key<*>, Any>) : Iterable<DependencyPair<Any>> {

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
	fun containsKey(key: Context.Key<*>): Boolean = inner.containsKey(key)

	operator fun <T : Any> get(key: Context.Key<T>): T? {
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
				return n.key as Context.Key<Any> to n.value
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

private fun Iterable<DependencyPair<*>>.toDependencyMap(): Map<Context.Key<*>, Any> {
	val map = HashMap<Context.Key<*>, Any>()
	for ((key, value) in this) {
		var p: Context.Key<*>? = key
		while (p != null) {
			map[p] = value
			p = p.extends
		}
	}
	return map
}

val emptyDependencies = DependencyMap()

fun dependencyMapOf(vararg dependencies: DependencyPair<*>) =
		if (dependencies.isEmpty()) emptyDependencies else DependencyMap(dependencies.toList())

/**
 * Creates a dependency key with the given factory function.
 */
fun <T : Any> contextKey(): Context.Key<T> {
	return object : Context.Key<T> {}
}

fun <T : SuperKey, SuperKey : Any> contextKey(extends: Context.Key<SuperKey>): Context.Key<T> {
	return object : Context.Key<T> {
		override val extends: Context.Key<SuperKey>? = extends
	}
}

private class LazyDependency<T : Any>(private val key: Context.Key<T>) : ReadOnlyProperty<Context, T> {

	private var value: T? = null

	override fun getValue(thisRef: Context, property: KProperty<*>): T {
		if (value == null)
			value = thisRef.inject(key)
		return value as T
	}
}

private class OptionalLazyDependency<T : Any>(private val key: Context.Key<T>) : ReadOnlyProperty<Context, T?> {

	private var valueIsSet = false
	private var value: T? = null

	override fun getValue(thisRef: Context, property: KProperty<*>): T? {
		if (!valueIsSet) {
			value = thisRef.injectOptional(key)
			valueIsSet = true
		}
		return value
	}
}

class CyclicDependencyException(message: String) : Exception(message)
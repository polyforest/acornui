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
import com.acornui.assertionsEnabled
import com.acornui.async.Work
import com.acornui.collection.removeFirst
import com.acornui.component.ComponentInit
import com.acornui.logging.Log
import kotlinx.coroutines.*
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.CoroutineContext
import kotlin.jvm.Synchronized
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlin.time.Duration
import kotlin.time.seconds

class Bootstrap(
		override val coroutineContext: CoroutineContext,
		private val defaultTaskTimeout: Duration = 10.seconds
) : Disposable, CoroutineScope {

	private val dependenciesList = ArrayList<DependencyPair<*>>()
	private var hasStarted = false

	suspend fun dependencies(): DependencyMap {
		awaitAll()
		return DependencyMap(dependenciesList)
	}

	private val _map = HashMap<DKey<*>, Deferred<Any>>()

	suspend fun <T : Any> get(key: DKey<T>): T {
		hasStarted = true
		val d = _map.getOrElse(key) {
			error("No task has been registered that provides key $key.")
		}
		@Suppress("UNCHECKED_CAST")
		return d.await() as T
	}

	suspend fun <T : Any> getOptional(key: DKey<T>): T? {
		val d = _map[key] ?: return null
		@Suppress("UNCHECKED_CAST")
		return d.await() as T
	}

	/**
	 * Sets a dependency directly without creating a task.
	 */
	@Synchronized
	fun <T : Any> set(dKey: DKey<T>, value: T) {
		check(!hasStarted) { "Cannot set a dependency after the bootstrap has been started." }
		var p: DKey<*>? = dKey
		while (p != null) {
			dependenciesList.removeFirst { it.key == p }
			_map[p] = async { value }
			p = p.extends
		}
		dependenciesList.add(dKey to value)
	}

	@Synchronized
	private fun <T : Any> addDependency(dKey: DKey<T>, value: T) {
		val pair = dKey to value

		if (assertionsEnabled && dependenciesList.any { existingDependency ->
					var p: DKey<*>? = dKey
					while (p != null) {
						if (existingDependency.key == p)
							return@any true
						p = p.extends
					}
					return@any false
				})
			throw Exception("Dependency for $dKey is already set.")

		dependenciesList.add(pair)
	}

	suspend fun awaitAll() {
		hasStarted = true
		_map.values.awaitAll()
	}

	override fun dispose() {
		for (i in dependenciesList.lastIndex downTo 0) {
			(dependenciesList[i].value as? Disposable)?.dispose()
		}
	}

	fun <R, T : Any> task(dKey: DKey<T>, timeout: Duration = defaultTaskTimeout, isOptional: Boolean = false, work: Work<T>) = BootTaskProperty<R, T>(this, dKey, timeout, isOptional, work)

	fun <T : Any> task(name: String, dKey: DKey<T>, timeout: Duration = defaultTaskTimeout, isOptional: Boolean = false, work: Work<T>) {
		check(!hasStarted) { "Cannot add a task after the bootstrap has been started." }
		var p: DKey<*>? = dKey
		val deferred = async(start = CoroutineStart.LAZY) {
			try {
				withTimeout(timeout.toLongMilliseconds()) {
					val dependency = work()
					addDependency(dKey, dependency)
					dependency
				}
			} catch (e: Throwable) {
				if (isOptional) {
					Log.info("Optional task failed: $name")
				} else {
					Log.error("Task failed: $name $e")
					if (e is TimeoutCancellationException) throw BootstrapTaskTimeoutException(e.message)
					else throw e
				}
			}
		}
		while (p != null) {
			_map[p] = deferred
			p = p.extends
		}
	}

	class BootTaskProperty<R, T : Any>(
			private val bootstrap: Bootstrap,
			private val dKey: DKey<T>,
			private val timeout: Duration,
			private val isOptional: Boolean,
			private val work: Work<T>
	) : ReadOnlyProperty<R, Work<T>> {

		operator fun provideDelegate(
				thisRef: R,
				prop: KProperty<*>
		): ReadOnlyProperty<R, Work<T>> {
			bootstrap.task(prop.name, dKey, timeout, isOptional, work)
			return this
		}

		override fun getValue(thisRef: R, property: KProperty<*>): Work<T> {
			return work
		}
	}
}

class BootstrapTaskTimeoutException(message: String?) : IllegalStateException(message)

inline fun CoroutineScope.bootstrap(defaultTaskTimeout: Duration = 10.seconds, init: ComponentInit<Bootstrap> = {}): Bootstrap {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return Bootstrap(coroutineContext, defaultTaskTimeout).apply(init)
}
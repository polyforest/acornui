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

import com.acornui.async.*
import com.acornui.Disposable
import com.acornui.assertionsEnabled
import com.acornui.logging.Log
import kotlinx.coroutines.*
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class Bootstrap(private val defaultTaskTimeout: Float = 10f) : Disposable {

	private val dependenciesList = ArrayList<DependencyPair<*>>()

	suspend fun dependenciesList(): List<DependencyPair<*>> {
		awaitAll()
		return dependenciesList
	}

	private val _map = HashMap<DKey<*>, Deferred<Any>>()

	suspend fun <T : Any> get(key: DKey<T>): T {
		val d = _map.getOrElse(key) {
			throw Exception("No task has been registered that provides key $key.")
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
	fun <T : Any> set(dKey: DKey<T>, value: T) {
		addDependency(dKey, value)
		var p: DKey<*>? = dKey
		while (p != null) {
			if (_map.containsKey(p))
				throw Exception("value already set for key $p")
			_map[p] = globalAsync { value }
			p = p.extends
		}
	}

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

		_map.values.awaitAll()
	}

	override fun dispose() {
		globalLaunch {
			// Waits for all of the dependencies to be calculated before attempting to dispose.
			awaitAll()
			// Dispose the dependencies in the reverse order they were added:
			for (i in dependenciesList.lastIndex downTo 0) {
				(dependenciesList[i].value as? Disposable)?.dispose()
			}
		}
	}

	fun <R, T : Any> task(dKey: DKey<T>, timeout: Float = defaultTaskTimeout, isOptional: Boolean = false, work: Work<T>) = BootTaskProperty<R, T>(this, dKey, timeout, isOptional, work)

	fun <T : Any> task(name: String, dKey: DKey<T>, timeout: Float = defaultTaskTimeout, isOptional: Boolean = false, work: Work<T>) {
		var p: DKey<*>? = dKey
		val deferred = GlobalScope.async(Dispatchers.Unconfined, CoroutineStart.LAZY) {
			try {
				withTimeout((timeout * 1000f).toLong()) {
					val dependency = work()
					addDependency(dKey, dependency)
					dependency
				}
			} catch (e: Throwable) {
				if (isOptional) {
					Log.info("Optional task failed: $name")
				} else {
					Log.error("Task failed: $name $e")
					throw e
				}
			}
		}
		while (p != null) {
			if (_map.containsKey(p))
				throw Exception("value already set for key $p")
			_map[p] = deferred
			p = p.extends
		}
	}

	class BootTaskProperty<S, T : Any>(
			private val bootstrap: Bootstrap,
			private val dKey: DKey<T>,
			private val timeout: Float,
			private val isOptional: Boolean,
			private val work: Work<T>
	) : ReadOnlyProperty<S, Work<T>> {

		operator fun provideDelegate(
				thisRef: S,
				prop: KProperty<*>
		): ReadOnlyProperty<S, Work<T>> {
			bootstrap.task(prop.name, dKey, timeout, isOptional, work)
			return this
		}

		override fun getValue(thisRef: S, property: KProperty<*>): Work<T> {
			return work
		}
	}
}

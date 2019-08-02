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
import com.acornui.logging.Log
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class Bootstrap(val defaultTaskTimeout: Float = 10f) : Disposable {

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

	/**
	 * Sets a dependency directly without creating a task.
	 */
	fun <T : Any> set(dKey: DKey<T>, value: T) {
		dependenciesList.add(dKey to value)
		var p: DKey<*>? = dKey
		while (p != null) {
			if (_map.containsKey(p))
				throw Exception("value already set for key $p")
			_map[p] = NonDeferred(value)
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

	fun <T : Any> task(dKey: DKey<T>, timeout: Float = defaultTaskTimeout, work: Work<T>) = BootTaskProperty(this, dKey, timeout, work)
	fun <T : Any> task(name: String, dKey: DKey<T>, timeout: Float = defaultTaskTimeout, work: Work<T>) {
		val t = BootTask(name, dKey, timeout, work)
		var p: DKey<*>? = dKey
		while (p != null) {
			if (_map.containsKey(p))
				throw Exception("value already set for key $p")
			_map[p] = t
			p = p.extends
		}
	}

	class BootTaskProperty<T : Any>(
			private val bootstrap: Bootstrap,
			private val dKey: DKey<T>,
			private val timeout: Float,
			private val work: Work<T>
	) : ReadOnlyProperty<Any, Work<T>> {

		operator fun provideDelegate(
				thisRef: Any,
				prop: KProperty<*>
		): ReadOnlyProperty<Any, Work<T>> {
			bootstrap.task(prop.name, dKey, timeout, work)
			return this
		}

		override fun getValue(thisRef: Any, property: KProperty<*>): Work<T> {
			return work
		}
	}

	private inner class BootTask<T : Any>(
			val name: String,
			private val dKey: DKey<T>,
			private val timeout: Float,
			private val work: Work<T>
	) : Promise<T>() {

		var isInvoked = false
			private set

		operator fun invoke() {
			if (isInvoked) return
			isInvoked = true
			launch {
				try {
					val dependency = work()
					dependenciesList.add(dKey to dependency)
					success(dependency)
				} catch (e: Throwable) {
					Log.error("Task failed: $name $e")
					fail(e)
					throw e
				}
			}
			if (timeout > 0f) {
				launch {
					delay(timeout)
					if (isPending) {
						Log.warn("Task $name is taking longer than expected.")
					}
				}
			}
		}

		override suspend fun await(): T {
			invoke()
			return super.await()
		}
	}
}

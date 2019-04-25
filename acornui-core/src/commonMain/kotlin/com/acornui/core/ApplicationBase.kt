/*
 * Copyright 2017 Nicholas Bilyk
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

package com.acornui.core

import com.acornui.async.*
import com.acornui.component.Stage
import com.acornui.core.di.Bootstrap
import com.acornui.core.di.DKey
import com.acornui.core.di.Injector
import com.acornui.core.di.InjectorImpl
import com.acornui.logging.Log
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Utilities for boot tasks in an application.
 */
abstract class ApplicationBase : Disposable {

	protected suspend fun config(): AppConfig = get(AppConfig)

	private val pendingTasks = HashMap<String, Pair<String, suspend () -> Unit>>()
	private val bootstrap = Bootstrap()

	protected fun <T : Any> set(key: DKey<T>, value: T) = bootstrap.set(key, value)

	protected suspend fun <T : Any> get(key: DKey<T>): T = bootstrap.get(key)

	protected suspend fun createInjector(): Injector = InjectorImpl(bootstrap.dependenciesList())

	protected suspend fun awaitAll() {
		val waitFor = ArrayList<Deferred<Unit>>()
		for (pendingTask in pendingTasks.values) {
			waitFor.add(async {
				try {
					//Log.debug("Task started: ${pendingTask.first}")
					pendingTask.second()
					//Log.debug("Task finished: ${pendingTask.first}")
				} catch (e: Throwable) {
					Log.error("Task failed: ${pendingTask.first} $e")
				}
			})
		}
		waitFor.awaitAll()
		pendingTasks.clear()
	}

	protected class BootTask(private val work: suspend () -> Unit) : ReadOnlyProperty<ApplicationBase, suspend () -> Unit> {

		operator fun provideDelegate(
				thisRef: ApplicationBase,
				prop: KProperty<*>
		): ReadOnlyProperty<ApplicationBase, suspend () -> Unit> {
			thisRef.pendingTasks[prop.name] = prop.name to work
			return this
		}

		override fun getValue(thisRef: ApplicationBase, property: KProperty<*>): suspend () -> Unit {
			return work
		}
	}

	override fun dispose() {
		Log.info("Application disposing")
		launch {
			awaitAll()
			PendingDisposablesRegistry.dispose()
			bootstrap.dispose()
			Log.info("Application disposed")
		}
	}
}
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

package com.acornui

import com.acornui.asset.Loaders
import com.acornui.async.PendingDisposablesRegistry
import com.acornui.async.Work
import com.acornui.async.applicationScopeKey
import com.acornui.async.mainScope
import com.acornui.component.Stage
import com.acornui.component.StageImpl
import com.acornui.di.*
import com.acornui.io.BinaryLoader
import com.acornui.io.TextLoader
import com.acornui.logging.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * The common interface to all Acorn UI applications.
 */
interface Application {

	suspend fun start(appConfig: AppConfig = AppConfig(), onReady: Stage.() -> Unit)
}

/**
 * Utilities for boot tasks in an application.
 */
abstract class ApplicationBase : Application {

	/**
	 * The number of seconds before logs are created for any remaining boot tasks.
	 */
	var timeout = 10f

	protected suspend fun config() = get(AppConfig)

	/**
	 * A supervisor is created to say that if a child job fails, the bootstrap shouldn't fail.
	 */
	private val supervisor = SupervisorJob()
	protected val applicationScope = CoroutineScope(mainScope.coroutineContext + supervisor)
	protected val bootstrap = Bootstrap(applicationScope)

	init {
		bootstrap.set(applicationScopeKey, applicationScope)
		kotlinBugFixes()
	}

	protected fun <T : Any> set(key: DKey<T>, value: T) = bootstrap.set(key, value)

	protected suspend fun <T : Any> get(key: DKey<T>): T = bootstrap.get(key)
	protected suspend fun <T : Any> getOptional(key: DKey<T>): T? = bootstrap.getOptional(key)

	protected open suspend fun createInjector(): Injector = InjectorImpl(bootstrap.dependenciesList())

	protected suspend fun awaitAll() {
		bootstrap.awaitAll()
	}

	protected open val versionTask by task(Version) {
//		// Copy the app config and set the build number.
//		val buildFile = get(Files).getFile("assets/build.txt")
//		val version = if (buildFile == null) Version(0, 0, 0) else {
//			val buildTimestamp = get(Loaders.textLoader).load(buildFile.path)
//			Version.fromStr(buildTimestamp)
//		}
//		version
		Version(0, 0, 0) // TODO
	}

	protected open val textLoader by task(Loaders.textLoader) {
		TextLoader()
	}

	protected open val binaryLoader by task(Loaders.binaryLoader) {
		BinaryLoader()
	}

	protected open fun createStage(injector: Injector): Stage = StageImpl(injector)

	fun <T : Any> task(dKey: DKey<T>, timeout: Float = 10f, isOptional: Boolean = false, work: Work<T>) = bootstrap.task<ApplicationBase, T>(dKey, timeout, isOptional, work)

	protected open fun dispose() {
		Log.debug("Application disposing")
		applicationScope.cancel(CancellationException("Application exiting"))
		PendingDisposablesRegistry.disposeAll()
		bootstrap.dispose()
	}

	companion object {
		val uncaughtExceptionHandlerKey: DKey<(error: Throwable) -> Unit> = dKey()
	}
}
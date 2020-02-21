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
import com.acornui.async.Work
import com.acornui.component.Stage
import com.acornui.component.StageImpl
import com.acornui.di.*
import com.acornui.function.as1
import com.acornui.graphic.Window
import com.acornui.graphic.updateAndRender
import com.acornui.io.BinaryLoader
import com.acornui.io.TextLoader
import com.acornui.logging.Log
import com.acornui.signal.addOnce
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlin.time.Duration
import kotlin.time.seconds

/**
 * The common interface to all Acorn UI applications.
 */
interface Application {

	/**
	 * Spins up the application with the given configuration and immediately returns.
	 * This is typically run within a [runMain] block, which is responsible for starting the global frame looper.
	 *
	 * @param appConfig Application configuration parameters.
	 * @param onReady A callback to invoke when the bootstrap has been completed and a `Stage` is ready to be
	 * used.
	 *
	 * @return Returns a [Job] which will be completed when the application's window is closed, or completed
	 * exceptionally if the bootstrap fails.
	 */
	fun startAsync(appConfig: AppConfig = AppConfig(), onReady: Stage.() -> Unit): Job
}

/**
 * Utilities for boot tasks in an application.
 */
abstract class ApplicationBase(protected val mainContext: MainContext) : Application {

	/**
	 * The number of seconds before logs are created for any remaining boot tasks.
	 */
	var timeout = 10f

	protected suspend fun config() = get(AppConfig)

	protected val applicationJob = Job(mainContext.coroutineContext[Job])
	protected val applicationScope: CoroutineScope = mainContext + applicationJob
	protected val bootstrap = Bootstrap(applicationScope.coroutineContext)

	protected fun <T : Any> set(key: Context.Key<T>, value: T) = bootstrap.set(key, value)

	protected suspend fun <T : Any> get(key: Context.Key<T>): T = bootstrap.get(key)
	protected suspend fun <T : Any> getOptional(key: Context.Key<T>): T? = bootstrap.getOptional(key)

	protected open suspend fun createContext() = ContextImpl(
			owner = mainContext,
			dependencies = bootstrap.dependencies(),
			coroutineContext = applicationScope.coroutineContext + Job(applicationJob),
			marker = ContextMarker.APPLICATION
	)

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

	protected open suspend fun createStage(context: Context): Stage = StageImpl(context)

	fun <T : Any> task(dKey: Context.Key<T>, timeout: Duration = 10.seconds, isOptional: Boolean = false, work: Work<T>) = bootstrap.task<ApplicationBase, T>(dKey, timeout, isOptional, work)

	final override fun startAsync(appConfig: AppConfig, onReady: Stage.() -> Unit): Job {
		applicationScope.launch {
			set(AppConfig, appConfig)
			onBeforeStart()
			val context = createContext()
			require(context.marker == ContextMarker.APPLICATION)
			require(context.findOwner { it.marker == ContextMarker.MAIN } != null)
			val window = context.inject(Window)
			window.makeCurrent()
			val stage = createStage(context)
			onStageCreated(stage)
			val looper = createLooper(stage)
			looper.disposed.addOnce {
				// The window has closed
				window.makeCurrent()
				context.dispose()
				dispose()
				applicationJob.complete()
			}
			stage.onReady()
		}
		return applicationJob
	}

	/**
	 * An application doesn't make its dispose public; the way to trigger the disposal of an application is to close
	 * its window: [com.acornui.graphic.Window.requestClose]
	 */
	protected open fun dispose() {
		Log.debug("Application disposing")
		bootstrap.dispose()
	}

	open suspend fun onBeforeStart() {}

	open suspend fun onStageCreated(stage: Stage) {
		stage.activate()
	}

	open suspend fun createLooper(owner: Context): ApplicationLooper = ApplicationLooperImpl(owner, mainContext.looper)

	companion object {
		val uncaughtExceptionHandlerKey: Context.Key<(error: Throwable) -> Unit> = contextKey()
	}
}


/**
 * Handles the application's update and rendering.
 */
interface ApplicationLooper : Context

open class ApplicationLooperImpl(
		owner: Context,
		private val mainLooper: Looper
) : ContextImpl(owner), ApplicationLooper {

	protected val stage = inject(Stage)
	protected val window = inject(Window)

	init {
		mainLooper.updateAndRender.add(::tick.as1)
		window.refresh.add(::tick)
	}

	protected fun tick() {
//		window.makeCurrent()
		if (window.isCloseRequested()) {
			Log.debug("Window closed")
			dispose()
		} else {
			window.updateAndRender(stage)
		}
	}

	override fun dispose() {
		window.makeCurrent()
		window.refresh.remove(::tick)
		mainLooper.updateAndRender.remove(::tick.as1)
		super.dispose()
	}
}
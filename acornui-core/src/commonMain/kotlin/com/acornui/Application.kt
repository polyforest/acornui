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
import com.acornui.async.AcornDispatcher
import com.acornui.async.MainDispatcherKey
import com.acornui.async.Work
import com.acornui.component.Stage
import com.acornui.component.StageImpl
import com.acornui.di.*
import com.acornui.graphic.Window
import com.acornui.graphic.updateAndRender
import com.acornui.io.*
import com.acornui.logging.Log
import com.acornui.signal.addOnce
import com.acornui.time.FrameDriver
import com.acornui.time.FrameDriverImpl
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
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

	private val appFrameDriver = FrameDriverImpl()
	private val appDispatcher: MainCoroutineDispatcher = AcornDispatcher(appFrameDriver)
	private val applicationJob = Job(mainContext.coroutineContext[Job])
	protected val applicationScope: CoroutineScope = mainContext + applicationJob
	protected val bootstrap = Bootstrap(applicationScope.coroutineContext)

	protected fun <T : Any> set(key: Context.Key<T>, value: T) = bootstrap.set(key, value)

	protected suspend fun <T : Any> get(key: Context.Key<T>): T = bootstrap.get(key)
	protected suspend fun <T : Any> getOptional(key: Context.Key<T>): T? = bootstrap.getOptional(key)

	/**
	 * The coroutine context to be used in the application.
	 * This
	 */
	protected fun createCoroutineContext(): CoroutineContext
		= mainContext.coroutineContext + appDispatcher + Job(applicationJob)

	protected open suspend fun createContext() = ContextImpl(
			owner = mainContext,
			dependencies = bootstrap.dependencies(),
			coroutineContext = createCoroutineContext(),
			marker = ContextMarker.APPLICATION
	)

	protected suspend fun awaitAll() {
		bootstrap.awaitAll()
	}

	private val frameDriverTask by task(FrameDriver) { appFrameDriver }
	private val appDispatcherTask by task(MainDispatcherKey) { appDispatcher }

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

	protected open val progressReporterTask by task(progressReporterKey) { ProgressReporterImpl() }

	protected open val defaultRequestSettingsTask by task(defaultRequestSettingsKey) {
		RequestSettings(config().rootPath, get(progressReporterKey))
	}

	protected open val textLoader by task(Loaders.textLoader) {
		TextLoader(get(defaultRequestSettingsKey))
	}

	protected open val binaryLoader by task(Loaders.binaryLoader) {
		BinaryLoader(get(defaultRequestSettingsKey))
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
		appFrameDriver.clear()
		bootstrap.dispose()
	}

	open suspend fun onBeforeStart() {}

	open suspend fun onStageCreated(stage: Stage) {
		stage.activate()
	}

	open suspend fun createLooper(owner: Context): ApplicationLooper = ApplicationLooperImpl(owner, mainContext.looper)

	companion object {

		val uncaughtExceptionHandlerKey: Context.Key<(error: Throwable) -> Unit> = contextKey()
		val defaultRequestSettingsKey: Context.Key<RequestSettings> = contextKey()
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
	protected val frameDriver = inject(FrameDriver)
	protected val applicationJob = coroutineContext[Job]!!

	init {
		mainLooper.frameDriver.add(::tick)
		window.refresh.add(::refreshHandler)
	}

	private fun refreshHandler() = tick(0f)

	protected fun tick(dT: Float) {
		window.makeCurrent()
		frameDriver.dispatch(dT)
		if (window.isCloseRequested() || !applicationJob.isActive) {
			Log.debug("Window closed: $window")
			dispose()
		} else {
			window.updateAndRender(stage)
		}
	}

	override fun dispose() {
		window.refresh.remove(::refreshHandler)
		mainLooper.frameDriver.remove(::tick)
		window.makeCurrent()
		super.dispose()
	}
}
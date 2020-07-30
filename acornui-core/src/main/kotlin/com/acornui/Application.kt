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

@file:Suppress("MemberVisibilityCanBePrivate", "unused", "CssUnusedSymbol")

package com.acornui

import com.acornui.async.Work
import com.acornui.collection.find
import com.acornui.component.Stage
import com.acornui.component.StageImpl
import com.acornui.di.*
import com.acornui.dom.body
import com.acornui.dom.createElement
import com.acornui.dom.head
import com.acornui.logging.Log
import kotlinx.coroutines.*
import org.w3c.dom.HTMLElement
import kotlinx.browser.document
import kotlinx.browser.window
import kotlin.coroutines.CoroutineContext
import kotlinx.dom.clear
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
open class ApplicationImpl(
	private val mainContext: MainContext,
	private val rootElement: HTMLElement
) : Application {

	constructor(mainContext: MainContext, rootId: String) : this(mainContext, document.getElementById(rootId).unsafeCast<HTMLElement?>() ?: throw Exception("The root element with id $rootId could not be found."))
	constructor(mainContext: MainContext) : this(mainContext, createElement<HTMLElement>("div") {
		style.width = "100%"
		style.height = "100%"
		body.appendChild(this)
	})

	private val applicationJob = Job(parent = mainContext.coroutineContext[Job])
	protected val applicationScope: CoroutineScope = mainContext + applicationJob
	protected val bootstrap = Bootstrap(applicationScope.coroutineContext)

	protected fun <T : Any> set(key: Context.Key<T>, value: T) = bootstrap.set(key, value)

	protected suspend fun <T : Any> get(key: Context.Key<T>): T = bootstrap.get(key)
	protected suspend fun <T : Any> getOptional(key: Context.Key<T>): T? = bootstrap.getOptional(key)

	init {
		// Uncaught exception handler
		val prevOnError = window.onerror
		window.onerror = { message, source, lineNo, colNo, error ->
			prevOnError?.invoke(message, source, lineNo, colNo, error)
			if (error is Throwable)
				uncaughtExceptionHandler(error)
			else
				uncaughtExceptionHandler(Exception("Unknown error: $message $lineNo $source $colNo $error"))
		}

		val oBU = window.onbeforeunload
		window.onbeforeunload = {
			oBU?.invoke(it)
			dispose()
			undefined // Necessary for ie11 not to alert user.
		}
	}

	/**
	 * The coroutine context to be used in the application.
	 */
	protected fun createCoroutineContext(): CoroutineContext
		= mainContext.coroutineContext + Job(applicationJob)

	protected open suspend fun createContext() = ContextImpl(
			owner = mainContext,
			dependencies = bootstrap.dependencies(),
			coroutineContext = createCoroutineContext(),
			marker = ContextMarker.APPLICATION
	)

	/**
	 * If there is a meta tag where name == "version" and content is a version string,
	 */
	protected open val versionTask by task(versionKey) {
		val versionMeta = head.getElementsByTagName("META").find {
			it?.attributes?.getNamedItem("name")?.value == "version"
		}
		val version = versionMeta?.attributes?.getNamedItem("content")?.value
		version?.toVersion() ?: KotlinVersion(0, 0)
	}

	protected open suspend fun createStage(context: Context): Stage = StageImpl(context)

	fun <T : Any> task(dKey: Context.Key<T>, timeout: Duration = 10.seconds, isOptional: Boolean = false, work: Work<T>) = bootstrap.task<ApplicationImpl, T>(dKey, timeout, isOptional, work)

	final override fun startAsync(appConfig: AppConfig, onReady: Stage.() -> Unit): Job {
		applicationJob.invokeOnCompletion {
			dispose()
		}
		applicationScope.launch {
			set(AppConfig, appConfig)
			onBeforeStart()
			val context = createContext()
			require(context.marker == ContextMarker.APPLICATION)
			require(context.findOwner { it.marker == ContextMarker.MAIN } != null)
			val stage = createStage(context)
			onStageCreated(stage)
			stage.onReady()
		}
		return applicationJob
	}

	/**
	 * An application doesn't make its dispose public; the way to trigger the disposal of an application is to cancel
	 * its job:
	 * [Context.exit]
	 */
	protected open fun dispose() {
		Log.debug("Application disposing")
		bootstrap.dispose()
	}

	open suspend fun onBeforeStart() {
	}

	open suspend fun onStageCreated(stage: Stage) {
		rootElement.clear()
		rootElement.appendChild(stage.dom)
	}
}

@Suppress("unused")
fun MainContext.application(rootId: String, appConfig: AppConfig = AppConfig(), onReady: Stage.() -> Unit): Job =
	ApplicationImpl(this, rootId).startAsync(appConfig, onReady)

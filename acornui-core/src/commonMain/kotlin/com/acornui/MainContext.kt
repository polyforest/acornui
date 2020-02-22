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

package com.acornui

import com.acornui.async.*
import com.acornui.async.AcornDispatcher
import com.acornui.di.ContextImpl
import com.acornui.di.ContextMarker
import com.acornui.di.DependencyMap
import com.acornui.di.dependencyMapOf
import com.acornui.logging.Log
import com.acornui.time.FrameDriverRo
import kotlinx.coroutines.*
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.seconds

class MainContext(
		val looper: Looper,
		dependencies: DependencyMap = DependencyMap(),
		coroutineContext: CoroutineContext
) : ContextImpl(null, dependencies, coroutineContext, ContextMarker.MAIN) {

	/**
	 * Cancels the main loop, exits all applications, and cancels all child coroutines.
	 */
	fun exitMain() = cancel()
}

/**
 * Invokes the main entry point and initializes the global frame loop.
 * On JVM backends this is a blocking function, on JS backends this method returns a `Promise` immediately.
 * This will run a [Looper] until all child coroutines started from [block] have finished.
 *
 * @param timeout If greater than zero, a [com.acornui.async.MainTimeoutException] will be thrown if the timeout has been
 * reached before all work has completed.
 * @param block This will be invoked in place with a main context receiver. Within this block applications may be
 * created.
 *
 * @return Returns `Unit` on JVM backends, or a `Promise` on JS backends. This is so that runMain can easily be used
 * in unit tests.
 */
fun runMain(timeout: Duration = Duration.ZERO, block: suspend MainContext.() -> Unit) {
	runMainJob(timeout, block).toPromiseOrBlocking()
}

internal fun runMainJob(timeout: Duration = Duration.ZERO, block: suspend MainContext.() -> Unit): Job {
	contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
	kotlinBugFixes()
	val looper = looper()
	AcornDispatcherFactory.frameDriver = looper.frameDriver // Used
	@UseExperimental(InternalCoroutinesApi::class)
	val dispatcher = AcornDispatcher(looper.frameDriver)
	val scope = GlobalScope + dispatcher + Log.uncaughtExceptionHandler
	val mainJob = scope.async {
		val mainContext = MainContext(looper, dependencyMapOf(MainDispatcherKey to dispatcher, FrameDriverRo to looper.frameDriver), coroutineContext)
		mainContext.block()
	}
	mainJob.withTimeout(timeout, scope)
	// Prefer the main loop outside of a coroutine for easier debugging.
	// Note that this will not be blocking on JS backends.
	looper.loop(mainJob)
	return mainJob
}

/**
 * [runMain] but with a more sensible timeout default for unit tests.
 */
expect fun runMainTest(timeout: Duration = 30.seconds, block: suspend MainContext.() -> Unit)
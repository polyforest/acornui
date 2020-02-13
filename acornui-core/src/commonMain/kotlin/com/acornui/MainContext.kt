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

import com.acornui.async.UI
import com.acornui.async.setUiThread
import com.acornui.async.toPromiseOrVoid
import com.acornui.async.withTimeout
import kotlinx.coroutines.*
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.minutes

class MainContext(
		val looper: Looper,
		override val coroutineContext: CoroutineContext
) : CoroutineScope {

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
 * @param timeout If greater than zero, a [com.acornui.async.TimeoutException] will be thrown if the timeout has been
 * reached before all work has completed.
 * @param block This will be invoked in place with a main context receiver. Within this block applications may be
 * created.
 *
 * @return Returns `void` on JVM backends, or a `Promise` on JS backends. This is so that runMain can easily be used
 * in unit tests.
 */
@Suppress("unused")
fun runMain(timeout: Duration = Duration.ZERO, block: suspend MainContext.() -> Unit) {
	contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
	kotlinBugFixes()
	setUiThread()
	val looper = looper()

	val scope = GlobalScope + Dispatchers.UI
	val mainJob = scope.async {
		val mainContext = MainContext(looper, coroutineContext)
		mainContext.block()
	}
	mainJob.withTimeout(timeout, scope)
	// Prefer the main loop outside of a coroutine for easier debugging.
	// Note that this will not be blocking on JS backends.
	looper.loop(mainJob)
	return mainJob.toPromiseOrVoid()
}

/**
 * [runMain] but with a more sensible timeout default for unit tests.
 */
fun runMainTest(timeout: Duration = 2.minutes, block: suspend MainContext.() -> Unit) = runMain(timeout, block)
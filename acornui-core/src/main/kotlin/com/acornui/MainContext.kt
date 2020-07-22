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

import com.acornui.async.withTimeout
import com.acornui.di.*
import com.acornui.logging.Log
import kotlinx.coroutines.*
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.CoroutineContext
import kotlin.js.Promise
import kotlin.time.Duration

class MainContext(
	dependencies: DependencyMap = DependencyMap(),
	coroutineContext: CoroutineContext
) : ContextImpl(null, dependencies, coroutineContext, ContextMarker.MAIN)

/**
 * Finds the [MainContext] on the owner ancestry.
 */
val Context.mainContext: MainContext
	get() = findOwner { it.marker == ContextMarker.MAIN }!! as MainContext

fun Context.exitMain() {
	findOwner { it.marker == ContextMarker.MAIN }!!.cancel()
}

/**
 * @param timeout If greater than zero, a [com.acornui.async.MainTimeoutException] will be thrown if the timeout has
 * been reached before all work has completed.
 * @param block This will be invoked in place with a main context receiver. Within this block applications may be
 * created.
 */
fun runMain(timeout: Duration? = null, block: suspend MainContext.() -> Unit) {
	runMainInternal(timeout, block)
}

fun runMainInternal(timeout: Duration? = null, block: suspend MainContext.() -> Unit): Promise<Unit> {
	contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
	polyfills()

	return GlobalScope.async {
		withTimeout(timeout) {
			MainContext(coroutineContext = coroutineContext + Log.uncaughtExceptionHandler + Job(parent = coroutineContext[Job])).block()
			Unit
		}
	}.asPromise()
}
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
import com.acornui.signal.addOnce
import kotlinx.coroutines.*
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class MainContext(
		val looper: Looper,
		val job: Job,
		val scope: CoroutineScope
)

/**
 * Invokes the main entry point and initializes the global frame loop.
 * This will block until every application launched within the callback is closed.
 *
 * @param block This will be invoked in place with a main context receiver. Within this block applications may be
 * created.
 */
@UseExperimental(InternalCoroutinesApi::class)
@Suppress("unused")
fun runMain(block: MainContext.() -> Unit) {
	contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
	kotlinBugFixes()
	// This check is needed
	setUiThread()
	val looper = looper()
	val job = looper.job
	looper.completed.addOnce {
		job.getCancellationException().cause?.let { throw it }
	}
	val mainContext = MainContext(looper, job, GlobalScope + job + Dispatchers.UI)
	mainContext.block()
	// Prefer the main loop outside of a coroutine for easier debugging.
	// Note that this will not be blocking on JS backends.
	looper.loop()
}

// TODO: Add runMain with timeout
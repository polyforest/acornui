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

package com.acornui.async


import com.acornui.async.Acorn.delay
import com.acornui.time.callLater
import com.acornui.time.timer
import kotlinx.coroutines.*
import kotlinx.coroutines.internal.MainDispatcherFactory
import kotlin.coroutines.CoroutineContext
import kotlin.jvm.JvmStatic

/**
 * Dispatches execution onto Acorn UI Thread.
 */
@UseExperimental(InternalCoroutinesApi::class)
@Suppress("unused")
@Deprecated("Use Dispatchers.Main", ReplaceWith("Dispatchers.Main"))
val Dispatchers.UI : AcornDispatcher
	get() = Acorn

/**
 * Dispatcher for Acorn event dispatching thread.
 *
 * This class provides type-safety and a point for future extensions.
 */
@InternalCoroutinesApi
sealed class AcornDispatcher : MainCoroutineDispatcher(), Delay {

	/**
	 * @suppress
	 */
	override fun dispatch(context: CoroutineContext, block: Runnable) {
		callLater {
			block.run()
		}
	}

	/**
	 * @suppress
	 */
	@UseExperimental(ExperimentalCoroutinesApi::class)
	override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>) {
		val timer = timer(timeMillis / 1000f) {
			with(continuation) { resumeUndispatched(Unit) }
		}
		continuation.invokeOnCancellation { timer.dispose() }
	}

	/**
	 * @suppress
	 */
	override fun invokeOnTimeout(timeMillis: Long, block: Runnable): DisposableHandle {
		val timer = timer(timeMillis / 1000f) {
			block.run()
		}
		return object : DisposableHandle {
			override fun dispose() {
				timer.dispose()
			}
		}
	}
}

@InternalCoroutinesApi
private object ImmediateAcornDispatcher : AcornDispatcher() {
	override val immediate: MainCoroutineDispatcher
		get() = this

	override fun isDispatchNeeded(context: CoroutineContext): Boolean {
		return !isUiThread()
	}

	override fun toString() = "Acorn UI [immediate]"
}

/**
 * Dispatches execution onto Acorn event dispatching thread and provides native [delay] support.
 */
@InternalCoroutinesApi
internal object Acorn : AcornDispatcher() {
	override val immediate: MainCoroutineDispatcher
		get() = ImmediateAcornDispatcher

	override fun toString() = "Acorn UI"
}

@Suppress("unused")
@UseExperimental(InternalCoroutinesApi::class)
class AcornDispatcherFactory : MainDispatcherFactory {
	companion object {
		@JvmStatic // accessed reflectively from core
		fun getDispatcher(): MainCoroutineDispatcher = Acorn
	}

	override fun createDispatcher(allFactories: List<MainDispatcherFactory>): MainCoroutineDispatcher = Acorn

	override val loadPriority: Int get() = Int.MAX_VALUE
}
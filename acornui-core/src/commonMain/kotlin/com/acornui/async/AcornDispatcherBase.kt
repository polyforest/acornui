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

import com.acornui.di.Context
import com.acornui.signal.addOnce
import com.acornui.start
import com.acornui.time.FrameDriverRo
import com.acornui.time.Timer
import kotlinx.coroutines.*
import kotlinx.coroutines.internal.MainDispatcherFactory
import kotlin.coroutines.CoroutineContext
import kotlin.jvm.JvmStatic

/**
 * Dispatcher for Acorn event dispatching thread.
 *
 * This class provides type-safety and a point for future extensions.
 */
@InternalCoroutinesApi
sealed class AcornDispatcherBase(
		private val frameDriver: FrameDriverRo
) : MainCoroutineDispatcher(), Delay {

	/**
	 * @suppress
	 */
	override fun dispatch(context: CoroutineContext, block: Runnable) {
		frameDriver.addOnce {
			block.run()
		}
	}

	/**
	 * @suppress
	 */
	@UseExperimental(ExperimentalCoroutinesApi::class)
	override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>) {
		val timer = Timer(frameDriver,timeMillis / 1000f) {
			with(continuation) {
				if (isActive)
					resumeUndispatched(Unit)
			}
		}.start()
		continuation.invokeOnCancellation { timer.dispose() }
	}

	/**
	 * @suppress
	 */
	override fun invokeOnTimeout(timeMillis: Long, block: Runnable): DisposableHandle {
		return Timer(frameDriver,timeMillis / 1000f) {
			block.run()
		}.start()
	}
}

@InternalCoroutinesApi
private class ImmediateAcornDispatcher(frameDriver: FrameDriverRo) : AcornDispatcherBase(frameDriver) {

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
internal class AcornDispatcher(frameDriver: FrameDriverRo) : AcornDispatcherBase(frameDriver) {
	override val immediate: MainCoroutineDispatcher = ImmediateAcornDispatcher(frameDriver)

	override fun toString() = "Acorn UI"
}

/**
 * Acorn does provide a service for supplying [Dispatchers.Main], however it's recommended to use the
 * [MainDispatcher] dependency. [Dispatchers.Main] will guarantee invocation on the main thread, but
 */
@Suppress("unused")
@UseExperimental(InternalCoroutinesApi::class)
class AcornDispatcherFactory : MainDispatcherFactory {

	companion object {

		lateinit var frameDriver: FrameDriverRo

		@JvmStatic // accessed reflectively from core
		fun getDispatcher(): MainCoroutineDispatcher = AcornDispatcher(frameDriver)
	}

	override fun createDispatcher(allFactories: List<MainDispatcherFactory>): MainCoroutineDispatcher = AcornDispatcher(frameDriver)

	override val loadPriority: Int get() = Int.MAX_VALUE
}

/**
 * The dependency key for the Application coroutine dispatcher that:
 * 1) Dispatches in the Main thread.
 * 2) Dispatches with the application's window set to current
 * 3) Cancels scheduling on application close.
 */
object MainDispatcherKey : Context.Key<MainCoroutineDispatcher>

/**
 * The [MainDispatcherKey] instance.
 */
val Context.MainDispatcher: MainCoroutineDispatcher
	get() = inject(MainDispatcherKey)
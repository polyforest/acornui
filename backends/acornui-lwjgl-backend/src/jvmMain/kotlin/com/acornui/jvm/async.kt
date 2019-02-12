/*
 * Copyright 2015 Nicholas Bilyk
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

package com.acornui.jvm

import com.acornui.async.Deferred
import com.acornui.async.PendingDisposablesRegistry
import com.acornui.async.Promise
import com.acornui.async.TimeoutException
import com.acornui.core.Disposable
import com.acornui.core.UpdatableChildBase
import com.acornui.core.time.TimeDriver
import com.acornui.uncaughtExceptionHandler
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future

private val executor by lazy {
	val e = Executors.newSingleThreadExecutor {
		r ->
		val thread = Thread(r, "AcornUI_IO")
		thread.setUncaughtExceptionHandler {
			_, exception ->
			uncaughtExceptionHandler(exception)
		}
		thread
	}
	val d = object : Disposable {
		override fun dispose() {
			e.shutdownNow()
			PendingDisposablesRegistry.unregister(this)
		}
	}
	PendingDisposablesRegistry.register(d)
	e
}

fun <T> asyncThread(timeDriver: TimeDriver, timeout: Float = 60f, work: () -> T): Deferred<T> {

//	return async(work)
	return object : Promise<T>(), Deferred<T>, Disposable {

		private var future: Future<T>

		init {
			val r = this
			PendingDisposablesRegistry.register(this)
			future = executor.submit(Callable<T> { work() })

			val watcher = object : UpdatableChildBase() {
				private var timeRemaining = timeout

				override fun update(tickTime: Float) {
					if (future.isDone) {
						remove()
						val result = try {
							future.get()
						} catch (e: Throwable) {
							fail(e)
							null
						}
						@Suppress("UNCHECKED_CAST")
						if (status == Deferred.Status.PENDING)
							success(result as T)
						PendingDisposablesRegistry.unregister(r)
					} else {
						timeRemaining -= tickTime
						if (timeRemaining < 0f) {
							remove()
							fail(TimeoutException(timeout))
							PendingDisposablesRegistry.unregister(r)
						}
					}
				}
			}

			timeDriver.addChild(watcher)
		}

		override fun dispose() {
			future.cancel(true)
			PendingDisposablesRegistry.unregister(this)
		}
	}
}
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

@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.acornui.async

import com.acornui.time.schedule
import com.acornui.time.toDelayMillis
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.js.Promise
import kotlin.properties.ObservableProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.time.Duration

typealias Work<R> = suspend () -> R

/**
 * Suspends the coroutine until this job is complete, then returns the result if the job has completed, or null if
 * the job was cancelled.
 */
suspend fun <T> Deferred<T>.awaitOrNull(): T? {
	join()
	return getCompletedOrNull()
}

/**
 * If this deferred object [Deferred.isCompleted] and has not completed exceptionally, the [Deferred.getCompleted] value
 * will be returned. Otherwise, null.
 */
fun <T> Deferred<T>.getCompletedOrNull(): T? = if (isCompleted && getCompletionExceptionOrNull() == null) getCompleted() else null

suspend fun <K, V> Map<K, Deferred<V>>.awaitAll(): Map<K, V> {
	values.awaitAll()
	return mapValues { it.value.await() }
}

/**
 * @see kotlinx.coroutines.delay
 */
suspend fun delay(time: Duration) {
	delay(time.toLongMilliseconds())
}

/**
 * If the given [timeout] is null, runs the block without a timeout, otherwise uses [kotlinx.coroutines.withTimeout].
 */
suspend fun <T> withTimeout(timeout: Duration?, block: suspend CoroutineScope.() -> T): T {
	return if (timeout == null)
		coroutineScope(block)
	else
		withTimeout(timeout.toDelayMillis(), block)
}

class MainTimeoutException(timeout: Duration) : CancellationException("Job timed out after ${timeout.inSeconds} seconds.")

/**
 * Launches a coroutine inside a supervisor scope.
 *
 * - Exceptions thrown in this supervised job will not cancel the parent job.
 * - Cancellations in the parent job will cancel this supervised job.
 */
fun CoroutineScope.launchSupervised(
	context: CoroutineContext = EmptyCoroutineContext,
	start: CoroutineStart = CoroutineStart.DEFAULT,
	block: suspend CoroutineScope.() -> Unit
): Job {
	return launch(context, start) {
		try {
			supervisorScope {
				block()
			}
		} catch (ignore: Throwable) {}
	}
}

/**
 * Returns a property for type Job, where upon setting will cancel the previously set job.
 */
fun <T : Job> cancellingJobProp(): ReadWriteProperty<Any?, T?> {
	return object : ObservableProperty<T?>(null) {
		override fun afterChange(property: KProperty<*>, oldValue: T?, newValue: T?) {
			oldValue?.cancel()
		}
	}
}

/**
 * Creates a [Promise.race] with `this` promise and a window timeout.
 */
fun <T> Promise<T>.withTimeout(timeout: Duration): Promise<T> {
	val timeoutPromise = Promise<T> {
		_, reject ->
		val timeoutHandle = schedule(timeout) {
			reject(TimeoutException(timeout))
		}
		this@withTimeout.then {
			timeoutHandle.dispose()
		}
	}
	return Promise.race(arrayOf(this, timeoutPromise))
}

class TimeoutException(val timeout: Duration) : Exception("Promise timed out after $timeout")
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
@file:JvmName("AsyncUtils")
@file:JvmMultifileClass

package com.acornui.async

import com.acornui.Disposable
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName
import kotlin.properties.ObservableProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.time.Duration

typealias Work<R> = suspend () -> R

@Deprecated("No longer used.", level = DeprecationLevel.ERROR)
fun <T : Disposable> disposeOnShutdown(disposable: T): T = error("Unused")

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
 * @see kotlinx.coroutines.withTimeout
 */
suspend fun <T> withTimeout(time: Duration, block: suspend CoroutineScope.() -> T): T = withTimeout(time.inMilliseconds.toLong(), block)

/**
 * @see kotlinx.coroutines.withTimeoutOrNull
 */
suspend fun <T> withTimeoutOrNull(time: Duration, block: suspend CoroutineScope.() -> T): T? = withTimeoutOrNull(time.inMilliseconds.toLong(), block)

/**
 * To be used with Unit testing or main functions, this returns a Promise on JS backends, or blocks on JVM backends.
 * Exceptions on this job will be propagated to the blocking function or promise.
 */
expect fun Job.toPromiseOrBlocking()

/**
 * Adds a timeout to an already created job.
 */
fun Job.withTimeout(timeout: Duration, scope: CoroutineScope = GlobalScope) {
	if (timeout > Duration.ZERO && !isCompleted) {
		val timeoutJob = scope.launch {
			delay(timeout.toLongMilliseconds())
			this@withTimeout.cancel(MainTimeoutException(timeout))
		}
		this.invokeOnCompletion {
			timeoutJob.cancel()
		}
	}
}

class MainTimeoutException(timeout: Duration) : CancellationException("Job timed out after ${timeout.inSeconds} seconds.")

/**
 * Launches a coroutine with a [SupervisorJob] whose parent is set to the current scope's job.
 *
 * - Exceptions thrown in this supervised job will not cancel the parent job.
 * - Cancellations in the parent job will cancel this supervised job.
 */
fun CoroutineScope.launchSupervised(
		context: CoroutineContext = EmptyCoroutineContext,
		start: CoroutineStart = CoroutineStart.DEFAULT,
		block: suspend CoroutineScope.() -> Unit
): Job = launch(context + SupervisorJob(context[Job]), start) {
	block()
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
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

import com.acornui.Disposable
import kotlinx.coroutines.*
import kotlin.collections.set
import kotlin.time.Duration

typealias Work<R> = suspend () -> R

object PendingDisposablesRegistry {

	private val allDisposables = HashMap<Disposable, Unit>()
	private var isDisposing = false

	fun <T : Disposable> register(disposable: T): T {
		if (isDisposing) throw IllegalStateException("Cannot register a disposable instance with PendingDisposablesRegistry on dispose.")
		allDisposables[disposable] = Unit
		return disposable
	}

	fun unregister(disposable: Disposable) {
		if (isDisposing) return
		allDisposables.remove(disposable)
	}

	/**
	 * Disposes all pending disposables.
	 */
	fun disposeAll() {
		if (isDisposing) return
		isDisposing = true
		for (disposable in allDisposables.keys) {
			disposable.dispose()
		}
		allDisposables.clear()
		isDisposing = false
	}
}

fun <T : Disposable> disposeOnShutdown(disposable: T): T = PendingDisposablesRegistry.register(disposable)

/**
 * Wraps await in a try/catch block, returning null if there was an exception.
 */
suspend fun <T> Deferred<T>.awaitOrNull(): T? {
	return try {
		await()
	} catch (e: Throwable) {
		null
	}
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
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

/**
 * Launches a new coroutine in the Global Scope on the main thread.
 */
fun globalLaunch(block: suspend CoroutineScope.() -> Unit) {
	GlobalScope.launch(Dispatchers.Unconfined, block = block)
}

/**
 * Creates a coroutine and returns its future result as an implementation of [Deferred] in the global scope on the
 * main thread.
 */
fun <R> globalAsync(block: suspend CoroutineScope.() -> R): Deferred<R> {
	return GlobalScope.async(Dispatchers.Unconfined, block = block)
}

typealias Work<R> = suspend () -> R

object PendingDisposablesRegistry {

	private val allPending = HashMap<Disposable, Unit>()
	private var isDisposing = false

	fun <T : Disposable> register(disposable: T): T {
		if (isDisposing) throw IllegalStateException("Cannot addBinding a disposable to the registry on dispose.")
		allPending[disposable] = Unit
		return disposable
	}

	fun unregister(disposable: Disposable) {
		if (isDisposing) return
		allPending.remove(disposable)
	}

	/**
	 * Cancels all currently active continuations.
	 */
	fun dispose() {
		if (isDisposing) return
		isDisposing = true
		for (disposable in allPending.keys) {
			disposable.dispose()
		}
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
 * If this deferred object [Deferred.isCompleted] the [Deferred.getCompleted] value will be returned. Otherwise, null.
 */
fun <T> Deferred<T>.getCompletedOrNull(): T? = if (isCompleted) getCompleted() else null

suspend fun <K, V> Map<K, Deferred<V>>.awaitAll(): Map<K, V> {
	values.awaitAll()
	return mapValues { it.value.await() }
}

/**
 * Acorn conventions use seconds, not milliseconds.
 * @see kotlinx.coroutines.delay
 */
suspend fun delay(timeSeconds: Float) {
	delay((timeSeconds * 1000f).toLong())
}

/**
 * Acorn conventions use seconds, not milliseconds.
 * @see kotlinx.coroutines.withTimeout
 */
suspend fun <T> withTimeout(timeSeconds: Float, block: suspend CoroutineScope.() -> T): T = withTimeout((timeSeconds * 1000f).toLong(), block)

/**
 * Acorn conventions use seconds, not milliseconds.
 * @see kotlinx.coroutines.withTimeoutOrNull
 */
suspend fun <T> withTimeoutOrNull(timeSeconds: Float, block: suspend CoroutineScope.() -> T): T? = withTimeoutOrNull((timeSeconds * 1000f).toLong(), block)
/*
 * Copyright 2017 Nicholas Bilyk
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

import com.acornui.collection.ArrayList
import com.acornui.collection.copy
import com.acornui.collection.mapTo
import com.acornui.collection.poll
import kotlin.coroutines.experimental.*

/**
 * Launches a new coroutine on this same thread.
 */
fun launch(block: suspend () -> Unit) {
	block.startCoroutine(BasicContinuationImpl())
}

typealias Work<R> = suspend () -> R

/**
 * Launches a new coroutine on this same thread, exposing the ability to suspend execution, awaiting a result.
 */
fun <T> async(work: Work<T>): Deferred<T> = AsyncWorker(work)

/**
 * A suspension point with no result.
 */
class BasicContinuationImpl(
		override val context: CoroutineContext = EmptyCoroutineContext
) : Continuation<Unit> {

	override fun resume(value: Unit) {
	}

	override fun resumeWithException(exception: Throwable) {
		//println("Coroutine failed: $exception")
		throw exception
	}
}

interface Deferred<out T> {

	/**
	 * Suspends the co-routine until the result is calculated.
	 */
	suspend fun await(): T
}

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

interface CancelableDeferred<out T> : Deferred<T> {

	/**
	 * Cancels the operation if it is currently in progress.
	 * This may throw a [CancellationException] on [await].
	 */
	fun cancel()

}

open class CancellationException(message: String = "Aborted") : Throwable(message)

/**
 * Invokes a callback when the deferred value has been computed successfully. This callback will not be invoked on
 * failure.
 */
fun <T> Deferred<T>.then(callback: (result: T) -> Unit) {
	launch {
		var successful = false
		var result: T? = null
		try {
			result = await()
			successful = true
		} catch (t: Throwable) {}
		@Suppress("UNCHECKED_CAST")
		if (successful)
			callback(result as T)
	}
}

/**
 * Invokes a callback when this deferred object has failed to produce a result.
 */
fun <T> Deferred<T>.catch(callback: (Throwable) -> Unit) {
	launch {
		try {
			await()
		} catch (t: Throwable) {
			callback(t)
		}
	}
}

@Suppress("AddVarianceModifier")
private class AsyncWorker<T>(work: Work<T>) : Deferred<T> {

	private var status = DeferredStatus.PENDING
	private var result: T? = null
	private var error: Throwable? = null
	private val children = ArrayList<Continuation<T>>()

	init {
		launch {
			try {
				result = work()
				status = DeferredStatus.SUCCESSFUL
			} catch (e: Throwable) {
				error = e
				status = DeferredStatus.FAILED
			}
			@Suppress("UNCHECKED_CAST")
			when (status) {
				DeferredStatus.SUCCESSFUL -> {
					val r = result as T
					while (children.isNotEmpty()) {
						children.poll().resume(r)
					}
				}
				DeferredStatus.FAILED -> {
					val e = error as Throwable
					while (children.isNotEmpty()) {
						children.poll().resumeWithException(e)
					}
				}
				else -> throw Exception("Status should not be pending.")
			}
		}
	}

	override suspend fun await(): T {
		@Suppress("UNCHECKED_CAST")
		return when (status) {
			DeferredStatus.PENDING -> suspendCoroutine {
				cont: Continuation<T> ->
				children.add(cont)
			}
			DeferredStatus.FAILED -> throw error!!
			else -> result as T
		}
	}

	private enum class DeferredStatus {
		PENDING,
		SUCCESSFUL,
		FAILED
	}
}

/**
 * A promise with exposed [setValue] and [setError] to defer to the [success] and [fail] methods.
 */
class LateValue<T> : Promise<T>() {

	val isPending: Boolean
		get() = status == Status.PENDING

	fun setValue(value: T) = success(value)

	fun setError(value: Throwable) = fail(value)
}

open class Promise<T> : Deferred<T> {

	private var _status = Status.PENDING
	protected val status: Status
		get() = _status

	private var _result: T? = null
	protected val result: T?
		get() = _result

	private var _error: Throwable? = null
	private val error: Throwable?
		get() = _error

	private val continuations = ArrayList<Continuation<T>>()

	protected fun success(result: T) {
		if (_status != Status.PENDING)
			throw IllegalStateException("Promise is not in pending state.")
		this._result = result
		_status = Status.SUCCESSFUL
		while (continuations.isNotEmpty()) {
			continuations.poll().resume(result)
		}
	}

	protected fun fail(error: Throwable) {
		if (_status != Status.PENDING)
			throw IllegalStateException("Promise is not in pending state.")
		this._error = error
		_status = Status.FAILED
		while (continuations.isNotEmpty()) {
			continuations.poll().resumeWithException(error)
		}
	}

	override suspend fun await(): T = suspendCoroutine {
		cont: Continuation<T> ->
		@Suppress("UNCHECKED_CAST")
		when (_status) {
			Status.PENDING -> continuations.add(cont)
			Status.SUCCESSFUL -> cont.resume(_result as T)
			Status.FAILED -> cont.resumeWithException(_error as Throwable)
		}
	}

	enum class Status {
		PENDING,
		SUCCESSFUL,
		FAILED
	}
}

/**
 * A deferred implementation that doesn't have any asynchronous work to do.
 */
class NonDeferred<out T>(val value: T) : Deferred<T> {
	override suspend fun await(): T {
		return value
	}
}

suspend fun <T> List<Deferred<T>>.awaitAll(): List<T> {
	val copy = copy() // Copy the list so that it can't mutate in-between awaits.
	return ArrayList(copy.size, { copy[it].await() })
}

suspend fun <K, V> Map<K, Deferred<V>>.awaitAll(): Map<K, V> {
	// Copy the map so that it can't mutate in-between awaits.
	return copy().mapTo { key, value -> key to value.await() }
}
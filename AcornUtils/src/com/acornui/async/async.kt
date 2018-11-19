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

import com.acornui.async.Deferred.Status
import com.acornui.collection.*
import com.acornui.core.Disposable
import kotlin.coroutines.*

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

	override fun resumeWith(result: Result<Unit>) {
		result.onFailure { throw it }
	}
}

object PendingDisposablesRegistry {

	private val allPending = HashMap<Disposable, Unit>()
	private var isDisposing = false

	fun register(continuation: Disposable) {
		if (isDisposing) throw IllegalStateException("Cannot addBinding a disposable to the registry on dispose.")
		allPending[continuation] = Unit
	}

	fun unregister(continuation: Disposable) {
		if (isDisposing) return
		allPending.remove(continuation)
	}

	/**
	 * Cancels all currently active continuations.
	 */
	fun dispose() {
		if (isDisposing) return
		isDisposing = true
		for (continuation in allPending.keys) {
			continuation.dispose()
		}
	}
}

interface Deferred<out T> {

	/**
	 * Suspends the co-routine until the result is calculated.
	 */
	suspend fun await(): T

	val status: Status

	/**
	 * Returns the result if and only if [status] is [Status.SUCCESSFUL]. If status is pending or failed, this will
	 * throw an exception.
	 * @see await
	 */
	val result: T

	/**
	 * Returns the result if and only if [status] is [Status.FAILED]. If status is pending or successful, this will
	 * throw an exception.
	 * @see await
	 */
	val error: Throwable

	enum class Status {
		PENDING,
		SUCCESSFUL,
		FAILED
	}
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

/**
 * If [Deferred.status] is [Status.SUCCESSFUL] this will return the result. Otherwise, null.
 */
fun <T> Deferred<T>.resultOrNull(): T? = if (status == Status.SUCCESSFUL) result else null

interface CancelableDeferred<out T> : Deferred<T> {

	/**
	 * Cancels the operation if it is currently in progress.
	 * This may throw a [CancellationException] on [await].
	 */
	fun cancel()

}

open class CancellationException(message: String = "Aborted") : Throwable(message)
open class TimeoutException(timeout: Float, message: String = "Timed out after $timeout seconds") : Throwable(message)

/**
 * Invokes a callback when the deferred value has been computed successfully. This callback will not be invoked on
 * failure.
 */
infix fun <T> Deferred<T>.then(callback: (result: T) -> Unit): Deferred<T> {
	launch {
		var successful = false
		var result: T? = null
		try {
			result = await()
			successful = true
		} catch (t: Throwable) {
		}
		@Suppress("UNCHECKED_CAST")
		if (successful)
			callback(result as T)
	}
	return this
}

infix fun <A, B> Deferred<Pair<A, B>>.then(callback: (result: A, B) -> Unit): Deferred<Pair<A, B>> {
	return then<Pair<A, B>> { callback(it.first, it.second) }
}

infix fun <A, B, C> Deferred<Triple<A, B, C>>.then(callback: (result: A, B, C) -> Unit): Deferred<Triple<A, B, C>> {
	return then<Triple<A, B, C>> { callback(it.first, it.second, it.third) }
}

infix fun <A, B, C, D> Deferred<Tuple4<A, B, C, D>>.then(callback: (result: A, B, C, D) -> Unit): Deferred<Tuple4<A, B, C, D>> {
	return then<Tuple4<A, B, C, D>> { callback(it.first, it.second, it.third, it.fourth) }
}

/**
 * Invokes a callback when this deferred object has failed to produce a result.
 */
infix fun <T> Deferred<T>.catch(callback: (Throwable) -> Unit): Deferred<T> {
	launch {
		try {
			await()
		} catch (t: Throwable) {
			callback(t.cause ?: t)
		}
	}
	return this
}

/**
 * Invokes a callback when the deferred value has been either been computed successfully or failed.
 */
infix fun <T> Deferred<T>.finally(callback: (result: T?) -> Unit): Deferred<T> {
	launch {
		var result: T? = null
		try {
			result = await()
		} catch (t: Throwable) {}
		callback(result)
	}
	return this
}

@Suppress("AddVarianceModifier")
private class AsyncWorker<T>(work: Work<T>) : Deferred<T> {

	private var _status = Status.PENDING
	override val status: Status
		get() = _status

	private var _result: T? = null
	override val result: T
		get() {
			if (_status != Status.SUCCESSFUL) throw Exception("status is not SUCCESSFUL")
			@Suppress("UNCHECKED_CAST")
			return _result as T
		}

	private var _error: Throwable? = null
	override val error: Throwable
		get() {
			if (_status != Status.FAILED) throw Exception("status is not FAILED")
			return _error as Throwable
		}

	private val children = ArrayList<Continuation<T>>()

	init {
		launch {
			try {
				_result = work()
				_status = Status.SUCCESSFUL
			} catch (e: Throwable) {
				_error = e
				_status = Status.FAILED
			}
			@Suppress("UNCHECKED_CAST")
			when (_status) {
				Status.SUCCESSFUL -> {
					val r = _result as T
					while (children.isNotEmpty()) {
						children.poll().resume(r)
					}
				}
				Status.FAILED -> {
					val e = _error as Throwable
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
		return when (_status) {
			Status.PENDING -> suspendCoroutine { cont: Continuation<T> ->
				children.add(cont)
			}
			Status.FAILED -> throw _error!!
			else -> _result as T
		}
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
	override val status: Status
		get() = _status

	private var _result: T? = null
	@Suppress("UNCHECKED_CAST")
	override val result: T
		get() {
			if (_status != Status.SUCCESSFUL) throw Exception("status is not SUCCESSFUL.")
			return _result as T
		}

	private var _error: Throwable? = null
	override val error: Throwable
		get() {
			if (_status != Status.FAILED) throw Exception("status is not FAILED.")
			return _error as Throwable
		}

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

	override suspend fun await(): T = suspendCoroutine { cont: Continuation<T> ->
		@Suppress("UNCHECKED_CAST")
		when (_status) {
			Status.PENDING -> continuations.add(cont)
			Status.SUCCESSFUL -> cont.resume(_result as T)
			Status.FAILED -> cont.resumeWithException(_error as Throwable)
		}
	}
}

/**
 * A deferred implementation that doesn't have any asynchronous work to do.
 */
class NonDeferred<out T>(val value: T) : Deferred<T> {

	override suspend fun await(): T {
		return value
	}

	override val status: Status
		get() = Status.SUCCESSFUL

	override val result: T
		get() = value

	override val error: Throwable
		get() {
			throw Exception("status is not FAILED")
		}
}

/**
 * Awaits all deferred values, populating a list with the results.
 */
suspend fun <T> List<Deferred<T>>.awaitAll(): List<T> {
	val copy = copy() // Copy the list so that it can't mutate in-between awaits.
	return ArrayList(copy.size, { copy[it].await() })
}

/**
 * Like awaitAll but the values will be null if await fails.
 */
suspend fun <T> List<Deferred<T>>.awaitAllChecked(): List<T?> {
	val copy = copy() // Copy the list so that it can't mutate in-between awaits.
	return ArrayList(copy.size, { copy[it].awaitOrNull() })
}

/**
 * Like awaitAll but the values will be null if await fails.
 */
suspend fun <K, V> Map<K, Deferred<V>>.awaitAll(): Map<K, V> {
	// Copy the map so that it can't mutate in-between awaits.
	return copy().mapTo { key, value -> key to value.await() }
}

/**
 * Awaits all deferred values, populating a map with the results.
 */
suspend fun <K, V> Map<K, Deferred<V>>.awaitAllChecked(): Map<K, V?> {
	// Copy the map so that it can't mutate in-between awaits.
	return copy().mapTo { key, value -> key to value.awaitOrNull() }
}
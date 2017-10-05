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
fun <T> async(work: Work<T>): Deferred<T> = DeferredImpl(work)

/**
 * A suspension point with no result.
 */
class BasicContinuationImpl(
		override val context: CoroutineContext = EmptyCoroutineContext
) : Continuation<Unit> {

	override fun resume(value: Unit) {
	}

	override fun resumeWithException(exception: Throwable) {
		println("Coroutine failed: $exception")
	}
}

interface Deferred<out T> {

	/**
	 * Suspends the co-routine until the result is calculated.
	 */
	suspend fun await(): T
}

private class DeferredImpl<out T>(private val work: Work<T>) : Deferred<T> {

	private var status = DeferredStatus.PENDING
	private var result: T? = null
	private var error: Throwable? = null
	private val continuations = ArrayList<Continuation<T>>()

	init {
		launch {
			if (status != DeferredStatus.PENDING)
				throw IllegalStateException("Deferred job is not currently pending.")
			try {
				result = work()
				status = DeferredStatus.SUCCESSFUL
			} catch(e: Throwable) {
				error = e
				status = DeferredStatus.FAILED
			} finally {
				when (status) {
					DeferredStatus.SUCCESSFUL -> {
						for (i in 0..continuations.lastIndex) {
							continuations[i].resume(result as T)
						}
					}
					DeferredStatus.FAILED -> {
						for (i in 0..continuations.lastIndex) {
							continuations[i].resumeWithException(error as Throwable)
						}
					}
					else -> throw IllegalStateException("Status is not successful or failed.")
				}
				continuations.clear()
			}
		}
	}

	override suspend fun await(): T = suspendCoroutine {
		cont: Continuation<T> ->
		when (status) {
			DeferredStatus.PENDING -> continuations.add(cont)
			DeferredStatus.SUCCESSFUL -> cont.resume(result as T)
			DeferredStatus.FAILED -> cont.resumeWithException(error as Throwable)
		}
	}

	private enum class DeferredStatus {
		PENDING,
		SUCCESSFUL,
		FAILED
	}
}
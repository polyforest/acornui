/*
 * Copyright 2020 Poly Forest, LLC
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

import kotlinx.coroutines.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext

/**
 * A composition of a [MutableProgress] and a [Continuation].
 * This ensures that a coroutine suspension that reports its progress marks the progress as complete when the
 * continuation resumes.
 */
open class ContinuationWithProgress<T>(
	protected val progress: MutableProgress,
	private val continuation: Continuation<T>
) : Continuation<T>, MutableProgress by progress {

	override val context: CoroutineContext
		get() = continuation.context

	override fun resumeWith(result: Result<T>) {
		continuation.resumeWith(result)
		progress.complete()
	}

	/**
	 * Use the [kotlin.coroutines.resume] methods to mark this progress as complete.
	 */
	override fun complete(): Nothing {
		error("Can only complete through resume methods.")
	}
}

/**
 * Wraps a continuation with progress tracking.
 * When this continuation is resumed, the progress tracker will be marked completed.
 */
fun <T> Continuation<T>.withProgress(progress: MutableProgress) =
	ContinuationWithProgress(progress, this)

/**
 * A composition of a [MutableProgress] and a [CancellableContinuation].
 * This ensures that a coroutine suspension that reports its progress marks the progress as complete when the
 * continuation resumes.
 */
open class CancellableContinuationWithProgress<T>(
	progress: MutableProgress,
	private val cancellableContinuation: CancellableContinuation<T>
) : CancellableContinuation<T>, ContinuationWithProgress<T>(progress, cancellableContinuation) {

	override val isActive: Boolean
		get() = cancellableContinuation.isActive

	override val isCancelled: Boolean
		get() = cancellableContinuation.isCancelled

	override val isCompleted: Boolean
		get() = cancellableContinuation.isCompleted

	override fun cancel(cause: Throwable?): Boolean {
		return cancellableContinuation.cancel(cause).also {
			progress.complete()
		}
	}

	@InternalCoroutinesApi
	override fun completeResume(token: Any) {
		cancellableContinuation.completeResume(token)
		progress.complete()
	}

	@InternalCoroutinesApi
	override fun initCancellability() = cancellableContinuation.initCancellability()

	override fun invokeOnCancellation(handler: CompletionHandler) = cancellableContinuation.invokeOnCancellation(handler)

	@ExperimentalCoroutinesApi
	override fun resume(value: T, onCancellation: ((cause: Throwable) -> Unit)?) {
		cancellableContinuation.resume(value, onCancellation)
		progress.complete()
	}

	@InternalCoroutinesApi
	override fun tryResume(value: T, idempotent: Any?): Any? {
		return cancellableContinuation.tryResume(value, idempotent).also {
			progress.complete()
		}
	}

	@InternalCoroutinesApi
	override fun tryResume(value: T, idempotent: Any?, onCancellation: ((cause: Throwable) -> Unit)?): Any? {
		return cancellableContinuation.tryResume(value, idempotent, onCancellation).also {
			progress.complete()
		}
	}

	@InternalCoroutinesApi
	override fun tryResumeWithException(exception: Throwable): Any? {
		return cancellableContinuation.tryResumeWithException(exception).also {
			progress.complete()
		}
	}

	@ExperimentalCoroutinesApi
	override fun CoroutineDispatcher.resumeUndispatched(value: T) {
		resumeUndispatched(value)
		progress.complete()
	}

	@ExperimentalCoroutinesApi
	override fun CoroutineDispatcher.resumeUndispatchedWithException(exception: Throwable) {
		resumeUndispatchedWithException(exception)
		progress.complete()
	}
}

/**
 * Wraps a cancellable continuation with progress tracking.
 * When this continuation is resumed, the progress tracker will be marked completed.
 */
fun <T> CancellableContinuation<T>.withProgress(progress: MutableProgress) =
	CancellableContinuationWithProgress(progress, this)
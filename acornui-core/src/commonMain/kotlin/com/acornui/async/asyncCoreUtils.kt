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

import com.acornui.Disposable
import com.acornui.collection.Tuple4
import com.acornui.component.UiComponentRo
import com.acornui.component.createOrReuseAttachment
import com.acornui.di.*
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

/**
 * @see applicationScope
 */
val applicationScopeKey: DKey<CoroutineScope> = dKey()

/**
 * The coroutine scope for this application.
 * All jobs started in this scope will be cancelled on application close.
 */
val Scoped.applicationScope: CoroutineScope
	get() = inject(applicationScopeKey)

/**
 * Launches a new coroutine in the Acorn Global Scope with A coroutine dispatcher that is not confined to any specific thread.
 */
fun Scoped.globalLaunch(context: CoroutineContext = Dispatchers.Unconfined, block: suspend CoroutineScope.() -> Unit): Job {
	return applicationScope.launch(context, block = block)
}

/**
 * Creates a coroutine and returns its future result as an implementation of [Deferred] in the global scope on the
 * main thread.
 */
fun <R> Scoped.globalAsync(context: CoroutineContext = Dispatchers.Unconfined, block: suspend CoroutineScope.() -> R): Deferred<R> {
	return applicationScope.async(context, block = block)
}


/**
 * Invokes a callback when the deferred value has been computed successfully. This callback will not be invoked on
 * failure.
 */
infix fun <T> Deferred<T>.then(callback: (result: T) -> Unit): Deferred<T> {
	GlobalScope.launch(Dispatchers.Unconfined) {
		var successful = false
		var result: T? = null
		try {
			result = await()
			successful = true
		} catch (t: Throwable) {
		}
		@Suppress("UNCHECKED_CAST")
		if (successful) {
			launch(Dispatchers.UI) {
				callback(result as T)
			}
		}
	}
	return this
}

infix fun Deferred<Unit>.then(callback: () -> Unit): Deferred<Unit> {
	return then<Unit> { callback() }
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
	GlobalScope.launch(Dispatchers.Unconfined) {
		try {
			await()
		} catch (t: Throwable) {
			launch(Dispatchers.UI) {
				callback(t.cause ?: t)
			}
		}
	}
	return this
}

/**
 * Invokes a callback when the deferred value has been either been computed successfully or failed.
 */
infix fun <T> Deferred<T>.finally(callback: (result: T?) -> Unit): Deferred<T> {
	GlobalScope.launch(Dispatchers.Unconfined) {
		var result: T? = null
		try {
			result = await()
		} catch (t: Throwable) {
		}
		launch(Dispatchers.UI) {
			callback(result)
		}
	}
	return this
}

class CoroutineScopeAttachment(injector: Injector) : Disposable {

	private val supervisor = SupervisorJob()
	val componentScope = CoroutineScope(injector.inject(applicationScopeKey).coroutineContext + supervisor)

	override fun dispose() {
		supervisor.cancel()
	}

	companion object
}

val UiComponentRo.coroutineScopeAttachment: CoroutineScopeAttachment
	get() = createOrReuseAttachment(CoroutineScopeAttachment) { CoroutineScopeAttachment(injector) }

val UiComponentRo.coroutineScope: CoroutineScope
	get() = coroutineScopeAttachment.componentScope

/**
 * Launches a new coroutine in the scope of this component without blocking the current thread and returns a reference
 * to the coroutine as a [Job].
 * When this component is disposed, the job will automatically be cancelled.
 */
fun UiComponentRo.launch(context: CoroutineContext = Dispatchers.UI, block: suspend CoroutineScope.() -> Unit): Job {
	return coroutineScope.launch(context, block = block)
}

/**
 * Creates a coroutine in the scope of this component and returns its future result as an implementation of [Deferred].
 * When this component is disposed, the job will automatically be cancelled.
 */
fun <R> UiComponentRo.async(context: CoroutineContext = Dispatchers.UI, block: suspend CoroutineScope.() -> R): Deferred<R> {
	return coroutineScope.async(context, block = block)
}
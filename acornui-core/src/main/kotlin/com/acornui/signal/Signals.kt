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

package com.acornui.signal

import com.acornui.*
import com.acornui.collection.Filter
import com.acornui.function.as1
import com.acornui.logging.Log
import com.acornui.observe.Bindable
import com.acornui.recycle.Clearable
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

interface Signal<out T> : Bindable {

	/**
	 * Adds a handler to this signal.
	 *
	 * If this signal is currently dispatching, the added handler will not be dispatched until the next dispatch call.
	 *
	 * @param isOnce A flag, where if true, will cause the handler to be removed immediately after the next dispatch.
	 * @param handler The callback that will be invoked when the signal is dispatched. The handler will be given
	 * a single argument; the value provided on the dispatch.
	 * @return Returns a signal subscription that may be paused or disposed.
	 */
	fun listen(isOnce: Boolean = false, handler: (T) -> Unit): SignalSubscription

	/**
	 * Adds a handler to this signal.
	 *
	 * If this signal is currently dispatching, the added handler will not be dispatched until the next dispatch call.
	 *
	 * @param handler The callback that will be invoked when the signal is dispatched. The handler will be given
	 * a single argument; the value provided on the dispatch.
	 * @return Returns a signal subscription that may be paused or disposed.
	 */
	fun listen(handler: (T) -> Unit): SignalSubscription = listen(false, handler)

	override fun addBinding(callback: () -> Unit): SignalSubscription = listen(callback.as1)

	@Deprecated("use listen", ReplaceWith("listen(handler)"))
	fun add(handler: (T) -> Unit) = listen(false, handler)

	@Deprecated("use listen", ReplaceWith("listen(isOnce, handler)"), DeprecationLevel.ERROR)
	fun add(isOnce: Boolean, handler: (T) -> Unit): Nothing = error("use invoke")

	@Deprecated("", ReplaceWith(""), DeprecationLevel.ERROR)
	fun remove(handler: (T) -> Unit): Nothing = error("")
}

interface MutableSignal<T> : Signal<T>, Clearable, Disposable {

	fun dispatch(value: T)
}

/**
 * Adds a handler to this signal that will be automatically removed the next time the signal is dispatched.
 * @see Signal.listen
 */
fun <T> Signal<T>.once(handler: (T) -> Unit) =
	listen(isOnce = true, handler = handler)

/**
 * Returns a Signal that only invokes the handler if the event passes the given filter.
 */
fun <T : Any> Signal<T>.filtered(filter: Filter<T>): Signal<T> {
	return object : Signal<T> {
		override fun listen(isOnce: Boolean, handler: (T) -> Unit): SignalSubscription {
			return this@filtered.listen(isOnce) {
				if (filter(it))
					handler(it)
			}
		}
	}
}

/**
 * Returns a Signal that transforms this signal's output.
 */
fun <T : Any, R : Any> Signal<T>.map(transform: (T) -> R): Signal<R> {
	return object : Signal<R> {
		override fun listen(isOnce: Boolean, handler: (R) -> Unit): SignalSubscription {
			return this@map.listen(isOnce) {
				handler(transform(it))
			}
		}
	}
}

/**
 * Signals are a way to implement an observer relationship for a single event.
 *
 * The expected concurrent behavior for a handler adding or removing is as follows:
 *
 * If a handler is added within a handler, the new handler will NOT be invoked in the current dispatch.
 * If a handler is removed within a handler, the removed handler will not be invoked in the current dispatch.
 *
 * @author nbilyk
 */
open class SignalImpl<T>() : MutableSignal<T>, Disposable {

	private val subscriptions = arrayListOf<SignalSubscriptionImpl<T>>()
	private var cursor = -1
	private var n = -1

	/**
	 * True if the signal has no handlers.
	 */
	fun isNotEmpty(): Boolean = !isEmpty()

	/**
	 * True if the signal has no handlers.
	 */
	fun isEmpty(): Boolean = subscriptions.isEmpty()

	protected open fun removeSubscription(subscription: SignalSubscription) {
		removeAt(subscriptions.indexOf(subscription))
	}

	private fun removeAt(index: Int) {
		if (index == -1) return
		if (index <= cursor)
			cursor--
		subscriptions.removeAt(index)
		n--
	}

	override fun listen(isOnce: Boolean, handler: (T) -> Unit): SignalSubscription {
		val subscription = SignalSubscriptionImpl(this, handler, isOnce, ::removeSubscription)
		subscriptions.add(subscription)
		return subscription
	}

	/**
	 * True if this signal is currently dispatching.
	 */
	val isDispatching: Boolean
		get() = cursor != -1

	/**
	 * Calls executor on each handler in this signal.
	 */
	final override fun dispatch(value: T) {
		if (cursor != -1)
			Log.error("This signal is currently dispatching.")
		cursor = 0
		n = subscriptions.size
		while (cursor < n) {
			subscriptions[cursor].invoke(value)
			cursor++
		}
		cursor = -1
		n = -1
	}

	final override fun clear() {
		while (subscriptions.isNotEmpty())
			removeAt(subscriptions.lastIndex)
		n = -1
	}

	override fun dispose() {
		clear()
	}
}

class ManagedSignal<T> : SignalImpl<T>(), ManagedDisposable

/**
 * Creates an owned signal that will be disposed automatically when the owner is disposed.
 */
fun <T> Owner.signal(): SignalImpl<T> {
	val s = ManagedSignal<T>()
	own(s as MutableSignal<T>)
	return s
}

/**
 * Creates an unowned signal that must be disposed manually.
 */
fun <T> unmanagedSignal() = SignalImpl<T>()

interface SignalSubscription : Disposable {

	/**
	 * Returns true if this subscription will be removed after the first invocation.
	 * The subscription will not be removed while it's paused.
	 */
	val isOnce: Boolean

	/**
	 * If set to true, the handler will not be invoked
	 */
	var isPaused: Boolean

	/**
	 * Pauses the subscription. Handlers will not be invoked.
	 */
	fun pause() {
		isPaused = true
	}

	/**
	 * Resumes the subscription.
	 */
	fun resume() {
		isPaused = false
	}
}

private class SignalSubscriptionImpl<T>(
	val signal: Signal<T>,
	private val handler: (data: T) -> Unit,
	override val isOnce: Boolean,
	private val disposer: (SignalSubscriptionImpl<T>) -> Unit
) : SignalSubscription {

	override var isPaused = false

	fun invoke(data: T) {
		if (isPaused) return
		if (isOnce)
			dispose()
		handler.invoke(data)
	}

	override fun dispose() {
		disposer(this)
	}
}

class SignalSubscriptionBuilder<T>(override val isOnce: Boolean, val handler: (T) -> Unit) : SignalSubscription {

	private val disposables = ArrayList<Disposable>()

	override var isPaused = false

	var isDisposed = false
		private set

	fun invoke(data: T) {
		if (isPaused || isDisposed) return
		if (isOnce)
			dispose()
		handler.invoke(data)
	}

	operator fun <T : Disposable> T.unaryPlus(): T {
		if (isDisposed) throw DisposedException()
		disposables += this
		return this
	}

	override fun dispose() {
		isDisposed = true
		disposables.forEach(Disposable::dispose)
		disposables.clear()
	}
}

/**
 * When building a signal subscription as a sequence of events, this may be used to track the disposables for cleanup.
 */
fun <T> Signal<T>.buildSubscription(isOnce: Boolean, handler: (T) -> Unit, builder: SignalSubscriptionBuilder<T>.() -> Unit): SignalSubscription {
	return SignalSubscriptionBuilder(isOnce, handler).apply(builder)
}

suspend fun <T> Signal<T>.await() = suspendCancellableCoroutine { cont: CancellableContinuation<T> ->
	this@await.once {
		cont.resume(it)
	}
}
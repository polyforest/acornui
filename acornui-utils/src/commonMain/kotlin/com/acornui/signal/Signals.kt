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

import com.acornui.Disposable
import com.acornui.function.*
import com.acornui.logging.Log
import com.acornui.observe.Bindable
import com.acornui.recycle.Clearable
import com.acornui.toDisposable
import kotlin.jvm.Synchronized

interface Signal<in T : Any> : Bindable {

	/**
	 * Returns true if this signal is currently dispatching.
	 */
	val isDispatching: Boolean

	/**
	 * True if the signal has handlers.
	 */
	fun isNotEmpty(): Boolean

	/**
	 * True if the signal has no handlers.
	 */
	fun isEmpty(): Boolean

	fun add(handler: T) = add(handler, isOnce = false)

	/**
	 * Adds a handler to this signal.
	 *
	 * If this signal is currently dispatching, the added handler will not be dispatched until the next dispatch call.
	 *
	 * @param handler The callback that will be invoked when dispatch() is called on the signal.
	 * @param isOnce A flag, where if true, will cause the handler to be removed immediately after the next dispatch.
	 */
	fun add(handler: T, isOnce: Boolean)

	/**
	 * Removes the given handler from the list.
	 *
	 * If this signal is currently dispatching, the handler will no longer be invoked.
	 */
	fun remove(handler: T)

	/**
	 * Returns true if the handler is currently in the list.
	 */
	fun contains(handler: T): Boolean

}

/**
 * Adds a handler to this signal that will be automatically removed the next time the signal is dispatched.
 * @see Signal.add
 */
fun <T : Any> Signal<T>.addOnce(handler: T) {
	add(handler, isOnce = true)
}

/**
 * Adds a signal and creates a [Disposable] handle that, when invoked, will remove the handler.
 */
fun <T : Any> Signal<T>.addWithHandle(isOnce: Boolean, handler: T): Disposable {
	add(handler, isOnce)
	return { remove(handler) }.toDisposable()
}

fun <T : Any> Signal<T>.addWithHandle(handler: T): Disposable = addWithHandle(false, handler)

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
abstract class SignalBase<T : Any> : Signal<T>, Clearable, Disposable {

	protected val handlers = arrayListOf<T>()
	protected val isOnces = arrayListOf<Boolean>()
	protected var cursor = -1
	protected var n = -1

	@Synchronized
	override fun isEmpty(): Boolean = handlers.isEmpty()

	@Synchronized
	override fun isNotEmpty(): Boolean = !isEmpty()

	@Synchronized
	override fun add(handler: T, isOnce: Boolean) {
		handlers.add(handler)
		isOnces.add(isOnce)
	}

	@Synchronized
	override fun remove(handler: T) {
		val index = handlers.indexOf(handler)
		if (index != -1) {
			removeAt(index)
		}
	}

	@Synchronized
	protected fun removeAt(index: Int) {
		if (index <= cursor) {
			cursor--
		}
		handlers.removeAt(index)
		isOnces.removeAt(index)
		n--
	}

	/**
	 * Returns true if the handler is currently in the list.
	 */
	override fun contains(handler: T): Boolean = handlers.contains(handler)

	/**
	 * Immediately halts the current dispatch.
	 */
	@Synchronized
	open fun halt() {
		if (cursor != -1)
			cursor = 999999999
	}

	override val isDispatching: Boolean
		get() = cursor != -1

	/**
	 * Calls executor on each handler in this signal.
	 */
	protected inline fun dispatch(executor: (T) -> Unit) {
		if (cursor != -1)
			Log.error("This signal is currently dispatching.")
		cursor = 0
		n = handlers.size
		while (cursor < n) {
			val isOnce = isOnces[cursor]
			val handler = handlers[cursor]
			if (isOnce) removeAt(cursor)
			executor(handler)
			cursor++
		}
		cursor = -1
		n = -1
	}

	override fun clear() {
		handlers.clear()
		isOnces.clear()
		n = -1
	}

	override fun dispose() {
		clear()
	}
}

class Signal0 : SignalBase<() -> Unit>() {

	@Synchronized
	fun dispatch() = dispatch { it() }

	override fun addBinding(callback: () -> Unit) = add(callback)
	override fun removeBinding(callback: () -> Unit) = remove(callback)

	fun asRo(): Signal<() -> Unit> = this
}

class Signal1<P1> : SignalBase<(P1) -> Unit>() {

	@Synchronized
	fun dispatch(p1: P1) = dispatch { it(p1) }

	override fun addBinding(callback: () -> Unit) = add(callback.as1)
	override fun removeBinding(callback: () -> Unit) = remove(callback.as1)

	fun asRo(): Signal<(P1) -> Unit> = this
}

class Signal2<P1, P2> : SignalBase<(P1, P2) -> Unit>() {

	@Synchronized
	fun dispatch(p1: P1, p2: P2) = dispatch { it(p1, p2) }

	override fun addBinding(callback: () -> Unit) = add(callback.as2)
	override fun removeBinding(callback: () -> Unit) = remove(callback.as2)

	fun asRo(): Signal<(P1, P2) -> Unit> = this
}

class Signal3<P1, P2, P3> : SignalBase<(P1, P2, P3) -> Unit>() {

	@Synchronized
	fun dispatch(p1: P1, p2: P2, p3: P3) = dispatch { it(p1, p2, p3) }

	override fun addBinding(callback: () -> Unit) = add(callback.as3)
	override fun removeBinding(callback: () -> Unit) = remove(callback.as3)

	fun asRo(): Signal<(P1, P2, P3) -> Unit> = this
}

class Signal4<P1, P2, P3, P4> : SignalBase<(P1, P2, P3, P4) -> Unit>() {

	@Synchronized
	fun dispatch(p1: P1, p2: P2, p3: P3, p4: P4) = dispatch { it(p1, p2, p3, p4) }

	override fun addBinding(callback: () -> Unit) = add(callback.as4)
	override fun removeBinding(callback: () -> Unit) = remove(callback.as4)

	fun asRo(): Signal<(P1, P2, P3, P4) -> Unit> = this
}

class Signal5<P1, P2, P3, P4, P5> : SignalBase<(P1, P2, P3, P4, P5) -> Unit>() {

	@Synchronized
	fun dispatch(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5) = dispatch { it(p1, p2, p3, p4, p5) }

	override fun addBinding(callback: () -> Unit) = add(callback.as5)
	override fun removeBinding(callback: () -> Unit) = remove(callback.as5)

	fun asRo(): Signal<(P1, P2, P3, P4, P5) -> Unit> = this
}

class Signal6<P1, P2, P3, P4, P5, P6> : SignalBase<(P1, P2, P3, P4, P5, P6) -> Unit>() {

	@Synchronized
	fun dispatch(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6) = dispatch { it(p1, p2, p3, p4, p5, p6) }

	override fun addBinding(callback: () -> Unit) = add(callback.as6)
	override fun removeBinding(callback: () -> Unit) = remove(callback.as6)

	fun asRo(): Signal<(P1, P2, P3, P4, P5, P6) -> Unit> = this
}

class Signal7<P1, P2, P3, P4, P5, P6, P7> : SignalBase<(P1, P2, P3, P4, P5, P6, P7) -> Unit>() {

	@Synchronized
	fun dispatch(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7) = dispatch { it(p1, p2, p3, p4, p5, p6, p7) }

	override fun addBinding(callback: () -> Unit) = add(callback.as7)
	override fun removeBinding(callback: () -> Unit) = remove(callback.as7)

	fun asRo(): Signal<(P1, P2, P3, P4, P5, P6, P7) -> Unit> = this
}

class Signal8<P1, P2, P3, P4, P5, P6, P7, P8> : SignalBase<(P1, P2, P3, P4, P5, P6, P7, P8) -> Unit>() {

	@Synchronized
	fun dispatch(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8) = dispatch { it(p1, p2, p3, p4, p5, p6, p7, p8) }

	override fun addBinding(callback: () -> Unit) = add(callback.as8)
	override fun removeBinding(callback: () -> Unit) = remove(callback.as8)

	fun asRo(): Signal<(P1, P2, P3, P4, P5, P6, P7, P8) -> Unit> = this
}

class Signal9<P1, P2, P3, P4, P5, P6, P7, P8, P9> : SignalBase<(P1, P2, P3, P4, P5, P6, P7, P8, P9) -> Unit>() {

	@Synchronized
	fun dispatch(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9) = dispatch { it(p1, p2, p3, p4, p5, p6, p7, p8, p9) }

	override fun addBinding(callback: () -> Unit) = add(callback.as9)
	override fun removeBinding(callback: () -> Unit) = remove(callback.as9)

	fun asRo(): Signal<(P1, P2, P3, P4, P5, P6, P7, P8, P9) -> Unit> = this
}

interface Stoppable {
	fun isStopped(): Boolean
}

/**
 * A utility class to use as a parameter within a Signal that indicates that the behavior of signal should be
 * cancelled. Typically, a signal that can be cancelled should be named as a gerund. Such as, changing, invalidating, etc.
 */
interface CancelRo {

	val isCancelled: Boolean

	fun cancel()
}

class Cancel : CancelRo {

	override var isCancelled: Boolean = false
		private set

	override fun cancel() {
		isCancelled = true
	}

	fun reset(): Cancel {
		isCancelled = false
		return this
	}
}

/**
 * A convenience interface for a signal that may be halted by its single, stoppable parameter.
 */
interface StoppableSignal<out P1 : Stoppable> : Signal<(P1) -> Unit>

/**
 * A signal where the single argument has the ability to halt the signal.
 * @see [SignalBase.halt]
 */
open class StoppableSignalImpl<P1 : Stoppable> : SignalBase<(P1) -> Unit>(), StoppableSignal<P1> {
	fun dispatch(p1: P1) = dispatch {
		it(p1)
		if (p1.isStopped()) halt()
	}

	override fun addBinding(callback: () -> Unit) = add(callback.as1)
	override fun removeBinding(callback: () -> Unit) = remove(callback.as1)

	fun asRo(): StoppableSignal<P1> = this
}

///**
// * An indicator that the handler's type parameter can be safely cast to the dispatching class.
// * Kotlin doesn't support the Self type, so this is a workaround to that limitation.
// */
//typealias Self = Any
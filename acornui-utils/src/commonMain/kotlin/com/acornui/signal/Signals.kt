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

import com.acornui.core.Disposable
import com.acornui.function.*

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
	 *
	 */
	fun add(handler: T, isOnce: Boolean)

	/**
	 * Removes the given handler from the list.
	 *
	 * If this signal is currently dispatching, the handler will be removed after the dispatch has finished.
	 */
	fun remove(handler: T)

	/**
	 * Returns true if the handler is currently in the list.
	 */
	fun contains(handler: T): Boolean

}

fun <T : Any> Signal<T>.addOnce(handler: T) {
	add(handler, true)
}


/**
 * Signals are a way to implement an observer relationship for a single event.
 *
 * The expected concurrent behavior for a handler adding or removing is as follows:
 *
 * If a handler is added within a handler, the new handler will be invoked in the current iteration.
 * If a handler is removed within a handler, the removed handler will not be invoked in the current iteration.
 *
 * @author nbilyk
 */
abstract class SignalBase<T : Any> : Signal<T>, Disposable {

	protected val handlers = arrayListOf<T>()
	protected val isOnces = arrayListOf<Boolean>()
	protected var cursor = -1

	/**
	 * True if the signal has handlers.
	 */
	override fun isNotEmpty(): Boolean = handlers.isNotEmpty()

	/**
	 * True if the signal has no handlers.
	 */
	override fun isEmpty(): Boolean = handlers.isEmpty()

	/**
	 * Adds a handler to this signal.
	 *
	 * If this signal is currently dispatching, the added handler will not be dispatched until the next dispatch call.
	 *
	 * @param handler The callback that will be invoked when dispatch() is called on the signal.
	 * @param isOnce A flag, where if true, will cause the handler to be removed immediately after the next dispatch.
	 */
	override fun add(handler: T, isOnce: Boolean) {
		handlers.add(handler)
		isOnces.add(isOnce)
	}

	/**
	 * Removes the given handler from the list.
	 *
	 * If this signal is currently dispatching, the handler will be removed after the dispatch has finished.
	 */
	override fun remove(handler: T) {
		val index = handlers.indexOf(handler)
		if (index != -1) {
			removeAt(index)
		}
	}

	protected fun removeAt(index: Int) {
		if (index <= cursor) {
			cursor--
		}
		handlers.removeAt(index)
		isOnces.removeAt(index)
	}

	/**
	 * Returns true if the handler is currently in the list.
	 */
	override fun contains(handler: T): Boolean = handlers.contains(handler)

	/**
	 * Immediately halts the current dispatch.
	 */
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
			throw Exception("This signal is currently dispatching.")
		cursor = 0
		try {
			if (handlers.size <= 4) {
				if (cursor < handlers.size) {
					val isOnce1 = isOnces[cursor]
					val handler1 = handlers[cursor]
					if (isOnce1) removeAt(cursor)
					executor(handler1)
					cursor++
				}
				if (cursor < handlers.size) {
					val isOnce2 = isOnces[cursor]
					val handler2 = handlers[cursor]
					if (isOnce2) removeAt(cursor)
					executor(handler2)
					cursor++
				}
				if (cursor < handlers.size) {
					val isOnce3 = isOnces[cursor]
					val handler3 = handlers[cursor]
					if (isOnce3) removeAt(cursor)
					executor(handler3)
					cursor++
				}
				if (cursor < handlers.size) {
					val isOnce4 = isOnces[cursor]
					val handler4 = handlers[cursor]
					if (isOnce4) removeAt(cursor)
					executor(handler4)
					cursor++
				}
			}
			while (cursor < handlers.size) {
				val isOnce = isOnces[cursor]
				val handler = handlers[cursor]
				if (isOnce) removeAt(cursor)
				executor(handler)
				cursor++
			}
		} catch(e: Throwable) {
			throw e
		} finally {
			cursor = -1
		}
	}

	fun clear() {
		handlers.clear()
		isOnces.clear()
	}

	override fun dispose() {
		clear()
	}
}

class Signal0 : SignalBase<() -> Unit>() {
	fun dispatch() = dispatch { it() }

	override fun addBinding(callback: () -> Unit) = add(callback)
	override fun removeBinding(callback: () -> Unit) = remove(callback)

	fun asRo(): Signal<() -> Unit> = this
}

class Signal1<P1> : SignalBase<(P1) -> Unit>() {
	fun dispatch(p1: P1) = dispatch { it(p1) }

	override fun addBinding(callback: () -> Unit) = add(callback.as1)
	override fun removeBinding(callback: () -> Unit) = remove(callback.as1)

	fun asRo(): Signal<(P1) -> Unit> = this
}

class Signal2<P1, P2> : SignalBase<(P1, P2) -> Unit>() {
	fun dispatch(p1: P1, p2: P2) = dispatch { it(p1, p2) }

	override fun addBinding(callback: () -> Unit) = add(callback.as2)
	override fun removeBinding(callback: () -> Unit) = remove(callback.as2)

	fun asRo(): Signal<(P1, P2) -> Unit> = this
}

class Signal3<P1, P2, P3> : SignalBase<(P1, P2, P3) -> Unit>() {
	fun dispatch(p1: P1, p2: P2, p3: P3) = dispatch { it(p1, p2, p3) }

	override fun addBinding(callback: () -> Unit) = add(callback.as3)
	override fun removeBinding(callback: () -> Unit) = remove(callback.as3)

	fun asRo(): Signal<(P1, P2, P3) -> Unit> = this
}

class Signal4<P1, P2, P3, P4> : SignalBase<(P1, P2, P3, P4) -> Unit>() {
	fun dispatch(p1: P1, p2: P2, p3: P3, p4: P4) = dispatch { it(p1, p2, p3, p4) }

	override fun addBinding(callback: () -> Unit) = add(callback.as4)
	override fun removeBinding(callback: () -> Unit) = remove(callback.as4)

	fun asRo(): Signal<(P1, P2, P3, P4) -> Unit> = this
}

class Signal5<P1, P2, P3, P4, P5> : SignalBase<(P1, P2, P3, P4, P5) -> Unit>() {
	fun dispatch(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5) = dispatch { it(p1, p2, p3, p4, p5) }

	override fun addBinding(callback: () -> Unit) = add(callback.as5)
	override fun removeBinding(callback: () -> Unit) = remove(callback.as5)

	fun asRo(): Signal<(P1, P2, P3, P4, P5) -> Unit> = this
}

class Signal6<P1, P2, P3, P4, P5, P6> : SignalBase<(P1, P2, P3, P4, P5, P6) -> Unit>() {
	fun dispatch(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6) = dispatch { it(p1, p2, p3, p4, p5, p6) }

	override fun addBinding(callback: () -> Unit) = add(callback.as6)
	override fun removeBinding(callback: () -> Unit) = remove(callback.as6)

	fun asRo(): Signal<(P1, P2, P3, P4, P5, P6) -> Unit> = this
}

class Signal7<P1, P2, P3, P4, P5, P6, P7> : SignalBase<(P1, P2, P3, P4, P5, P6, P7) -> Unit>() {
	fun dispatch(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7) = dispatch { it(p1, p2, p3, p4, p5, p6, p7) }

	override fun addBinding(callback: () -> Unit) = add(callback.as7)
	override fun removeBinding(callback: () -> Unit) = remove(callback.as7)

	fun asRo(): Signal<(P1, P2, P3, P4, P5, P6, P7) -> Unit> = this
}

class Signal8<P1, P2, P3, P4, P5, P6, P7, P8> : SignalBase<(P1, P2, P3, P4, P5, P6, P7, P8) -> Unit>() {
	fun dispatch(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8) = dispatch { it(p1, p2, p3, p4, p5, p6, p7, p8) }

	override fun addBinding(callback: () -> Unit) = add(callback.as8)
	override fun removeBinding(callback: () -> Unit) = remove(callback.as8)

	fun asRo(): Signal<(P1, P2, P3, P4, P5, P6, P7, P8) -> Unit> = this
}

class Signal9<P1, P2, P3, P4, P5, P6, P7, P8, P9> : SignalBase<(P1, P2, P3, P4, P5, P6, P7, P8, P9) -> Unit>() {
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
 * canceled. Typically, a signal that can be canceled should be named as a gerund. Such as, changing, invalidating, etc.
 */
open class Cancel {

	private var _canceled: Boolean = false

	val canceled: Boolean
		get() = _canceled

	open fun cancel() {
		_canceled = true
	}

	fun reset(): Cancel {
		_canceled = false
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
}

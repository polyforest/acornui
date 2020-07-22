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

package com.acornui.observe

import com.acornui.Disposable
import com.acornui.Owner
import com.acornui.function.as1
import com.acornui.own
import com.acornui.signal.Signal
import com.acornui.signal.unmanagedSignal
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

interface Observable : Bindable {

	/**
	 * Dispatched when this object has changed.
	 */
	val changed: Signal<Observable>

	override fun addBinding(callback: () -> Unit): Disposable =
		changed.addBinding(callback)
}

/**
 * A property delegate that will watch the set list of values for changes, invoking [onChanged] when the list is set,
 * or when any of the elements' [Observable.changed] signal is dispatched.
 */
class ObservableListProperty<E : Iterable<Observable?>>(private var list: E, private val onChanged: () -> Unit) :
	ReadWriteProperty<Any, E>, Disposable {

	private val handles = mutableMapOf<Observable, Disposable>()

	override fun getValue(thisRef: Any, property: KProperty<*>): E {
		return list
	}

	override fun setValue(thisRef: Any, property: KProperty<*>, value: E) {
		if (list != value) {
			(list - value).forEach {
				if (it != null)
					handles.remove(it)?.dispose()
			}
			(value - list).forEach {
				if (it != null)
					handles[it] = it.changed.listen(onChanged.as1)
			}
			list = value
			onChanged()
		}
	}

	override fun dispose() {
		handles.values.forEach(Disposable::dispose)
		handles.clear()
	}

	operator fun provideDelegate(
		thisRef: Owner,
		prop: KProperty<*>
	): ReadWriteProperty<Owner, E> = thisRef.own(this)

	operator fun provideDelegate(
		thisRef: Any,
		prop: KProperty<*>
	): ReadWriteProperty<Any, E> = this
}

/**
 * A property delegate that will watch the set value for changes, invoking [onChanged] when the property is set,
 * or when the set property's [Observable.changed] signal is dispatched.
 */
class ObservableProperty<E : Observable?>(private var value: E, private val onChanged: () -> Unit) :
	ReadWriteProperty<Any, E>, Disposable {

	private var handle: Disposable? = null

	override fun getValue(thisRef: Any, property: KProperty<*>): E = value

	override fun setValue(thisRef: Any, property: KProperty<*>, value: E) {
		if (this.value != value) {
			handle?.dispose()
			handle = value?.changed?.listen(onChanged.as1)
			this.value = value
			onChanged()
		}
	}

	operator fun provideDelegate(
		thisRef: Owner,
		prop: KProperty<*>
	): ReadWriteProperty<Owner, E> = thisRef.own(this)

	operator fun provideDelegate(
		thisRef: Any,
		prop: KProperty<*>
	): ReadWriteProperty<Any, E> = this

	override fun dispose() {
		handle?.dispose()
		handle = null
	}
}

fun <E : Observable?> observableListProp(list: List<E> = emptyList(), onChanged: () -> Unit) =
	ObservableListProperty(list, onChanged)

fun <E : Iterable<Observable?>> observableProp(value: E, onChanged: () -> Unit) =
	ObservableListProperty(value, onChanged)

fun <E : Observable?> observableProp(value: E, onChanged: () -> Unit) =
	ObservableProperty(value, onChanged)


/**
 * A [MutableList] that dispatches a [changed] signal when the list changes.
 */
class ObservableList<E> : AbstractMutableList<E>(), Observable, Disposable {

	override val changed = unmanagedSignal<Observable>()

	private val list = ArrayList<E>()

	override fun add(index: Int, element: E) {
		list.add(index, element)
		notifyChanged()
	}

	override fun removeAt(index: Int): E {
		val e = list.removeAt(index)
		notifyChanged()
		return e
	}

	override val size: Int
		get() = list.size

	override fun get(index: Int): E = list[index]

	override fun set(index: Int, element: E): E {
		val old = list.set(index, element)
		notifyChanged()
		return old
	}

	private fun notifyChanged() {
		changed.dispatch(this)
	}

	override fun clear() {
		list.clear()
		notifyChanged()
	}

	override fun dispose() {
		clear()
		changed.dispose()
	}
}

/**
 * A [MutableList] of [Observable] elements that watches each element for changes.
 */
class WatchedObservableList<E : Observable> : AbstractMutableList<E>(), Observable, Disposable {

	override val changed = unmanagedSignal<Observable>()

	private val list = ArrayList<E>()
	private val handlers = ArrayList<Disposable>()

	override fun add(index: Int, element: E) {
		handlers.add(index, element.changed.listen(::notifyChanged.as1))
		list.add(index, element)
		notifyChanged()
	}

	override fun removeAt(index: Int): E {
		handlers.removeAt(index).dispose()
		val e = list.removeAt(index)
		notifyChanged()
		return e
	}

	override val size: Int
		get() = list.size

	override fun get(index: Int): E = list[index]

	override fun set(index: Int, element: E): E {
		val old = list.set(index, element)
		handlers.set(index, element.changed.listen(::notifyChanged.as1)).dispose()
		notifyChanged()
		return old
	}

	private fun notifyChanged() {
		changed.dispatch(this)
	}

	override fun dispose() {
		clear()
		changed.dispose()
	}
}

fun <E : Observable> watchedObservableListOf(vararg e: E) = WatchedObservableList<E>().apply {
	addAll(e)
}

fun <E> observableListOf(vararg e: E) = ObservableList<E>().apply {
	addAll(e)
}
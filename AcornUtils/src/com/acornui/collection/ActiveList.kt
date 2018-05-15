package com.acornui.collection

import com.acornui.core.Disposable
import com.acornui.observe.Observable
import com.acornui.signal.Signal
import com.acornui.signal.Signal0
import com.acornui.signal.Signal2
import com.acornui.signal.Signal3

/**
 * A list that allows for modification during iteration when using special iteration methods.
 * @author nbilyk
 */
open class ActiveList<E>(initialCapacity: Int) : Clearable, MutableObservableList<E>, Disposable {

	protected val wrapped = ArrayList<E>(initialCapacity)

	private val _added = Signal2<Int, E>()
	override val added: Signal<(Int, E) -> Unit>
		get() = _added

	private val _removed = Signal2<Int, E>()
	override val removed: Signal<(Int, E) -> Unit>
		get() = _removed

	private val _changed = Signal3<Int, E, E>()
	override val changed: Signal<(Int, E, E) -> Unit>
		get() = _changed

	private val _modified = Signal2<Int, E>()
	override val modified: Signal<(Int, E) -> Unit>
		get() = _modified

	private val _reset = Signal0()
	override val reset: Signal<() -> Unit>
		get() = _reset

	protected var updatesEnabled: Boolean = true

	private val iteratorPool = ClearableObjectPool { ConcurrentListIteratorImpl(this) }

	constructor() : this(8)

	constructor(source: List<E>) : this() {
		batchUpdate {
			addAll(source)
		}
	}

	constructor(source: Array<E>) : this() {
		batchUpdate {
			addAll(source)
		}
	}

	override fun remove(element: E): Boolean {
		val index = indexOf(element)
		if (index == -1) return false
		removeAt(index)
		return true
	}

	override fun addAll(index: Int, elements: Collection<E>): Boolean {
		if (elements.isEmpty()) return false
		var i = index
		for (e in elements) {
			add(i++, e)
		}
		return true
	}

	override fun removeAll(elements: Collection<E>): Boolean {
		var changed = false
		for (i in elements) {
			changed = changed && remove(i)
		}
		return changed
	}

	override fun retainAll(elements: Collection<E>): Boolean {
		var changed = false
		iterate {
			if (!elements.contains(it)) {
				changed = true
				remove(it)
			}
			true
		}
		return changed
	}

	override fun clear() {
		wrapped.clear()
		dirty()
	}

	override fun add(index: Int, element: E) {
		wrapped.add(index, element)
		if (updatesEnabled) _added.dispatch(index, element)
	}

	override fun removeAt(index: Int): E {
		val element = wrapped.removeAt(index)
		if (updatesEnabled) _removed.dispatch(index, element)
		return element
	}

	override fun subList(fromIndex: Int, toIndex: Int): MutableList<E> {
		return wrapped.subList(fromIndex, toIndex)
	}

	override val size: Int
		get() = wrapped.size

	override fun add(element: E): Boolean {
		add(size, element)
		return true
	}

	override fun addAll(elements: Collection<E>): Boolean {
		if (elements.isEmpty()) return false
		for (i in elements) {
			add(i)
		}
		return true
	}

	override fun set(index: Int, element: E): E {
		val oldElement = wrapped.set(index, element)
		if (oldElement === element) return oldElement
		if (updatesEnabled) _changed.dispatch(index, oldElement, element)
		return oldElement
	}

	override fun isEmpty(): Boolean {
		return wrapped.isEmpty()
	}

	override fun contains(element: E): Boolean {
		return wrapped.contains(element)
	}

	override fun containsAll(elements: Collection<E>): Boolean {
		return wrapped.containsAll(elements)
	}

	override fun get(index: Int): E {
		return wrapped[index]
	}

	override fun indexOf(element: E): Int {
		return wrapped.indexOf(element)
	}

	override fun lastIndexOf(element: E): Int {
		return wrapped.lastIndexOf(element)
	}

	override fun concurrentIterator(): MutableConcurrentListIterator<E> = MutableConcurrentListIteratorImpl(this)

	override fun iterator(): MutableConcurrentListIterator<E> = MutableConcurrentListIteratorImpl(this)

	override fun listIterator(): MutableConcurrentListIterator<E> {
		return MutableConcurrentListIteratorImpl(this)
	}

	override fun listIterator(index: Int): MutableConcurrentListIterator<E> {
		val iterator = MutableConcurrentListIteratorImpl(this)
		iterator.cursor = index
		return iterator
	}

	override fun iterate(body: (E) -> Boolean) {
		val iterator = iteratorPool.obtain()
		while (iterator.hasNext()) {
			val shouldContinue = body(iterator.next())
			if (!shouldContinue) break
		}
		iteratorPool.free(iterator)
	}

	override fun iterateReversed(body: (E) -> Boolean) {
		val iterator = iteratorPool.obtain()
		iterator.cursor = size
		while (iterator.hasPrevious()) {
			val shouldContinue = body(iterator.previous())
			if (!shouldContinue) break
		}
		iteratorPool.free(iterator)
	}

	final override fun batchUpdate(inner: () -> Unit) {
		updatesEnabled = false
		inner()
		updatesEnabled = true
		_reset.dispatch()
	}

	override fun notifyElementModified(index: Int) {
		_modified.dispatch(index, get(index))
	}

	/**
	 * Force notifies a reset.
	 */
	fun dirty() {
		if (updatesEnabled)
			_reset.dispatch()
	}

	override fun toString(): String {
		return wrapped.toString()
	}

	override fun dispose() {
		clear()
		_added.dispose()
		_removed.dispose()
		_changed.dispose()
		_modified.dispose()
		_reset.dispose()
	}
}

open class WatchedElementsActiveList<E : Observable>(initialCapacity: Int) : ActiveList<E>(initialCapacity) {

	constructor() : this(8)

	constructor(source: List<E>) : this() {
		batchUpdate {
			addAll(source)
		}
	}

	constructor(source: Array<E>) : this() {
		batchUpdate {
			addAll(source)
		}
	}

	override fun removeAt(index: Int): E {
		val e = super.removeAt(index)
		e.changed.remove(this::elementModifiedHandler)
		return e
	}

	override fun add(index: Int, element: E) {
		element.changed.add(this::elementModifiedHandler)
		super.add(index, element)
	}

	private fun elementModifiedHandler(o: Observable) {
		val i = indexOfFirst { it === o } // Use identity equals
		notifyElementModified(i)
	}

	override fun clear() {
		for (i in 0..wrapped.lastIndex) {
			wrapped[i].changed.remove(this::elementModifiedHandler)
		}
		super.clear()
	}
}

fun <E> activeListOf(vararg elements: E): ActiveList<E> {
	val list = ActiveList<E>()
	list.batchUpdate {
		for (i in 0..elements.lastIndex) {
			list.add(elements[i])
		}
	}
	return list
}
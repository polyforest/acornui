package com.acornui.collection


/**
 * A ring buffer with fast push/pop/unshift/shift.

 * Good for queues and stacks.
 * Does not support arbitrary insertion or removal, only push/pop/unshift/shift.

 * @author nbilyk
 */
class CyclicList<E>(initialCapacity: Int = 16) : Clearable, MutableListBase<E>() {

	private var items = ArrayList<E?>(initialCapacity)

	private var _size: Int = 0
	private var capacity: Int = initialCapacity

	override val size: Int
		get() = _size

	private var start: Int = 0

	init {
		items.fill(initialCapacity, { null })
	}

	/**
	 * pushFront
	 * Add each of the elements to beginning of the list. The added items will be in reverse order.
	 */
	fun unshift(vararg values: E) {
		for (i in 0..values.lastIndex) {
			unshift(values[i])
		}
	}

	/**
	 * pushFront
	 * Adds an element to the beginning of the list.
	 */
	fun unshift(value: E) {
		if (_size == capacity) {
			resize(maxOf(8, (_size * 1.75f).toInt()))
		}
		val localIndex = getLocalIndex(-1)
		items[localIndex] = value
		start = localIndex
		_size++
	}

	/**
	 * popFront
	 * Removes an item from the beginning of the list.
	 */
	fun shift(): E {
		if (_size == 0) throw Exception("List is empty")
		val item = items[start]!!
		items[start] = null
		start = getLocalIndex(1)
		_size--
		return item
	}

	/**
	 * pushBack
	 */
	fun addAll(values: Iterable<E>) {
		for (t in values) {
			add(t)
		}
	}

	/**
	 * pushBack
	 */
	fun addAll(vararg values: E) {
		for (i in 0..values.lastIndex) {
			add(values[i])
		}
	}

	override fun add(index: Int, element: E) {
		if (index == 0) {
			unshift(element)
		} else if (index == size) {
			add(element)
		} else {
			val localIndex = getLocalIndex(index)
			items.add(localIndex, element)
			_size++
			capacity++
		}
	}

	override fun removeAt(index: Int): E {
		if (index >= _size) throw IndexOutOfBoundsException(index)
		return if (index == 0) {
			shift()
		} else if (index == lastIndex) {
			pop()
		} else {
			val localIndex = getLocalIndex(index)
			val e = items.removeAt(localIndex)!!
			_size--
			capacity--
			e
		}
	}

	override fun set(index: Int, element: E): E {
		val localIndex = getLocalIndex(index)
		val previous = items[localIndex] ?: throw IndexOutOfBoundsException(index)
		items[localIndex] = element
		return previous
	}

	/**
	 * pushBack
	 */
	override fun add(element: E): Boolean {
		if (_size == capacity) {
			resize(maxOf(8, (_size * 1.75f).toInt()))
		}
		val localIndex = getLocalIndex(_size)
		items[localIndex] = element
		_size++
		return true
	}

	/**
	 * popBack
	 */
	fun pop(): E {
		if (_size == 0) throw Exception("List is empty")
		val localIndex = getLocalIndex(_size - 1)
		val item = items[localIndex]!!
		items[localIndex] = null
		_size--
		return item
	}

	@Suppress("UNCHECKED_CAST")
	override fun get(index: Int): E {
		if (index < 0 || index >= size) throw IllegalArgumentException("Index is out of bounds: $index")
		val localIndex = getLocalIndex(index)
		return items[localIndex] as E
	}

	private fun getLocalIndex(index: Int): Int {
		var localIndex = start + index
		if (localIndex >= capacity) {
			localIndex -= capacity
			if (localIndex >= start) {
				throw IllegalArgumentException("Index is out of bounds: $index")
			}
		}
		if (localIndex < 0) {
			localIndex += capacity
			if (localIndex < start + _size) {
				throw IllegalArgumentException("Index is out of bounds: $index")
			}
		}
		return localIndex
	}

	override fun clear() {
		for (i in 0.._size - 1) {
			var localIndex = i + start
			if (localIndex >= capacity)
				localIndex -= capacity
			else if (localIndex < 0)
				localIndex += capacity
			items[localIndex] = null
		}
		_size = 0
		start = 0
	}

	/**
	 * Resizes the backing items array to meet the new capacity.
	 */
	private fun resize(newCapacity: Int) {
		val newItems = ArrayList<E?>(newCapacity)
		newItems.fill(newCapacity, { null })
		if (start + size > capacity) {
			// Items wrap.
			arrayCopy(items, start, newItems, 0, capacity - start)
			arrayCopy(items, 0, newItems, capacity - start, size - capacity + start)
		} else {
			arrayCopy(items, start, newItems, 0, size)
		}

		this.items = newItems
		this.start = 0
		this.capacity = newCapacity
	}
}

val cyclicListPool = ClearableObjectPool<CyclicList<*>> { CyclicList<Any>() }

/**
 * Obtains a cyclic list from [cyclicListPool]. Be sure to call `cyclicListPool.free(v)` when the list is no longer
 * used.
 */
fun <E> cyclicListObtain(): CyclicList<E> {
	@Suppress("UNCHECKED_CAST")
	return cyclicListPool.obtain() as CyclicList<E>
}
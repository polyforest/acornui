package com.acornui.collection


fun <E> arrayCopy(src: List<E>,
				  srcPos: Int,
				  dest: MutableList<E>,
				  destPos: Int = 0,
				  length: Int = src.size) {

	if (destPos > srcPos) {
		var destIndex = length + destPos - 1
		for (i in srcPos + length - 1 downTo srcPos) {
			dest.addOrSet(destIndex--, src[i])
		}
	} else {
		var destIndex = destPos
		for (i in srcPos..srcPos + length - 1) {
			dest.addOrSet(destIndex++, src[i])
		}
	}
}

fun <E> List<E>.copy(): MutableList<E> {
	val newList = ArrayList<E>(size)
	arrayCopy(this, 0, newList)
	return newList
}

/**
 * Adds all the items in the [other] list that this list does not already contain.
 * This uses List as opposed to Iterable to avoid allocation.
 */
fun <E> MutableList<in E>.addAllUnique(other: List<E>) {
	for (i in 0..other.lastIndex) {
		val item = other[i]
		if (!contains(item)) {
			add(item)
		}
	}
}


/**
 * Adds all the items in the [other] list that this list does not already contain.
 * This uses List as opposed to Iterable to avoid allocation.
 */
fun <E> MutableList<in E>.addAllUnique(other: Array<out E>) {
	for (i in 0..other.lastIndex) {
		val item = other[i]
		if (!contains(item)) {
			add(item)
		}
	}
}

/**
 * Given a sorted list of comparable objects, this finds the insertion index of the given element.
 * If there are equal elements, the insertion index returned will be after.
 */
fun <E : Comparable<E>> List<E>.sortedInsertionIndex(element: E, startIndex: Int = 0, endIndex: Int = size, matchForwards: Boolean = true): Int {
	var indexA = startIndex
	var indexB = endIndex

	while (indexA < indexB) {
		val midIndex = (indexA + indexB) ushr 1
		val comparison = element.compareTo(this[midIndex])

		if (comparison == 0) {
			if (matchForwards) {
				indexA = midIndex + 1
			} else {
				indexB = midIndex
			}
		} else if (comparison > 0) {
			indexA = midIndex + 1
		} else {
			indexB = midIndex
		}
	}
	return indexA
}

/**
 * @param element The element with which to calculate the insertion index.
 *
 * @param comparator A comparison function used to determine the sorting order of elements in the sorted
 * list. A comparison function should take two arguments to compare. Given the elements A and B, the
 * result of compareFunction can have a negative, 0, or positive value:
 * A negative return value specifies that A appears before B in the sorted sequence.
 * A return value of 0 specifies that A and B have the same sort order.
 * A positive return value specifies that A appears after B in the sorted sequence.
 * The compareFunction must never return return ambiguous results.
 * That is, (A, B) != (B, A), unless == 0
 *
 * @param matchForwards If true, the returned index will be after comparisons of 0, if false, before.
 */
fun <K, E> List<E>.sortedInsertionIndex(element: K, comparator: (K, E) -> Int, startIndex: Int = 0, endIndex: Int = size, matchForwards: Boolean = true): Int {
	var indexA = startIndex
	var indexB = endIndex

	while (indexA < indexB) {
		val midIndex = (indexA + indexB) ushr 1
		val comparison = comparator(element, this[midIndex])

		if (comparison == 0) {
			if (matchForwards) {
				indexA = midIndex + 1
			} else {
				indexB = midIndex
			}
		} else if (comparison > 0) {
			indexA = midIndex + 1
		} else {
			indexB = midIndex
		}
	}
	return indexA
}

/**
 * Adds an element to a sorted list based on the provided comparator function.
 */
fun <E> MutableList<E>.addSorted(element: E, comparator: (o1: E, o2: E) -> Int, matchForwards: Boolean = true): Int {
	val index = sortedInsertionIndex(element, comparator, matchForwards = matchForwards)
	add(index, element)
	return index
}

/**
 * Adds an element to a sorted list based on the element's compareTo.
 */
fun <E : Comparable<E>> MutableList<E>.addSorted(element: E, matchForwards: Boolean = true): Int {
	val index = sortedInsertionIndex(element, matchForwards = matchForwards)
	add(index, element)
	return index
}


/**
 * Provides a partial implementation of the List interface.
 */
abstract class ListBase<E> : List<E> {

	val lastIndex: Int
		get() = size - 1

	override fun indexOf(element: E): Int {
		for (i in 0..lastIndex) {
			if (this[i] == element) return i
		}
		return -1
	}

	override fun lastIndexOf(element: E): Int {
		for (i in lastIndex downTo 0) {
			if (this[i] == element) return i
		}
		return -1
	}

	override fun contains(element: E): Boolean {
		for (i in 0..lastIndex) {
			if (this[i] == element) return true
		}
		return false
	}

	override fun containsAll(elements: Collection<E>): Boolean {
		for (element in elements) {
			if (!contains(element)) return false
		}
		return true
	}

	override fun isEmpty(): Boolean {
		return size == 0
	}

	override fun iterator(): Iterator<E> {
		return ListIteratorImpl(this)
	}

	override fun listIterator(): ListIterator<E> {
		return ListIteratorImpl(this)
	}

	override fun listIterator(index: Int): ListIterator<E> {
		val t = ListIteratorImpl(this)
		t.cursor = index
		return t
	}

	override fun subList(fromIndex: Int, toIndex: Int): List<E> {
		return SubList(this, fromIndex, toIndex)
	}
}

abstract class MutableListBase<E> : ListBase<E>(), Clearable, MutableList<E> {

	override fun add(element: E): Boolean {
		add(size, element)
		return true
	}

	override fun addAll(index: Int, elements: Collection<E>): Boolean {
		var i = index
		for (element in elements) {
			add(i++, element)
		}
		return true
	}

	override fun addAll(elements: Collection<E>): Boolean {
		for (element in elements) {
			add(element)
		}
		return true
	}

	override fun clear() {
		for (i in lastIndex downTo 0) {
			removeAt(i)
		}
	}

	override fun remove(element: E): Boolean {
		val index = indexOf(element)
		if (index == -1) return false
		removeAt(index)
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
		for (i in 0..lastIndex) {
			val element = this[i]
			if (!elements.contains(element)) {
				changed = true
				remove(element)
			}
		}
		return changed
	}

	override fun iterator(): MutableIterator<E> {
		return MutableListIteratorImpl(this)
	}

	override fun listIterator(): MutableListIterator<E> {
		return MutableListIteratorImpl(this)
	}

	override fun listIterator(index: Int): MutableListIterator<E> {
		val iterator = MutableListIteratorImpl(this)
		iterator.cursor = index
		return iterator
	}

	override fun subList(fromIndex: Int, toIndex: Int): MutableList<E> {
		return MutableSubList(this, fromIndex, toIndex)
	}
}

/**
 * An iterator object for a simple List.
 */
open class ListIteratorImpl<out E>(
		val list: List<E>
) : Clearable, ListIterator<E>, Iterable<E> {

	var cursor: Int = 0     // index of next element to return
	var lastRet: Int = -1   // index of last element returned; -1 if no such

	override fun hasNext(): Boolean {
		return cursor < list.size
	}

	override fun next(): E {
		val i = cursor
		if (i >= list.size)
			throw Exception("Iterator does not have next.")
		cursor = i + 1
		lastRet = i
		return list[i]
	}

	override fun nextIndex(): Int {
		return cursor
	}

	override fun hasPrevious(): Boolean {
		return cursor != 0
	}

	override fun previous(): E {
		val i = cursor - 1
		if (i < 0)
			throw Exception("Iterator does not have previous.")
		cursor = i
		lastRet = i
		return list[i]
	}

	override fun previousIndex(): Int {
		return cursor - 1
	}

	/**
	 * Resets the iterator to the beginning.
	 */
	override fun clear() {
		cursor = 0
		lastRet = -1
	}

	override fun iterator(): Iterator<E> {
		return this
	}

}

open class MutableListIteratorImpl<E>(private val mutableList: MutableList<E>) : ListIteratorImpl<E>(mutableList), MutableListIterator<E> {

	override fun add(element: E) {
		mutableList.add(cursor, element)
		cursor++
		lastRet++
	}

	override fun remove() {
		if (lastRet < 0)
			throw Exception("Cannot remove before iteration.")
		mutableList.removeAt(lastRet)
	}

	override fun set(element: E) {
		if (lastRet < 0)
			throw Exception("Cannot set before iteration.")
		mutableList[lastRet] = element
	}
}

class SubList<E>(
		private val target: List<E>,
		private val fromIndex: Int,
		private val toIndex: Int
) : ListBase<E>() {

	override val size: Int
		get() {
			return toIndex - fromIndex
		}

	override fun get(index: Int): E = target[index - fromIndex]
}

class MutableSubList<E>(
		private val target: MutableList<E>,
		private val fromIndex: Int,
		private val toIndex: Int
) : MutableListBase<E>() {

	override val size: Int
		get() {
			return toIndex - fromIndex
		}

	override fun get(index: Int): E = target[index - fromIndex]

	override fun add(index: Int, element: E) {
		target.add(index - fromIndex, element)
	}

	override fun removeAt(index: Int): E {
		return target.removeAt(index - fromIndex)
	}

	override fun set(index: Int, element: E): E {
		return target.set(index - fromIndex, element)
	}
}

val arrayListPool = object : ObjectPool<MutableList<*>>(8, { ArrayList<Any?>() }) {
	override fun free(obj: MutableList<*>) {
		obj.clear()
		super.free(obj)
	}
}

/**
 * Obtains a mutable list from the [arrayListPool]. Be sure to call `arrayListPool.free(v)` when it's no longer used.
 */
fun <E> arrayListObtain(): MutableList<E> {
	@Suppress("UNCHECKED_CAST")
	return arrayListPool.obtain() as MutableList<E>
}

fun <E> MutableList<E>.addOrSet(i: Int, value: E) {
	if (i == size) add(value)
	else set(i, value)
}

inline fun <E> MutableList<E>.fill(newSize: Int, factory: () -> E) {
	for (i in size..newSize - 1) {
		add(factory())
	}
}

fun <E> MutableList<E>.addAll2(list: List<E>) = addAll2(0, list)

/**
 * A workaround to KT-7809
 */
fun <E> MutableList<E>.addAll2(index: Int, list: List<E>) {
	for (i in 0..list.lastIndex) {
		add(index + i, list[i])
	}
}


fun <E> Iterator<E>.toList(): List<E> {
	val list = ArrayList<E>()
	while (hasNext()) {
		list.add(next())
	}
	return list
}

inline fun <E> ArrayList(size: Int, factory: (index: Int) -> E): ArrayList<E> {
	val a = ArrayList<E>(size)
	for (i in 0..size - 1) {
		a.add(factory(i))
	}
	return a
}

fun <E> arrayList2(array: Array<E>): ArrayList<E> {
	val a = ArrayList<E>(maxOf(16, array.size))
	for (i in 0..array.lastIndex) {
		a.add(array[i])
	}
	return a
}

/**
 * Appends all elements matching the given [predicate] to the given [destination].
 * Does not cause allocation.
 */
inline fun <T, C : MutableCollection<in T>> List<T>.filterTo2(destination: C, predicate: (T) -> Boolean): C {
	for (i in 0..lastIndex) {
		val element = this[i]
		if (predicate(element)) destination.add(element)
	}
	return destination
}

/**
 * Returns the first element matching the given [predicate], or `null` if no such element was found.
 * Does not cause allocation.
 */
inline fun <T> List<T>.find2(startIndex: Int = 0, predicate: (T) -> Boolean): T? {
	val index = indexOfFirst2(startIndex, lastIndex, predicate)
	return if (index == -1) null else this[index]
}

/**
 * Returns index of the first element matching the given [predicate], or -1 if this list does not contain such element.
 * The search goes starting from startIndex to lastIndex
 * @param startIndex The starting index to search from (inclusive).
 * @param lastIndex The ending index to search to (inclusive). lastIndex >= startIndex
 */
inline fun <T> List<T>.indexOfFirst2(startIndex: Int = 0, lastIndex: Int = this.lastIndex, predicate: (T) -> Boolean): Int {
	if (startIndex == lastIndex) return if (predicate(this[startIndex])) startIndex else -1
	for (i in startIndex..lastIndex) {
		if (predicate(this[i]))
			return i
	}
	return -1
}

/**
 * Returns index of the last element matching the given [predicate], or -1 if this list does not contain such element.
 * The search goes in reverse starting from lastIndex downTo startIndex
 * @param lastIndex The starting index to search from (inclusive).
 * @param startIndex The ending index to search to (inclusive).
 */
inline fun <T> List<T>.indexOfLast2(lastIndex: Int = this.lastIndex, startIndex: Int = 0, predicate: (T) -> Boolean): Int {
	if (lastIndex == startIndex) return if (predicate(this[lastIndex])) lastIndex else -1
	for (i in lastIndex downTo startIndex) {
		if (predicate(this[i]))
			return i
	}
	return -1
}

inline fun <T> List<T>.forEach2(action: (T) -> Unit): Unit {
	for (i in 0..lastIndex) action(this[i])
}

inline fun <T> List<T>.forEachReversed2(action: (T) -> Unit): Unit {
	for (i in lastIndex downTo 0) action(this[i])
}

fun List<Float>.sum2(): Float {
	var t = 0f
	for (i in 0..lastIndex) {
		t += this[i]
	}
	return t
}

typealias SortComparator<E> = (o1: E, o2: E) -> Int

fun <E> MutableList<E>.addAll(vararg elements: E) {
	addAll(elements.toList())
}

/**
 * Creates a wrapper to a target list that maps the elements on retrieval.
 */
class ListTransform<T, R>(private val target: List<T>, private val transform: (T) -> R) : ListBase<R>() {
	override val size: Int
		get() = target.size

	override fun get(index: Int): R {
		return transform(target[index])
	}
}

/**
 * Returns the number of elements matching the given [predicate].
 * @param predicate A method that returns true if the counter should increment.
 * @param startIndex The index to start counting form (inclusive)
 * @param lastIndex The index to count until (inclusive)
 * @return Returns a count representing the number of times [predicate] returned true. This will always be within the
 * range 0 and (lastIndex - startIndex + 1)
 */
inline fun <T> List<T>.count2(startIndex: Int = 0, lastIndex: Int = this.lastIndex, predicate: (T) -> Boolean): Int {
	var count = 0
	for (i in startIndex..lastIndex) if (predicate(this[i])) count++
	return count
}


inline fun <T> MutableList<T>.removeFirst(predicate: (T) -> Boolean): T? {
	val index = indexOfFirst2(0, lastIndex, predicate)
	if (index == -1) return null
	return removeAt(index)
}

/**
 * Returns the sum of all values produced by [selector] function applied to each element in the collection.
 */
inline fun <T> List<T>.sumBy2(selector: (T) -> Int): Int {
	var sum: Int = 0
	for (i in 0..lastIndex) {
		sum += selector(this[i])
	}
	return sum
}

/**
 * Returns the sum of all values produced by [selector] function applied to each element in the collection.
 */
inline fun <T> List<T>.sumByFloat2(selector: (T) -> Float): Float {
	var sum: Float = 0f
	for (i in 0..lastIndex) {
		sum += selector(this[i])
	}
	return sum
}
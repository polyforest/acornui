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

fun <E> Collection<E>.copy(): MutableList<E> {
	val newList = ArrayList<E>(size)
	newList.addAll(this)
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
fun <K, E> List<E>.sortedInsertionIndex(element: K, fromIndex: Int = 0, toIndex: Int = size, matchForwards: Boolean = true, comparator: (K, E) -> Int): Int {
	var indexA = fromIndex
	var indexB = toIndex

	if (indexA < indexB) {
		val midIndex = toIndex - 1
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
	} else {
		return indexA
	}

	if (indexA < indexB) {
		val midIndex = indexA
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
	} else {
		return indexA
	}

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
 * Given a sorted list of comparable objects, this finds the insertion index of the given element.
 * If there are equal elements, the insertion index returned will be after.
 */
fun <E : Comparable<E>> List<E>.sortedInsertionIndex(element: E, fromIndex: Int = 0, toIndex: Int = size, matchForwards: Boolean = true): Int {
	return sortedInsertionIndex(element, fromIndex, toIndex, matchForwards, { o1, o2 -> o1.compareTo(o2) })
}

/**
 * @param comparator A comparison function used to determine the sorting order of elements in the sorted
 * list. A return value of 1 will insert later in the list, -1 will insert earlier in the list, and a return value of
 * 0 will be later if [matchForwards] is true, or earlier if [matchForwards] is false.
 *
 * @param matchForwards If true, the returned index will be after comparisons of 0, if false, before.
 */
fun <E> List<E>.sortedInsertionIndex(fromIndex: Int = 0, toIndex: Int = size, matchForwards: Boolean = true, comparator: (E) -> Int): Int {
	return sortedInsertionIndex(null, fromIndex, toIndex, matchForwards, { _, o2 -> comparator(o2) })
}

/**
 * Adds an element to a sorted list based on the provided comparator function.
 */
fun <E> MutableList<E>.addSorted(element: E, matchForwards: Boolean = true, comparator: (o1: E, o2: E) -> Int): Int {
	val index = sortedInsertionIndex(element, matchForwards = matchForwards, comparator = comparator)
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
abstract class ListBase<out E> : List<E> {

	val lastIndex: Int
		get() = size - 1

	override fun indexOf(element: @UnsafeVariance E): Int {
		for (i in 0..lastIndex) {
			if (this[i] == element) return i
		}
		return -1
	}

	override fun lastIndexOf(element: @UnsafeVariance E): Int {
		for (i in lastIndex downTo 0) {
			if (this[i] == element) return i
		}
		return -1
	}

	override fun contains(element: @UnsafeVariance E): Boolean {
		for (i in 0..lastIndex) {
			if (this[i] == element) return true
		}
		return false
	}

	override fun containsAll(elements: Collection<@UnsafeVariance E>): Boolean {
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
inline fun <E, C : MutableCollection<in E>> List<E>.filterTo2(destination: C, predicate: (E) -> Boolean): C {
	for (i in 0..lastIndex) {
		val element = this[i]
		if (predicate(element)) destination.add(element)
	}
	return destination
}

@Deprecated("renamed to first2", ReplaceWith("this.firstOrNull2(startIndex, predicate)"), DeprecationLevel.ERROR)
inline fun <E> List<E>.find2(startIndex: Int = 0, predicate: (E) -> Boolean): E? = this.firstOrNull2(startIndex, predicate)

/**
 * Returns the first element matching the given [predicate], or `null` if no such element was found.
 * Does not cause allocation.
 */
inline fun <E> List<E>.firstOrNull2(startIndex: Int = 0, predicate: (E) -> Boolean): E? {
	val index = indexOfFirst2(startIndex, lastIndex, predicate)
	return if (index == -1) null else this[index]
}

/**
 * Returns the first element matching the given [predicate], or throws an exception if no such element was found.
 * Does not cause allocation.
 */
inline fun <E> List<E>.first2(startIndex: Int = 0, predicate: (E) -> Boolean): E? {
	val index = indexOfFirst2(startIndex, lastIndex, predicate)
	return if (index == -1) throw Exception("Element not found matching predicate") else this[index]
}

/**
 * Returns index of the first element matching the given [predicate], or -1 if this list does not contain such element.
 * The search goes starting from startIndex to lastIndex
 * @param startIndex The starting index to search from (inclusive).
 * @param lastIndex The ending index to search to (inclusive). lastIndex >= startIndex
 */
inline fun <E> List<E>.indexOfFirst2(startIndex: Int = 0, lastIndex: Int = this.lastIndex, predicate: (E) -> Boolean): Int {
	if (isEmpty()) return -1
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
inline fun <E> List<E>.indexOfLast2(lastIndex: Int = this.lastIndex, startIndex: Int = 0, predicate: (E) -> Boolean): Int {
	if (isEmpty()) return -1
	if (lastIndex == startIndex) return if (predicate(this[lastIndex])) lastIndex else -1
	for (i in lastIndex downTo startIndex) {
		if (predicate(this[i]))
			return i
	}
	return -1
}

inline fun <E> List<E>.forEach2(action: (E) -> Unit): Unit {
	for (i in 0..lastIndex) action(this[i])
}

inline fun <E> List<E>.forEachReversed2(action: (E) -> Unit): Unit {
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
typealias Filter<E> = (E) -> Boolean

fun <E> MutableList<E>.addAll(vararg elements: E) {
	addAll(elements.toList())
}

/**
 * Creates a wrapper to a target list that maps the elements on retrieval.
 */
class ListTransform<E, R>(private val target: List<E>, private val transform: (E) -> R) : ListBase<R>() {
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
inline fun <E> List<E>.count2(startIndex: Int = 0, lastIndex: Int = this.lastIndex, predicate: (E) -> Boolean): Int {
	var count = 0
	for (i in startIndex..lastIndex) if (predicate(this[i])) count++
	return count
}


inline fun <E> MutableList<E>.removeFirst(predicate: (E) -> Boolean): E? {
	val index = indexOfFirst2(0, lastIndex, predicate)
	if (index == -1) return null
	return removeAt(index)
}

/**
 * Returns the sum of all values produced by [selector] function applied to each element in the collection.
 */
inline fun <E> List<E>.sumBy2(selector: (E) -> Int): Int {
	var sum = 0
	for (i in 0..lastIndex) {
		sum += selector(this[i])
	}
	return sum
}

/**
 * Returns the sum of all values produced by [selector] function applied to each element in the collection.
 */
inline fun <E> List<E>.sumByFloat2(selector: (E) -> Float): Float {
	var sum = 0f
	for (i in 0..lastIndex) {
		sum += selector(this[i])
	}
	return sum
}

/**
 * Returns a view of the portion of this list between the specified 0 and [size] (exclusive).
 * The returned list is backed by this list, so non-structural changes in the returned list are reflected in this list.
 *
 * Structural changes in the base list make the behavior of the view undefined.
 * @see List.subList
 */
fun <E> List<E>.subList(size: Int): List<E> = subList(0, size)

/**
 * Modifies this list to become the new size.
 */
fun <E> MutableList<E>.setSize(newSize: Int, factory: () -> E) {
	if (newSize < size) {
		for (i in 0 until size - newSize)
			pop()
	} else if (newSize > size) {
		for (i in 0 until newSize - size)
			add(factory())
	}
}

/**
 * Clones this list, replacing the value at the given index with the new value.
 */
fun <E> List<E>.replaceAt(index: Int, newValue: E): List<E> {
	val newList = ArrayList<E>(size)
	for (i in 0..lastIndex) {
		newList.add(if (i == index) newValue else this[i])
	}
	return newList
}

/**
 * Clones this list, replacing values that identity equals [oldValue] with [newValue].
 * @throws Exception Throws exception when [oldValue] was not found.
 */
fun <E> List<E>.replace(oldValue: E, newValue: E): List<E> {
	val newList = ArrayList<E>(size)
	var found = false
	for (i in 0..lastIndex) {
		newList.add(if (this[i] === oldValue) {
			found = true
			newValue
		} else this[i])
	}
	if (!found) throw Exception("Could not find $oldValue")
	return newList
}

fun <E> List<E>.replaceFirstWhere(newValue: E, predicate: (E) -> Boolean): List<E> {
	val index = indexOfFirst2(0, lastIndex, predicate)
	return if (index == -1) throw Exception("Could not find a value matching the predicate")
	else replaceAt(index, newValue)
}

/**
 * Clones this list, replacing the given range with the new elements.
 * @param startIndex The starting index of the replacement.
 * @param endIndex The range [startIndex] to [endIndex] (exclusive) will not be added to the new list. Must not be less
 * than startIndex
 */
fun <E> List<E>.replaceRange(startIndex: Int, endIndex: Int = startIndex, newElements: List<E>): List<E> {
	if (endIndex > size) throw IllegalArgumentException("endIndex ($endIndex) may not be greater than size ($size)")
	if (endIndex < startIndex) throw IllegalArgumentException("endIndex ($endIndex) may not be less than startIndex ($startIndex)")
	if (endIndex == startIndex && newElements.isEmpty()) return copy()
	val newSize = size - (endIndex - startIndex) + newElements.size
	val newList = ArrayList<E>(newSize)
	for (i in 0..startIndex - 1) {
		newList.add(this[i])
	}
	for (i in 0..newElements.lastIndex) {
		newList.add(newElements[i])
	}
	for (i in endIndex..lastIndex) {
		newList.add(this[i])
	}
	return newList
}

/**
 * Clears this list and adds all elements from [other].
 */
fun <E> MutableList<E>.setTo(other: List<E>) {
	clear()
	addAll(other)
}

/**
 * Returns true if this list is currently sorted.
 */
fun <E : Comparable<E>> List<E>.isSorted(): Boolean {
	for (i in 1..lastIndex) {
		val a = this[i - 1]
		val b = this[i]
		if (a > b) return false
	}
	return true
}

/**
 * Returns true if this list is currently descendingly sorted.
 */
fun <E : Comparable<E>> List<E>.isReverseSorted(): Boolean {
	for (i in 1..lastIndex) {
		val a = this[i - 1]
		val b = this[i]
		if (a < b) return false
	}
	return true
}
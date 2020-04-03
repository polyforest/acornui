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

package com.acornui.collection

/**
 * A SnapshotList provides a [begin] method that returns a snapshot list that guarantees no modifications until [end].   
 */
interface SnapshotList<out E> : List<E> {

	/**
	 * Returns a list which will be guaranteed to not be modified until [end] is called.
	 * If multiple [begin] calls are nested, nested [begin] calls will return the snapshot of the first [begin].
	 */
	fun begin(): List<E>

	/**
	 * Ends the lock on the immutable lists returned by [begin].
	 */
	fun end()
}

interface MutableSnapshotList<E> : SnapshotList<E>, MutableList<E>

/**
 * A mutable list that allows modification during iteration.
 * This idea is borrowed from LibGdx:
 * https://github.com/libgdx/libgdx/blob/master/gdx/src/com/badlogic/gdx/utils/SnapshotArray.java
 *
 * ```
 * val snapshotList = SnapshotList<Any>()
 *
 * val list = snapshotList.begin() // Guaranteed not to change if snapshotList is modified.
 * for (i in 0..list.lastIndex) {
 *   val item = list[i]
 * }
 * snapshotList.end()
 * ```
 */
class SnapshotListImpl<E> : MutableSnapshotList<E>, MutableListBase<E>() {

	/**
	 * For testing purposes; the number of times an array copy has occurred.
	 */
	internal var copyCount = 0

	private val itemsA = ArrayList<E>()
	private val itemsB = ArrayList<E>()

	private var mutableItems = itemsA
	private var immutableItems = itemsA

	private var snapshots = 0

	override val size: Int
		get() = mutableItems.size

	override fun get(index: Int): E {
		return mutableItems[index]
	}

	override fun add(index: Int, element: E) {
		modified()
		mutableItems.add(index, element)
	}

	override fun removeAt(index: Int): E {
		modified()
		return mutableItems.removeAt(index)
	}

	override fun set(index: Int, element: E): E {
		modified()
		return mutableItems.set(index, element)
	}

	private fun modified() {
		if (snapshots == 0) return
		if (mutableItems === immutableItems) {
			mutableItems = if (mutableItems === itemsA) itemsB else itemsA
			immutableItems.copyInto(mutableItems)
			copyCount++
		}
	}

	override fun begin(): List<E> {
		snapshots++
		return immutableItems
	}

	override fun end() {
		if (--snapshots == 0) {
			if (mutableItems !== immutableItems) {
				val tmp = immutableItems
				immutableItems = mutableItems
				tmp.clear()
			}
		}
		require(snapshots >= 0) { "SnapshotList begin() and end() are mismatched. "}
	}


}

/**
 * Performs the given [action] on each element.
 * Does not cause allocation.
 * Iteration is wrapped in a [SnapshotList.begin], [SnapshotList.end] pair, which will guarantee that the iteration
 * is on a snapshot of the list's state on the first [SnapshotList.begin]
 *
 * @param startIndex The index (inclusive) to begin iteration.
 * @param lastIndex The index (inclusive) to end iteration.
 * @param action Each element within the range will be provided, in order.
 */
inline fun <E> SnapshotList<E>.forEach2(startIndex: Int, lastIndex: Int, action: (E) -> Unit) {
	val list = begin()
	for (i in startIndex..lastIndex) action(list[i])
	end()
}

inline fun <E> SnapshotList<E>.forEach2(action: (E) -> Unit) {
	val list = begin()
	for (i in 0..list.lastIndex) action(list[i])
	end()
}

inline fun <E> SnapshotList<E>.forEachReversed2(action: (E) -> Unit) {
	val list = begin()
	for (i in list.lastIndex downTo 0) action(list[i])
	end()
}

inline fun <E> SnapshotList<E>.forEachReversed2(lastIndex: Int, startIndex: Int, action: (E) -> Unit) {
	val list = begin()
	for (i in lastIndex downTo startIndex) action(list[i])
	end()
}

fun <E> snapshotListOf(vararg elements: E): SnapshotListImpl<E> {
	val list = SnapshotListImpl<E>()
	list.addAll(*elements)
	return list
}
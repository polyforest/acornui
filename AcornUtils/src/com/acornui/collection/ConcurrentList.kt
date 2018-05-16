/*
 * Copyright 2018 Poly Forest
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

class ConcurrentListImpl<E> : MutableListBase<E>(), MutableConcurrentList<E> {

	private val iteratorStack = ArrayList<ConcurrentListImplIterator<E>>()
	private val list = ArrayList<E>()
	private val iteratorPool = ClearableObjectPool { ConcurrentListImplIterator(this, iteratorStack) }

	override fun add(index: Int, element: E) {
		list.add(index, element)
		if (iteratorStack.isNotEmpty()) {
			for (i in 0..iteratorStack.lastIndex) {
				iteratorStack[i].notifyAddedAt(index)
			}
		}
	}

	override val size: Int
		get() = list.size

	override fun get(index: Int): E = list[index]

	override fun removeAt(index: Int): E {
		val e = list.removeAt(index)
		if (iteratorStack.isNotEmpty()) {
			for (i in 0..iteratorStack.lastIndex) {
				iteratorStack[i].notifyRemovedAt(index)
			}
		}
		return e
	}

	override fun set(index: Int, element: E): E {
		return list.set(index, element)
	}

	override fun concurrentIterator(): MutableConcurrentListIterator<E> = ConcurrentListImplIterator(this, iteratorStack)

	override fun iterator(): MutableListIterator<E> = MutableListIteratorImpl(this)

	override fun listIterator(): MutableListIterator<E> = MutableListIteratorImpl(this)

	override fun listIterator(index: Int): MutableListIterator<E> {
		val iterator = MutableListIteratorImpl(this)
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
}

private class ConcurrentListImplIterator<E>(
		target: MutableList<E>,
		private val stack: MutableList<ConcurrentListImplIterator<E>>
) : MutableConcurrentListIteratorImpl<E>(target) {

	init {
		stack.add(this)
		if (stack.size > 50) throw IllegalStateException("Concurrent list iterators are being created, but not disposed.")
	}

	override fun dispose() {
		super.dispose()
		stack.remove(this)
	}
}